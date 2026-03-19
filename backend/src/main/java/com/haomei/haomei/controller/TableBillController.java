package com.haomei.haomei.controller;

import com.haomei.haomei.dto.TableBillPayRequest;
import com.haomei.haomei.dto.TableBillSummaryResponse;
import com.haomei.haomei.repository.TableSessionRepository;
import com.haomei.haomei.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/tables")
public class TableBillController {

    private final TableSessionRepository tableSessionRepository;
    private final OrderService orderService;

    public TableBillController(TableSessionRepository tableSessionRepository, OrderService orderService) {
        this.tableSessionRepository = tableSessionRepository;
        this.orderService = orderService;
    }

    @GetMapping("/bill")
    public TableBillSummaryResponse bill(@RequestParam String tableToken) {
        Integer tableNo = tableSessionRepository.findByToken(tableToken)
                .orElseThrow(() -> new IllegalArgumentException("无效二维码/桌台token"))
                .getTableNo();

        var orders = orderService.listUnpaidWaitPayOrdersForTable(tableNo).stream()
                .map(orderService::toResponseForController)
                .toList();

        BigDecimal total = orders.stream()
                .map(o -> o.amount() == null ? BigDecimal.ZERO : o.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TableBillSummaryResponse(tableNo, total, orders.size(), orders);
    }

    @PostMapping("/bill/pay")
    public TableBillSummaryResponse pay(@Valid @RequestBody TableBillPayRequest req) {
        Integer tableNo = tableSessionRepository.findByToken(req.tableToken())
                .orElseThrow(() -> new IllegalArgumentException("无效二维码/桌台token"))
                .getTableNo();

        var settled = orderService.settleTable(tableNo, req.paymentMethod(), req.success());
        BigDecimal total = settled.stream()
                .map(o -> o.amount() == null ? BigDecimal.ZERO : o.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TableBillSummaryResponse(tableNo, total, settled.size(), settled);
    }
}

