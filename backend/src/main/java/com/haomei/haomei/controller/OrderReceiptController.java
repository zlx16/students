package com.haomei.haomei.controller;

import com.haomei.haomei.dto.OrderResponse;
import com.haomei.haomei.repository.TableSessionRepository;
import com.haomei.haomei.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderReceiptController {

    private final TableSessionRepository tableSessionRepository;
    private final OrderService orderService;

    public OrderReceiptController(TableSessionRepository tableSessionRepository, OrderService orderService) {
        this.tableSessionRepository = tableSessionRepository;
        this.orderService = orderService;
    }

    @GetMapping("/{id}/receipt")
    public OrderResponse receipt(@PathVariable Long id, @RequestParam String tableToken) {
        Integer tableNo = tableSessionRepository.findByToken(tableToken)
                .orElseThrow(() -> new IllegalArgumentException("无效二维码/桌台token"))
                .getTableNo();
        return orderService.getReceipt(id, tableNo);
    }
}

