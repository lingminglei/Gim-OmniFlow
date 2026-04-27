package org.lml.service.file;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;
import org.lml.entity.dto.file.UserFile;
import org.lml.entity.resp.FileListVO;

import java.util.List;

/**
 * <p>
 *  鏈嶅姟绫?
 * </p>
 *
 * @author lml
 * @since 2025-07-21
 */
public interface IUserFileService extends IService<UserFile> {

    List<UserFile> selectFilePathTreeByUserId(String userId);

    IPage<FileListVO> userFileList(String userId, String filePath, Long beginCount, Long pageCount);

    IPage<FileListVO> getFileByFileType(Integer fileTypeId, Long currentPage, Long pageCount, String userId);

    List<UserFile> selectUserFileByLikeRightFilePath(@Param("filePath") String filePath, @Param("userId") String userId);

    void deleteUserFile(String userFileId, String sessionUserId);

    List<UserFile> selectUserFileByNameAndPath(String fileName, String filePath, String userId);

    void updateFilepathByUserFileId(String userFileId, String newfilePath, String userId);

    void userFileCopy(String userId, String userFileId, String newfilePath);

    List<UserFile> selectSameUserFile(String fileName, String filePath, String extendName, String userId);
}
