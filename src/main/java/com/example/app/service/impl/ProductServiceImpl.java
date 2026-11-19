package com.example.app.service.impl;

import com.example.app.dto.request.CreateProductRequest;
import com.example.app.dto.request.UpdateProductRequest;
import com.example.app.dto.response.ProductResponse;
import com.example.app.entity.Product;
import com.example.app.exception.ConflictException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.mapper.ProductMapper;
import com.example.app.repository.ProductRepository;
import com.example.app.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final String RESOURCE = "Product";

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ConflictException(skuTakenMessage(request.sku()));
        }
        Product saved = productRepository.save(productMapper.toEntity(request));
        log.info("Created product id={} sku={}", saved.getId(), saved.getSku());
        return productMapper.toResponse(saved);
    }

    @Override
    public ProductResponse getById(UUID id) {
        return productMapper.toResponse(findOrThrow(id));
    }

    @Override
    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = findOrThrow(id);
        if (productRepository.existsBySkuAndIdNot(request.sku(), id)) {
            throw new ConflictException(skuTakenMessage(request.sku()));
        }
        productMapper.updateEntity(request, product);
        // saveAndFlush so audit fields (updatedAt/updatedBy) are populated before we build the response.
        Product saved = productRepository.saveAndFlush(product);
        log.info("Updated product id={}", id);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        productRepository.delete(findOrThrow(id));
        log.info("Deleted product id={}", id);
    }

    private Product findOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private static String skuTakenMessage(String sku) {
        return "Product with SKU '%s' already exists".formatted(sku);
    }
}
