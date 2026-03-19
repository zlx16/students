package com.haomei.haomei.service;

import com.haomei.haomei.dto.OrderCreateRequest;
import com.haomei.haomei.dto.OrderItemCreateRequest;
import com.haomei.haomei.dto.OrderItemResponse;
import com.haomei.haomei.dto.OrderResponse;
import com.haomei.haomei.repository.DishRepository;
import com.haomei.haomei.repository.OrderRepository;
import com.haomei.haomei.repository.TableSessionRepository;
import com.haomei.haomei.entity.Dish;
import com.haomei.haomei.entity.Order;
import com.haomei.haomei.entity.OrderItem;
import com.haomei.haomei.entity.OrderStatus;
import com.haomei.haomei.entity.PaymentStatus;
import com.haomei.haomei.entity.Portion;
import com.haomei.haomei.service.websocket.OrderUpdateMessage;
import com.haomei.haomei.service.websocket.RealtimeNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
    // 给控制器/其它服务复用（避免把私有方法暴露成public字段访问）
    public OrderResponse toResponseForController(Order order) {
        return toResponse(order);
    }

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final TableSessionRepository tableSessionRepository;
    private final RealtimeNotificationService realtimeNotificationService;

    public OrderService(OrderRepository orderRepository, DishRepository dishRepository, TableSessionRepository tableSessionRepository, RealtimeNotificationService realtimeNotificationService) {
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.realtimeNotificationService = realtimeNotificationService;
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be empty");
        }
        if (req.diners() == null || req.diners() <= 0) {
            throw new IllegalArgumentException("用餐人数必须大于0");
        }

        Integer tableNo = tableSessionRepository.findByToken(req.tableToken())
                .orElseThrow(() -> new IllegalArgumentException("无效二维码/桌台token"))
                .getTableNo();

        List<Long> dishIds = req.items().stream().map(OrderItemCreateRequest::dishId).toList();
        List<Dish> dishes = dishRepository.findAllById(dishIds);
        Map<Long, Dish> dishMap = dishes.stream().collect(Collectors.toMap(Dish::getId, d -> d));

        // Ensure all requested dishes exist and are available.
        for (OrderItemCreateRequest itemReq : req.items()) {
            Dish dish = dishMap.get(itemReq.dishId());
            if (dish == null) {
                throw new IllegalArgumentException("Dish not found: " + itemReq.dishId());
            }
            if (!dish.isAvailable()) {
                throw new IllegalArgumentException("Dish unavailable: " + dish.getName());
            }
        }

        Order order = new Order();
        order.setTableNo(tableNo);
        order.setDiners(req.diners());
        order.setCustomerName(req.customerName());
        order.setRemark(req.remark());
        order.setStatus(OrderStatus.WAIT_PAY);
        order.setCreatedAt(Instant.now());
        order.setPaymentMethod("UNPAID");
        order.setPaid(false);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setPaymentExpiresAt(order.getCreatedAt().plus(5, ChronoUnit.MINUTES));

        BigDecimal amount = BigDecimal.ZERO;

        for (OrderItemCreateRequest itemReq : req.items()) {
            Dish dish = dishMap.get(itemReq.dishId());
            int qty = itemReq.quantity();
            Portion portion = parsePortion(itemReq.portion());
            BigDecimal unitPrice = computeUnitPrice(dish.getPrice(), portion);
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty));
            amount = amount.add(totalPrice);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setDishId(dish.getId());
            item.setDishName(dish.getName());
            item.setQuantity(qty);
            item.setPortion(portion);
            item.setUnitPrice(unitPrice);
            item.setTotalPrice(totalPrice);
            order.getItems().add(item);
        }

        order.setAmount(amount);
        Order saved = orderRepository.save(order);
        OrderResponse resp = toResponse(saved);
        realtimeNotificationService.broadcastOrderUpdate(toWs(saved, resp));
        return resp;
    }

    @Transactional
    public OrderResponse startPay(Long id, String paymentMethod) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        if (order.isPaid()) {
            return toResponse(order);
        }
        if (order.getStatus() != OrderStatus.WAIT_PAY) throw new IllegalArgumentException("当前订单状态不可支付：" + order.getStatus());
        if (order.getPaymentExpiresAt() != null && Instant.now().isAfter(order.getPaymentExpiresAt())) {
            order.setPaymentStatus(PaymentStatus.EXPIRED);
            order.setPaymentMethod("UNPAID");
            order.setPaid(false);
            Order savedExpired = orderRepository.save(order);
            OrderResponse respExpired = toResponse(savedExpired);
            realtimeNotificationService.broadcastOrderUpdate(toWs(savedExpired, respExpired));
            throw new IllegalArgumentException("订单已超时，请重新下单");
        }

        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(PaymentStatus.PAYING);
        order.setPaymentAttemptId(UUID.randomUUID().toString().replace("-", ""));
        order.setPaymentStartedAt(Instant.now());

        Order saved = orderRepository.save(order);
        OrderResponse resp = toResponse(saved);
        realtimeNotificationService.broadcastOrderUpdate(toWs(saved, resp));
        return resp;
    }

    @Transactional
    public OrderResponse confirmPay(Long id, String paymentAttemptId, boolean success) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        if (order.isPaid()) return toResponse(order);
        if (order.getStatus() != OrderStatus.WAIT_PAY) throw new IllegalArgumentException("当前订单状态不可支付：" + order.getStatus());
        if (order.getPaymentStatus() != PaymentStatus.PAYING) {
            throw new IllegalArgumentException("当前订单未处于支付中，不能确认支付");
        }
        if (order.getPaymentAttemptId() == null || !order.getPaymentAttemptId().equals(paymentAttemptId)) {
            throw new IllegalArgumentException("支付单号不匹配（可能是重复回调/过期回调）");
        }

        if (order.getPaymentExpiresAt() != null && Instant.now().isAfter(order.getPaymentExpiresAt())) {
            order.setPaymentStatus(PaymentStatus.EXPIRED);
            order.setPaymentMethod("UNPAID");
            order.setPaid(false);
            order.setPaymentAttemptId(null);
            order.setPaymentStartedAt(null);
            Order saved = orderRepository.save(order);
            OrderResponse resp = toResponse(saved);
            realtimeNotificationService.broadcastOrderUpdate(toWs(saved, resp));
            throw new IllegalArgumentException("订单已超时，请重新下单");
        }

        if (!success) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setPaid(false);
            // allow retry: keep WAIT_PAY but clear attempt info
            order.setPaymentAttemptId(null);
            order.setPaymentStartedAt(null);
            Order saved = orderRepository.save(order);
            OrderResponse resp = toResponse(saved);
            realtimeNotificationService.broadcastOrderUpdate(toWs(saved, resp));
            return resp;
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaid(true);
        order.setStatus(OrderStatus.PENDING);
        Order saved = orderRepository.save(order);
        OrderResponse resp = toResponse(saved);
        realtimeNotificationService.broadcastOrderUpdate(toWs(saved, resp));
        return resp;
    }

    @Transactional
    public OrderResponse cancelPay(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        if (order.isPaid()) return toResponse(order);
        if (order.getStatus() != OrderStatus.WAIT_PAY) throw new IllegalArgumentException("当前订单状态不可取消支付：" + order.getStatus());
        if (order.getPaymentExpiresAt() != null && Instant.now().isAfter(order.getPaymentExpiresAt())) {
            order.setPaymentStatus(PaymentStatus.EXPIRED);
        } else {
            order.setPaymentStatus(PaymentStatus.UNPAID);
        }
        order.setPaymentMethod("UNPAID");
        order.setPaid(false);
        order.setPaymentAttemptId(null);
        order.setPaymentStartedAt(null);
        Order saved = orderRepository.save(order);
        OrderResponse resp = toResponse(saved);
        realtimeNotificationService.broadcastOrderUpdate(toWs(saved, resp));
        return resp;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrdersForCustomer(String customerName, String statusOpt) {
        if (statusOpt != null && !statusOpt.isBlank()) {
            OrderStatus status = parseStatus(statusOpt);
            return orderRepository.findByCustomerNameAndStatusOrderByCreatedAtDesc(customerName, status).stream()
                    .map(this::toResponse)
                    .toList();
        }
        return orderRepository.findByCustomerNameOrderByCreatedAtDesc(customerName).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getReceipt(Long orderId, Integer tableNo) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!Objects.equals(order.getTableNo(), tableNo)) {
            throw new IllegalArgumentException("无权限查看该订单小票");
        }
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrdersForTable(Integer tableNo, String statusOpt) {
        // 简化：直接全量拉取再过滤（10桌规模很小）；后续可加 Repository 方法优化
        return orderRepository.findAll().stream()
                .filter(o -> Objects.equals(o.getTableNo(), tableNo))
                .filter(o -> statusOpt == null || statusOpt.isBlank() || o.getStatus() == parseStatus(statusOpt))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Order> listUnpaidWaitPayOrdersForTable(Integer tableNo) {
        return orderRepository.findAll().stream()
                .filter(o -> Objects.equals(o.getTableNo(), tableNo))
                .filter(o -> o.getStatus() == OrderStatus.WAIT_PAY)
                .filter(o -> !o.isPaid())
                .filter(o -> o.getPaymentStatus() == PaymentStatus.UNPAID || o.getPaymentStatus() == PaymentStatus.FAILED)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    @Transactional
    public List<OrderResponse> settleTable(Integer tableNo, String paymentMethod, boolean success) {
        List<Order> targets = listUnpaidWaitPayOrdersForTable(tableNo);
        if (targets.isEmpty()) {
            return List.of();
        }
        if (!success) {
            // 模拟结账失败：把支付状态统一置为 FAILED（仍可再次结账）
            for (Order o : targets) {
                o.setPaymentStatus(PaymentStatus.FAILED);
                o.setPaymentMethod(paymentMethod);
                o.setPaid(false);
                o.setPaymentAttemptId(null);
                o.setPaymentStartedAt(null);
                orderRepository.save(o);
                OrderResponse resp = toResponse(o);
                realtimeNotificationService.broadcastOrderUpdate(toWs(o, resp));
            }
            return targets.stream().map(this::toResponse).toList();
        }

        // 模拟桌台合并结账成功：批量置为已支付并进入待制作
        for (Order o : targets) {
            o.setPaymentStatus(PaymentStatus.PAID);
            o.setPaymentMethod(paymentMethod);
            o.setPaid(true);
            o.setStatus(OrderStatus.PENDING);
            orderRepository.save(o);
            OrderResponse resp = toResponse(o);
            realtimeNotificationService.broadcastOrderUpdate(toWs(o, resp));
        }
        return targets.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrdersByTableNo(Integer tableNo) {
        return orderRepository.findByTableNoOrderByCreatedAtDesc(tableNo).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrdersForAdmin(String statusOpt) {
        if (statusOpt != null && !statusOpt.isBlank()) {
            OrderStatus status = parseStatus(statusOpt);
            return orderRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                    .map(this::toResponse)
                    .toList();
        }
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, String newStatus) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        OrderStatus status = parseStatus(newStatus);
        order.setStatus(status);
        Order saved = orderRepository.save(order);

        OrderResponse resp = toResponse(saved);
        realtimeNotificationService.broadcastOrderUpdate(toWs(saved, resp));
        return resp;
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getDishId(),
                        i.getDishName(),
                        i.getPortion().name(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getTotalPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getTableNo(),
                order.getDiners(),
                order.getCustomerName(),
                order.getRemark(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getAmount(),
                order.getPaymentMethod(),
                order.isPaid(),
                order.getPaymentStatus() == null ? null : order.getPaymentStatus().name(),
                order.getPaymentExpiresAt(),
                order.getPaymentAttemptId(),
                order.getPaymentStartedAt(),
                items
        );
    }

    private Portion parsePortion(String portion) {
        try {
            return Portion.valueOf(portion.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid portion: " + portion);
        }
    }

    private BigDecimal computeUnitPrice(BigDecimal base, Portion portion) {
        if (base == null) return BigDecimal.ZERO;
        if (portion == Portion.LARGE) {
            return base.multiply(new BigDecimal("1.5"));
        }
        return base;
    }

    private OrderUpdateMessage toWs(Order saved, OrderResponse resp) {
        return new OrderUpdateMessage(
                saved.getId(),
                saved.getTableNo(),
                saved.getDiners(),
                saved.getCustomerName(),
                saved.getRemark(),
                saved.getStatus().name(),
                saved.getCreatedAt(),
                saved.getAmount(),
                saved.getPaymentMethod(),
                saved.isPaid(),
                saved.getPaymentStatus() == null ? null : saved.getPaymentStatus().name(),
                saved.getPaymentExpiresAt(),
                saved.getPaymentAttemptId(),
                saved.getPaymentStartedAt(),
                resp.items()
        );
    }
}

