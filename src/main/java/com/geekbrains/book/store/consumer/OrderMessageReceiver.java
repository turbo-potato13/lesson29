package com.geekbrains.book.store.consumer;

import com.geekbrains.book.store.entities.Order;
import com.geekbrains.book.store.services.OrderService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderMessageReceiver {
    private final OrderService orderService;

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
    }

    @Autowired
    public OrderMessageReceiver(OrderService orderService) {
        this.orderService = orderService;
    }

    public void receiveMessage(byte[] message) {
        System.out.println("Received from topic <" + new String(message) + ">");
        Order order = orderService.findById(Long.parseLong(new String(message)));
        order.setStatus(Order.Status.READY);
        orderService.saveOrder(order);
    }
}