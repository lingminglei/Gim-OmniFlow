package org.lml.service.user.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lml.entity.dto.user.User;
import org.lml.mapper.user.UserMapper;
import org.lml.service.user.IUserService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author lml
 * @since 2025-12-24
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}
