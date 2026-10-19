package cl.duoc.barriodigital.admin;

import cl.duoc.barriodigital.admin.dto.CreateExchangeRequest;
import cl.duoc.barriodigital.admin.dto.CreateQueueRequest;
import cl.duoc.barriodigital.admin.service.RabbitAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RabbitAdminApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitAdminService rabbitAdminService;

    @Test
    void contextLoads() {
        assertNotNull(rabbitAdminService, "RabbitAdminService debe estar cargado en el contexto");
    }

    @Test
    void testCreateQueueValidation_EmptyName_ReturnsBadRequest() throws Exception {
        CreateQueueRequest invalidRequest = CreateQueueRequest.builder()
                .name("")
                .durable(true)
                .build();

        mockMvc.perform(post("/api/admin/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void testCreateQueueValidation_InvalidCharacters_ReturnsBadRequest() throws Exception {
        CreateQueueRequest invalidRequest = CreateQueueRequest.builder()
                .name("cola con espacios y simbolos!@#")
                .durable(true)
                .build();

        mockMvc.perform(post("/api/admin/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testCreateExchangeValidation_InvalidType_ReturnsBadRequest() throws Exception {
        CreateExchangeRequest invalidRequest = CreateExchangeRequest.builder()
                .name("ex.invalid")
                .type("unsupported_type")
                .build();

        mockMvc.perform(post("/api/admin/exchanges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testCreateQueueSuccess() throws Exception {
        when(rabbitAdmin.declareQueue(any())).thenReturn("q.test.custom");

        CreateQueueRequest validRequest = CreateQueueRequest.builder()
                .name("q.test.custom")
                .durable(true)
                .deadLetterExchange("cmd.dead.dlx")
                .deadLetterRoutingKey("custom.dlq")
                .build();

        mockMvc.perform(post("/api/admin/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.queueName").value("q.test.custom"));
    }

    @Test
    void testDeleteQueueSuccess() throws Exception {
        when(rabbitAdmin.deleteQueue("q.test.custom")).thenReturn(true);

        mockMvc.perform(delete("/api/admin/queues/q.test.custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deleted").value(true));
    }

    @Test
    void testOverviewEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ONLINE"))
                .andExpect(jsonPath("$.data.predefinedQueues").isArray());
    }
}
