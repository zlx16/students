package com.haomei.haomei.controller;

import com.haomei.haomei.dto.OrderCreateRequest;
import com.haomei.haomei.dto.OrderPayConfirmRequest;
import com.haomei.haomei.dto.OrderPayStartRequest;
import com.haomei.haomei.dto.OrderResponse;
import com.haomei.haomei.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest req) {
        return orderService.createOrder(req);
    }

    @PostMapping("/orders/{id}/pay/start")
    public OrderResponse startPay(@PathVariable Long id, @Valid @RequestBody OrderPayStartRequest req) {
        return orderService.startPay(id, req.paymentMethod());
    }

    @PostMapping("/orders/{id}/pay/confirm")
    public OrderResponse confirmPay(@PathVariable Long id, @Valid @RequestBody OrderPayConfirmRequest req) {
        return orderService.confirmPay(id, req.paymentAttemptId(), req.success());
    }

    @PostMapping("/orders/{id}/pay/cancel")
    public OrderResponse cancelPay(@PathVariable Long id) {
        return orderService.cancelPay(id);
    }

    @GetMapping("/orders")
    public List<OrderResponse> list(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Integer tableNo,
            @RequestParam(required = false) String status
    ) {
        if (tableNo != null) {
            return orderService.listOrdersForTable(tableNo, status);
        }
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("customerName 或 tableNo 必须提供一个");
        }
        return orderService.listOrdersForCustomer(customerName, status);
    }
}

