package com.ior.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ior.domain.entity.IorPostCategory;
import com.ior.domain.vo.Result;

public interface IorPostCategoryService extends IService<IorPostCategory> {

    /**
     * 获取所有启用的分类列表
     */
    Result getCategoryList();

    /**
     * 创建分类（管理员）
     */
    Result createCategory(String name, String description, Long parentId, Integer sortOrder);

    /**
     * 更新分类（管理员）
     */
    Result updateCategory(Long categoryId, String name, String description, Integer sortOrder);

    /**
     * 删除分类（管理员）
     */
    Result deleteCategory(Long categoryId);
}
