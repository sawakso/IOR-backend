package com.ior.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ior.domain.entity.IorPostCategory;
import com.ior.domain.vo.Result;
import com.ior.mapper.IorPostCategoryMapper;
import com.ior.service.IorPostCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class IorPostCategoryServiceImpl extends ServiceImpl<IorPostCategoryMapper, IorPostCategory> 
        implements IorPostCategoryService {

    @Override
    public Result getCategoryList() {
        List<IorPostCategory> categories = this.list(
            new LambdaQueryWrapper<IorPostCategory>()
                .eq(IorPostCategory::getIsActive, 1)
                .orderByAsc(IorPostCategory::getSortOrder)
        );
        
        log.debug("获取分类列表，共 {} 个分类", categories.size());
        return Result.ok(categories);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createCategory(String name, String description, Long parentId, Integer sortOrder) {
        // 参数校验
        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "分类名称不能为空");
        }

        // 检查名称是否重复
        long count = this.count(new LambdaQueryWrapper<IorPostCategory>()
                .eq(IorPostCategory::getName, name)
                .eq(IorPostCategory::getParentId, parentId != null ? parentId : 0));
        
        if (count > 0) {
            return Result.error(400, "分类名称已存在");
        }

        // 创建分类
        IorPostCategory category = IorPostCategory.builder()
                .name(name.trim())
                .description(description != null ? description : "")
                .parentId(parentId != null ? parentId : 0L)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .isActive(1)
                .build();

        this.save(category);
        log.info("创建分类成功: {}", category.getName());
        
        return Result.ok(category.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateCategory(Long categoryId, String name, String description, Integer sortOrder) {
        // 查询分类
        IorPostCategory category = this.getById(categoryId);
        if (category == null) {
            return Result.error(404, "分类不存在");
        }

        // 更新字段
        if (name != null && !name.trim().isEmpty()) {
            // 检查名称是否与其他分类重复
            long count = this.count(new LambdaQueryWrapper<IorPostCategory>()
                    .eq(IorPostCategory::getName, name.trim())
                    .eq(IorPostCategory::getParentId, category.getParentId())
                    .ne(IorPostCategory::getId, categoryId));
            
            if (count > 0) {
                return Result.error(400, "分类名称已存在");
            }
            category.setName(name.trim());
        }

        if (description != null) {
            category.setDescription(description);
        }

        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }

        this.updateById(category);
        log.info("更新分类成功: {}", category.getId());
        
        return Result.ok("更新成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteCategory(Long categoryId) {
        // 查询分类
        IorPostCategory category = this.getById(categoryId);
        if (category == null) {
            return Result.error(404, "分类不存在");
        }

        // TODO: 检查是否有帖子使用该分类，如果有则不允许删除
        
        // 软删除：设置为禁用
        category.setIsActive(0);
        this.updateById(category);
        
        log.info("删除分类成功: {}", category.getId());
        return Result.ok("删除成功");
    }
}
