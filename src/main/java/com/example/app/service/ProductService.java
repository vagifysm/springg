package com.example.app.service;

import com.example.app.dto.request.CreateProductRequest;
import com.example.app.dto.request.UpdateProductRequest;
import com.example.app.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    ProductResponse create(CreateProductRequest request);

    ProductResponse getById(UUID id);

    Page<ProductResponse> getAll(Pageable pageable);

    ProductResponse update(UUID id, UpdateProductRequest request);

    void delete(UUID id);
}
