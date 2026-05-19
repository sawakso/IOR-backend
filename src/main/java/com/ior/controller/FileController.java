package com.ior.controller;

import com.ior.domain.vo.Result;
import com.ior.service.R2StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理控制器
 */
@RestController
@RequestMapping("/file")
@Tag(name = "文件管理", description = "文件上传相关接口")
public class FileController {

    @Resource
    private R2StorageService r2StorageService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传图片或视频到 Cloudflare R2，返回访问URL")
    public Result uploadFile(
            @Parameter(description = "文件", required = true) 
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务类型：post-帖子, avatar-头像", required = true)
            @RequestParam String bizType) {
        
        try {
            // 验证文件
            if (file == null || file.isEmpty()) {
                return Result.error(400, "请选择要上传的文件");
            }

            // 验证文件大小（10MB）
            long maxSize = 10 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return Result.error(400, "文件大小不能超过 10MB");
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null) {
                return Result.error(400, "无法识别文件类型");
            }

            // 允许的类型：图片和视频
            boolean isImage = contentType.startsWith("image/");
            boolean isVideo = contentType.startsWith("video/");
            
            if (!isImage && !isVideo) {
                return Result.error(400, "只支持图片（JPG/PNG/GIF/WEBP）和视频（MP4/AVI/MOV）格式");
            }

            // 根据业务类型构建文件夹路径
            String folder;
            if ("avatar".equals(bizType)) {
                folder = "avatars";
            } else if ("post".equals(bizType)) {
                // 判断是图片还是视频
                String subFolder = isImage ? "images" : "videos";
                // 使用临时目录，发布后再移动到正式目录
                folder = "posts/temp/" + subFolder;
            } else {
                return Result.error(400, "不支持的业务类型: " + bizType);
            }

            // 上传到 R2
            String fileUrl = r2StorageService.uploadFile(file, folder);

            return Result.ok(fileUrl);

        } catch (Exception e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文件", description = "一次上传多个文件，返回URL数组")
    public Result uploadFiles(
            @Parameter(description = "文件列表", required = true) 
            @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "业务类型：post-帖子, avatar-头像", required = true)
            @RequestParam String bizType) {
        
        try {
            if (files == null || files.length == 0) {
                return Result.error(400, "请选择要上传的文件");
            }

            if (files.length > 9) {
                return Result.error(400, "最多只能上传9个文件");
            }

            // 批量上传
            java.util.List<String> urls = new java.util.ArrayList<>();
            for (MultipartFile file : files) {
                // 验证每个文件
                if (file.isEmpty()) {
                    continue;
                }

                long maxSize = 10 * 1024 * 1024;
                if (file.getSize() > maxSize) {
                    return Result.error(400, "文件 " + file.getOriginalFilename() + " 超过10MB");
                }

                String contentType = file.getContentType();
                if (contentType != null && (contentType.startsWith("image/") || contentType.startsWith("video/"))) {
                    // 根据业务类型构建文件夹路径
                    String folder;
                    if ("avatar".equals(bizType)) {
                        folder = "avatars";
                    } else if ("post".equals(bizType)) {
                        String subFolder = contentType.startsWith("image/") ? "images" : "videos";
                        folder = "posts/temp/" + subFolder;
                    } else {
                        return Result.error(400, "不支持的业务类型: " + bizType);
                    }
                    
                    String url = r2StorageService.uploadFile(file, folder);
                    urls.add(url);
                }
            }

            return Result.ok(urls);

        } catch (Exception e) {
            return Result.error(500, "批量上传失败: " + e.getMessage());
        }
    }
}
