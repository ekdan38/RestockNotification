package com.example.restocknotification.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductUserNotificationHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer restockRound;

    @Column(nullable = false)
    private LocalDateTime notificationDate;

    private ProductUserNotificationHistory(Product product, Long userId, Integer restockRound, LocalDateTime notificationDate) {
        this.product = product;
        this.userId = userId;
        this.restockRound = restockRound;
        this.notificationDate = notificationDate;
    }

    // 생성 메서드
    public static ProductUserNotificationHistory create(Product product, Long userId, Integer restockRound, LocalDateTime notificationDate){
        return new ProductUserNotificationHistory(product, userId, restockRound, notificationDate);
    }

}
