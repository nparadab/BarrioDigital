package cl.duoc.barriodigital.notify;

import cl.duoc.barriodigital.notify.config.RabbitMQConstants;
import cl.duoc.barriodigital.notify.consumer.certificate.CertificateGenerationConsumer;
import cl.duoc.barriodigital.notify.consumer.crew.CrewTicketConsumer;
import cl.duoc.barriodigital.notify.consumer.dlq.DeadLetterQueueConsumer;
import cl.duoc.barriodigital.notify.consumer.email.EmailNotificationConsumer;
import cl.duoc.barriodigital.notify.model.CertificateGenerationEvent;
import cl.duoc.barriodigital.notify.model.CrewTicketEvent;
import cl.duoc.barriodigital.notify.model.EmailNotificationEvent;
import cl.duoc.barriodigital.notify.model.EventEnvelope;
import cl.duoc.barriodigital.notify.service.NotificationService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class NotifyApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private EmailNotificationConsumer emailConsumer;

    @Autowired
    private CrewTicketConsumer crewConsumer;

    @Autowired
    private CertificateGenerationConsumer certificateConsumer;

    @Autowired
    private DeadLetterQueueConsumer dlqConsumer;

    @Autowired
    private NotificationService notificationService;

    @Test
    void contextLoads() {
        assertNotNull(emailConsumer);
        assertNotNull(crewConsumer);
        assertNotNull(certificateConsumer);
        assertNotNull(dlqConsumer);
    }

    @Test
    void testRabbitMqConfigurationAndDlqBindingBeans() {
        // Validar exchanges declarados
        DirectExchange directEx = applicationContext.getBean("cmdDirectExchange", DirectExchange.class);
        assertEquals(RabbitMQConstants.EXCHANGE_CMD_DIRECT, directEx.getName());

        DirectExchange dlxEx = applicationContext.getBean("cmdDeadDlxExchange", DirectExchange.class);
        assertEquals(RabbitMQConstants.EXCHANGE_CMD_DEAD_DLX, dlxEx.getName());

        // Validar colas y configuración de Dead Letter Exchange en argumentos
        Queue emailQ = applicationContext.getBean("emailQueue", Queue.class);
        assertEquals(RabbitMQConstants.QUEUE_CMD_EMAIL, emailQ.getName());
        assertEquals(RabbitMQConstants.EXCHANGE_CMD_DEAD_DLX, emailQ.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RabbitMQConstants.ROUTING_KEY_EMAIL_DLQ, emailQ.getArguments().get("x-dead-letter-routing-key"));

        Queue crewQ = applicationContext.getBean("crewQueue", Queue.class);
        assertEquals(RabbitMQConstants.EXCHANGE_CMD_DEAD_DLX, crewQ.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RabbitMQConstants.ROUTING_KEY_CREW_DLQ, crewQ.getArguments().get("x-dead-letter-routing-key"));

        Queue certQ = applicationContext.getBean("certificateQueue", Queue.class);
        assertEquals(RabbitMQConstants.EXCHANGE_CMD_DEAD_DLX, certQ.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RabbitMQConstants.ROUTING_KEY_CERTIFICATE_DLQ, certQ.getArguments().get("x-dead-letter-routing-key"));

        // Validar existencia de las 3 colas DLQ
        assertNotNull(applicationContext.getBean("emailDlqQueue", Queue.class));
        assertNotNull(applicationContext.getBean("crewDlqQueue", Queue.class));
        assertNotNull(applicationContext.getBean("certificateDlqQueue", Queue.class));
    }

    @Test
    void testEmailConsumer_SuccessSendsExplicitAck() throws IOException {
        Channel mockChannel = mock(Channel.class);
        long deliveryTag = 12345L;

        EmailNotificationEvent payload = EmailNotificationEvent.builder()
                .trackingNumber("BD-2025-0010")
                .recipientEmail("vecino.exitoso@barriodigital.cl")
                .recipientName("Carlos Vecino")
                .subject("Su solicitud ha sido admitida")
                .message("Trámite admitido")
                .eventType("ADMITIDO")
                .build();

        EventEnvelope<EmailNotificationEvent> envelope = EventEnvelope.<EmailNotificationEvent>builder()
                .eventId("evt-001")
                .type("EMAIL_NOTIFICATION")
                .payload(payload)
                .build();

        emailConsumer.consumeEmailMessage(envelope, mockChannel, deliveryTag);

        // Verifica que se invoca basicAck explícito
        verify(mockChannel, times(1)).basicAck(deliveryTag, false);
        verify(mockChannel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void testEmailConsumer_ErrorSendsNackToDlqWithoutRequeue() throws IOException {
        Channel mockChannel = mock(Channel.class);
        long deliveryTag = 99999L;

        // Correo intencionalmente provocador de fallo para prueba de DLQ
        EmailNotificationEvent poisonPayload = EmailNotificationEvent.builder()
                .trackingNumber("BD-2025-FAIL")
                .recipientEmail("poison.fallo@test.cl")
                .subject("Mensaje de prueba DLQ")
                .build();

        EventEnvelope<EmailNotificationEvent> envelope = EventEnvelope.<EmailNotificationEvent>builder()
                .eventId("evt-fail-001")
                .type("EMAIL_NOTIFICATION")
                .payload(poisonPayload)
                .build();

        emailConsumer.consumeEmailMessage(envelope, mockChannel, deliveryTag);

        // Verifica que se invoca basicNack(deliveryTag, false, false) para enviarlo a la DLQ
        verify(mockChannel, times(1)).basicNack(deliveryTag, false, false);
        verify(mockChannel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void testCrewConsumer_SuccessSendsExplicitAck() throws IOException {
        Channel mockChannel = mock(Channel.class);
        long deliveryTag = 22222L;

        CrewTicketEvent payload = CrewTicketEvent.builder()
                .trackingNumber("BD-2025-0020")
                .assignedCrew("Cuadrilla Aseo 1")
                .address("Calle Los Robles 100")
                .procedureName("Reparación calzada")
                .scheduledAt(LocalDateTime.now())
                .build();

        EventEnvelope<CrewTicketEvent> envelope = EventEnvelope.<CrewTicketEvent>builder()
                .eventId("evt-002")
                .type("CREW_TICKET")
                .payload(payload)
                .build();

        crewConsumer.consumeCrewTicket(envelope, mockChannel, deliveryTag);

        verify(mockChannel, times(1)).basicAck(deliveryTag, false);
    }

    @Test
    void testCertificateConsumer_SuccessSendsExplicitAck() throws IOException {
        Channel mockChannel = mock(Channel.class);
        long deliveryTag = 33333L;

        CertificateGenerationEvent payload = CertificateGenerationEvent.builder()
                .trackingNumber("BD-2025-0030")
                .documentType("COMPROBANTE_INGRESO")
                .citizenName("Patricia Gómez")
                .citizenDni("11.222.333-4")
                .build();

        EventEnvelope<CertificateGenerationEvent> envelope = EventEnvelope.<CertificateGenerationEvent>builder()
                .eventId("evt-003")
                .type("CERTIFICATE_GENERATION")
                .payload(payload)
                .build();

        certificateConsumer.consumeCertificateMessage(envelope, mockChannel, deliveryTag);

        verify(mockChannel, times(1)).basicAck(deliveryTag, false);
    }

    @Test
    void testDeadLetterConsumer_LogsAndAcksDeadLetter() throws IOException {
        Channel mockChannel = mock(Channel.class);
        long deliveryTag = 44444L;

        MessageProperties props = new MessageProperties();
        props.setHeader("x-death", "rejected-by-consumer");
        Message message = new Message("{\"trackingNumber\":\"BD-FAIL-99\"}".getBytes(), props);

        dlqConsumer.consumeDeadLetterMessage(message, mockChannel, RabbitMQConstants.QUEUE_CMD_EMAIL_DLQ, deliveryTag);

        verify(mockChannel, times(1)).basicAck(deliveryTag, false);
    }
}
