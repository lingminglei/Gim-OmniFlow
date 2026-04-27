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
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.mysql.cj.util.TimeUtil.DATE_FORMATTER;
import static org.lml.utils.HttpUtils.doGet;

@Component
//@EnableScheduling
@Slf4j
public class WangYiNewsDataBath {

    private static final String API_URL = "https://m.163.com/";

    @Resource
    private INewsInfoService iNewsInfoService;

//    @Scheduled(cron = "0 */5 * * * ?")  // 每5分钟执行一次
    @Transactional
    public String runTask() {
        log.info("同步");

        String path = "/fe/api/hot/news/flow";

        Map<String,String> headers = new HashMap<String,String>();

        try {
            // 1. 请求接口
            HttpResponse response = doGet(API_URL, path, headers, null);

            String json = EntityUtils.toString(response.getEntity(), "UTF-8");

            JSONObject root = JSON.parseObject(json);
            JSONArray list = root.getJSONObject("data").getJSONArray("list");
            if (list == null || list.isEmpty()) {
                log.warn("网易新闻返回空数据");
                return "";
            }

            // 2. 解析并入库
            List<NewsInfo> newsList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                JSONObject item = list.getJSONObject(i);

                NewsInfo news = new NewsInfo();
                news.setSourceCode(NewsSourceType.WANGYIXINWEN.getCode());
                news.setSourceName(NewsSourceType.WANGYIXINWEN.getName());
                news.setSourceIcon(NewsSourceType.WANGYIXINWEN.getIcon());
                news.setSourceUniqueId(item.getString("docid"));
                news.setUrl(item.getString("url"));
                news.setTitle(item.getString("title"));
                news.setSummary(null); // 网易返回的摘要字段为空，可以后续解析补充
                news.setContent(null);
                news.setCoverImage(getCover(item));
                news.setTag("热榜");
                news.setAuthor(item.getString("source"));

                // 解析时间
                String timeStr = item.getString("publishTime");
                if (timeStr != null) {
                    Date publishTime = Date.from(LocalDateTime.parse(timeStr,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            .atZone(ZoneId.systemDefault()).toInstant());
                    news.setPublishTime(publishTime);
                }
                news.setHotValue(0L);
                news.setCommentCount(0);
                news.setLikeCount(0);
                news.setFetchTime(new Date());
                news.setStatus(1);
                news.setExtraJson(item.toJSONString());
                news.setCreateBy("wangyi-task");
                news.setUpdateBy("wangyi-task");

                newsList.add(news);
            }

            // 3. 去重插入（根据 source_code + source_unique_id）
            int inserted = 0;
            for (NewsInfo n : newsList) {
                QueryWrapper<NewsInfo> qw = new QueryWrapper<>();
                qw.eq("source_code",n.getSourceCode());
                qw.eq("source_unique_id",n.getSourceUniqueId());
                List<NewsInfo> exist = iNewsInfoService.list(qw);
                if (!ObjectUtil.isNull(exist) && ObjectUtil.isEmpty(exist)) {
                    iNewsInfoService.save(n);
                    inserted++;
                }
            }

            log.info("【网易新闻】同步完成，共处理 {} 条，新增 {} 条", newsList.size(), inserted);
        } catch (Exception e) {
            log.error("【网易新闻】同步异常", e);
        }

        return "";
    }


    /**
     * 获取封面图优先顺序：recImgsrc > imgsrc > picInfo[0].url
     */
    private String getCover(JSONObject item) {
        String cover = item.getString("recImgsrc");
        if (cover == null || cover.isEmpty()) {
            cover = item.getString("imgsrc");
        }
        if ((cover == null || cover.isEmpty()) && item.containsKey("picInfo")) {
            JSONArray picInfo = item.getJSONArray("picInfo");
            if (picInfo != null && !picInfo.isEmpty()) {
                cover = picInfo.getJSONObject(0).getString("url");
            }
        }
        return cover;
    }



}
