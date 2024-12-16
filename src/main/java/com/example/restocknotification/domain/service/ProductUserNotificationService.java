package com.example.restocknotification.domain.service;

import com.example.restocknotification.domain.entity.Product;
import com.example.restocknotification.domain.entity.ProductUserNotification;
import com.example.restocknotification.domain.repository.ProductUserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[ProductUserNotificationService]")
@Transactional(readOnly = true)
public class ProductUserNotificationService {

    private final ProductUserNotificationRepository productUserNotificationRepository;
    private final ProductService productService;
    // 재고 없는 상품에 대한 알림 생성
    @Transactional
    public Long create(Product product, Long userId, Boolean isActive){
        // 해당 상품에 재고가 있으면
        // 예외 발생
        if(!productService.isOutOfStock(product.getId())){
            log.error("재고가 존재하여 알림 생성 불가 {}", product.getId());
            throw new IllegalArgumentException("재고가 존재하여 알림 생성이 불가합니다.");
        }

        // 해당 상품에 재고가 없으면
        // 알림 생성
        ProductUserNotification productUserNotification = ProductUserNotification.create(product, userId, isActive);
        return productUserNotificationRepository.save(productUserNotification).getId();
    }

    public List<ProductUserNotification> getProductUserNotifications(Long productId, Long cursor, PageRequest pageRequest){
        return productUserNotificationRepository.findByProductIdAndCursor(productId, cursor, pageRequest);
    }




}
