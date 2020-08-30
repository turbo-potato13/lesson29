package com.geekbrains.book.store.controllers;

import com.geekbrains.book.store.beans.Cart;
import com.geekbrains.book.store.consumer.OrderMessageReceiver;
import com.geekbrains.book.store.entities.Order;
import com.geekbrains.book.store.entities.User;
import com.geekbrains.book.store.services.OrderService;
import com.geekbrains.book.store.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/orders")
@AllArgsConstructor
public class OrderController {
    public static final String EXCHANGE_FOR_PROCESSING_TASK = "processingExchanger";
    public static final String QUEUE_WITH_PROCESSING_TASK_RESULTS = "processingResultsQueue";
    private RabbitTemplate rabbitTemplate;

    private UserService userService;
    private OrderService orderService;
    private Cart cart;

    @GetMapping("/create")
    public String createOrder(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName()).get();
        Order order = new Order(user, cart);
        orderService.saveOrder(order);
        model.addAttribute("user", user);
        rabbitTemplate.convertAndSend(OrderController.EXCHANGE_FOR_PROCESSING_TASK, null,  order.getId().toString() );
        return "order_info";
    }

    @Bean
    public SimpleMessageListenerContainer containerForTopic(ConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(QUEUE_WITH_PROCESSING_TASK_RESULTS);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(OrderMessageReceiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }


    @PostMapping("/confirm")
    @ResponseBody
    public String confirmOrder(Principal principal) {
        User user = userService.findByUsername(principal.getName()).get();
        Order order = new Order(user, cart);
        order.setStatus(Order.Status.READY);
        order = orderService.saveOrder(order);
        return order.getId() + " " + order.getPrice();
    }
}
