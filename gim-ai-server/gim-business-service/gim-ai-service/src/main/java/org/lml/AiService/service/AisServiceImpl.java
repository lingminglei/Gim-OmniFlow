package org.lml.AiService.service;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.V;
import org.springframework.stereotype.Service;

@Service
public class AisServiceImpl implements AisServices {

    @Override
    public TokenStream chatV2(String userMessage) {
        return null;
    }

    @Override
    public String chat(String message) {
        return "";
    }

    @Override
    public String chat2(String userMessage) {
        return "";
    }

    @Override
    public String chat1(String userMessage) {
        return "";
    }

    @Override
    public String getWarmReminder(String weatherType) {
        return "";
    }

    @Override
    public String getAWarmWord(String weatherType) {
        return "";
    }

    @Override
    public String getHealthAdvice(String weatherType) {
        return "";
    }

    @Override
    public TokenStream chat11(String userMessage) {
        return null;
    }

    @Override
    public String chat22(String userMessage) {
        return null;
    }

    @Override
    public String chatGetPromt(@V("title") String title, @V("userPrompt") String userPrompt,
                                   @V("videoStyle") String videoStyle, @V("videoLength") Integer videoLength){return null;}

    @Override
    public String chatGetPromtSlice(String title, String userPrompt, String videoStyle, Integer videoLength) {
        return "";
    }

    @Override
    public String chatGetPromptImageToVideo(String prompt) {
        return "";
    }

    @Override
    public String chatGetPromtSliceV2(String userPrompt, String videoStyle, Integer size) {
        return "";
    }

    @Override
    public String chatGetPromtSliceV3(String userPrompt, String videoStyle, Integer size) {
        return "";
    }



}
