package org.lml.service.user.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lml.entity.dto.user.UserAssetsStream;
import org.lml.mapper.user.UserAssetsStreamMapper;
import org.lml.service.user.IUserAssetsStreamService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户资产变动流水表 服务实现类
 * </p>
 *
 * @author lml
 * @since 2026-03-12
 */
@Service
public class UserAssetsStreamServiceImpl extends ServiceImpl<UserAssetsStreamMapper, UserAssetsStream> implements IUserAssetsStreamService {

}
