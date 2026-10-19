package cl.duoc.barriodigital.requests.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static cl.duoc.barriodigital.requests.config.RabbitMQConstants.*;

/**
 * Configuración centralizada de RabbitMQ para el microservicio de Trámites.
 * Declara la topología completa (Exchanges, Queues, DLQs y Bindings) requerida por la pauta.
 */
@Configuration
public class RabbitMQConfig {

    // =========================================================================
    // EXCHANGES
    // =========================================================================

    /**
     * Exchange Direct principal para comandos directos específicos (email.send, crew.ticket, certificate.gen).
     */
    @Bean
    public DirectExchange cmdDirectExchange() {
        return new DirectExchange(EXCHANGE_CMD_DIRECT, true, false);
    }

    /**
     * Exchange Topic para enrutamiento por patrones flexibles (email.*, crew.#, certificate.*).
     */
    @Bean
    public TopicExchange cmdTopicExchange() {
        return new TopicExchange(EXCHANGE_CMD_TOPIC, true, false);
    }

    /**
     * Dead Letter Exchange (DLX) para recolectar mensajes rechazados o fallidos.
     */
    @Bean
    public DirectExchange cmdDeadDlxExchange() {
        return new DirectExchange(EXCHANGE_CMD_DEAD_DLX, true, false);
    }

    // =========================================================================
    // QUEUES PRINCIPALES (CON CONFIGURACIÓN DE DLQ ASOCIADA)
    // =========================================================================

    /**
     * Cola para notificaciones de Email / Push al vecino.
     */
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(QUEUE_CMD_EMAIL)
                .withArgument(X_DEAD_LETTER_EXCHANGE, EXCHANGE_CMD_DEAD_DLX)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, ROUTING_KEY_EMAIL_DLQ)
                .build();
    }

    /**
     * Cola para tickets de visita de cuadrilla en terreno.
     */
    @Bean
    public Queue crewQueue() {
        return QueueBuilder.durable(QUEUE_CMD_CREW)
                .withArgument(X_DEAD_LETTER_EXCHANGE, EXCHANGE_CMD_DEAD_DLX)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, ROUTING_KEY_CREW_DLQ)
                .build();
    }

    /**
     * Cola para generación de certificados y comprobantes PDF.
     */
    @Bean
    public Queue certificateQueue() {
        return QueueBuilder.durable(QUEUE_CMD_CERTIFICATE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, EXCHANGE_CMD_DEAD_DLX)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, ROUTING_KEY_CERTIFICATE_DLQ)
                .build();
    }

    // =========================================================================
    // QUEUES DEAD LETTER (DLQ)
    // =========================================================================

    @Bean
    public Queue emailDlqQueue() {
        return QueueBuilder.durable(QUEUE_CMD_EMAIL_DLQ).build();
    }

    @Bean
    public Queue crewDlqQueue() {
        return QueueBuilder.durable(QUEUE_CMD_CREW_DLQ).build();
    }

    @Bean
    public Queue certificateDlqQueue() {
        return QueueBuilder.durable(QUEUE_CMD_CERTIFICATE_DLQ).build();
    }

    // =========================================================================
    // BINDINGS DIRECTOS (cmd.direct)
    // =========================================================================

    @Bean
    public Binding emailDirectBinding(Queue emailQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(emailQueue).to(cmdDirectExchange).with(ROUTING_KEY_EMAIL_SEND);
    }

    @Bean
    public Binding crewDirectBinding(Queue crewQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(crewQueue).to(cmdDirectExchange).with(ROUTING_KEY_CREW_TICKET);
    }

    @Bean
    public Binding certificateDirectBinding(Queue certificateQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(certificateQueue).to(cmdDirectExchange).with(ROUTING_KEY_CERTIFICATE_GEN);
    }

    // =========================================================================
    // BINDINGS TOPIC (cmd.topic)
    // =========================================================================

    @Bean
    public Binding emailTopicBinding(Queue emailQueue, TopicExchange cmdTopicExchange) {
        return BindingBuilder.bind(emailQueue).to(cmdTopicExchange).with(ROUTING_KEY_EMAIL_TOPIC);
    }

    @Bean
    public Binding crewTopicBinding(Queue crewQueue, TopicExchange cmdTopicExchange) {
        return BindingBuilder.bind(crewQueue).to(cmdTopicExchange).with(ROUTING_KEY_CREW_TOPIC);
    }

    @Bean
    public Binding certificateTopicBinding(Queue certificateQueue, TopicExchange cmdTopicExchange) {
        return BindingBuilder.bind(certificateQueue).to(cmdTopicExchange).with(ROUTING_KEY_CERTIFICATE_TOPIC);
    }

    // =========================================================================
    // BINDINGS DLQ (cmd.dead.dlx)
    // =========================================================================

    @Bean
    public Binding emailDlqBinding(Queue emailDlqQueue, DirectExchange cmdDeadDlxExchange) {
        return BindingBuilder.bind(emailDlqQueue).to(cmdDeadDlxExchange).with(ROUTING_KEY_EMAIL_DLQ);
    }

    @Bean
    public Binding crewDlqBinding(Queue crewDlqQueue, DirectExchange cmdDeadDlxExchange) {
        return BindingBuilder.bind(crewDlqQueue).to(cmdDeadDlxExchange).with(ROUTING_KEY_CREW_DLQ);
    }

    @Bean
    public Binding certificateDlqBinding(Queue certificateDlqQueue, DirectExchange cmdDeadDlxExchange) {
        return BindingBuilder.bind(certificateDlqQueue).to(cmdDeadDlxExchange).with(ROUTING_KEY_CERTIFICATE_DLQ);
    }

    // =========================================================================
    // CONVERTER Y TEMPLATE JSON
    // =========================================================================

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
