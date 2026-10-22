package com.digital.platform.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务。当前实现是本地磁盘存储，
 * 以后要换成 OSS / MinIO，只要换一个实现类，调用方无感知。
 */
public interface FileService {

    /**
     * 保存图片。
     *
     * @param file   上传的文件
     * @param subDir 子目录，如 avatar、product
     * @return 可直接访问的相对地址，如 /uploads/avatar/2026/01/xxx.jpg
     */
    String uploadImage(MultipartFile file, String subDir);
}
