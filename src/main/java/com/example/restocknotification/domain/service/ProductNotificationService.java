package com.example.restocknotification.domain.service;

import com.example.restocknotification.domain.entity.Product;
import com.example.restocknotification.domain.entity.ProductNotificationHistory;
import com.example.restocknotification.domain.entity.ProductUserNotification;
import com.example.restocknotification.domain.entity.status.RestockNotificationStatus;
import com.example.restocknotification.domain.event.ProductNotificationEventListener;
import com.example.restocknotification.domain.repository.productnotificationhistory.ProductNotificationHistoryBulkRepository;
import com.example.restocknotification.domain.repository.productnotificationhistory.ProductNotificationHistoryRepository;
import com.example.restocknotification.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final ProductNotificationHistoryRepository productNotificationHistoryRepository;
    private final ProductUserNotificationService productUserNotificationService;
    private final ProductNotificationHistoryBulkRepository productNotificationHistoryBulkRepository;
    private final ProductNotificationEventListener eventListener;

    private static final Long TASK_TIME = 1_000_000_000L;
    private static Long lastSuccessUserId = 0L;


    @Transactional
    public void sendNotification(Long productId) throws InterruptedException {
        // 상품 조회 및 상태 확인(재고 없으면 true, 없으면 false), 즉 false일때만 알림 보내야함
        // 상품의 상태가 바뀌다는건 다른 스레드에서 상품 변경 api를 통해서 변경한다고 가정한다.
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
        Long notificationCursor = 0L;
        // 재고 있을때
        // 1초에 500개씩 처리 (nanoTime으로 처리)
        while (!isOutOfStock.get()){
            // 작업 시작 시간
            Long startTime = System.nanoTime();
            log.info("StartTime : {}", startTime);

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

            saveInProcess(notificationList, productNotificationHistoryList, product, stockRound);

            notificationCursor = saveProductNotificationState(product, notificationCursor, isOutOfStock);


            long endTime = System.nanoTime();
            long elapsedTime = endTime - startTime;
            log.info("elapsedTime : {}", elapsedTime );
            if (elapsedTime < TASK_TIME) {
                Thread.sleep((TASK_TIME - elapsedTime) / 1_000_000L);
            }

        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveInProcess(List<ProductUserNotification> notificationList, List<ProductNotificationHistory>  productNotificationHistoryList, Product product, int stockRound){
        // 우선 ProductNotificationHistory 에 알림 상태를 전송중이라고 저장
        // => ProductNotificationHistory 엔터티 생성하고 list에 모아뒀다가 한꺼번에 db에 저장
        for (ProductUserNotification notification : notificationList) {
            // ProductNotificationHistory 엔터티 만들고 전송중 이라고 하고 500개 일괄 저장
            ProductNotificationHistory productNotificationHistory =
                    ProductNotificationHistory.create(product, stockRound, RestockNotificationStatus.IN_PROGRESS, notification.getUserId());

            // list에 저장
            productNotificationHistoryList.add(productNotificationHistory);
        }
        // db 저장
        productNotificationHistoryBulkRepository.saveAll(productNotificationHistoryList);

    }

    @Transactional
    public Long saveProductNotificationState(Product product, Long cursor, AtomicBoolean isOutOfStock){
        // ProductNotificationHistory 에서 IN_PROGRESS 로 저장한 데이터들 가져온다.
        // IN_PROGRESS 처리 과정에서 JdbcTemplate 사용해서 Id 값이 반환되지 않는다. 따라서 500개씩 다시 조회 한다.
        // 기존 처리 과정 약 4초  =>  현재 약 0.4초
        PageRequest pageRequest = PageRequest.of(0, 500);
        List<ProductNotificationHistory> lists =
                productNotificationHistoryRepository.findByProductIdAndCursorAndStatus(product.getId(), cursor, pageRequest);
        // 알림 전송시 매번 재고 유무 확인
        log.info("saveProductNotificationState 실행 시작");

        for (ProductNotificationHistory productNotificationHistory : lists) {
            // 재고 있음
            if(!isOutOfStock.get()){
                try{
                    // 정상 처리
                    productNotificationHistory.updateNotificationStatus(RestockNotificationStatus.COMPLETED);
                    // 마지막 발송 userId 업데이트
                    lastSuccessUserId = productNotificationHistory.getUserId();
                    productNotificationHistory.updateUserId(lastSuccessUserId);

                }catch (Exception e){
                    // 서드 파티 연동 예외 처리
                    productNotificationHistory.updateNotificationStatus(RestockNotificationStatus.CANCELED_BY_ERROR);
                    // 마지막 발송 userId 업데이트
                    productNotificationHistory.updateUserId(lastSuccessUserId);
                }
            }
            // 재고 없음
            else if(isOutOfStock.get()){
                // 재고 없음 처리
                productNotificationHistory.updateNotificationStatus(RestockNotificationStatus.CANCELED_BY_SOLD_OUT);
                // 마지막 발송 userId 업데이트
                productNotificationHistory.updateUserId(lastSuccessUserId);
            }
        }
        // 상태 변경 db 저장
        productNotificationHistoryRepository.saveAll(lists);
        return lists.get(lists.size() - 1).getId();
    }

}
