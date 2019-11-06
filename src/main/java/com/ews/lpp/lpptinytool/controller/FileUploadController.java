package com.ews.lpp.lpptinytool.controller;

import com.ews.lpp.lpptinytool.component.FileUploadComponent;
import com.ews.lpp.lpptinytool.utils.JsonUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:v-stguo@expedia.com">steven</a>
 */
@RestController
@RequestMapping("/upload")
public class FileUploadController {

    private final FileUploadComponent fileUploadComponent;

    public FileUploadController(FileUploadComponent fileUploadComponent) {
        this.fileUploadComponent = fileUploadComponent;
    }

    @GetMapping(path = "/chunk")
    public ResponseEntity<String> chunk(@RequestParam("md5") String md5, @RequestParam("chunk") Integer chunk, @RequestParam("chunkSize") Integer chunkSize) {
        String message = fileUploadComponent.checkChunk(md5, chunk, chunkSize);
        Map<String, String> result = new HashMap<>();
        result.put("message", message);
        return new ResponseEntity<>(JsonUtil.toJson(result), HttpStatus.OK);
    }

    @PostMapping(path = "/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String md5 = request.getParameter("md5");
        Integer chunk = request.getParameter("chunk") == null ? 0 : Integer.parseInt(request.getParameter("chunk"));
        boolean isSuccess = fileUploadComponent.uploadFile(file, md5, chunk);
        if (isSuccess) {
            return new ResponseEntity<>("Upload Success!", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("Upload Failure!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = "/merge")
    public ResponseEntity<String> merge(@RequestParam("md5") String md5) {
        boolean isSuccess = fileUploadComponent.mergeFile(md5);
        if (isSuccess) {
            return new ResponseEntity<>("Merge Success!", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("Merge Failure!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
