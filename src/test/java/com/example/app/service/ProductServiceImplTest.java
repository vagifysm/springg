package com.example.app.service;

import com.example.app.dto.request.CreateProductRequest;
import com.example.app.dto.request.UpdateProductRequest;
import com.example.app.dto.response.ProductResponse;
import com.example.app.entity.Product;
import com.example.app.exception.ConflictException;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.mapper.ProductMapper;
import com.example.app.repository.ProductRepository;
import com.example.app.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductMapper productMapper;
    @InjectMocks
    private ProductServiceImpl productService;

    private final UUID id = UUID.randomUUID();

    private ProductResponse response(String sku) {
        return new ProductResponse(id, "Laptop", "desc", sku, new BigDecimal("999.99"), 5, Instant.now(), Instant.now());
    }

    @Test
    void create_savesProductAndReturnsResponse() {
        CreateProductRequest request = new CreateProductRequest("Laptop", "desc", "SKU-1", new BigDecimal("999.99"), 5);
        Product entity = new Product();
        Product saved = new Product();
        ProductResponse expected = response("SKU-1");

        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(productMapper.toEntity(request)).thenReturn(entity);
        when(productRepository.save(entity)).thenReturn(saved);
        when(productMapper.toResponse(saved)).thenReturn(expected);

        assertThat(productService.create(request)).isEqualTo(expected);
    }

    @Test
    void create_withDuplicateSku_throwsConflictAndDoesNotSave() {
        CreateProductRequest request = new CreateProductRequest("Laptop", "desc", "SKU-1", new BigDecimal("999.99"), 5);
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request)).isInstanceOf(ConflictException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void getById_whenMissing_throwsResourceNotFound() {
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void getAll_mapsEveryEntityOfThePage() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = new Product();
        ProductResponse expected = response("SKU-1");
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findAll(pageable)).thenReturn(page);
        when(productMapper.toResponse(product)).thenReturn(expected);

        Page<ProductResponse> result = productService.getAll(pageable);

        assertThat(result.getContent()).containsExactly(expected);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void update_withSkuOwnedByAnotherProduct_throwsConflict() {
        UpdateProductRequest request = new UpdateProductRequest("Laptop", "desc", "TAKEN", new BigDecimal("10.00"), 1);
        when(productRepository.findById(id)).thenReturn(Optional.of(new Product()));
        when(productRepository.existsBySkuAndIdNot("TAKEN", id)).thenReturn(true);

        assertThatThrownBy(() -> productService.update(id, request)).isInstanceOf(ConflictException.class);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_appliesChangesAndReturnsResponse() {
        UpdateProductRequest request = new UpdateProductRequest("Laptop", "desc", "SKU-1", new BigDecimal("10.00"), 1);
        Product existing = new Product();
        ProductResponse expected = response("SKU-1");

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuAndIdNot("SKU-1", id)).thenReturn(false);
        when(productRepository.saveAndFlush(existing)).thenReturn(existing);
        when(productMapper.toResponse(existing)).thenReturn(expected);

        assertThat(productService.update(id, request)).isEqualTo(expected);
        verify(productMapper).updateEntity(request, existing);
    }

    @Test
    void delete_whenMissing_throwsResourceNotFound() {
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(id)).isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).delete(any());
    }

    @Test
    void delete_removesExistingProduct() {
        Product existing = new Product();
        when(productRepository.findById(id)).thenReturn(Optional.of(existing));

        productService.delete(id);

        verify(productRepository).delete(existing);
    }
}
