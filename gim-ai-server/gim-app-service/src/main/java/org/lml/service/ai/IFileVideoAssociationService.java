package org.lml.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import org.lml.common.result.CommonResult;
import org.lml.entity.dto.ai.FileVideoAssociation;
import org.lml.entity.dto.ai.SegmentInfo;
import org.lml.entity.req.ai.CreateImageReq;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lml
 * @since 2025-11-21
 */
public interface IFileVideoAssociationService extends IService<FileVideoAssociation> {

    /**
     * 异步下载视频
     */
    public void downloadVideo(String videoUrl);
    /**
     * 异步下载图片
     */
    public void downloadImage(String videoUrl);

}
