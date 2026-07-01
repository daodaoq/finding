package com.finding.controller;

import com.finding.common.Result;
import com.finding.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(uploadService.uploadImage(
                file.getBytes(), file.getOriginalFilename(), file.getContentType()));
    }

    @PostMapping("/images")
    public Result<List<String>> uploadImages(@RequestParam("files") List<MultipartFile> files) throws IOException {
        List<String> urls = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadService.uploadImage(file.getBytes(), file.getOriginalFilename(), file.getContentType()));
        }
        return Result.ok(urls);
    }
}
