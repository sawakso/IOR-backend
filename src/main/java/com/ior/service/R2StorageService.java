package com.ior.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Slf4j
@Service
public class R2StorageService {

    private final S3Client s3Client;
    
    @Value("${cloudflare.r2.bucket}")
    private String bucket;
    
    @Value("${cloudflare.r2.public-url}")
    private String publicUrl;

    public R2StorageService(
            @Value("${cloudflare.r2.access-key}") String accessKey,
            @Value("${cloudflare.r2.secret-key}") String secretKey,
            @Value("${cloudflare.r2.account-id}") String accountId,
            @Value("${cloudflare.r2.endpoint}") String endpoint) {
        
        // 创建 AWS 凭证
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        // 配置 S3 客户端（兼容 Cloudflare R2）
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)  // R2 使用固定 region
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
        
        log.info("R2 存储服务初始化成功，Bucket: {}", bucket);
    }

    /**
     * 上传文件到 R2
     * @param file 上传的文件
     * @param folder 文件夹路径（如：avatars）
     * @return 文件的公网访问 URL
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        // 生成唯一的文件名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileName = UUID.randomUUID().toString() + extension;
        
        // 构建完整的对象键（路径）
        String objectKey = folder + "/" + fileName;
        
        try {
            // 创建上传请求
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();
            
            // 上传文件
            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromBytes(file.getBytes()));
            
            // 返回公网访问 URL
            String fileUrl = publicUrl + "/" + objectKey;
            log.info("文件上传成功: {}", fileUrl);
            
            return fileUrl;
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new IOException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传头像（专用方法）
     * @param file 头像文件
     * @return 头像的公网访问 URL
     */
    public String uploadAvatar(MultipartFile file) throws IOException {
        return uploadFile(file, "avatars");
    }

    /**
     * 上传文件到 R2（自动判断类型）
     * @param file 上传的文件
     * @return 文件的公网访问 URL
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // 根据文件类型自动选择文件夹
        String contentType = file.getContentType();
        String folder = "posts"; // 默认文件夹
        
        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                folder = "posts/images";
            } else if (contentType.startsWith("video/")) {
                folder = "posts/videos";
            }
        }
        
        return uploadFile(file, folder);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 删除文件
     * @param fileUrl 文件的完整 URL
     */
    public void deleteFile(String fileUrl) {
        try {
            // 从 URL 中提取对象键
            String objectKey = fileUrl.replace(publicUrl + "/", "");
            
            s3Client.deleteObject(builder -> builder
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
            
            log.info("文件删除成功: {}", objectKey);
        } catch (Exception e) {
            log.error("文件删除失败: {}", fileUrl, e);
        }
    }

    /**
     * 清理临时目录中超过指定时间的文件
     * @param hours 时间阈值（小时）
     * @return 删除的文件数量
     */
    public int deleteOldTempFiles(int hours) {
        int deletedCount = 0;
        
        try {
            // 列出 posts/temp 目录下的所有文件
            var listRequest = software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix("posts/temp/")
                    .build();
            
            var response = s3Client.listObjectsV2(listRequest);
            
            // 计算时间阈值
            long thresholdMillis = System.currentTimeMillis() - (hours * 60 * 60 * 1000L);
            
            // 遍历文件，删除超过阈值的
            for (var s3Object : response.contents()) {
                String key = s3Object.key();
                
                // 检查文件最后修改时间
                if (s3Object.lastModified() != null) {
                    long fileTime = s3Object.lastModified().toEpochMilli();
                    
                    if (fileTime < thresholdMillis) {
                        // 删除文件
                        s3Client.deleteObject(builder -> builder
                                .bucket(bucket)
                                .key(key)
                                .build());
                        
                        deletedCount++;
                        log.debug("删除孤儿文件: {}", key);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("清理临时文件失败", e);
        }
        
        return deletedCount;
    }
}
