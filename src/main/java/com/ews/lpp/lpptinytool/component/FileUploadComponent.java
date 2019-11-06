package com.ews.lpp.lpptinytool.component;

import com.ews.lpp.lpptinytool.common.FileUploadCommon;
import com.ews.lpp.lpptinytool.pojo.FileChunk;
import com.ews.lpp.lpptinytool.pojo.FileInfo;
import com.ews.lpp.lpptinytool.utils.FileUploadPathUtil;
import com.ews.lpp.lpptinytool.utils.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:v-stguo@expedia.com">steven</a>
 */
@Component
@Slf4j
public class FileUploadComponent {

    private static final String RESOLVE = "resolve";
    private static final String REJECT = "reject";

    @Value("${fileUpload.basePath}")
    private String basePath;
    @Value("${fileUpload.tmpPath}")
    private String tmpPath;
    @Value("${fileUpload.uploadPath}")
    private String uploadPath;
    @Value("${fileUpload.uploadFileInfosPath}")
    private String uploadFileInfosPath;

    @PostConstruct
    public void init() {
        readUploadFileInfos();
    }

    /**
     * 检查上传的分块，如果分块已经存在，则拒绝上传分块，如果不存在，则允许分块上传
     *
     * @param md5       文件的md5
     * @param chunk     分块
     * @param chunkSize 分块大小(字节)
     * @return 检查结果
     */
    public String checkChunk(String md5, Integer chunk, Integer chunkSize) {
        FileInfo fileInfo = FileUploadCommon.UPLOAD_FILE_INFOS.get(md5);
        if (fileInfo == null) {
            fileInfo = new FileInfo();
            fileInfo.setMd5(md5);
            fileInfo.getUploadFileChunks().put(chunk, new FileChunk(chunk, chunkSize));
            FileUploadCommon.UPLOAD_FILE_INFOS.put(md5, fileInfo);
            updateUploadFileInfos();
            return RESOLVE;
        }

        Map<Integer, FileChunk> fileChunks = fileInfo.getUploadFileChunks();
        if (fileChunks == null || fileChunks.size() == 0) {
            fileChunks = new HashMap<>();
            fileChunks.put(chunk, new FileChunk(chunk, chunkSize));
            fileInfo.setUploadFileChunks(fileChunks);
            updateUploadFileInfos();
            return RESOLVE;
        }

        FileChunk fileChunk = fileChunks.computeIfAbsent(chunk, key -> null);
        if (fileChunk == null) {
            fileChunks.put(chunk, new FileChunk(chunk, chunkSize));
            updateUploadFileInfos();
            return RESOLVE;
        }
        if (!fileChunk.getChunkSize().equals(chunkSize)) {
            fileChunk.setChunkSize(chunkSize);
            updateUploadFileInfos();
            return RESOLVE;
        }

        return REJECT;
    }

    /**
     * 上传文件（分块）
     *
     * @param file  文件
     * @param md5   md5
     * @param chunk 分块，如果文件大小未达到分块的条件，则视作一个分块
     * @return 上传结果
     */
    public boolean uploadFile(MultipartFile file, String md5, Integer chunk) {
        String tmpFileDirectory = FileUploadPathUtil.getDirectory(basePath, tmpPath, md5);
        Path path = Paths.get(tmpFileDirectory, chunk + "_" + file.getOriginalFilename());
        log.info("Starting upload file [{}].", path.toString());
        try {
            Files.deleteIfExists(path);
            file.transferTo(path);
            FileInfo fileInfo = FileUploadCommon.UPLOAD_FILE_INFOS.get(md5);
            if (fileInfo != null) {
                if (StringUtils.isBlank(fileInfo.getFileName())) {
                    fileInfo.setFileName(file.getOriginalFilename());
                }
                FileChunk fileChunk = fileInfo.getUploadFileChunks().get(chunk);
                fileChunk.setFileName(file.getOriginalFilename());
                fileChunk.setFilePath(path.toString());
                updateUploadFileInfos();
                log.info("Upload file [{}] success.", path.toString());
                return true;
            } else {
                log.error("Upload file info update failure!");
            }
        } catch (IOException e) {
            log.error(String.format("Upload file [%s] failure!", path.toString()), e);
        }
        return false;
    }

