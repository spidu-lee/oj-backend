package com.ln.oj.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ln
 */
@Component
public class MessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String exchange,String routingKey,String message) {
        rabbitTemplate.convertAndSend(exchange,routingKey,message);
    }

}
