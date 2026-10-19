package cl.duoc.barriodigital.admin.service;

import cl.duoc.barriodigital.admin.config.RabbitAdminConfig;
import cl.duoc.barriodigital.admin.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Servicio dedicado para encapsular todas las operaciones de administración sobre RabbitMQ.
 * Los controladores REST consumen únicamente estos métodos de alto nivel sin interactuar
 * con clases de bajo nivel ni detalles internos de la conexión RabbitMQ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitAdminService {

    private final RabbitAdmin rabbitAdmin;

    /**
     * Crea una nueva cola en RabbitMQ con soporte para configuración de DLQ y TTL.
     */
    public String createQueue(CreateQueueRequest request) {
        log.info("Creando cola '{}' (durable={}, exclusive={}, autoDelete={})",
                request.getName(), request.isDurable(), request.isExclusive(), request.isAutoDelete());

        QueueBuilder builder;
        if (request.isDurable()) {
            builder = QueueBuilder.durable(request.getName());
        } else {
            builder = QueueBuilder.nonDurable(request.getName());
        }

        if (request.isExclusive()) {
            builder.exclusive();
        }
        if (request.isAutoDelete()) {
            builder.autoDelete();
        }

        if (request.getDeadLetterExchange() != null && !request.getDeadLetterExchange().isBlank()) {
            builder.withArgument("x-dead-letter-exchange", request.getDeadLetterExchange());
        }
        if (request.getDeadLetterRoutingKey() != null && !request.getDeadLetterRoutingKey().isBlank()) {
            builder.withArgument("x-dead-letter-routing-key", request.getDeadLetterRoutingKey());
        }
        if (request.getMessageTtl() != null && request.getMessageTtl() > 0) {
            builder.ttl(request.getMessageTtl());
        }

        Queue queue = builder.build();
        String declared = rabbitAdmin.declareQueue(queue);
        log.info("Cola '{}' declarada exitosamente en RabbitMQ", declared);
        return declared != null ? declared : request.getName();
    }

    /**
     * Elimina una cola existente en RabbitMQ.
     */
    public boolean deleteQueue(String queueName, boolean ifUnused, boolean ifEmpty) {
        log.info("Eliminando cola '{}' (ifUnused={}, ifEmpty={})", queueName, ifUnused, ifEmpty);
        if (!ifUnused && !ifEmpty) {
            boolean deleted = rabbitAdmin.deleteQueue(queueName);
            log.info("Resultado eliminación cola '{}': {}", queueName, deleted);
            return deleted;
        } else {
            rabbitAdmin.deleteQueue(queueName, ifUnused, ifEmpty);
            log.info("Cola '{}' eliminada condicionalmente", queueName);
            return true;
        }
    }

    /**
     * Purga todos los mensajes contenidos en una cola.
     */
    public int purgeQueue(String queueName) {
        log.info("Purgando mensajes de cola '{}'", queueName);
        rabbitAdmin.purgeQueue(queueName, false);
        return 1;
    }

    /**
     * Obtiene información y estadísticas básicas de una cola.
     */
    public QueueInfoDto getQueueInfo(String queueName) {
        log.info("Consultando propiedades de la cola '{}'", queueName);
        Properties props = rabbitAdmin.getQueueProperties(queueName);
        if (props == null) {
            return QueueInfoDto.builder()
                    .name(queueName)
                    .exists(false)
                    .messageCount(0)
                    .consumerCount(0)
                    .arguments(Collections.emptyMap())
                    .build();
        }

        Integer messageCount = (Integer) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        Integer consumerCount = (Integer) props.get(RabbitAdmin.QUEUE_CONSUMER_COUNT);

        Map<String, Object> arguments = new HashMap<>();
        props.forEach((k, v) -> arguments.put(String.valueOf(k), v));

        return QueueInfoDto.builder()
                .name(queueName)
                .exists(true)
                .messageCount(messageCount != null ? messageCount : 0)
                .consumerCount(consumerCount != null ? consumerCount : 0)
                .arguments(arguments)
                .build();
    }

    /**
     * Crea un nuevo exchange (direct, topic, fanout o headers).
     */
    public void createExchange(CreateExchangeRequest request) {
        log.info("Creando exchange '{}' de tipo '{}' (durable={}, autoDelete={})",
                request.getName(), request.getType(), request.isDurable(), request.isAutoDelete());

        Exchange exchange;
        String type = request.getType().toLowerCase();
        switch (type) {
            case "direct" -> exchange = new DirectExchange(request.getName(), request.isDurable(), request.isAutoDelete());
            case "topic" -> exchange = new TopicExchange(request.getName(), request.isDurable(), request.isAutoDelete());
            case "fanout" -> exchange = new FanoutExchange(request.getName(), request.isDurable(), request.isAutoDelete());
            case "headers" -> exchange = new HeadersExchange(request.getName(), request.isDurable(), request.isAutoDelete());
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + request.getType());
        }

        rabbitAdmin.declareExchange(exchange);
        log.info("Exchange '{}' declarado exitosamente en RabbitMQ", request.getName());
    }

    /**
     * Elimina un exchange de RabbitMQ.
     */
    public boolean deleteExchange(String exchangeName, boolean ifUnused) {
        log.info("Eliminando exchange '{}' (ifUnused={})", exchangeName, ifUnused);
        boolean deleted = rabbitAdmin.deleteExchange(exchangeName);
        log.info("Resultado eliminación exchange '{}': {}", exchangeName, deleted);
        return deleted;
    }

    /**
     * Crea un binding entre una cola/exchange y un exchange origen con un routing key.
     */
    public void createBinding(CreateBindingRequest request) {
        log.info("Creando binding: Destino='{}' -> Exchange='{}' [RoutingKey='{}']",
                request.getDestination(), request.getExchange(), request.getRoutingKey());

        Binding.DestinationType destType = "EXCHANGE".equalsIgnoreCase(request.getDestinationType())
                ? Binding.DestinationType.EXCHANGE
                : Binding.DestinationType.QUEUE;

        Binding binding = new Binding(
                request.getDestination(),
                destType,
                request.getExchange(),
                request.getRoutingKey(),
                Collections.emptyMap()
        );

        rabbitAdmin.declareBinding(binding);
        log.info("Binding declarado exitosamente");
    }

    /**
     * Elimina un binding en RabbitMQ.
     */
    public void deleteBinding(DeleteBindingRequest request) {
        log.info("Eliminando binding: Destino='{}' -> Exchange='{}' [RoutingKey='{}']",
                request.getDestination(), request.getExchange(), request.getRoutingKey());

        Binding binding = new Binding(
                request.getDestination(),
                Binding.DestinationType.QUEUE,
                request.getExchange(),
                request.getRoutingKey(),
                Collections.emptyMap()
        );

        rabbitAdmin.removeBinding(binding);
        log.info("Binding eliminado exitosamente");
    }

    /**
     * Retorna el resumen de la topología predeterminada del caso BarrioDigital.
     */
    public AdminTopologySummaryDto getTopologyOverview() {
        return AdminTopologySummaryDto.builder()
                .status("ONLINE")
                .timestamp(LocalDateTime.now())
                .predefinedExchanges(List.of(
                        Map.of("name", RabbitAdminConfig.EXCHANGE_CMD_DIRECT, "type", "direct", "durable", true),
                        Map.of("name", RabbitAdminConfig.EXCHANGE_CMD_TOPIC, "type", "topic", "durable", true),
                        Map.of("name", RabbitAdminConfig.EXCHANGE_CMD_DEAD_DLX, "type", "direct (DLX)", "durable", true)
                ))
                .predefinedQueues(List.of(
                        Map.of("name", RabbitAdminConfig.QUEUE_CMD_EMAIL, "dlq", RabbitAdminConfig.QUEUE_CMD_EMAIL_DLQ, "purpose", "Notificaciones de correo/push al vecino"),
                        Map.of("name", RabbitAdminConfig.QUEUE_CMD_CREW, "dlq", RabbitAdminConfig.QUEUE_CMD_CREW_DLQ, "purpose", "Ticket de visita a cuadrilla en terreno"),
                        Map.of("name", RabbitAdminConfig.QUEUE_CMD_CERTIFICATE, "dlq", RabbitAdminConfig.QUEUE_CMD_CERTIFICATE_DLQ, "purpose", "Generación de comprobantes y certificados"),
                        Map.of("name", RabbitAdminConfig.QUEUE_CMD_EMAIL_DLQ, "type", "DLQ", "purpose", "Dead Letter Queue para emails fallidos"),
                        Map.of("name", RabbitAdminConfig.QUEUE_CMD_CREW_DLQ, "type", "DLQ", "purpose", "Dead Letter Queue para tickets de cuadrilla fallidos"),
                        Map.of("name", RabbitAdminConfig.QUEUE_CMD_CERTIFICATE_DLQ, "type", "DLQ", "purpose", "Dead Letter Queue para certificados fallidos")
                ))
                .predefinedBindings(List.of(
                        Map.of("queue", RabbitAdminConfig.QUEUE_CMD_EMAIL, "exchange", RabbitAdminConfig.EXCHANGE_CMD_DIRECT, "routingKey", RabbitAdminConfig.ROUTING_KEY_EMAIL_SEND),
                        Map.of("queue", RabbitAdminConfig.QUEUE_CMD_EMAIL, "exchange", RabbitAdminConfig.EXCHANGE_CMD_TOPIC, "routingKey", RabbitAdminConfig.ROUTING_KEY_EMAIL_TOPIC),
                        Map.of("queue", RabbitAdminConfig.QUEUE_CMD_EMAIL_DLQ, "exchange", RabbitAdminConfig.EXCHANGE_CMD_DEAD_DLX, "routingKey", RabbitAdminConfig.ROUTING_KEY_EMAIL_DLQ),
                        Map.of("queue", RabbitAdminConfig.QUEUE_CMD_CREW, "exchange", RabbitAdminConfig.EXCHANGE_CMD_DIRECT, "routingKey", RabbitAdminConfig.ROUTING_KEY_CREW_TICKET),
                        Map.of("queue", RabbitAdminConfig.QUEUE_CMD_CREW, "exchange", RabbitAdminConfig.EXCHANGE_CMD_TOPIC, "routingKey", RabbitAdminConfig.ROUTING_KEY_CREW_TOPIC),
                        Map.of("queue", RabbitAdminConfig.QUEUE_CMD_CREW_DLQ, "exchange", RabbitAdminConfig.EXCHANGE_CMD_DEAD_DLX, "routingKey", RabbitAdminConfig.ROUTING_KEY_CREW_DLQ),
                        Map.of("queue", RabbitAdminConfig.QUEUE_CMD_CERTIFICATE, "exchange", RabbitAdminConfig.EXCHANGE_CMD_DIRECT, "routingKey", RabbitAdminConfig.ROUTING_KEY_CERTIFICATE_GEN),
                        Map.of("queue", RabbitAdminConfig.QUEUE_CMD_CERTIFICATE, "exchange", RabbitAdminConfig.EXCHANGE_CMD_TOPIC, "routingKey", RabbitAdminConfig.ROUTING_KEY_CERTIFICATE_TOPIC),
                        Map.of("queue", RabbitAdminConfig.QUEUE_CMD_CERTIFICATE_DLQ, "exchange", RabbitAdminConfig.EXCHANGE_CMD_DEAD_DLX, "routingKey", RabbitAdminConfig.ROUTING_KEY_CERTIFICATE_DLQ)
                ))
                .build();
    }
}
