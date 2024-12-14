package com.example.restocknotification.domain.entity;

import com.example.restocknotification.domain.entity.base.TimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductUserNotification extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Boolean isActive;

    private ProductUserNotification(Product product, Long userId, Boolean isActive) {
        this.product = product;
        this.userId = userId;
        this.isActive = isActive;
    }

    // 생성 메서드
    public static ProductUserNotification create(Product product, Long userId, Boolean isActive){
        return new ProductUserNotification(product, userId, isActive);
    }
}
