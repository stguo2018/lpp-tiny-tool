package com.ews.lpp.lpptinytool.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.system.ApplicationHome;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author <a href="mailto:v-stguo@expedia.com">steven</a>
 */
@Slf4j
public class FileUploadPathUtil {

    private FileUploadPathUtil() {
    }

    public static String getDirectory(String... paths) {
        if (paths == null) {
            return null;
        }
        ApplicationHome applicationHome = new ApplicationHome();
        Path path = Paths.get(applicationHome.getDir().getAbsolutePath(), paths);
        try {
            String pathStr = URLDecoder.decode(path.toString(), "utf-8");
            File directory = new File(pathStr);
            if (!directory.exists()) {
                boolean isSuccessCreate = directory.mkdirs();
                if (isSuccessCreate) {
                    log.info("Directory [{}] create success!", directory.getAbsolutePath());
                } else {
                    log.warn("Directory [{}] create failure!", directory.getAbsolutePath());
                }
            }
            return pathStr;
        } catch (UnsupportedEncodingException e) {
            log.error("Decode URL failure!", e);
        }
        return null;
    }

}
