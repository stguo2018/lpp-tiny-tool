package com.ews.lpp.lpptinytool.common;

import com.ews.lpp.lpptinytool.pojo.FileInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:v-stguo@expedia.com">steven</a>
 */
public class FileUploadCommon {

    private FileUploadCommon() {}

    public static final String UPLOAD_FILE_INFOS_FILE = "upload-file-infos.json";
    public static final ConcurrentMap<String, FileInfo> UPLOAD_FILE_INFOS = new ConcurrentHashMap<>();

}
