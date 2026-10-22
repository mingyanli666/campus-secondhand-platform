package com.digital.platform.controller;

import com.digital.platform.common.result.Result;
import com.digital.platform.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口。
 * <p>
 * 注意这里没有标 @PublicApi —— 上传必须登录，否则任何人都能往你服务器塞文件，
 * 磁盘很快会被写满。这是很实际的安全考量。
 */
@Tag(name = "04-文件上传", description = "图片上传")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileService fileService;

    @Operation(summary = "上传商品图片")
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success("上传成功", fileService.uploadImage(file, "product"));
    }
}
