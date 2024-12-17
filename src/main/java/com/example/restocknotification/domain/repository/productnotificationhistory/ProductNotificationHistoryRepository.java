package com.example.restocknotification.domain.repository.productnotificationhistory;

import com.example.restocknotification.domain.entity.ProductNotificationHistory;
import com.example.restocknotification.domain.entity.ProductUserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductNotificationHistoryRepository extends JpaRepository<ProductNotificationHistory, Long> {
    // cursor 기반, notification_status가  IN_PROGRESS인 데이터들 가져온다.
    @Query("SELECT pnh " +
            "FROM ProductNotificationHistory pnh " +
            "WHERE pnh.product.id = :productId AND pnh.notificationStatus = 'IN_PROGRESS' AND pnh.id > :cursor " +
            "ORDER BY pnh.id ASC")
    List<ProductNotificationHistory> findByProductIdAndCursorAndStatus(@Param("productId") Long productId,
                                                           @Param("cursor") Long cursor,
                                                           Pageable pageable);
}
