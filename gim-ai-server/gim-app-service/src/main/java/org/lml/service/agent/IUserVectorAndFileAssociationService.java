package org.lml.service.agent;

import com.baomidou.mybatisplus.extension.service.IService;
import org.lml.entity.dto.agent.UserKnowledgeMapping;

/**
 * <p>
 * Agent-用户-文件-向量数据库关联 服务类
 * </p>
 *
 * @author lml
 * @since 2025-07-28
 */
public interface IUserVectorAndFileAssociationService extends IService<UserKnowledgeMapping> {

    /**
     * 该知识库名称是否存在
     */
    UserKnowledgeMapping isKonwLedgeNameExist(String knowledgeName);
}
