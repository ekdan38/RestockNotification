package com.example.restocknotification.domain.service;

import com.example.restocknotification.domain.entity.Product;
import com.example.restocknotification.domain.entity.status.StockStatus;
import com.example.restocknotification.domain.event.ProductOutOfStockEvent;
import com.example.restocknotification.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[ProductService]")
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Product create(){
        Product product = Product.create();
        return productRepository.save(product);
    }

    // 재고가 없으면 true
    public Boolean isOutOfStock(Long productId){
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        return product.getStockStatus() == StockStatus.OUT_OF_STOCK;
    }

    // 상품 상태 변경
    @Transactional
    public void updateStockStatus(Long productId, StockStatus status){
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        product.updateStockStatus(status);

        // 재고 있음으로 변경한거면
        if(status == StockStatus.OUT_OF_STOCK){
            // 이벤트 발행 => 이미 알림을 전송중인 스레드가 있다면 상태가 바뀌었다고 알려줘야한다.
            eventPublisher.publishEvent(new ProductOutOfStockEvent(productId));
        }
    }
}
