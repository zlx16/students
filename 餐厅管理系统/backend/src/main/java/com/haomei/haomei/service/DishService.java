package com.haomei.haomei.service;

import com.haomei.haomei.dto.DishCreateRequest;
import com.haomei.haomei.dto.DishResponse;
import com.haomei.haomei.dto.DishUpdateRequest;
import com.haomei.haomei.entity.Dish;
import com.haomei.haomei.entity.DishCategory;
import com.haomei.haomei.entity.DishPortionOption;
import com.haomei.haomei.repository.DishRepository;
import com.haomei.haomei.service.websocket.MenuUpdateMessage;
import com.haomei.haomei.service.websocket.RealtimeNotificationService;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishService {

    private final DishRepository dishRepository;
    private final RealtimeNotificationService realtimeNotificationService;

    public DishService(DishRepository dishRepository, RealtimeNotificationService realtimeNotificationService) {
        this.dishRepository = dishRepository;
        this.realtimeNotificationService = realtimeNotificationService;
    }

    public List<DishResponse> listAvailable(String q, String category) {
        List<Dish> base;
        if (category != null && !category.isBlank()) {
            DishCategory c = parseCategory(category);
            base = dishRepository.findAllByAvailableTrueAndCategory(c);
        } else {
            base = dishRepository.findAllByAvailableTrue();
        }

        if (q != null && !q.isBlank()) {
            String qq = q.trim().toLowerCase(Locale.ROOT);
            base = base.stream()
                    .filter(d ->
                            (d.getName() != null && d.getName().toLowerCase(Locale.ROOT).contains(qq)) ||
                                    (d.getDescription() != null && d.getDescription().toLowerCase(Locale.ROOT).contains(qq))
                    )
                    .toList();
        }

        return base.stream().map(this::toResponse).toList();
    }

    public List<DishResponse> listAll() {
        return dishRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public DishResponse create(DishCreateRequest req) {
        if (dishRepository.existsByName(req.name().trim())) {
            throw new IllegalArgumentException("菜品名称「" + req.name().trim() + "」已存在，不能重复添加");
        }

        Dish dish = new Dish();
        dish.setName(req.name().trim());
        dish.setDescription(req.description());
        dish.setPrice(req.price());
        dish.setAvailable(req.available());
        dish.setCategory(parseCategory(req.category()));
        dish.setPortionOption(parsePortionOption(req.portionOption()));
        dish.setImageFilename(null);

        Dish saved = dishRepository.save(dish);

        DishResponse resp = toResponse(saved);
        realtimeNotificationService.broadcastMenuUpdate(new MenuUpdateMessage("CREATED", saved.getId(), saved.getName(), resp.imageUrl()));
        return resp;
    }

    public DishResponse update(Long id, DishUpdateRequest req) {
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Dish not found: " + id));
        dish.setName(req.name());
        dish.setDescription(req.description());
        dish.setPrice(req.price());
        dish.setAvailable(req.available());
        dish.setCategory(parseCategory(req.category()));
        dish.setPortionOption(parsePortionOption(req.portionOption()));

        Dish saved = dishRepository.save(dish);
        DishResponse resp = toResponse(saved);
        realtimeNotificationService.broadcastMenuUpdate(new MenuUpdateMessage("UPDATED", saved.getId(), saved.getName(), resp.imageUrl()));
        return resp;
    }

    public void delete(Long id) {
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Dish not found: " + id));
        dishRepository.delete(dish);
        realtimeNotificationService.broadcastMenuUpdate(new MenuUpdateMessage("DELETED", id, dish.getName(), null));
    }

    private DishResponse toResponse(Dish dish) {
        return new DishResponse(
                dish.getId(),
                dish.getName(),
                dish.getDescription(),
                dish.getPrice(),
                dish.isAvailable(),
                dish.getCategory().name(),
                dish.getPortionOption() == null ? DishPortionOption.BOTH.name() : dish.getPortionOption().name(),
                dish.getImageFilename() == null ? null : "/api/images/" + dish.getImageFilename()
        );
    }

    private DishCategory parseCategory(String category) {
        try {
            return DishCategory.valueOf(category.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid category: " + category);
        }
    }

    private DishPortionOption parsePortionOption(String portionOption) {
        if (portionOption == null || portionOption.isBlank()) {
            return DishPortionOption.BOTH;
        }
        try {
            return DishPortionOption.valueOf(portionOption.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid portionOption: " + portionOption);
        }
    }
}

