package com.entitykart.cartservice.event;

import com.entitykart.cartservice.dto.CartCheckoutEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CartCheckoutPublisher {

    private static final String CART_CHECKOUT_TOPIC = "cart-checkout-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(CartCheckoutEvent event) {
        kafkaTemplate.send(CART_CHECKOUT_TOPIC, event);
    }
}
