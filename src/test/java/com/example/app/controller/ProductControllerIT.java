package com.example.app.controller;

import com.example.app.enums.Role;
import com.example.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "S3cure-passw0rd";
    private static final String BASE = "/api/v1/products";

    private String adminToken;
    private String userToken;

    @BeforeEach
    void createUsersAndTokens() throws Exception {
        createUser("admin@example.com", PASSWORD, Role.ADMIN);
        createUser("user@example.com", PASSWORD, Role.USER);
        adminToken = loginAndGetToken("admin@example.com", PASSWORD);
        userToken = loginAndGetToken("user@example.com", PASSWORD);
    }

    private static Map<String, Object> payload(String name, String sku, double price) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("description", "A fine product");
        body.put("sku", sku);
        body.put("price", price);
        body.put("stockQuantity", 10);
        return body;
    }

    private String createProduct(String name, String sku, double price) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload(name, sku, price))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    @Test
    void anyEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    void create_asUser_returns403() throws Exception {
        mockMvc.perform(post(BASE)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Laptop", "LAP-1", 999.99))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void create_asAdmin_returns201WithLocation() throws Exception {
        mockMvc.perform(post(BASE)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Laptop", "LAP-1", 999.99))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.sku").value("LAP-1"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    void create_withDuplicateSku_returns409() throws Exception {
        createProduct("Laptop", "LAP-1", 999.99);

        mockMvc.perform(post(BASE)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Other", "LAP-1", 5.0))))
                .andExpect(status().isConflict());
    }

    @Test
    void create_withInvalidPayload_returns400() throws Exception {
        Map<String, Object> body = payload("", "bad sku!", -1.0);

        mockMvc.perform(post(BASE)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(3)));
    }

    @Test
    void getById_asUser_returnsProduct() throws Exception {
        String id = createProduct("Laptop", "LAP-1", 999.99);

        mockMvc.perform(get(BASE + "/" + id).header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.name").value("Laptop"));
    }

    @Test
    void getById_unknownId_returns404() throws Exception {
        mockMvc.perform(get(BASE + "/" + UUID.randomUUID()).header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getById_malformedId_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/not-a-uuid").header("Authorization", bearer(userToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_supportsPaginationAndSorting() throws Exception {
        createProduct("Cheap", "SKU-A", 10.0);
        createProduct("Pricey", "SKU-B", 30.0);
        createProduct("Medium", "SKU-C", 20.0);

        mockMvc.perform(get(BASE)
                        .param("page", "0").param("size", "2").param("sort", "price,desc")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].sku").value("SKU-B"))
                .andExpect(jsonPath("$.data.content[1].sku").value("SKU-C"))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false));
    }

    @Test
    void getAll_withUnknownSortProperty_returns400() throws Exception {
        mockMvc.perform(get(BASE).param("sort", "doesNotExist,asc")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_asAdmin_replacesProduct() throws Exception {
        String id = createProduct("Laptop", "LAP-1", 999.99);

        mockMvc.perform(put(BASE + "/" + id)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Laptop Pro", "LAP-1", 1299.0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Laptop Pro"));
    }

    @Test
    void update_asUser_returns403() throws Exception {
        String id = createProduct("Laptop", "LAP-1", 999.99);

        mockMvc.perform(put(BASE + "/" + id)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Hacked", "LAP-1", 1.0))))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_asAdmin_removesProduct() throws Exception {
        String id = createProduct("Laptop", "LAP-1", 999.99);

        mockMvc.perform(delete(BASE + "/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_asUser_returns403() throws Exception {
        String id = createProduct("Laptop", "LAP-1", 999.99);

        mockMvc.perform(delete(BASE + "/" + id).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }
}
