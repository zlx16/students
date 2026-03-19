package com.haomei.haomei.controller.admin;

import com.haomei.haomei.dto.OrderResponse;
import com.haomei.haomei.dto.OrderStatusUpdateRequest;
import com.haomei.haomei.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(required = false) String status) {
        return orderService.listOrdersForAdmin(status);
    }

    @GetMapping("/table/{tableNo}")
    public List<OrderResponse> listByTable(@PathVariable Integer tableNo) {
        return orderService.listOrdersByTableNo(tableNo);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest req
    ) {
        return orderService.updateOrderStatus(id, req.status());
    }
}

