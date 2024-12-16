package com.example.restocknotification.domain.entity;

import com.example.restocknotification.domain.entity.status.RestockNotificationStatus;
import com.example.restocknotification.domain.entity.status.StockStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductNotificationHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer stockRound;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RestockNotificationStatus notificationStatus;

    // 마지막 발송 유저 아이디
    @Column(nullable = false)
    private Long userId;

    private ProductNotificationHistory(Product product, Integer stockRound, RestockNotificationStatus notificationStatus, Long userId) {
        this.product = product;
        this.stockRound = stockRound;
        this.notificationStatus = notificationStatus;
        this.userId = userId;
    }

    // 생성 메서드
    public static ProductNotificationHistory create(Product product, Integer stockRound, RestockNotificationStatus notificationStatus, Long userId){
        return new ProductNotificationHistory(product, stockRound, notificationStatus, userId);
    }

    public void updateNotificationStatus(RestockNotificationStatus status){
        this.notificationStatus = status;
    }
}
