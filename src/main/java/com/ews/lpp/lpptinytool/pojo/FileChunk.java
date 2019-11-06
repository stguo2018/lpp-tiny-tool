package com.ews.lpp.lpptinytool.pojo;

import lombok.Data;

/**
 * @author <a href="mailto:v-stguo@expedia.com">steven</a>
 */
@Data
public class FileChunk {

    public FileChunk() {}
    public FileChunk(Integer chunk, Integer chunkSize) {
        this.chunk = chunk;
        this.chunkSize = chunkSize;
    }

    private Integer chunk;
    private Integer chunkSize;
    private String fileName;
    private String filePath;

}
