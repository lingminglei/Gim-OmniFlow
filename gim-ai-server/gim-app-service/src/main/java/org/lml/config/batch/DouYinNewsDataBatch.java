package org.lml.config.batch;

import cn.hutool.core.date.DateUtil;
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
import java.util.*;

import static org.lml.utils.HttpUtils.doGet;

@Component
//@EnableScheduling
@Slf4j
public class DouYinNewsDataBatch {
    private static final String API_URL = "https://www.douyin.com/aweme/v1/web/hot/search/list/?device_platform=webapp&aid=6383&channel=channel_pc_web&detail_list=1";

    @Resource
    private INewsInfoService iNewsInfoService;

//    @Scheduled(cron = "0 */3 * * * ?")  // 每5分钟执行一次
    @Transactional
    public String runTask() {
        log.info("同步抖音热榜");

        String path = "";

        Map<String, String> headers = new HashMap<>();
        try {
            log.info("API_URL={},path={}",API_URL,path);
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0");
            headers.put("Referer", "https://www.douyin.com/");
            headers.put("content-type","application/json; charset=UTF-8");
            HttpResponse response = doGet(API_URL, path, headers, null);

            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            JSONObject root = JSON.parseObject(json);
            JSONObject datas = root.getJSONObject("data");

            JSONArray list = datas.getJSONArray("word_list");

            if (list == null || list.isEmpty()) {
                log.warn("抖音热榜返回空数据");
                return "";
            }

            // 2. 解析并入库
            List<NewsInfo> newsList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                JSONObject item = list.getJSONObject(i);

                NewsInfo news = new NewsInfo();
                news.setSourceCode(NewsSourceType.DOUYIN.getCode());
                news.setSourceName(NewsSourceType.DOUYIN.getName());
                news.setSourceIcon(NewsSourceType.DOUYIN.getIcon());

                news.setSourceUniqueId(item.getString("sentence_id"));
                news.setTitle(item.getString("word"));
                news.setUrl("https://www.douyin.com/hot/"+item.getString("sentence_id"));
                Date date = DateUtil.date(item.getLong("event_time"));
                news.setPublishTime(date);
                news.setFetchTime(new Date());
                news.setStatus(1);
                news.setExtraJson(item.toJSONString());
                news.setCreateBy("DouYinNewsDataBatch");
                news.setUpdateBy("DouYinNewsDataBatch");

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

            log.info("【抖音热榜】同步完成，共处理 {} 条，新增 {} 条", newsList.size(), inserted);
        } catch (Exception e) {
            log.error("【抖音热榜】同步异常", e);
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
