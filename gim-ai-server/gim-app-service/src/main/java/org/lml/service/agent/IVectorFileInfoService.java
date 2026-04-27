package org.lml.service.agent;

import com.baomidou.mybatisplus.extension.service.IService;
import org.lml.entity.dto.agent.VectorFileInfo;
import org.lml.entity.req.PreviewDTO;

import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Agent-向量文件信息表 服务类
 * </p>
 *
 * @author lml
 * @since 2025-07-28
 */
public interface IVectorFileInfoService extends IService<VectorFileInfo> {


    /**
     * 预览文件
     * @param httpServletResponse
     * @param previewDTO
     */
    void previewFile(HttpServletResponse httpServletResponse, PreviewDTO previewDTO);
}
