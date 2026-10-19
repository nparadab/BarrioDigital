package cl.duoc.barriodigital.notify.consumer.dlq;

import cl.duoc.barriodigital.notify.config.RabbitMQConstants;
import cl.duoc.barriodigital.notify.service.DeadLetterAuditService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Consumidor especializado para el monitoreo y auditoría de Dead Letter Queues (DLQ).
 * Captura todos los mensajes no entregados o rechazados de las 3 colas principales,
 * registra los metadatos de fallo en logs y confirma la recepción en DLQ.
 *
 * Cumple con el requisito: "Los microservicios deben manejar mensajes no entregados enviándolos a DLQ y registrándolos en logs."
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueConsumer {

    private final DeadLetterAuditService deadLetterAuditService;

    @RabbitListener(
            queues = {
                    RabbitMQConstants.QUEUE_CMD_EMAIL_DLQ,
                    RabbitMQConstants.QUEUE_CMD_CREW_DLQ,
                    RabbitMQConstants.QUEUE_CMD_CERTIFICATE_DLQ
            },
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consumeDeadLetterMessage(
            Message message,
            Channel channel,
            @Header(AmqpHeaders.CONSUMER_QUEUE) String queueName,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> headers = message.getMessageProperties().getHeaders();

        // Extraer metadatos de x-death proporcionados por RabbitMQ al enviar a DLX
        Object xDeathHeader = headers.get("x-death");
        String reason = xDeathHeader != null ? xDeathHeader.toString() : "Mensaje rechazado con NACK sin requeue";

        log.error("[DLQ-MONITOR-AUDIT] Procesando mensaje no entregado capturado en DLQ '{}'. DeliveryTag: {}. Headers: {}. Payload: {}",
                queueName, deliveryTag, headers, body);

        // Registrar en servicio de auditoría de errores
        deadLetterAuditService.logDeadLetter(queueName, body, reason, deliveryTag);

        // Confirmar ACK del mensaje en la cola DLQ tras registro seguro en log
        channel.basicAck(deliveryTag, false);
        log.info("[DLQ-MONITOR-AUDIT] Mensaje de DLQ '{}' procesado y confirmado con ACK tras registro en log.", queueName);
    }
}
