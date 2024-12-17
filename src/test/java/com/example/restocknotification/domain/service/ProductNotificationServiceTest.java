package com.example.restocknotification.domain.service;

import com.example.restocknotification.domain.entity.Product;
import com.example.restocknotification.domain.entity.ProductNotificationHistory;
import com.example.restocknotification.domain.entity.ProductUserNotification;
import com.example.restocknotification.domain.entity.status.RestockNotificationStatus;
import com.example.restocknotification.domain.entity.status.StockStatus;
import com.example.restocknotification.domain.repository.productnotificationhistory.ProductNotificationHistoryRepository;
import com.example.restocknotification.domain.repository.ProductUserNotificationRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@SpringBootTest
class ProductNotificationServiceTest {

    @Autowired
    ProductNotificationService productNotificationService;

    @Autowired
    ProductService productService;

    @Autowired
    ProductUserNotificationService productUserNotificationService;

    @Autowired
    ProductNotificationHistoryRepository productNotificationHistoryRepository;

    @Autowired
    ProductUserNotificationRepository productUserNotificationRepository;


    private Product product;

    @BeforeEach
    void insertData() {
        product = productService.create();
    }


    @Test
    public void ProductNotificationServiceTest() throws InterruptedException {
        //given
        List<ProductUserNotification> lists = new ArrayList<>();
        for (long i = 0; i < 2000; i++) {
            if (i % 2 == 0) lists.add(ProductUserNotification.create(product, i + 1, true));
            else lists.add(ProductUserNotification.create(product, i + 1, false));
        }
        productUserNotificationRepository.saveAll(lists);

        productService.updateStockStatus(product.getId(), StockStatus.IN_STOCK);

        // 중간에 상태 바뀌는거 테스트
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            try {
                // 일단 대기
                countDownLatch.await();

                // 1초에 500개 보장된다. 따라서 약 1초 넘게 대기했다가 상품 상태 변경
                Thread.sleep(1100);
                productService.updateStockStatus(product.getId(), StockStatus.OUT_OF_STOCK);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        //when
        thread.start();
        countDownLatch.countDown();
        productNotificationService.sendNotification(product.getId());
        thread.join();

        //then
        List<ProductNotificationHistory> list = productNotificationHistoryRepository.findAll();
        List<ProductNotificationHistory> completedList = list.stream().filter(productNotificationHistory ->
                productNotificationHistory.getNotificationStatus() == RestockNotificationStatus.COMPLETED).collect(Collectors.toList());
        List<ProductNotificationHistory> otherList = list.stream().filter(productNotificationHistory ->
                productNotificationHistory.getNotificationStatus() != RestockNotificationStatus.COMPLETED).collect(Collectors.toList());
        Assertions.assertThat(completedList.size()).isEqualTo(500);
        Assertions.assertThat(otherList.size()).isEqualTo(500);
    }



}