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
@EnableScheduling
@Slf4j
public class XinLangNewsDataBatch {

    @Resource
    private RedisUtil redisUtil;

    private static final String prefix = "Csdn_quality_sync_data";

    private static final int MAX_RETRY = 3;
    private static final int RETRY_DELAY_MS = 3000; // 3秒间隔

    private static final String API_URL = "https://top.finance.sina.com.cn/ws/GetTopDataList.php?top_type=day&top_cat=finance_0_suda&top_show_num=10";

    @Resource
    private INewsInfoService iNewsInfoService;

//    @Scheduled(cron = "0 */5 * * * ?")  // 每5分钟执行一次
    @Transactional
    public String runTask() {
        log.info("同步新浪微博热榜");

        String path = "";

        Map<String, String> headers = new HashMap<>();
        Date dates = new Date();
        String today = DateUtil.format(dates,"yyyyMMdd");
        String api = API_URL + "&top_time="+today;
        try {

            HttpResponse response = doGet(api, path, headers, null);

            String json = EntityUtils.toString(response.getEntity(), "UTF-8");

            // 1. 去掉开头的 var data =
            String jsonStr = json.replaceFirst("^var\\s+\\w+\\s*=\\s*", "");

            // 2. 去掉结尾可能的分号
            jsonStr = jsonStr.replaceAll(";$", "");
            JSONObject root = JSON.parseObject(jsonStr);
            JSONArray list = root.getJSONArray("data");
            if (list == null || list.isEmpty()) {
                log.warn("新浪微博热榜返回空数据");
                return "";
            }

            // 2. 解析并入库
            List<NewsInfo> newsList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                JSONObject item = list.getJSONObject(i);

                NewsInfo news = new NewsInfo();
                news.setSourceCode(NewsSourceType.XINLANGNEWS.getCode());
                news.setSourceName(NewsSourceType.XINLANGNEWS.getName());
                news.setSourceIcon(NewsSourceType.XINLANGNEWS.getIcon());
                news.setSourceUniqueId(item.getString("id"));
                news.setUrl(item.getString("url"));
                news.setTitle(item.getString("title"));
                news.setSummary(null);
                news.setContent(null);
                news.setCoverImage(getCover(item));
                news.setTag("热榜");

                // CSDN没有提供时间字段，使用当前时间作为发布时间
                String dateTimeStr = item.getString("create_date") + " " + item.getString("create_time");
                // 解析为 Date
                Date date = DateUtil.parse(dateTimeStr, "yyyy-MM-dd HH:mm:ss");
                news.setPublishTime(date);
                news.setFetchTime(new Date());
                news.setStatus(1);
                news.setExtraJson(item.toJSONString());
                news.setCreateBy("XinLangNewsDataBatch");
                news.setUpdateBy("XinLangNewsDataBatch");
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

            log.info("【新浪微博】同步完成，共处理 {} 条，新增 {} 条", newsList.size(), inserted);
        } catch (Exception e) {
            log.error("【新浪微博】同步异常", e);
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
