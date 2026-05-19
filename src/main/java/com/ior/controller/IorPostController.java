package com.ior.controller;

import com.ior.domain.vo.Result;
import com.ior.service.IorPostService;
import com.ior.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/post")
@Tag(name = "生命流管理", description = "帖子创建、修改、版本管理等接口")
public class IorPostController {

    @Resource
    private IorPostService iorPostService;

    @Resource
    private JwtUtil jwtUtil;

    @PostMapping("/create")
    @Operation(summary = "创建帖子", description = "创建新的生命流帖子")
    public Result createPost(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "标题", required = true) @RequestParam String title,
            @Parameter(description = "内容", required = true) @RequestParam String content,
            @Parameter(description = "媒体文件URL（JSON格式）") @RequestParam(required = false) String mediaUrls,
            @Parameter(description = "可见性：PUBLIC/PRIVATE") @RequestParam(required = false) String visibility,
            @Parameter(description = "标签（逗号分隔）") @RequestParam(required = false) String tags,
            @Parameter(description = "分类ID") @RequestParam Long categoryId) {
        
        Long userId = extractUserId(token);
        return iorPostService.createPost(userId, title, content, mediaUrls, visibility, tags, categoryId);
    }

    @PutMapping("/{postId}")
    @Operation(summary = "修改帖子", description = "修改帖子内容，自动生成新版本")
    public Result updatePost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId,
            @Parameter(description = "新标题", required = true) @RequestParam String title,
            @Parameter(description = "新内容", required = true) @RequestParam String content,
            @Parameter(description = "新媒体URL") @RequestParam(required = false) String mediaUrls,
            @Parameter(description = "修改说明") @RequestParam(required = false) String changeSummary) {
        
        Long userId = extractUserId(token);
        return iorPostService.updatePost(postId, userId, title, content, mediaUrls, changeSummary);
    }

    @GetMapping("/{postId}")
    @Operation(summary = "获取帖子详情", description = "获取帖子的当前版本内容")
    public Result getPostDetail(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long currentUserId = token != null ? extractUserId(token) : null;
        return iorPostService.getPostDetail(postId, currentUserId);
    }

    @GetMapping("/{postId}/versions")
    @Operation(summary = "获取版本列表", description = "获取帖子的所有历史版本")
    public Result getVersionList(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId) {
        
        Long userId = extractUserId(token);
        return iorPostService.getVersionList(postId, userId);
    }

    @GetMapping("/{postId}/versions/{versionId}")
    @Operation(summary = "获取版本详情", description = "获取指定版本的详细内容")
    public Result getVersionDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId,
            @PathVariable Long versionId) {
        
        Long userId = extractUserId(token);
        return iorPostService.getVersionDetail(versionId, userId);
    }

    @PostMapping("/{postId}/versions/{versionId}/restore")
    @Operation(summary = "恢复版本", description = "将帖子恢复到指定的历史版本")
    public Result restoreVersion(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId,
            @PathVariable Long versionId) {
        
        Long userId = extractUserId(token);
        return iorPostService.restoreVersion(postId, versionId, userId);
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "删除帖子", description = "软删除帖子")
    public Result deletePost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId) {
        
        Long userId = extractUserId(token);
        return iorPostService.deletePost(postId, userId);
    }

    /**
     * 辅助方法：从 Token 中提取用户 ID
     */
    private Long extractUserId(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Token 不能为空");
        }
        token = token.trim();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        if (token.isEmpty()) {
            throw new IllegalArgumentException("无效的 Token");
        }

        Claims claims = jwtUtil.getClaimsFromToken(token);
        return ((Number) claims.get("userId")).longValue();
    }
}
