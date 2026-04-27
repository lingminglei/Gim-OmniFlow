package org.lml.thirdService.ws.handle;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lml.AiService.factory.OpenAiModelFactory;
import org.lml.AiService.tool.WeatherTool;
import org.lml.thirdService.knowledgeService.service.DocumentIngestService;
import org.lml.thirdService.ws.memory.RedisChatMemoryStore;
import org.lml.thirdService.ws.memory.WsChatMemoryProvider;
import org.lml.thirdService.ws.service.WsChatAssistant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lml.thirdService.ocr.BaiduOcrUtil.recognizeMixedLang;

/**
 * WebSocket 聊天处理器。
 * 负责解析前端消息、组装提示词、加载会话记忆并将模型流式响应回推给前端。
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String WEBSOCKET_USER_ID = "WEBSOCKET_USER_ID";

    /**
     * 默认系统提示词。
     */
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一名专业的中文 AI 助手。
            回答要求：
            1. 优先结合系统提示、会话历史和当前问题作答；
            2. 如果给出了知识库上下文，优先基于知识库内容回答；
            3. 无法确认的内容要明确说明，不要编造事实；
            4. 回答尽量结构化、简洁、准确；
            5. 默认使用中文回复。
            """;

    /**
     * 预置角色提示词映射。
     */
    private static final Map<String, String> ROLE_PROMPT_MAP = Map.of(
            "assistant", DEFAULT_SYSTEM_PROMPT,
            "default", DEFAULT_SYSTEM_PROMPT,
            "general", DEFAULT_SYSTEM_PROMPT
    );

    /**
     * 会话发送线程池，保证同一个连接内消息顺序发送，避免并发写入 WebSocket。
     */
    private final Map<String, ExecutorService> sessionExecutors = new ConcurrentHashMap<>();

    @Value("${ufop.local-storage-path}")
    private String fileBasePath;

    @Resource
    private WeatherTool weatherTool;

    @Resource
    private DocumentIngestService documentIngestService;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private WsChatMemoryProvider wsChatMemoryProvider;

    @Resource
    private OpenAiModelFactory openAiModelFactory;

    /**
     * 获取或创建当前连接的串行发送线程池。
     */
    private ExecutorService getOrCreateExecutor(WebSocketSession session) {
        return sessionExecutors.computeIfAbsent(session.getId(), id ->
                Executors.newSingleThreadExecutor(r -> {
                    Thread thread = new Thread(r);
                    thread.setName("ws-sender-" + id);
                    thread.setDaemon(true);
                    return thread;
                }));
    }

    /**
     * 关闭并清理当前连接的发送线程池。
     */
    private void shutdownExecutor(WebSocketSession session) {
        ExecutorService executorService = sessionExecutors.remove(session.getId());
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    /**
     * 处理前端发送的文本消息。
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.info("收到聊天 WebSocket 消息，sessionId={}，消息体={}", session.getId(), payload);

        ChatRequest chatRequest;
        try {
            // 先将前端消息解析成统一请求对象，后续文本与附件都走同一套编排链路。
            chatRequest = parseChatRequest(payload);

            if(StringUtils.isBlank(chatRequest.getModelName())) {
                sendErrorMessage(session, "模型类型不能为空！");
            }

            if(StringUtils.isBlank(chatRequest.getConversationId())) {
                sendErrorMessage(session, "会话不能为空！");
            }

        } catch (Exception exception) {
            log.error("解析 WebSocket 聊天消息失败，sessionId={}，原因={}", session.getId(), exception.getMessage(), exception);
            sendErrorMessage(session, "消息格式不正确，请检查请求参数");
            return;
        }

        // 会话历史恢复接口要求 Redis key 必须带 userId，因此这里强制要求握手阶段已经绑定用户身份。
        String userId = resolveUserId(session);
        if (StringUtils.isBlank(userId)) {
            log.warn("当前连接缺少用户身份信息，sessionId={}", session.getId());
            sendErrorMessage(session, "当前连接未完成身份校验，请重新建立连接");
            return;
        }

        if (StringUtils.isBlank(chatRequest.getConversationId())) {
            log.warn("聊天请求缺少会话ID，sessionId={}，userId={}", session.getId(), userId);
            sendErrorMessage(session, "会话ID不能为空");
            return;
        }

        if (StringUtils.isBlank(chatRequest.getType())) {
            log.warn("聊天请求缺少消息类型，sessionId={}，userId={}，conversationId={}",
                    session.getId(), userId, chatRequest.getConversationId());
            sendErrorMessage(session, "消息类型不能为空");
            return;
        }

        switch (chatRequest.getType()) {
            // 文本消息直接进入聊天主链路。
            case "text" -> handleTextRequest(session, userId, chatRequest);
            // 附件消息需要先抽取附件文本，再进入聊天主链路。
            case "file" -> handleFileRequest(session, userId, chatRequest);
            default -> {
                log.warn("收到不支持的消息类型，sessionId={}，userId={}，conversationId={}，type={}",
                        session.getId(), userId, chatRequest.getConversationId(), chatRequest.getType());
                sendErrorMessage(session, "暂不支持当前消息类型");
            }
        }
    }

    /**
     * 处理普通文本聊天请求。
     */
    private void handleTextRequest(WebSocketSession session, String userId, ChatRequest chatRequest) {
        if (StringUtils.isBlank(chatRequest.getText())) {
            log.warn("文本消息缺少文本内容，sessionId={}，userId={}，conversationId={}",
                    session.getId(), userId, chatRequest.getConversationId());
            sendErrorMessage(session, "文本内容不能为空");
            return;
        }

        // 文本消息不需要额外预处理，直接把当前输入作为本轮问题送入模型。
        streamChatResponse(session, userId, chatRequest, chatRequest.getText().trim());
    }

    /**
     * 处理附件聊天请求。
     * 该方法负责读取附件内容，并将附件文本与用户输入合并成当前轮问题。
     */
    private void handleFileRequest(WebSocketSession session, String userId, ChatRequest chatRequest) {
        long startTime = System.currentTimeMillis();
        log.info("开始处理附件聊天请求，sessionId={}，userId={}，conversationId={}，附件数量={}",
                session.getId(), userId, chatRequest.getConversationId(), chatRequest.getFilePaths().size());

        try {
            if (chatRequest.getFilePaths().isEmpty() && StringUtils.isBlank(chatRequest.getText())) {
                log.warn("附件消息既没有文件也没有文本，sessionId={}，userId={}，conversationId={}",
                        session.getId(), userId, chatRequest.getConversationId());
                sendErrorMessage(session, "附件消息至少需要文件或文本内容");
                return;
            }

            StringBuilder extractedTextBuilder = new StringBuilder();

            for (String relativeFilePath : chatRequest.getFilePaths()) {
                String fullFilePath = fileBasePath + File.separator + relativeFilePath;
                File file = new File(fullFilePath);

                if (!file.exists()) {
                    log.warn("附件文件不存在，sessionId={}，userId={}，conversationId={}，文件路径={}",
                            session.getId(), userId, chatRequest.getConversationId(), fullFilePath);
                    continue;
                }

                String mimeType = Files.probeContentType(file.toPath());
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }

                log.info("开始读取附件内容，sessionId={}，userId={}，conversationId={}，文件路径={}，类型={}",
                        session.getId(), userId, chatRequest.getConversationId(), fullFilePath, mimeType);

                if (mimeType.startsWith("image/")) {
                    // 图片类型通过 OCR 识别文本，识别内容会作为当前轮输入的一部分进入模型。
                    String ocrText = recognizeMixedLang(fullFilePath);
                    if (StringUtils.isNotBlank(ocrText)) {
                        extractedTextBuilder.append(ocrText).append(System.lineSeparator());
                    }
                } else if (mimeType.startsWith("text/") || isTextFile(fullFilePath)) {
                    String fileContent = Files.readString(file.toPath());
                    if (StringUtils.isNotBlank(fileContent)) {
                        extractedTextBuilder.append(fileContent).append(System.lineSeparator());
                    }
                } else {
                    log.warn("跳过暂不支持解析的附件类型，sessionId={}，userId={}，conversationId={}，类型={}",
                            session.getId(), userId, chatRequest.getConversationId(), mimeType);
                }
            }

            // 将“用户补充文本 + 附件提取内容”折叠成一条当前轮问题，避免附件内容丢失。
            String currentRoundQuestion = buildFileQuestion(chatRequest.getText(), extractedTextBuilder.toString());
            if (StringUtils.isBlank(currentRoundQuestion)) {
                log.warn("附件内容解析后为空，sessionId={}，userId={}，conversationId={}",
                        session.getId(), userId, chatRequest.getConversationId());
                sendErrorMessage(session, "未能从附件中提取到可用内容");
                return;
            }

            streamChatResponse(session, userId, chatRequest, currentRoundQuestion);
        } catch (Exception exception) {
            log.error("处理附件聊天请求失败，sessionId={}，userId={}，conversationId={}，原因={}",
                    session.getId(), userId, chatRequest.getConversationId(), exception.getMessage(), exception);
            sendErrorMessage(session, "处理附件消息失败，请稍后重试");
        } finally {
            log.info("附件聊天请求处理结束，sessionId={}，userId={}，conversationId={}，耗时={}ms",
                    session.getId(), userId, chatRequest.getConversationId(), System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 组装当前轮问题并发起流式对话。
     */
    private void streamChatResponse(WebSocketSession session, String userId, ChatRequest chatRequest, String currentQuestion) {
        try {
            // 系统提示词固定放在最前面，用来定义当前会话的角色和回答约束。
            String systemPrompt = resolveSystemPrompt(chatRequest.getAiRole());
            // 长期记忆仍沿用现有 Qdrant 检索逻辑，不改变现有知识库入口。
            String ragContext = loadRagContext(chatRequest.getKnowledgeCode(), currentQuestion);
            // 当前轮消息里显式放入“知识库上下文 + 当前问题”，保证进入模型时顺序稳定。
            String promptMessage = buildPromptMessage(ragContext, currentQuestion);
            // Redis key 固定带 userId，保证同一个 conversationId 在不同用户之间完全隔离。
            String memoryId = buildMemoryId(userId, chatRequest.getConversationId());

            log.info("开始执行会话聊天，sessionId={}，userId={}，conversationId={}，memoryId={}，知识库编码={}",
                    session.getId(), userId, chatRequest.getConversationId(), memoryId, chatRequest.getKnowledgeCode());

            // 使用 conversationId 前先查询 Redis。
            // 有历史时继续旧会话；没有历史时由 LangChain4j 在首轮对话后自动创建新记录。
            if (redisChatMemoryStore.hasConversationHistory(memoryId)) {
                log.info("检测到历史会话记录，继续旧会话，memoryId={}", memoryId);
            } else {
                log.info("未检测到历史会话记录，将创建新会话，memoryId={}", memoryId);
                // 首次使用该 conversationId 时，先在 Redis 中初始化空会话，后续再由 LangChain4j 自动追加轮次内容。
                redisChatMemoryStore.initializeConversationIfAbsent(memoryId);
            }

            // 模型对象统一从现有模型工厂中获取，保持与你当前系统一致的模型来源。
            OpenAiStreamingChatModel model = openAiModelFactory.getStreamingModel(chatRequest.getModelName());;

            // 这里把 Redis 持久化记忆提供器交给 LangChain4j，后续消息的写入和读取都由框架自动完成。
            WsChatAssistant assistant = AiServices.builder(WsChatAssistant.class)
                    .streamingChatModel(model)
                    .chatMemoryProvider(wsChatMemoryProvider)
//                    .tools(List.of(weatherTool))
                    .build();

            // 调用时显式传入 memoryId，LangChain4j 会据此加载和更新同一会话的 ChatMemory。
            TokenStream tokenStream = assistant.chat(memoryId, systemPrompt, promptMessage);
            bindTokenStream(session, tokenStream);
        } catch (Exception exception) {
            log.error("执行会话聊天失败，sessionId={}，userId={}，conversationId={}，原因={}",
                    session.getId(), userId, chatRequest.getConversationId(), exception.getMessage(), exception);
            sendErrorMessage(session, "聊天服务处理失败，请稍后重试");
        }
    }

    /**
     * 将模型流式响应绑定到 WebSocket 输出。
     */
    private void bindTokenStream(WebSocketSession session, TokenStream tokenStream) {
        ExecutorService executor = getOrCreateExecutor(session);

        tokenStream
                // 增量 token 统一放进串行线程池，避免同一连接并发写 WebSocket。
                .onPartialResponse(partialResponse -> executor.submit(() -> sendIfSessionOpen(session, partialResponse)))
                // 流式完成后发送固定结束标记，前端据此关闭加载态。
                .onCompleteResponse(response -> executor.submit(() -> sendIfSessionOpen(session, "[DONE]")))
                // 模型异常统一透传错误前缀，便于前端区分普通消息和异常消息。
                .onError(error -> executor.submit(() -> sendIfSessionOpen(session, "[ERROR]" + error.getMessage())))
                .start();
    }

    /**
     * 解析前端聊天消息。
     */
    private ChatRequest parseChatRequest(String payload) throws Exception {
        JsonNode jsonNode = OBJECT_MAPPER.readTree(payload);

        ChatRequest chatRequest = new ChatRequest();
        // 这里统一做字段提取，避免后续分支各自解析 JSON，减少字段漏读和规则分叉。
        chatRequest.setModelName(readText(jsonNode, "modelName"));//需要进行非空校验
        chatRequest.setConversationId(readText(jsonNode, "conversationId"));//需要进行非空校验
        chatRequest.setType(readText(jsonNode, "type"));
        chatRequest.setText(readText(jsonNode, "text"));
        chatRequest.setKnowledgeCode(readText(jsonNode, "knowledgeCode"));
        chatRequest.setAiRole(readText(jsonNode, "aiRole"));
        chatRequest.setFileName(readText(jsonNode, "name"));

        if (jsonNode.hasNonNull("data") && jsonNode.get("data").isArray()) {
            for (JsonNode node : jsonNode.get("data")) {
                chatRequest.getFilePaths().add(node.asText());
            }
        }

        return chatRequest;
    }

    /**
     * 读取 JSON 中的字符串字段。
     */
    private String readText(JsonNode jsonNode, String fieldName) {
        if (jsonNode.hasNonNull(fieldName)) {
            return jsonNode.get(fieldName).asText();
        }
        return null;
    }

    /**
     * 解析系统提示词。
     * 如果 aiRole 命中预置角色，则返回预置提示词，否则把 aiRole 当作自定义系统提示词。
     */
    private String resolveSystemPrompt(String aiRole) {
        if (StringUtils.isBlank(aiRole)) {
            return DEFAULT_SYSTEM_PROMPT;
        }

        String normalizedRole = aiRole.trim();
        if (ROLE_PROMPT_MAP.containsKey(normalizedRole)) {
            return ROLE_PROMPT_MAP.get(normalizedRole);
        }

        return normalizedRole;
    }

    /**
     * 从知识库中查询与当前问题相关的上下文。
     */
    private String loadRagContext(String knowledgeCode, String currentQuestion) {
        if (StringUtils.isBlank(knowledgeCode)) {
            return "";
        }

        log.info("开始查询知识库上下文，知识库编码={}，问题长度={}", knowledgeCode, currentQuestion.length());
        String ragContext = documentIngestService.queryFromQdrantAndIntegrate(knowledgeCode, currentQuestion);

        // 没有命中知识库时直接返回空串，让本轮问题只包含最新提问，不污染提示词结构。
        if (StringUtils.isBlank(ragContext) || "没有找到相关信息。".equals(ragContext.trim())) {
            log.info("当前问题未命中知识库有效上下文，知识库编码={}", knowledgeCode);
            return "";
        }

        log.info("知识库上下文查询完成，知识库编码={}，上下文长度={}", knowledgeCode, ragContext.length());
        return ragContext;
    }

    /**
     * 按固定顺序组装当前轮用户消息。
     * 当前轮消息内部始终保持“知识库上下文在前、当前问题在后”的顺序。
     */
    private String buildPromptMessage(String ragContext, String currentQuestion) {
        if (StringUtils.isBlank(ragContext)) {
            return "【当前问题】\n" + currentQuestion;
        }

        // 把 RAG 结果和最新问题压到同一条用户消息里，顺序上位于历史记忆之后、最新问题之前。
        return """
                【知识库上下文】
                %s

                【当前问题】
                %s
                """.formatted(ragContext, currentQuestion);
    }

    /**
     * 拼装附件聊天场景下的当前轮问题。
     */
    private String buildFileQuestion(String textMessage, String extractedText) {
        String normalizedText = StringUtils.defaultString(textMessage).trim();
        String normalizedExtractedText = StringUtils.defaultString(extractedText).trim();

        if (StringUtils.isBlank(normalizedExtractedText)) {
            return normalizedText;
        }

        if (StringUtils.isBlank(normalizedText)) {
            return "【附件内容】\n" + normalizedExtractedText;
        }

        return """
                【用户补充说明】
                %s

                【附件内容】
                %s
                """.formatted(normalizedText, normalizedExtractedText);
    }

    /**
     * 生成会话记忆主键。
     * Redis key 规范要求固定使用“用户ID:会话ID”。
     */
    private String buildMemoryId(String userId, String conversationId) {
        return userId + ":" + conversationId;
    }

    /**
     * 从 WebSocket Session 中解析当前登录用户。
     */
    private String resolveUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get(WEBSOCKET_USER_ID);
        if (ObjectUtil.isNull(userId)) {
            return null;
        }
        // 统一转成字符串，避免后续 Redis key 和 memoryId 因类型差异导致不一致。
        return String.valueOf(userId);
    }

    /**
     * 判断是否为常见文本文件。
     */
    private boolean isTextFile(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".txt")
                || lowerFileName.endsWith(".md")
                || lowerFileName.endsWith(".java")
                || lowerFileName.endsWith(".js")
                || lowerFileName.endsWith(".json")
                || lowerFileName.endsWith(".xml")
                || lowerFileName.endsWith(".pdf");
    }

    /**
     * 发送普通文本消息。
     */
    private void sendTextMessage(WebSocketSession session, String message) {
        ExecutorService executor = getOrCreateExecutor(session);
        executor.submit(() -> sendIfSessionOpen(session, message));
    }

    /**
     * 发送错误消息。
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        sendTextMessage(session, "[ERROR]" + errorMessage);
    }

    /**
     * 在连接可用时发送消息，避免关闭连接后继续写入。
     */
    private void sendIfSessionOpen(WebSocketSession session, String message) {
        try {
            if (!session.isOpen()) {
                log.warn("当前 WebSocket 已关闭，跳过消息发送，sessionId={}", session.getId());
                return;
            }

            session.sendMessage(new TextMessage(message));
        } catch (Exception exception) {
            log.error("发送 WebSocket 消息失败，sessionId={}，原因={}", session.getId(), exception.getMessage(), exception);
        }
    }

    /**
     * 连接关闭时清理发送线程池。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("聊天 WebSocket 连接关闭，sessionId={}，状态={}", session.getId(), status);
        shutdownExecutor(session);
        super.afterConnectionClosed(session, status);
    }

    /**
     * 连接建立时记录日志。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("聊天 WebSocket 连接建立成功，sessionId={}", session.getId());
        super.afterConnectionEstablished(session);
    }

    /**
     * WebSocket 聊天请求对象。
     */
    private static class ChatRequest {
        private String type;
        private String text;
        private String modelName;
        private String knowledgeCode;
        private String aiRole;
        private String conversationId;
        private String fileName;
        private final List<String> filePaths = new ArrayList<>();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public String getKnowledgeCode() {
            return knowledgeCode;
        }

        public void setKnowledgeCode(String knowledgeCode) {
            this.knowledgeCode = knowledgeCode;
        }

        public String getAiRole() {
            return aiRole;
        }

        public void setAiRole(String aiRole) {
            this.aiRole = aiRole;
        }

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public List<String> getFilePaths() {
            return filePaths;
        }
    }
}
