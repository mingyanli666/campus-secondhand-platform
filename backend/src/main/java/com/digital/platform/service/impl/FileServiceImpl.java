package com.digital.platform.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.config.PlatformProperties;
import com.digital.platform.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 本地磁盘文件服务实现。
 * <p>
 * 存储策略：uploads/{业务目录}/{年}/{月}/{UUID}.{后缀}
 * 按年月分目录，避免单目录文件过多导致文件系统性能下降。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final PlatformProperties properties;

    @Override
    public String uploadImage(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_EMPTY);
        }

        String originalName = file.getOriginalFilename();
        if (StrUtil.isBlank(originalName)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }

        // 1. 校验后缀：白名单机制，杜绝上传 jsp/php 等可执行文件
        String ext = FileUtil.extName(originalName).toLowerCase(Locale.ROOT);
        List<String> allowExt = Arrays.asList(properties.getFile().getAllowExt().split(","));
        if (!allowExt.contains(ext)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED,
                    "仅支持 " + String.join("/", allowExt) + " 格式的图片");
        }

        // 2. 校验大小（Spring 的 multipart 限制是第一道，这里再兜一层）
        if (file.getSize() > properties.getFile().getMaxSize()) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE,
                    "图片不能超过 " + (properties.getFile().getMaxSize() / 1024 / 1024) + "MB");
        }

        // 3. 生成存储路径：用 UUID 重命名，防止同名覆盖和路径穿越
        String datePath = DateUtil.format(new Date(), "yyyy/MM");
        String fileName = IdUtil.fastSimpleUUID() + "." + ext;
        String relativePath = subDir + "/" + datePath + "/" + fileName;

        File target = new File(properties.getFile().getUploadPath(), relativePath);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }

        try {
            file.transferTo(target.getAbsoluteFile());
        } catch (IOException e) {
            log.error("文件保存失败: {}", target.getAbsolutePath(), e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }

        // 4. 返回前端可直接访问的地址（与 WebMvcConfig 中 /uploads/** 映射对应）
        return "/uploads/" + relativePath;
    }
}