    /**
     * 合并分块
     *
     * @param md5 md5
     * @return 合并结果
     */
    public boolean mergeFile(String md5) {
        String uploadFileDirectory = FileUploadPathUtil.getDirectory(basePath, uploadPath, md5);
        FileInfo fileInfo = FileUploadCommon.UPLOAD_FILE_INFOS.get(md5);

        if (fileInfo == null) {
            log.error("Not found upload file info!", new IllegalArgumentException("Not found upload file info!"));
            return false;
        }

        Path path = Paths.get(uploadFileDirectory, fileInfo.getFileName());
        log.info("Starting merge file [{}].", path.toString());

        if (StringUtils.isNotBlank(fileInfo.getFilePath())) {
            fileInfo.setLocalDateTime(LocalDateTime.now());
            updateUploadFileInfos();
            log.info("File [{}] is already exists.", path.toString());
            return true;
        }

        if (fileInfo.getUploadFileChunks() == null || fileInfo.getUploadFileChunks().size() == 0) {
            log.error(String.format("[%s] not found any chunk!", path.toString()), new IllegalArgumentException("Not found chunk."));
            return false;
        }

        try (FileOutputStream fos = new FileOutputStream(path.toFile());
             FileChannel fosChannel = fos.getChannel()) {
            for (int i = 0; i < fileInfo.getUploadFileChunks().size(); i++) {
                FileChunk fileChunk = fileInfo.getUploadFileChunks().get(i);
                if (fileChunk == null) {
                    log.error(String.format("[%s] chunk-%d damage!", path.toString(), i), new IllegalArgumentException("Chunk damage."));
                    return false;
                }

                if (fileInfo.getFileSize() == null) {
                    fileInfo.setFileSize(0);
                }
                fileInfo.setFileSize(fileInfo.getFileSize() + fileChunk.getChunkSize());

                mergeChunk(fosChannel, fileChunk.getFilePath());
            }
            fileInfo.setLocalDateTime(LocalDateTime.now());
            fileInfo.setFilePath(path.toString());
            updateUploadFileInfos();
            log.info("Merge [{}] success.", path.toString());
        } catch (IOException e) {
            log.error(String.format("Merge [%s] failure!", path.toString()), e);
        }
        return false;
    }

    private void mergeChunk(FileChannel fosChannel, String chunkFilePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(chunkFilePath);
             FileChannel fisChannel = fis.getChannel()) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            while (fisChannel.read(byteBuffer) != -1) {
                byteBuffer.flip();
                fosChannel.write(byteBuffer);
                byteBuffer.clear();
            }
        }
    }

    private void readUploadFileInfos() {
        String uploadFileInfoDirectory = FileUploadPathUtil.getDirectory(basePath, uploadFileInfosPath);
        Path path = Paths.get(uploadFileInfoDirectory, FileUploadCommon.UPLOAD_FILE_INFOS_FILE);
        if (path.toFile().exists()) {
            try (FileInputStream fis = new FileInputStream(path.toFile());
                 FileChannel fisChannel = fis.getChannel()) {
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                byteBuffer.clear();
                StringBuilder sb = new StringBuilder();
                while (fisChannel.read(byteBuffer) > 0) {
                    sb.append(new String(getBytes(byteBuffer), StandardCharsets.UTF_8));
                    byteBuffer.clear();
                }
                ConcurrentMap<String, FileInfo> uploadFileInfos = JsonUtil
                        .fromJsonByTypeReference(sb.toString(), new TypeReference<ConcurrentMap<String, FileInfo>>() {});
                if (uploadFileInfos != null && uploadFileInfos.size() > 0) {
                    for (Map.Entry<String, FileInfo> entry : uploadFileInfos.entrySet()) {
                        FileUploadCommon.UPLOAD_FILE_INFOS.put(entry.getKey(), entry.getValue());
                    }
                }
            } catch (IOException e) {
                log.error("Read upload file infos failure!", e);
            }
        }
    }

    private byte[] getBytes(ByteBuffer byteBuffer) {
        byteBuffer.flip();
        if (byteBuffer.limit() == byteBuffer.capacity()) {
            return byteBuffer.array();
        } else {
            int limit = byteBuffer.limit();
            byte[] oldBytes = byteBuffer.array();
            byte[] newBytes = new byte[limit];
            for (int i = 0; i < limit; i++) {
                newBytes[i] = oldBytes[i];
            }
            return newBytes;
        }
    }

    /**
     * 更新上传文件信息
     */
    private void updateUploadFileInfos() {
        String uploadFileInfoDirectory = FileUploadPathUtil.getDirectory(basePath, uploadFileInfosPath);
        Path path = Paths.get(uploadFileInfoDirectory, FileUploadCommon.UPLOAD_FILE_INFOS_FILE);
        String uploadFileInfosJson = Optional.ofNullable(JsonUtil.toJson(FileUploadCommon.UPLOAD_FILE_INFOS)).orElse("");
        byte[] datas = uploadFileInfosJson.getBytes(StandardCharsets.UTF_8);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error(String.format("Delete [%s] failure!", path.toString()), e);
        }
        try (FileOutputStream fos = new FileOutputStream(path.toFile());
             FileChannel fosChannel = fos.getChannel()) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(datas.length);
            byteBuffer.put(datas);
            byteBuffer.flip();
            fosChannel.write(byteBuffer);
        } catch (IOException e) {
            log.error("Update upload file infos failure!", e);
        }
    }

}
