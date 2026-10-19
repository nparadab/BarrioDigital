package cl.duoc.barriodigital.notify.consumer.crew;

import cl.duoc.barriodigital.notify.config.RabbitMQConstants;
import cl.duoc.barriodigital.notify.model.CrewTicketEvent;
import cl.duoc.barriodigital.notify.model.EventEnvelope;
import cl.duoc.barriodigital.notify.service.CrewTicketService;
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
 * Consumidor del dominio funcional de Cuadrillas en Terreno.
 * Implementa confirmación manual explícita (ACK) y desvío a DLQ con NACK sin requeue en caso de error.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CrewTicketConsumer {

    private final CrewTicketService crewTicketService;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_CMD_CREW, containerFactory = "rabbitListenerContainerFactory")
    public void consumeCrewTicket(
            @Payload EventEnvelope<CrewTicketEvent> envelope,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        String tracking = envelope.getPayload() != null ? envelope.getPayload().getTrackingNumber() : "N/A";
        log.info("[CREW-CONSUMER] Ticket recibido desde cola '{}'. Tracking: {}, DeliveryTag: {}, EventId: {}",
                RabbitMQConstants.QUEUE_CMD_CREW, tracking, deliveryTag, envelope.getEventId());

        try {
            CrewTicketEvent payload = envelope.getPayload();
            if (payload == null) {
                throw new IllegalArgumentException("Payload de ticket de cuadrilla nulo en envelope");
            }

            // Procesar lógica de negocio
            crewTicketService.processCrewTicket(payload);

            // Confirmación explícita (ACK)
            channel.basicAck(deliveryTag, false);
            log.info("[CREW-CONSUMER] Ticket confirmado exitosamente con ACK. DeliveryTag: {}", deliveryTag);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("[CREW-CONSUMER-ERROR] Error al procesar ticket de cuadrilla para solicitud '{}': {}. Despachando a DLQ vía NACK.",
                    tracking, e.getMessage());
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception e) {
            log.error("[CREW-CONSUMER-ERROR] Excepción inesperada procesando ticket [DeliveryTag: {}]: {}. Enviando a DLQ.",
                    deliveryTag, e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
