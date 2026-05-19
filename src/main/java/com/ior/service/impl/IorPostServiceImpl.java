package com.ior.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ior.domain.entity.IorPost;
import com.ior.domain.entity.IorPostVersion;
import com.ior.domain.vo.Result;
import com.ior.mapper.IorPostMapper;
import com.ior.mapper.IorPostVersionMapper;
import com.ior.service.IorPostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class IorPostServiceImpl extends ServiceImpl<IorPostMapper, IorPost> implements IorPostService {

    private final IorPostVersionMapper postVersionMapper;

    public IorPostServiceImpl(IorPostVersionMapper postVersionMapper) {
        this.postVersionMapper = postVersionMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createPost(Long userId, String title, String content, String mediaUrls,
                            String visibility, String tags, Long categoryId) {
        // 1. 创建帖子
        IorPost post = IorPost.builder()
                .userId(userId)
                .parentId(0L)
                .title(title)
                .content(content)
                .mediaUrls(mediaUrls)
                .visibility(visibility != null ? visibility : "PUBLIC")
                .status(1)
                .tags(tags)
                .categoryId(categoryId)
                .versionCount(1)
                .build();

        this.save(post);

        // 2. 创建第一个版本（v1）
        IorPostVersion version = IorPostVersion.builder()
                .postId(post.getId())
                .versionNumber(1)
                .title(title)
                .content(content)
                .mediaUrls(mediaUrls)
                .changeSummary("初始版本")
                .createdBy(userId)
                .build();

        postVersionMapper.insert(version);

        // 3. 更新帖子的当前版本ID
        post.setCurrentVersionId(version.getId());
        this.updateById(post);

        log.info("用户 {} 创建帖子 {}，版本 v1", userId, post.getId());
        return Result.ok(post.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updatePost(Long postId, Long userId, String title, String content,
                            String mediaUrls, String changeSummary) {
        // 1. 查询帖子
        IorPost post = this.getById(postId);
        if (post == null) {
            return Result.error(404, "帖子不存在");
        }

        // 2. 权限检查：只有作者可以修改
        if (!post.getUserId().equals(userId)) {
            return Result.error(403, "无权修改此帖子");
        }

        // 3. 获取当前最大版本号
        LambdaQueryWrapper<IorPostVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IorPostVersion::getPostId, postId)
               .orderByDesc(IorPostVersion::getVersionNumber)
               .last("LIMIT 1");
        
        IorPostVersion latestVersion = postVersionMapper.selectOne(wrapper);
        int newVersionNumber = latestVersion != null ? latestVersion.getVersionNumber() + 1 : 1;

        // 4. 创建新版本（保存修改前的内容作为快照）
        IorPostVersion newVersion = IorPostVersion.builder()
                .postId(postId)
                .versionNumber(newVersionNumber)
                .title(title)
                .content(content)
                .mediaUrls(mediaUrls)
                .changeSummary(changeSummary != null ? changeSummary : "")
                .createdBy(userId)
                .build();

        postVersionMapper.insert(newVersion);

        // 5. 更新帖子主表
        post.setTitle(title);
        post.setContent(content);
        post.setMediaUrls(mediaUrls);
        post.setVersionCount(newVersionNumber);
        post.setCurrentVersionId(newVersion.getId());
        this.updateById(post);

        log.info("用户 {} 修改帖子 {}，生成新版本 v{}", userId, postId, newVersionNumber);
        return Result.ok(newVersionNumber);
    }

    @Override
    public Result getPostDetail(Long postId, Long currentUserId) {
        // 1. 查询帖子
        IorPost post = this.getById(postId);
        if (post == null) {
            return Result.error(404, "帖子不存在");
        }

        // 2. 权限检查：私密帖子只能作者查看
        if ("PRIVATE".equals(post.getVisibility()) && 
            !post.getUserId().equals(currentUserId)) {
            return Result.error(403, "无权查看此帖子");
        }

        // 3. 返回帖子详情（包含当前版本信息）
        return Result.ok(post);
    }

    @Override
    public Result getVersionList(Long postId, Long currentUserId) {
        // 1. 查询帖子
        IorPost post = this.getById(postId);
        if (post == null) {
            return Result.error(404, "帖子不存在");
        }

        // 2. 权限检查：只有作者可以查看版本历史
        if (!post.getUserId().equals(currentUserId)) {
            return Result.error(403, "无权查看版本历史");
        }

        // 3. 查询所有版本（按版本号倒序）
        LambdaQueryWrapper<IorPostVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IorPostVersion::getPostId, postId)
               .orderByDesc(IorPostVersion::getVersionNumber);
        
        List<IorPostVersion> versions = postVersionMapper.selectList(wrapper);

        return Result.ok(versions);
    }

    @Override
    public Result getVersionDetail(Long versionId, Long currentUserId) {
        // 1. 查询版本
        IorPostVersion version = postVersionMapper.selectById(versionId);
        if (version == null) {
            return Result.error(404, "版本不存在");
        }

        // 2. 查询帖子进行权限检查
        IorPost post = this.getById(version.getPostId());
        if (!post.getUserId().equals(currentUserId)) {
            return Result.error(403, "无权查看此版本");
        }

        return Result.ok(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result restoreVersion(Long postId, Long versionId, Long userId) {
        // 1. 查询目标版本
        IorPostVersion targetVersion = postVersionMapper.selectById(versionId);
        if (targetVersion == null) {
            return Result.error(404, "版本不存在");
        }

        // 2. 验证版本属于该帖子
        if (!targetVersion.getPostId().equals(postId)) {
            return Result.error(400, "版本不属于该帖子");
        }

        // 3. 查询帖子
        IorPost post = this.getById(postId);
        if (post == null) {
            return Result.error(404, "帖子不存在");
        }

        // 4. 权限检查
        if (!post.getUserId().equals(userId)) {
            return Result.error(403, "无权恢复此版本");
        }

        // 5. 获取当前最大版本号
        int newVersionNumber = post.getVersionCount() + 1;

        // 6. 创建新版本（内容与目标版本相同）
        IorPostVersion newVersion = IorPostVersion.builder()
                .postId(postId)
                .versionNumber(newVersionNumber)
                .title(targetVersion.getTitle())
                .content(targetVersion.getContent())
                .mediaUrls(targetVersion.getMediaUrls())
                .changeSummary("恢复到版本 v" + targetVersion.getVersionNumber())
                .createdBy(userId)
                .build();

        postVersionMapper.insert(newVersion);

        // 7. 更新帖子主表
        post.setTitle(targetVersion.getTitle());
        post.setContent(targetVersion.getContent());
        post.setMediaUrls(targetVersion.getMediaUrls());
        post.setVersionCount(newVersionNumber);
        post.setCurrentVersionId(newVersion.getId());
        this.updateById(post);

        log.info("用户 {} 将帖子 {} 恢复到版本 v{}", userId, postId, targetVersion.getVersionNumber());
        return Result.ok(newVersionNumber);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deletePost(Long postId, Long userId) {
        // 1. 查询帖子
        IorPost post = this.getById(postId);
        if (post == null) {
            return Result.error(404, "帖子不存在");
        }

        // 2. 权限检查
        if (!post.getUserId().equals(userId)) {
            return Result.error(403, "无权删除此帖子");
        }

        // 3. 软删除
        post.setStatus(0);
        post.setDeletedAt(LocalDateTime.now());
        this.updateById(post);

        log.info("用户 {} 删除帖子 {}", userId, postId);
        return Result.ok("删除成功");
    }
}
