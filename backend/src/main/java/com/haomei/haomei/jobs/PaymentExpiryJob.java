package com.haomei.haomei.jobs;

import com.haomei.haomei.entity.OrderStatus;
import com.haomei.haomei.entity.PaymentStatus;
import com.haomei.haomei.repository.OrderRepository;
import com.haomei.haomei.service.OrderService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@EnableScheduling
public class PaymentExpiryJob {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public PaymentExpiryJob(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 10_000)
    public void expireUnpaidOrders() {
        Instant now = Instant.now();
        // 规模很小：直接全量过滤；需要时可加JPA查询优化
        orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.WAIT_PAY)
                .filter(o -> !o.isPaid())
                .filter(o -> o.getPaymentExpiresAt() != null && now.isAfter(o.getPaymentExpiresAt()))
                .filter(o -> o.getPaymentStatus() != PaymentStatus.EXPIRED)
                .forEach(o -> {
                    // reuse cancelPay path but mark expired deterministically
                    orderService.cancelPay(o.getId());
                });
    }
}

