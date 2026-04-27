package org.lml.config.batch;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiwenshare.ufop.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.lml.common.enums.NewsSourceType;
import org.lml.entity.dto.news.NewsInfo;
import org.lml.service.news.INewsInfoService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.lml.utils.HttpUtils.doGet;

@Component
@Slf4j
public class CSDNNewsDataBatch {

    private static final String API_URL = "https://blog.csdn.net";

    @Resource
    private INewsInfoService iNewsInfoService;

//    @Scheduled(cron = "0 */2 * * * ?")  // 每5分钟执行一次
    @Transactional
    public String runTask() {
        log.info("同步CSDN热榜");

        String path = "/phoenix/web/blog/hot-rank";  // CSDN热榜接口路径

        Map<String, String> headers = new HashMap<>();
        headers.put("page","0");
        headers.put("pageSize","10");
        try {
            // 1. 请求CSDN热榜接口
            HttpResponse response = doGet(API_URL, path, headers, null);

            String json = EntityUtils.toString(response.getEntity(), "UTF-8");

            JSONObject root = JSON.parseObject(json);
            JSONArray list = root.getJSONArray("data");
            if (list == null || list.isEmpty()) {
                log.warn("CSDN热榜返回空数据");
                return "";
            }

            // 2. 解析并入库
            List<NewsInfo> newsList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                JSONObject item = list.getJSONObject(i);

                NewsInfo news = new NewsInfo();
                news.setSourceCode(NewsSourceType.CSDN.getCode());
                news.setSourceName(NewsSourceType.CSDN.getName());
                news.setSourceIcon(NewsSourceType.CSDN.getIcon());
                news.setSourceUniqueId(item.getString("productId"));
                news.setUrl(item.getString("articleDetailUrl"));
                news.setTitle(item.getString("articleTitle"));
                news.setSummary(null); // CSDN没有提供摘要字段
                news.setContent(null); // CSDN没有提供完整内容
                news.setCoverImage(getCover(item));
                news.setTag("热榜");
                news.setAuthor(item.getString("nickName"));

                // CSDN没有提供时间字段，使用当前时间作为发布时间
                news.setPublishTime(new Date());
                news.setHotValue(Long.parseLong(item.getString("hotRankScore")));
                news.setCommentCount(Integer.parseInt(item.getString("commentCount")));
                news.setLikeCount(Integer.parseInt(item.getString("favorCount")));
                news.setFetchTime(new Date());
                news.setStatus(1);
                news.setExtraJson(item.toJSONString());
                news.setCreateBy("csdn-task");
                news.setUpdateBy("csdn-task");

                newsList.add(news);
            }

            // 3. 去重插入（根据 source_code + source_unique_id）
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

            log.info("【CSDN热榜】同步完成，共处理 {} 条，新增 {} 条", newsList.size(), inserted);
        } catch (Exception e) {
            log.error("【CSDN热榜】同步异常", e);
        }

        return "";
    }

    /**
     * 获取封面图优先顺序：picList[0] > avatarUrl
     */
    private String getCover(JSONObject item) {
        String cover = null;
        if (item.containsKey("picList") && item.getJSONArray("picList") != null) {
            JSONArray picList = item.getJSONArray("picList");
            if (!picList.isEmpty()) {
                cover = picList.getString(0);
            }
        }
        if (cover == null && item.containsKey("avatarUrl")) {
            cover = item.getString("avatarUrl");
        }
        return cover;
    }
}
