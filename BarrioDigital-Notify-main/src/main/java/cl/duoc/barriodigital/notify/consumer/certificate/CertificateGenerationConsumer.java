package cl.duoc.barriodigital.notify.consumer.certificate;

import cl.duoc.barriodigital.notify.config.RabbitMQConstants;
import cl.duoc.barriodigital.notify.model.CertificateGenerationEvent;
import cl.duoc.barriodigital.notify.model.EventEnvelope;
import cl.duoc.barriodigital.notify.service.CertificateService;
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
 * Consumidor del dominio funcional de Certificados y Comprobantes Comunales.
 * Implementa confirmación manual explícita (ACK) y desvío a DLQ con NACK sin requeue en caso de error.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateGenerationConsumer {

    private final CertificateService certificateService;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_CMD_CERTIFICATE, containerFactory = "rabbitListenerContainerFactory")
    public void consumeCertificateMessage(
            @Payload EventEnvelope<CertificateGenerationEvent> envelope,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        String tracking = envelope.getPayload() != null ? envelope.getPayload().getTrackingNumber() : "N/A";
        log.info("[CERTIFICATE-CONSUMER] Solicitud de certificado recibida desde cola '{}'. Tracking: {}, DeliveryTag: {}, EventId: {}",
                RabbitMQConstants.QUEUE_CMD_CERTIFICATE, tracking, deliveryTag, envelope.getEventId());

        try {
            CertificateGenerationEvent payload = envelope.getPayload();
            if (payload == null) {
                throw new IllegalArgumentException("Payload de certificado nulo en envelope");
            }

            // Procesar lógica de negocio
            certificateService.processCertificateGeneration(payload);

            // Confirmación explícita (ACK)
            channel.basicAck(deliveryTag, false);
            log.info("[CERTIFICATE-CONSUMER] Certificado confirmado exitosamente con ACK. DeliveryTag: {}", deliveryTag);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("[CERTIFICATE-CONSUMER-ERROR] Error al procesar certificado para solicitud '{}': {}. Despachando a DLQ vía NACK.",
                    tracking, e.getMessage());
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception e) {
            log.error("[CERTIFICATE-CONSUMER-ERROR] Excepción inesperada procesando certificado [DeliveryTag: {}]: {}. Enviando a DLQ.",
                    deliveryTag, e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
