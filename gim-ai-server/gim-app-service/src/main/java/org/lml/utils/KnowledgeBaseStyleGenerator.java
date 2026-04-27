package org.lml.utils;

import java.util.*;

/**
 * 创建知识库：随机分配颜色和图标
 */
public class KnowledgeBaseStyleGenerator {

    // 预定义bgColor列表，更多渐变色
    private static final List<String> BG_COLORS = Arrays.asList(
            "linear-gradient(135deg, #9F48F5, #6B2EDA)",
            "linear-gradient(135deg, #FF7E5F, #FEB47B)",
            "linear-gradient(135deg, #43CEA2, #185A9D)",
            "linear-gradient(135deg, #F7971E, #FFD200)",
            "linear-gradient(135deg, #7F00FF, #E100FF)",
            "linear-gradient(135deg, #00F260, #0575E6)",
            "linear-gradient(135deg, #FF512F, #DD2476)",
            "linear-gradient(135deg, #1FA2FF, #12D8FA, #A6FFCB)",
            "linear-gradient(135deg, #667EEA, #764BA2)",
            "linear-gradient(135deg, #FF6A00, #EE0979)",
            "linear-gradient(135deg, #11998E, #38EF7D)",
            "linear-gradient(135deg, #6A11CB, #2575FC)"
    );

    // 预定义icon列表，更多 Element UI 图标类名
    private static final List<String> ICONS = Arrays.asList(
            "el-icon-lightning",
            "el-icon-star-on",
            "el-icon-s-claim",
            "el-icon-s-cooperation",
            "el-icon-s-order",
            "el-icon-user",
            "el-icon-folder",
            "el-icon-document",
            "el-icon-setting",
            "el-icon-message",
            "el-icon-camera",
            "el-icon-bell",
            "el-icon-goods",
            "el-icon-date",
            "el-icon-picture",
            "el-icon-film",
            "el-icon-chat-dot-round",
            "el-icon-success",
            "el-icon-error",
            "el-icon-warning"
    );

    private static final Random RANDOM = new Random();

    public static Map<String, String> generateRandomStyle() {
        String bgColor = BG_COLORS.get(RANDOM.nextInt(BG_COLORS.size()));
        String icon = ICONS.get(RANDOM.nextInt(ICONS.size()));

        Map<String, String> styleMap = new HashMap<>();
        styleMap.put("bgColor", bgColor);
        styleMap.put("icon", icon);

        return styleMap;
    }

    public static void main(String[] args) {
        Map<String, String> randomStyle = generateRandomStyle();
        System.out.println("bgColor: " + randomStyle.get("bgColor"));
        System.out.println("icon: " + randomStyle.get("icon"));
    }
}
