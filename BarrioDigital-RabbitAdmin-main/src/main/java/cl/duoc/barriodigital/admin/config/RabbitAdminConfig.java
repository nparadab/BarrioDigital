package cl.duoc.barriodigital.admin.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitAdmin y beans de topología inicial para BarrioDigital.
 */
@Configuration
public class RabbitAdminConfig {

    public static final String EXCHANGE_CMD_DIRECT = "cmd.direct";
    public static final String EXCHANGE_CMD_TOPIC = "cmd.topic";
    public static final String EXCHANGE_CMD_DEAD_DLX = "cmd.dead.dlx";

    public static final String QUEUE_CMD_EMAIL = "q.cmd.email";
    public static final String QUEUE_CMD_CREW = "q.cmd.crew";
    public static final String QUEUE_CMD_CERTIFICATE = "q.cmd.certificate";

    public static final String QUEUE_CMD_EMAIL_DLQ = "q.cmd.email.dlq";
    public static final String QUEUE_CMD_CREW_DLQ = "q.cmd.crew.dlq";
    public static final String QUEUE_CMD_CERTIFICATE_DLQ = "q.cmd.certificate.dlq";

    public static final String ROUTING_KEY_EMAIL_SEND = "email.send";
    public static final String ROUTING_KEY_CREW_TICKET = "crew.ticket";
    public static final String ROUTING_KEY_CERTIFICATE_GEN = "certificate.gen";

    public static final String ROUTING_KEY_EMAIL_TOPIC = "email.*";
    public static final String ROUTING_KEY_CREW_TOPIC = "crew.#";
    public static final String ROUTING_KEY_CERTIFICATE_TOPIC = "certificate.*";

    public static final String ROUTING_KEY_EMAIL_DLQ = "email.dlq";
    public static final String ROUTING_KEY_CREW_DLQ = "crew.dlq";
    public static final String ROUTING_KEY_CERTIFICATE_DLQ = "certificate.dlq";

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    // Topología base
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_CMD_DIRECT, true, false);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE_CMD_TOPIC, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_CMD_DEAD_DLX, true, false);
    }

    @Bean
    public Queue adminEmailQueue() {
        return QueueBuilder.durable(QUEUE_CMD_EMAIL)
                .withArgument("x-dead-letter-exchange", EXCHANGE_CMD_DEAD_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_EMAIL_DLQ)
                .build();
    }

    @Bean
    public Queue adminCrewQueue() {
        return QueueBuilder.durable(QUEUE_CMD_CREW)
                .withArgument("x-dead-letter-exchange", EXCHANGE_CMD_DEAD_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_CREW_DLQ)
                .build();
    }

    @Bean
    public Queue adminCertificateQueue() {
        return QueueBuilder.durable(QUEUE_CMD_CERTIFICATE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_CMD_DEAD_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_CERTIFICATE_DLQ)
                .build();
    }

    @Bean
    public Queue adminEmailDlq() {
        return QueueBuilder.durable(QUEUE_CMD_EMAIL_DLQ).build();
    }

    @Bean
    public Queue adminCrewDlq() {
        return QueueBuilder.durable(QUEUE_CMD_CREW_DLQ).build();
    }

    @Bean
    public Queue adminCertificateDlq() {
        return QueueBuilder.durable(QUEUE_CMD_CERTIFICATE_DLQ).build();
    }

    @Bean
    public Binding adminEmailDirectBinding(Queue adminEmailQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(adminEmailQueue).to(directExchange).with(ROUTING_KEY_EMAIL_SEND);
    }

    @Bean
    public Binding adminCrewDirectBinding(Queue adminCrewQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(adminCrewQueue).to(directExchange).with(ROUTING_KEY_CREW_TICKET);
    }

    @Bean
    public Binding adminCertificateDirectBinding(Queue adminCertificateQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(adminCertificateQueue).to(directExchange).with(ROUTING_KEY_CERTIFICATE_GEN);
    }

    @Bean
    public Binding adminEmailTopicBinding(Queue adminEmailQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(adminEmailQueue).to(topicExchange).with(ROUTING_KEY_EMAIL_TOPIC);
    }

    @Bean
    public Binding adminCrewTopicBinding(Queue adminCrewQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(adminCrewQueue).to(topicExchange).with(ROUTING_KEY_CREW_TOPIC);
    }

    @Bean
    public Binding adminCertificateTopicBinding(Queue adminCertificateQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(adminCertificateQueue).to(topicExchange).with(ROUTING_KEY_CERTIFICATE_TOPIC);
    }

    @Bean
    public Binding adminEmailDlqBinding(Queue adminEmailDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(adminEmailDlq).to(deadLetterExchange).with(ROUTING_KEY_EMAIL_DLQ);
    }

    @Bean
    public Binding adminCrewDlqBinding(Queue adminCrewDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(adminCrewDlq).to(deadLetterExchange).with(ROUTING_KEY_CREW_DLQ);
    }

    @Bean
    public Binding adminCertificateDlqBinding(Queue adminCertificateDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(adminCertificateDlq).to(deadLetterExchange).with(ROUTING_KEY_CERTIFICATE_DLQ);
    }
}
