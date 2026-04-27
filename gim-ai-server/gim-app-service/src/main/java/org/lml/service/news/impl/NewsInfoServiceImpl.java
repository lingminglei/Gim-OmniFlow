package org.lml.service.news.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lml.entity.dto.news.NewsInfo;
import org.lml.mapper.news.NewsInfoMapper;
import org.lml.service.news.INewsInfoService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 多平台新闻聚合表 服务实现类
 * </p>
 *
 * @author lml
 * @since 2025-11-14
 */
@Service
public class NewsInfoServiceImpl extends ServiceImpl<NewsInfoMapper, NewsInfo> implements INewsInfoService {

}
