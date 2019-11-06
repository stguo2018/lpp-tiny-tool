package com.ews.lpp.lpptinytool.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:v-stguo@expedia.com">steven</a>
 */
@Data
public class FileInfo {

    private String md5;
    private String fileName;
    private String filePath;
    private Integer fileSize;
    private LocalDateTime localDateTime;
    private Map<Integer, FileChunk> uploadFileChunks = new HashMap<>();

}
