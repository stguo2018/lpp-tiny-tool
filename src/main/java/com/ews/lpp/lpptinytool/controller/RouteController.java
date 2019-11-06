package com.ews.lpp.lpptinytool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author <a href="mailto:v-stguo@expedia.com">steven</a>
 */
@Controller("/")
public class RouteController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/file-upload")
    public String fileUpload() {
        return "file-upload";
    }

}
