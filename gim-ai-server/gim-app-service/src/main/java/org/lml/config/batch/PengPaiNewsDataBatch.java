package org.lml.config.batch;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.lml.common.enums.NewsSourceType;
import org.lml.entity.dto.news.NewsInfo;
import org.lml.service.news.INewsInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.lml.utils.HttpUtils.doGet;

@Slf4j
@Component
public class PengPaiNewsDataBatch {

    private static final String API_URL = "https://cache.thepaper.cn";

    @Autowired
    private INewsInfoService iNewsInfoService;


    /**
     * 每 5 分钟执行一次
     */
//    @Scheduled(cron = "0 */4 * * * ?")
    public void runTask() {

        String path = "/contentapi/wwwIndex/rightSidebar";

        Map<String, String> headers = new HashMap<>();

        log.info("开始同步 澎湃 hotNews 数据...");

        try {
            HttpResponse response = doGet(API_URL, path, headers, null);

            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            JSONObject root = JSON.parseObject(json);
            JSONObject data = root.getJSONObject("data");
            JSONArray list = data.getJSONArray("hotNews");

            if (list == null || list.isEmpty()) {
                log.warn("澎湃热榜返回空数据");
                return;
            }

            // 2. 解析并入库
            List<NewsInfo> newsList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                JSONObject item = list.getJSONObject(i);

                NewsInfo news = new NewsInfo();
                news.setSourceCode(NewsSourceType.PENGPAI.getCode());
                news.setSourceName(NewsSourceType.PENGPAI.getName());
                news.setSourceIcon(NewsSourceType.PENGPAI.getIcon());
                news.setSourceUniqueId(item.getString("contId"));
                news.setTitle(item.getString("name"));
                news.setPublishTime(new Date());
                news.setTag("热榜");
                news.setStatus(1);
                news.setUrl("https://www.thepaper.cn/newsDetail_forward_"+item.getString("contId"));
                news.setCreateBy("PengPaiNewsDataBatch");
                news.setUpdateBy("PengPaiNewsDataBatch");
                JSONObject nodeInfo = item.getJSONObject("nodeInfo");

                news.setCoverImage(item.getString("sharePic"));
                news.setFetchTime(new Date());
                news.setExtraJson(item.toJSONString());
                newsList.add(news);
            }

            int inserted = 0;
            for (NewsInfo n : newsList) {
                QueryWrapper<NewsInfo> qw = new QueryWrapper<>();
                qw.eq("source_code", n.getSourceCode());
                qw.eq("source_unique_id", n.getSourceUniqueId());
                List<NewsInfo> exist = iNewsInfoService.list(qw);
                if (ObjectUtil.isNull(exist) || exist.isEmpty()) {
                    iNewsInfoService.save(n);
                    inserted++;
                }
            }

            log.info("【澎湃新闻热榜】同步完成，共处理 {} 条，新增 {} 条", newsList.size(), inserted);

        } catch (Exception e) {
            log.error("同步 hotNews 异常", e);
        }
    }
}
