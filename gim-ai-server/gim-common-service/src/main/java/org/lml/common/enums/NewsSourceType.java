package org.lml.common.enums;

public enum NewsSourceType {
    CSDN("CSDN", "CSDN", "/upload/csdn.png"),
    DOUYIN("DOUYIN", "抖音热榜", "/upload/dy.png"),
    PENGPAI("PENGPAI", "澎湃新闻", "/upload/pp.png"),
    WANGYIXINWEN("WANGYIXINWEN", "网易新闻", "/upload/wy.png"),
    XINLANGB("XINLANGB", "新浪微博-财经榜", "/upload/xlwb.png"),
    XINLANGNEWS("XINLANGNEWS", "新浪微博", "/upload/xlwb.png")
    ;

    private final String code;
    private final String name;
    private final String icon;

    NewsSourceType(String code, String name, String icon) {
        this.code = code;
        this.name = name;
        this.icon = icon;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }
}
