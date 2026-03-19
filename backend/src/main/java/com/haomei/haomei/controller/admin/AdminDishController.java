package com.haomei.haomei.controller.admin;

import com.haomei.haomei.dto.DishCreateRequest;
import com.haomei.haomei.dto.DishResponse;
import com.haomei.haomei.dto.DishUpdateRequest;
import com.haomei.haomei.service.DishService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dishes")
public class AdminDishController {

    private final DishService dishService;

    public AdminDishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    public List<DishResponse> listAll() {
        return dishService.listAll();
    }

    @PostMapping
    public DishResponse create(@Valid @RequestBody DishCreateRequest req) {
        return dishService.create(req);
    }

    @PutMapping("/{id}")
    public DishResponse update(@PathVariable Long id, @Valid @RequestBody DishUpdateRequest req) {
        return dishService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        dishService.delete(id);
    }
}

