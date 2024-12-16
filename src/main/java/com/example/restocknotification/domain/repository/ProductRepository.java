package com.example.restocknotification.domain.repository;

import com.example.restocknotification.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
