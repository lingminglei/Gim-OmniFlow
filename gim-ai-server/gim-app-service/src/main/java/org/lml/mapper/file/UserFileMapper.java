package org.lml.mapper.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.lml.entity.dto.file.UserFile;
import org.lml.entity.resp.FileListVO;

import java.util.List;

/**
 * <p>
 *  Mapper 鎺ュ彛
 * </p>
 *
 * @author lml
 * @since 2025-07-21
 */
public interface UserFileMapper extends BaseMapper<UserFile> {

    IPage<FileListVO> selectPageVo(Page<?> page, @Param("userFile") UserFile userFile, @Param("fileTypeId") Integer fileTypeId);

    List<UserFile> selectUserFileByLikeRightFilePath(@Param("filePath") String filePath, @Param("userId") String userId);
}
