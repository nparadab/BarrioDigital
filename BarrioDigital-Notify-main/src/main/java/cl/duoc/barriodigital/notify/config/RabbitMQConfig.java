package cl.duoc.barriodigital.notify.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static cl.duoc.barriodigital.notify.config.RabbitMQConstants.*;

/**
 * Configuración centralizada de RabbitMQ para el microservicio consumidor de Notificaciones.
 * Declara las 6 colas (3 de trabajo + 3 DLQ), exchanges y bindings, garantizando aislamiento
 * total de la infraestructura respecto a la lógica de negocio.
 */
@Configuration
public class RabbitMQConfig {

    // =========================================================================
    // EXCHANGES
    // =========================================================================

    @Bean
    public DirectExchange cmdDirectExchange() {
        return new DirectExchange(EXCHANGE_CMD_DIRECT, true, false);
    }

    @Bean
    public TopicExchange cmdTopicExchange() {
        return new TopicExchange(EXCHANGE_CMD_TOPIC, true, false);
    }

    @Bean
    public DirectExchange cmdDeadDlxExchange() {
        return new DirectExchange(EXCHANGE_CMD_DEAD_DLX, true, false);
    }

    // =========================================================================
    // COLAS PRINCIPALES (CONFIGURADAS CON DLQ ASOCIADA)
    // =========================================================================

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(QUEUE_CMD_EMAIL)
                .withArgument("x-dead-letter-exchange", EXCHANGE_CMD_DEAD_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_EMAIL_DLQ)
                .build();
    }

    @Bean
    public Queue crewQueue() {
        return QueueBuilder.durable(QUEUE_CMD_CREW)
                .withArgument("x-dead-letter-exchange", EXCHANGE_CMD_DEAD_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_CREW_DLQ)
                .build();
    }

    @Bean
    public Queue certificateQueue() {
        return QueueBuilder.durable(QUEUE_CMD_CERTIFICATE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_CMD_DEAD_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_CERTIFICATE_DLQ)
                .build();
    }

    // =========================================================================
    // COLAS DEAD LETTER (DLQ)
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
    // BINDINGS DIRECT
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
    // BINDINGS TOPIC
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
    // BINDINGS DLQ
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
    // SERIALIZACIÓN JSON Y LISTENER CONTAINER FACTORY
    // =========================================================================

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setAutoStartup(true);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
