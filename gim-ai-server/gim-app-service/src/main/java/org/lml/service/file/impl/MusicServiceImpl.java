package org.lml.service.file.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lml.entity.dto.file.Music;
import org.lml.mapper.file.MusicMapper;
import org.lml.service.file.IMusicService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author lml
 * @since 2025-07-22
 */
@Service
public class MusicServiceImpl extends ServiceImpl<MusicMapper, Music> implements IMusicService {

}
