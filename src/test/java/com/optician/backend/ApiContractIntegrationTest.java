package com.optician.backend;

import com.optician.backend.model.Appointment;
import com.optician.backend.model.Order;
import com.optician.backend.model.Product;
import com.optician.backend.repository.AppointmentRepository;
import com.optician.backend.repository.OrderRepository;
import com.optician.backend.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks down the URLs consumed by the Angular ApiService.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void filtersProductsUsingTheCategoryPathExpectedByTheFrontend() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("Test frame")
                .brand("Test brand")
                .category("TEST_CATEGORY")
                .build());

        mockMvc.perform(get("/api/products/category/TEST_CATEGORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId()))
                .andExpect(jsonPath("$[0].category").value("TEST_CATEGORY"))
                // Le modèle sans déclinaison expose des agrégats dérivés vides
                .andExpect(jsonPath("$[0].stock").value(0))
                .andExpect(jsonPath("$[0].price").value(nullValue()));
    }

    @Test
    void updatesAnAppointmentStatusWithPutUsedByTheFrontend() throws Exception {
        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .clientName("Test client")
                .clientEmail("client@example.com")
                .appointmentDateTime(LocalDateTime.of(2026, 9, 20, 10, 30))
                .serviceType("EXAMEN_VUE")
                .storeLocation("Test store")
                .status("EN_ATTENTE")
                .build());

        mockMvc.perform(put("/api/appointments/{id}/status", appointment.getId())
                        .param("status", "CONFIRME")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRME"));
    }

    @Test
    void updatesAnOrderStatusWithPutUsedByTheFrontend() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .orderReference("TEST-ORDER")
                .clientName("Test client")
                .totalAmount(100.0)
                .status("EN_PREPARATION")
                .build());

        mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                        .param("status", "EXPEDIEE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPEDIEE"));
    }
}
