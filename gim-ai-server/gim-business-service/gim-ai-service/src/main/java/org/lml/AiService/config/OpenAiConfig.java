package org.lml.AiService.config;

/**
 * 现在弃用：该用 工厂模式注册AI服务Bean
 */

/**
 * 实现单例模式：
 *
 * 1、双重检查锁定（Double-Checked Locking） 实现的线程安全单例模式
 *
 *
 * 2、使用 @Bean 和 @Configuration 实现单例模式：
 */
//@Configuration
//@Slf4j
//public class OpenAiConfig {
//
//    @Value("${uiuiapi.apiKey}")
//    private String apiKey;
//
//    @Value("${uiuiapi.baseUrl}")
//    private String baseUrl;
//
//    @Bean
//    public OpenAiChatModel openAiChatModel() {
//
//        log.info("注册OpenAiChatModel实例! apiKey:{},baseUrl:{}",apiKey,baseUrl);
//
//        OpenAiChatModel model = OpenAiChatModel.builder()
//                /**
//                 * API key
//                 */
//                .apiKey(apiKey)
//                /**
//                 * GPT 模型
//                 */
////                GPT_4_TURBO [最佳模型]
////                .modelName(GPT_4_TURBO)
//                .modelName("")
//                /**
//                 * 介于 0 和 2 之间。
//                 *
//                 * 较高的值（如 0.8）将使输出更加随机，而较低的值（如 0.2）将使其更加集中和确定。
//                 */
//                .temperature(0.3)
//                /**
//                 * 超时时间为 60 秒
//                 */
//                .timeout(ofSeconds(60))
//                /**
//                 *聊天补全中可以生成的令牌的最大数量。
//                 */
//                .maxTokens(1000)
//                /**
//                 * 介于 -2.0 和 2.0 之间的数字。
//                 * 正值会根据新标记到目前为止在文本中的现有频率来惩罚新标记，从而降低模型逐字重复同一行的可能性。
//                 */
//                .frequencyPenalty(1.0)
//                /**
//                 * 记录请求
//                 */
//                .logRequests(true)
//                .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
//                .strictJsonSchema(true)
//                /**
//                 * 记录响应
//                 */
//                .logResponses(true)
//                .baseUrl(baseUrl)
//                .build();
//
//        return model;
//    }
//
//    @Bean
//    public OpenAiChatModel openAiChatModelJson() {
//
//        log.info("注册OpenAiChatModel实例! apiKey:{},baseUrl:{}",apiKey,baseUrl);
//
//        OpenAiChatModel model = OpenAiChatModel.builder()
//                /**
//                 * API key
//                 */
//                .apiKey(apiKey)
//                /**
//                 * GPT 模型
//                 */
////                .modelName(GPT_4_TURBO)
//                .modelName("")
//                /**
//                 * 介于 0 和 2 之间。
//                 *
//                 * 较高的值（如 0.8）将使输出更加随机，而较低的值（如 0.2）将使其更加集中和确定。
//                 */
//                .temperature(0.3)
//                /**
//                 * 超时时间为 60 秒
//                 */
//                .timeout(ofSeconds(60))
//                /**
//                 *聊天补全中可以生成的令牌的最大数量。
//                 */
//                .maxTokens(1000)
//                /**
//                 * 介于 -2.0 和 2.0 之间的数字。
//                 * 正值会根据新标记到目前为止在文本中的现有频率来惩罚新标记，从而降低模型逐字重复同一行的可能性。
//                 */
//                .frequencyPenalty(1.0)
//                /**
//                 * 记录请求
//                 */
//                .logRequests(true)
//                .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
//                .strictJsonSchema(true)
//                /**
//                 * 记录响应
//                 */
//                .logResponses(true)
//                .baseUrl(baseUrl)
//                .build();
//
//        return model;
//    }
//}
