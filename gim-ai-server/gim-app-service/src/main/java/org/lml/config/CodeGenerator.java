package org.lml.config;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

/**
 * @Description:
 * @Author: lml
 * @Date: 代码生成器
 */
public class CodeGenerator {
    public static void main(String[] args) {
        String url= "jdbc:mysql://47.120.60.61:3306/g-im-chat?useSSL=true&useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8";
        String username = "gimchat";
        String password = "LMLlml062424";
        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> {
                    builder.author("lml") // 设置作者
//                            .enableSwagger() // 开启 swagger 模式
//                            .fileOverride() // 覆盖已生成文件
                            .outputDir("C:\\generator"); // 指定输出目录
                })
                .packageConfig(builder -> {
                    builder.parent("org.lml") // 设置父包名
                            .moduleName("") // 设置父包模块名
                            .pathInfo(Collections.singletonMap(OutputFile.xml, "C:\\generator")); // 设置mapperXml生成路径
                })
                .strategyConfig(builder -> {
                    builder.addInclude("pay_order") // 设置需要生成的表名
                            .addTablePrefix("t_", "l_"); // 设置过滤表前缀
                })
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();
    }
}
