package com.example.restocknotification.domain.event;

import lombok.Getter;

@Getter
public class ProductOutOfStockEvent {
    private final Long productId;

    public ProductOutOfStockEvent(Long productId) {
        this.productId = productId;
    }
}
