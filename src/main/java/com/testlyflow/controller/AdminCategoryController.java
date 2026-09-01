package com.testlyflow.controller;

import com.testlyflow.dto.AdminCategoryDto;
import com.testlyflow.dto.CategoryUpsertRequest;
import com.testlyflow.dto.PrepLinksUpdateRequest;
import com.testlyflow.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<AdminCategoryDto> list() {
        return categoryService.listAdmin();
    }

    @PostMapping
    public AdminCategoryDto create(@Valid @RequestBody CategoryUpsertRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    public AdminCategoryDto update(@PathVariable Long id, @Valid @RequestBody CategoryUpsertRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }

    @PutMapping("/{id}/prep-links")
    public void updatePrepLinks(@PathVariable Long id, @Valid @RequestBody PrepLinksUpdateRequest request) {
        categoryService.updatePrepLinks(id, request.links());
    }
}
