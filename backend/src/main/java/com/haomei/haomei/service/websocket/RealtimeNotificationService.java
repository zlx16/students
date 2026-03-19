package com.haomei.haomei.service.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastOrderUpdate(OrderUpdateMessage msg) {
        messagingTemplate.convertAndSend("/topic/orders", msg);
    }

    public void broadcastMenuUpdate(MenuUpdateMessage msg) {
        messagingTemplate.convertAndSend("/topic/menu", msg);
    }
}

