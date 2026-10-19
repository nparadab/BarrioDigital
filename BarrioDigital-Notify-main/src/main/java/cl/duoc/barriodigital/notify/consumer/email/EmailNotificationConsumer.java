package cl.duoc.barriodigital.notify.consumer.email;

import cl.duoc.barriodigital.notify.config.RabbitMQConstants;
import cl.duoc.barriodigital.notify.model.EmailNotificationEvent;
import cl.duoc.barriodigital.notify.model.EventEnvelope;
import cl.duoc.barriodigital.notify.service.NotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor del dominio funcional de Notificaciones por Email y Push.
 * Implementa confirmación manual explícita (ACK) y desvío a DLQ con NACK sin requeue en caso de error.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_CMD_EMAIL, containerFactory = "rabbitListenerContainerFactory")
    public void consumeEmailMessage(
            @Payload EventEnvelope<EmailNotificationEvent> envelope,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        String tracking = envelope.getPayload() != null ? envelope.getPayload().getTrackingNumber() : "N/A";
        log.info("[EMAIL-CONSUMER] Mensaje recibido desde cola '{}'. Tracking: {}, DeliveryTag: {}, EventId: {}",
                RabbitMQConstants.QUEUE_CMD_EMAIL, tracking, deliveryTag, envelope.getEventId());

        try {
            EmailNotificationEvent payload = envelope.getPayload();
            if (payload == null) {
                throw new IllegalArgumentException("Payload de email nulo en envelope");
            }

            // Procesar lógica de negocio
            notificationService.processEmailNotification(payload);

            // Confirmación explícita (ACK)
            channel.basicAck(deliveryTag, false);
            log.info("[EMAIL-CONSUMER] Mensaje confirmado exitosamente con ACK. DeliveryTag: {}", deliveryTag);

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Error de negocio o validación: desvío controlado directo a DLQ (NACK con requeue = false)
            log.error("[EMAIL-CONSUMER-ERROR] Error no recuperable al procesar email para trámite '{}': {}. Despachando a DLQ vía NACK.",
                    tracking, e.getMessage());
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception e) {
            // Error general: registro exhaustivo y envío a DLQ para evitar bloqueo del canal
            log.error("[EMAIL-CONSUMER-ERROR] Excepción inesperada procesando email [DeliveryTag: {}]: {}. Enviando a DLQ.",
                    deliveryTag, e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
