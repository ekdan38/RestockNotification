package com.example.restocknotification.domain.entity;

import com.example.restocknotification.domain.entity.status.StockStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 재입고 회차
    @Column(nullable = false)
    private Integer restockRound;

    // 재고 상태 (IN_STOCK, OUT_OF_STOCK)
    // 재고 있음 없음으로 분류
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StockStatus stockStatus;

    private Product(Integer restockRound, StockStatus stockStatus) {
        this.restockRound = restockRound;
        this.stockStatus = stockStatus;
    }

    // 생성 메서드(초기값 0, OUT_OF_STOCK)
    public static Product create() {
        return new Product(0, StockStatus.OUT_OF_STOCK);
    }

    // 상품 상태 처리
    public void updateStockStatus(StockStatus status) {
        this.stockStatus = status;
    }

    public int increaseRestockRound(){
        this.restockRound++;
        return this.restockRound;
    }
}
