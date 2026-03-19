package com.haomei.haomei.repository;

import com.haomei.haomei.entity.Dish;
import com.haomei.haomei.entity.DishCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findAllByAvailableTrue();
    List<Dish> findAllByAvailableTrueAndCategory(DishCategory category);
    boolean existsByName(String name);
}

