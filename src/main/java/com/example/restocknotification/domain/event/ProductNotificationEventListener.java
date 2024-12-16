package com.example.restocknotification.domain.event;

import com.example.restocknotification.domain.service.ProductNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ProductNotificationEventListener {
    // 해당 상품이 out_of_stock 인지 메모리에 저장?
    // 스레드에서도 안전해야한다. => 해당 상품이 재고가 있는지 없는지 boolean으로 관리하자. => AtomicBoolean 사용
    // Atomic 변수는 멀티 스레드 환경에서 동시성 보장
    // 상품마다 AtomicBoolean을 관리해야한다. map 사용 => 동시성 고려해서 ConcurrentHashMap 사용

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
