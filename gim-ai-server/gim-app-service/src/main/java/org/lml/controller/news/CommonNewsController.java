package org.lml.controller.news;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.lml.common.result.CommonResult;
import org.lml.entity.dto.news.NewsInfo;
import org.lml.service.news.INewsInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/news")
public class CommonNewsController {

    @Resource
    private INewsInfoService iNewsInfoService;

    @GetMapping("/getListTop10")
    public CommonResult<Map<String,List<Map<String,String>>>> getListTop10(){

        List<String> typeList = List.of("WANGYIXINWEN", "CSDN", "PENGPAI","XINLANGNEWS","DOUYIN","XINLANGB");

        Map<String,List<Map<String,String>>> map = new HashMap<>();

        for(String typeString : typeList){
            QueryWrapper<NewsInfo> qw = new QueryWrapper<>();
            qw.orderByDesc("publish_time");
            qw.eq("source_code",typeString);
            qw.last("LIMIT 10");
            List<NewsInfo> list = iNewsInfoService.list(qw);
            List<Map<String,String>> dataList = new ArrayList<>();

            for(NewsInfo itemData : list){
                Map<String,String> maps = new HashMap<>();
                maps.put("url",itemData.getUrl());
                maps.put("title",itemData.getTitle());
                dataList.add(maps);
            }
            map.put(typeString,dataList);
        }

        return CommonResult.successResponse(map);
    }
}
