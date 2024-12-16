package com.example.restocknotification.domain.service;

import com.example.restocknotification.RestockNotificationApplication;
import com.example.restocknotification.domain.entity.Product;
import com.example.restocknotification.domain.entity.ProductNotificationHistory;
import com.example.restocknotification.domain.entity.ProductUserNotification;
import com.example.restocknotification.domain.entity.ProductUserNotificationHistory;
import com.example.restocknotification.domain.entity.status.RestockNotificationStatus;
import com.example.restocknotification.domain.event.ProductNotificationEventListener;
import com.example.restocknotification.domain.repository.ProductNotificationHistoryRepository;
import com.example.restocknotification.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[ProductNotificationService]")
public class ProductNotificationService {
    /**
     * 재입고 회차 1 증가시키고 알림
     * 오케이 nanotime 측정해서 처리한다.
     * 500 개씩 가져온다.(1초 이내에 끝난다고 보장 해야한다.)
     * 상품의 상태는 cucurrentMap에다가 저장하고, AtomicBoolean으로 관리한다. => 메모리가 공유되서 멀티 스레드에서 괜찮다.
     * 알림 전송은 동시성 문제를 생각해야한다. 이메소드를 여러 스레드가 실행하면 안된다.
     */
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final ProductNotificationHistoryRepository productNotificationHistoryRepository;
    private final ProductUserNotificationService productUserNotificationService;
    private final ProductNotificationHistoryService productNotificationHistoryService;
    private final ProductNotificationEventListener eventListener;

    private static final Long TASK_TIME = 1_000_000_000L;

    public void sendNotification(Long productId) throws InterruptedException {
        // 상품 조회 및 상태 확인(재고 없으면 true, 없으면 false), 즉 false일때만 알림 보내야함
        // AtomicBoolean 에 저장하고, 다른 스레드에서 상품의 상태 바꿀때 처리해주자
        AtomicBoolean isOutOfStock = new AtomicBoolean(productService.isOutOfStock(productId));
        // concurrentMap에 저장
        eventListener.addProductState(productId, isOutOfStock);

        // 재고 회차 +1 처리
        // isOutOfStock에서 product 조회 해서 1차 캐시에 남아 있음, 추가 쿼리 x
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        // 재고 회차 + 1
        int stockRound = product.increaseRestockRound();

        Long cursor = 0L;
        // 재고 있을때
        // 1초에 500개씩 처리 (nanoTime으로 처리)
        while (!isOutOfStock.get()){
            // 작업 시작 시간
            Long startTime = System.nanoTime();
            // 작업 종료 시간
            Long endTime = startTime + TASK_TIME;

            // ProductUserNotification 에서 active true 데이터들 500개씩 가져온다.
            PageRequest pageRequest = PageRequest.of(0, 500);

            // 500개씩 조회
            List<ProductUserNotification> notificationList = productUserNotificationService
                    .getProductUserNotifications(productId, cursor, pageRequest);


            // 처리할 값 없으면 break;
            if(notificationList.isEmpty()) break;

            // cursor 위치값 조정
            cursor = notificationList.get(notificationList.size() - 1).getId();

            List<ProductNotificationHistory> productNotificationHistoryList = new ArrayList<>();

            //
            saveInProcess(notificationList, productNotificationHistoryList, product, stockRound);
            //
            saveProductNotificationState(productNotificationHistoryList, isOutOfStock);

            long elapsedTime = System.nanoTime() - startTime;
            if (elapsedTime < TASK_TIME) {
                Thread.sleep((TASK_TIME - elapsedTime) / 1_000_000L);
            }

        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveInProcess(List<ProductUserNotification> notificationList, List<ProductNotificationHistory>  productNotificationHistoryList, Product product, int stockRound){
        // 우선 ProductNotificationHistory 에 알림 상태를 전송중이라고 저장해야한다.
        // => ProductNotificationHistory 엔터티 생성하고 list에 모아뒀다가 한꺼번에 db에 저장하자.
        for (ProductUserNotification notification : notificationList) {
            // ProductNotificationHistory 엔터티 만들고 전송중 이라고 하고 500개 일괄 저장
            ProductNotificationHistory productNotificationHistory =
                    ProductNotificationHistory.create(product, stockRound, RestockNotificationStatus.IN_PROGRESS, notification.getUserId());

            // list에 저장
            productNotificationHistoryList.add(productNotificationHistory);

        }
        // db 저장
        productNotificationHistoryRepository.saveAll(productNotificationHistoryList);

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveProductNotificationState(List<ProductNotificationHistory> productNotificationHistoryList, AtomicBoolean isOutOfStock){
        // 알림 전송시 매번 재고 유무 확인
        for (ProductNotificationHistory productNotificationHistory : productNotificationHistoryList) {
            // 재고 있음
            if(!isOutOfStock.get()){
                try{
                    // 정상 처리
                    productNotificationHistory.updateNotificationStatus(RestockNotificationStatus.COMPLETED);
                }catch (Exception e){
                    // 서드 파티 연동 예외 처리
                    productNotificationHistory.updateNotificationStatus(RestockNotificationStatus.CANCELED_BY_ERROR);
                }
            }
            // 재고 없음
            else if(isOutOfStock.get()){
                // 재고 없음 처리
                productNotificationHistory.updateNotificationStatus(RestockNotificationStatus.CANCELED_BY_SOLD_OUT);
            }
        }
        // 상태 변경 db 저장
        productNotificationHistoryRepository.saveAll(productNotificationHistoryList);
    }

}
