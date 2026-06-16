package com.entitykart.productservice.service;

import com.entitykart.productservice.dto.StockUpdateEvent;
import com.entitykart.productservice.entity.ProductEntity;
import com.entitykart.productservice.exception.ProductNotFoundException;
import com.entitykart.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockConsumer {

    private final ProductRepository productRepository;

    @KafkaListener(topics = "stock-updates", groupId = "product-service-group")
    @Transactional
    public void handleStockUpdate(StockUpdateEvent event) {
        ProductEntity product = productRepository.findById(event.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + event.getProductId()));

        int quantity = event.getQuantity() == null ? 0 : event.getQuantity();
        int currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();

        if ("ORDER_PLACED".equals(event.getEventType())) {
            int newStock = currentStock - quantity;
            if (newStock < 0) {
                log.error(
                        "Insufficient stock for product {}: requested {}, available {}",
                        event.getProductId(),
                        quantity,
                        currentStock);
                throw new RuntimeException("Stock insufficient");
            }
            product.setStockQuantity(newStock);
        } else if ("ORDER_CANCELLED".equals(event.getEventType())) {
            product.setStockQuantity(currentStock + quantity);
        } else {
            throw new RuntimeException("Unsupported stock event type: " + event.getEventType());
        }

        productRepository.save(product);
        log.info("Stock updated for product {}: new stock = {}", event.getProductId(), product.getStockQuantity());
    }
}
