package org.lml.service.agent.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lml.entity.dto.agent.UserKnowledgeMapping;
import org.lml.mapper.agent.UserVectorAndFileAssociationMapper;
import org.lml.service.agent.IUserVectorAndFileAssociationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * Agent-用户-文件-向量数据库关联 服务实现类
 * </p>
 *
 * @author lml
 * @since 2025-07-28
 */
@Service
public class UserVectorAndFileAssociationServiceImpl extends ServiceImpl<UserVectorAndFileAssociationMapper, UserKnowledgeMapping> implements IUserVectorAndFileAssociationService {

    @Resource
    private UserVectorAndFileAssociationMapper userVectorAndFileAssociationMapper;

    @Override
    public UserKnowledgeMapping isKonwLedgeNameExist(String knowledgeName) {

        QueryWrapper<UserKnowledgeMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name",knowledgeName);
        queryWrapper.eq("status","1");
        queryWrapper.eq("user_id",String.valueOf(StpUtil.getLoginId()));

        List<UserKnowledgeMapping> list =  userVectorAndFileAssociationMapper.selectList(queryWrapper);

        if(ObjectUtil.isNotEmpty(list) && list.size()>0){
            return list.get(0);
        }else {
            return null;
        }

    }
}
