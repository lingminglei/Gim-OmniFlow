package org.lml.config.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Component
@EnableScheduling
@Slf4j
public class CommNewsDataBath {

    @Resource
    private CSDNNewsDataBatch csdnNewsDataBatch;

    @Resource
    private DouYinNewsDataBatch douYinNewsDataBatch;

    @Resource
    private PengPaiNewsDataBatch pengPaiNewsDataBatch;

    @Resource
    private WangYiNewsDataBath wangYiNewsDataBath;

    @Resource
    private XinLangNewsDataBatch xinLangNewsDataBatch;

    @Resource
    private XinLangFinanceDataBatch xinLangFinanceDataBatch;

//    @Scheduled(cron = "0 */30 * * * ?")  // 每5分钟执行一次
    @Transactional
    public String runTask() {

        csdnNewsDataBatch.runTask();

        douYinNewsDataBatch.runTask();

        pengPaiNewsDataBatch.runTask();

        wangYiNewsDataBath.runTask();

        xinLangNewsDataBatch.runTask();

        xinLangFinanceDataBatch.runTask();

        return "";
    }
}
