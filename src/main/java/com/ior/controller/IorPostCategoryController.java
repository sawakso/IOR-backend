package com.ior.controller;

import com.ior.domain.vo.Result;
import com.ior.service.IorPostCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/post/category")
@Tag(name = "帖子分类管理", description = "分类的增删改查接口")
public class IorPostCategoryController {

    @Resource
    private IorPostCategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "获取分类列表", description = "获取所有启用的分类，按排序权重升序")
    public Result getCategoryList() {
        return categoryService.getCategoryList();
    }

    @PostMapping("/create")
    @Operation(summary = "创建分类", description = "创建新的帖子分类（管理员功能）")
    public Result createCategory(
            @Parameter(description = "分类名称", required = true) @RequestParam String name,
            @Parameter(description = "分类描述") @RequestParam(required = false) String description,
            @Parameter(description = "父分类ID（0表示一级分类）") @RequestParam(required = false) Long parentId,
            @Parameter(description = "排序权重") @RequestParam(required = false) Integer sortOrder) {
        
        return categoryService.createCategory(name, description, parentId, sortOrder);
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "更新分类", description = "修改分类信息（管理员功能）")
    public Result updateCategory(
            @PathVariable Long categoryId,
            @Parameter(description = "新分类名称") @RequestParam(required = false) String name,
            @Parameter(description = "新分类描述") @RequestParam(required = false) String description,
            @Parameter(description = "新排序权重") @RequestParam(required = false) Integer sortOrder) {
        
        return categoryService.updateCategory(categoryId, name, description, sortOrder);
    }

    @DeleteMapping("/{categoryId}")
    @Operation(summary = "删除分类", description = "禁用分类（软删除，管理员功能）")
    public Result deleteCategory(@PathVariable Long categoryId) {
        return categoryService.deleteCategory(categoryId);
    }
}
