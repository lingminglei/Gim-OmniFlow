package org.lml.entity.req.ai;

import lombok.Data;
import org.lml.entity.dto.ai.SegmentInfo;

import java.util.List;

@Data
public class CreateVideoReq {

    /**
     * 分片列表信息
     */
    private List<SegmentInfo> segmentInfoList;

    private String prompt;

    private String filePath;
}
