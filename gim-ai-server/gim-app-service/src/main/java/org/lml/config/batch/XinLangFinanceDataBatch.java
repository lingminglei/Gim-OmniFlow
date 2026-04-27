package org.lml.config.batch;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
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
public class XinLangFinanceDataBatch {

    private static final String API_URL = "https://newsapp.sina.cn/api/hotlist?newsId=HB-1-snhs%2Ftop_news_list-finance";

    @Resource
    private INewsInfoService iNewsInfoService;

//    @Scheduled(cron = "0 */5 * * * ?")  // 每5分钟执行一次
    @Transactional
    public String runTask() {
        log.info("同步新浪微博-财经榜");

        String path = "";  // CSDN热榜接口路径

        Map<String, String> headers = new HashMap<>();
        try {
            HttpResponse response = doGet(API_URL, path, headers, null);

            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
            JSONObject root = JSON.parseObject(json);
            JSONObject dataList = root.getJSONObject("data");
            JSONArray list = dataList.getJSONArray("hotList");

            if (list == null || list.isEmpty()) {
                log.warn("同步新浪微博-财经榜");
                return "";
            }

            // 2. 解析并入库
            List<NewsInfo> newsList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                JSONObject item = list.getJSONObject(i);

                NewsInfo news = new NewsInfo();
                news.setSourceCode(NewsSourceType.XINLANGB.getCode());
                news.setSourceName(NewsSourceType.XINLANGB.getName());
                news.setSourceIcon(NewsSourceType.XINLANGB.getIcon());

                JSONObject baseData = item.getJSONObject("base");
                JSONObject infoData = item.getJSONObject("info");
                news.setTitle(infoData.getString("title"));
                JSONObject baseBaseData = baseData.getJSONObject("base");
                news.setSourceUniqueId(baseBaseData.getString("uniqueId"));
                news.setUrl(baseBaseData.getString("url"));
                news.setTag("热榜");
                news.setPublishTime(new Date());
                news.setFetchTime(new Date());
                news.setStatus(1);
                news.setExtraJson(item.toJSONString());
                news.setCreateBy("XinLangFinanceDataBatch");
                news.setUpdateBy("XinLangFinanceDataBatch");
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

            log.info("【同步新浪微博-财经榜】同步完成，共处理 {} 条，新增 {} 条", newsList.size(), inserted);
        } catch (Exception e) {
            log.error("【同步新浪微博-财经榜】同步异常", e);
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
