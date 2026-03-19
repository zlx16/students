package com.haomei.haomei.controller;

import com.haomei.haomei.dto.DishResponse;
import com.haomei.haomei.service.DishService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
public class DishController {

    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    public List<DishResponse> listAvailable(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category
    ) {
        return dishService.listAvailable(q, category);
    }
}

