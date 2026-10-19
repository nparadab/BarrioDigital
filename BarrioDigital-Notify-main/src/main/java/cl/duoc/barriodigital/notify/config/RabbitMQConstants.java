package cl.duoc.barriodigital.notify.config;

/**
 * Constantes centralizadas de nombres de colas, exchanges y routing keys de RabbitMQ para Notify.
 * Cumple con el criterio de evitar valores dispersos en el código.
 */
public final class RabbitMQConstants {

    private RabbitMQConstants() {
    }

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
}
