package com.example.restocknotification.web.controller;

import com.example.restocknotification.domain.service.ProductNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[ProductNotificationController]")
public class ProductNotificationController {

    private final ProductNotificationService productNotificationService;

    // 해당 경로 요청 => 재입고 회차 + 1 시키고, 알림 전송하는 엔드포인트
    @PostMapping("/products/{productId}/notifications/re-stock")
    public ResponseEntity<?> reStockNotifications(@PathVariable("productId") Long productId) throws InterruptedException {

        productNotificationService.sendNotification(productId);
        return ResponseEntity.ok().build();
    }


    // 해당 경로 요청 => 예외에 의해서 알림 전송 실패 경우에 호출되는 엔드포인트
    // 해당 상품에 대해 마지막으로 전송 성공한 이후 유저부터 다시 알림 메시지 전송
    @PostMapping("/admin/products/{productId}/notifications/re-stock")
    public ResponseEntity<?> manualReStockNotifications(){

        return ResponseEntity.ok().build();
    }

}
