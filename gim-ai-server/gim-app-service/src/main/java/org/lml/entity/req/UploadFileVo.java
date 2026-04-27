package org.lml.entity.req;

import lombok.Data;

import java.util.List;

/**
 * 文件上传Vo
 */
@Data
public class UploadFileVo {

    /**
     * 时间戳
     */
    private String timeStampName;
    /**
     * 跳过上传
     */
    private boolean skipUpload;
    /**
     * 是否需要合并分片
     */
    private boolean needMerge;
    /**
     * 已经上传的分片
     */
    private List<Integer> uploaded;


}
