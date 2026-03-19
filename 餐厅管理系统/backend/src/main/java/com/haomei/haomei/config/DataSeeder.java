package com.haomei.haomei.config;

import com.haomei.haomei.entity.Dish;
import com.haomei.haomei.entity.DishCategory;
import com.haomei.haomei.entity.TableSession;
import com.haomei.haomei.repository.DishRepository;
import com.haomei.haomei.repository.TableSessionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private final DishRepository dishRepository;
    private final TableSessionRepository tableSessionRepository;

    public DataSeeder(DishRepository dishRepository, TableSessionRepository tableSessionRepository) {
        this.dishRepository = dishRepository;
        this.tableSessionRepository = tableSessionRepository;
    }

    @Override
    public void run(String... args) {
        seedTablesIfNeeded();
        seedDishesIfNeeded();
    }

    private void seedTablesIfNeeded() {
        if (!tableSessionRepository.findAll().isEmpty()) return;

        for (int i = 1; i <= 10; i++) {
            TableSession t = new TableSession();
            t.setTableNo(i);
            t.setToken(UUID.randomUUID().toString().replace("-", ""));
            tableSessionRepository.save(t);
        }
    }

    private void seedDishesIfNeeded() {
        if (!dishRepository.findAll().isEmpty()) return;

        // Seed with demo dishes (images are optional; admin can upload later).
        List<Dish> dishes = List.of(
                demoDish("宫保鸡丁", "花生香辣风味", new BigDecimal("28.00"), DishCategory.RECOMMEND),
                demoDish("麻婆豆腐", "嫩豆腐搭配经典麻辣酱", new BigDecimal("22.00"), DishCategory.HOT),
                demoDish("番茄牛腩", "慢炖牛腩，浓郁番茄香", new BigDecimal("45.00"), DishCategory.RECOMMEND),
                demoDish("青椒肉丝", "青椒清香，肉丝入味", new BigDecimal("26.00"), DishCategory.HOT),
                demoDish("蛋炒饭", "粒粒分明，香气扑鼻", new BigDecimal("18.00"), DishCategory.STAPLE),
                demoDish("鲜榨橙汁", "清爽果香，自制冰镇", new BigDecimal("16.00"), DishCategory.DRINK),
                demoDish("焦糖布丁", "香甜细腻，冰爽口感", new BigDecimal("20.00"), DishCategory.DESSERT),
                demoDish("冰美式", "清爽提神，微苦回甘", new BigDecimal("18.00"), DishCategory.DRINK)
        );

        dishRepository.saveAll(dishes);
    }

    private Dish demoDish(String name, String desc, BigDecimal price, DishCategory category) {
        Dish d = new Dish();
        d.setName(name);
        d.setDescription(desc);
        d.setPrice(price);
        d.setAvailable(true);
        d.setCategory(category);
        d.setImageFilename(null);
        return d;
    }
}

