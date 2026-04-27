package org.lml.entity.dto.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.sql.Blob;

/**
 * <p>
 *
 * </p>
 *
 * @author lml
 * @since 2025-07-22
 */
@Data
public class Music implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "music_id", type = IdType.AUTO)
    private Long musicId;

    private String album;

    private String albumArtist;

    private String albumImage;

    private String artist;

    private String comment;

    private String composer;

    private String copyright;

    private String encoder;

    /**
     * 文件id
     */
    private Long fileId;

    private String genre;

    /**
     * 歌词
     */
    private String lyrics;

    private String originalArtist;

    private String publicer;

    private String title;

    private String track;

    private Float trackLength;

    private String url;

    private String year;

}
