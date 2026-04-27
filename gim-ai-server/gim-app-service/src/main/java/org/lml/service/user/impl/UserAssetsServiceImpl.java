package org.lml.service.user.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lml.entity.dto.user.UserAssets;
import org.lml.mapper.user.UserAssetsMapper;
import org.lml.service.user.IUserAssetsService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户资产表 服务实现类
 * </p>
 *
 * @author lml
 * @since 2026-03-12
 */
@Service
public class UserAssetsServiceImpl extends ServiceImpl<UserAssetsMapper, UserAssets> implements IUserAssetsService {

}
