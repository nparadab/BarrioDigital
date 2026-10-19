package cl.duoc.barriodigital.requests;

import cl.duoc.barriodigital.requests.dto.CreateRequestDto;
import cl.duoc.barriodigital.requests.dto.UpdateStatusDto;
import cl.duoc.barriodigital.requests.model.Request;
import cl.duoc.barriodigital.requests.model.RequestAuditLog;
import cl.duoc.barriodigital.requests.model.RequestStatus;
import cl.duoc.barriodigital.requests.service.RequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RequestsApplicationTests {

    @Autowired
    private RequestService requestService;

    @Test
    void contextLoads() {
        assertNotNull(requestService);
    }

    @Test
    void testCreateRequestAndInitialStatus() {
        CreateRequestDto dto = CreateRequestDto.builder()
                .citizenDni("19.876.543-2")
                .citizenName("Ana Morales")
                .citizenEmail("ana.morales@vecinos.cl")
                .citizenPhone("+56988776655")
                .procedureTypeId(1L)
                .procedureName("Reparación de Luminarias Públicas")
                .description("Luz rota en plaza central")
                .address("Plaza Central sector B")
                .createdBy("Ana Morales")
                .build();

        Request created = requestService.createRequest(dto);
        assertNotNull(created.getId());
        assertNotNull(created.getTrackingNumber());
        assertEquals(RequestStatus.INGRESADO, created.getStatus());

        List<RequestAuditLog> timeline = requestService.getTimeline(created.getId());
        assertFalse(timeline.isEmpty(), "Debe existir registro en la línea de tiempo de auditoría");
        assertEquals("CREACION_SOLICITUD", timeline.get(0).getAction());
    }

    @Test
    void testBusinessRule_CannotPassToEnTerrenoWithoutAdmitir() {
        // Crear nueva solicitud en estado INGRESADO
        CreateRequestDto dto = CreateRequestDto.builder()
                .citizenDni("12.345.678-9")
                .citizenName("Carlos Rojas")
                .citizenEmail("carlos.rojas@vecinos.cl")
                .procedureTypeId(2L)
                .procedureName("Bacheo y Reparación de Calzadas")
                .description("Bache en calle principal")
                .address("Calle Principal 500")
                .build();

        Request created = requestService.createRequest(dto);
        assertEquals(RequestStatus.INGRESADO, created.getStatus());

        // Intentar pasar DIRECTAMENTE a EN_TERRENO sin haber sido ADMITIDO
        UpdateStatusDto directToField = UpdateStatusDto.builder()
                .status(RequestStatus.EN_TERRENO)
                .assignedCrew("Cuadrilla 1")
                .performedBy("funcionario@barriodigital.cl")
                .build();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            requestService.updateRequestStatus(created.getId(), directToField);
        });

        assertTrue(exception.getMessage().contains("No se puede pasar a EN_TERRENO sin antes haber sido ADMITIDO"),
                "Debe lanzar la excepción de negocio exigida por la pauta");
    }

    @Test
    void testProperFlow_AdmitirThenEnTerreno() {
        // Crear solicitud
        CreateRequestDto dto = CreateRequestDto.builder()
                .citizenDni("15.111.222-3")
                .citizenName("Patricia Silva")
                .procedureTypeId(3L)
                .procedureName("Poda de Árboles en Vía Pública")
                .description("Ramas tocando cables")
                .address("Pasaje Los Tilos 45")
                .build();

        Request created = requestService.createRequest(dto);

        // 1. Admitir solicitud
        UpdateStatusDto admitirDto = UpdateStatusDto.builder()
                .status(RequestStatus.ADMITIDO)
                .performedBy("funcionario@barriodigital.cl")
                .userRole("Funcionario")
                .comments("Admitido y validado")
                .build();
        Request admitted = requestService.updateRequestStatus(created.getId(), admitirDto);
        assertEquals(RequestStatus.ADMITIDO, admitted.getStatus());

        // 2. Ahora sí pasar a EN_TERRENO (debe ser permitido)
        UpdateStatusDto terrenoDto = UpdateStatusDto.builder()
                .status(RequestStatus.EN_TERRENO)
                .assignedCrew("Cuadrilla Aseo 2")
                .performedBy("funcionario@barriodigital.cl")
                .userRole("Funcionario")
                .comments("Despachado a terreno")
                .build();
        Request inField = requestService.updateRequestStatus(created.getId(), terrenoDto);
        assertEquals(RequestStatus.EN_TERRENO, inField.getStatus());
        assertEquals("Cuadrilla Aseo 2", inField.getAssignedCrew());
    }

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Autowired
    private cl.duoc.barriodigital.requests.messaging.producer.RequestEventProducer requestEventProducer;

    @Test
    void testRabbitMqBeansAndDlqConfiguration() {
        assertNotNull(requestEventProducer, "RequestEventProducer debe estar inyectado");

        // Verificar Exchanges
        org.springframework.amqp.core.DirectExchange directExchange = applicationContext.getBean("cmdDirectExchange", org.springframework.amqp.core.DirectExchange.class);
        assertEquals(cl.duoc.barriodigital.requests.config.RabbitMQConstants.EXCHANGE_CMD_DIRECT, directExchange.getName());

        org.springframework.amqp.core.TopicExchange topicExchange = applicationContext.getBean("cmdTopicExchange", org.springframework.amqp.core.TopicExchange.class);
        assertEquals(cl.duoc.barriodigital.requests.config.RabbitMQConstants.EXCHANGE_CMD_TOPIC, topicExchange.getName());

        org.springframework.amqp.core.DirectExchange dlxExchange = applicationContext.getBean("cmdDeadDlxExchange", org.springframework.amqp.core.DirectExchange.class);
        assertEquals(cl.duoc.barriodigital.requests.config.RabbitMQConstants.EXCHANGE_CMD_DEAD_DLX, dlxExchange.getName());

        // Verificar Colas y sus argumentos DLQ
        org.springframework.amqp.core.Queue emailQueue = applicationContext.getBean("emailQueue", org.springframework.amqp.core.Queue.class);
        assertEquals(cl.duoc.barriodigital.requests.config.RabbitMQConstants.QUEUE_CMD_EMAIL, emailQueue.getName());
        assertEquals(cl.duoc.barriodigital.requests.config.RabbitMQConstants.EXCHANGE_CMD_DEAD_DLX,
                emailQueue.getArguments().get(cl.duoc.barriodigital.requests.config.RabbitMQConstants.X_DEAD_LETTER_EXCHANGE));
        assertEquals(cl.duoc.barriodigital.requests.config.RabbitMQConstants.ROUTING_KEY_EMAIL_DLQ,
                emailQueue.getArguments().get(cl.duoc.barriodigital.requests.config.RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY));

        org.springframework.amqp.core.Queue crewQueue = applicationContext.getBean("crewQueue", org.springframework.amqp.core.Queue.class);
        assertEquals(cl.duoc.barriodigital.requests.config.RabbitMQConstants.QUEUE_CMD_CREW, crewQueue.getName());
        assertEquals(cl.duoc.barriodigital.requests.config.RabbitMQConstants.EXCHANGE_CMD_DEAD_DLX,
                crewQueue.getArguments().get(cl.duoc.barriodigital.requests.config.RabbitMQConstants.X_DEAD_LETTER_EXCHANGE));

        org.springframework.amqp.core.Queue certQueue = applicationContext.getBean("certificateQueue", org.springframework.amqp.core.Queue.class);
        assertEquals(cl.duoc.barriodigital.requests.config.RabbitMQConstants.QUEUE_CMD_CERTIFICATE, certQueue.getName());

        // Verificar colas DLQ
        assertNotNull(applicationContext.getBean("emailDlqQueue", org.springframework.amqp.core.Queue.class));
        assertNotNull(applicationContext.getBean("crewDlqQueue", org.springframework.amqp.core.Queue.class));
        assertNotNull(applicationContext.getBean("certificateDlqQueue", org.springframework.amqp.core.Queue.class));
    }
}
