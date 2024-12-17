package com.example.restocknotification.domain.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ProductNotificationEventListener {
    // 상품의 상태를 다른 스레드에서 api호출을 통해 변경한다고 가정
    // 상품의 상태를 매번 DB에서 가져올 수 없다.
    // Atomic 변수는 다른 스레드에서 변경해도 값이 공유되기에 상품에 대한 상태 변경 감지 가능
    // 상품마다 AtomicBoolean을 관리해야한다. => 동시성 고려해서 ConcurrentHashMap 사용

    private final Map<Long, AtomicBoolean> productStateMap = new ConcurrentHashMap<>();

    // map에 저장
    public void addProductState(Long productId, AtomicBoolean atomicBoolean) {
        productStateMap.put(productId, atomicBoolean);
    }

    // map에서 삭제
    public void removeProductState(Long productId) {
        productStateMap.remove(productId);
    }

    @EventListener
    public void outOfStock(ProductOutOfStockEvent event){
        // map에 값이 있을때만 작돟애야한다.
        if (productStateMap.containsKey(event.getProductId())){
            AtomicBoolean isOutOfStock = productStateMap.get(event.getProductId());
            isOutOfStock.set(true);
        }
    }
}
