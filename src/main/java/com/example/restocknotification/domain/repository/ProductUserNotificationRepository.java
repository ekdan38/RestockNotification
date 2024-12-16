package com.example.restocknotification.domain.repository;

import com.example.restocknotification.domain.entity.ProductUserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductUserNotificationRepository extends JpaRepository<ProductUserNotification, Long> {

    // cursor 기반, isActive true인 데이터들 가져온다.
    @Query("SELECT pun " +
            "FROM ProductUserNotification pun " +
            "WHERE pun.product.id = :productId AND pun.isActive = true AND pun.id > :cursor " +
            "ORDER BY pun.id ASC")
    List<ProductUserNotification> findByProductIdAndCursor(@Param("productId") Long productId,
                                                             @Param("cursor") Long cursor,
                                                             Pageable pageable);
}
