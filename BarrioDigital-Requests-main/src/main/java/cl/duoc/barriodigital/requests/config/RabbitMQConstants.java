package cl.duoc.barriodigital.requests.config;

/**
 * Constantes centralizadas de nombres de colas, exchanges y routing keys de RabbitMQ.
 * Evita la dispersión de valores en código cumpliendo con la pauta de evaluación.
 */
public final class RabbitMQConstants {

    private RabbitMQConstants() {
        // Utility class
    }

    // EXCHANGES
    public static final String EXCHANGE_CMD_DIRECT = "cmd.direct";
    public static final String EXCHANGE_CMD_TOPIC = "cmd.topic";
    public static final String EXCHANGE_CMD_DEAD_DLX = "cmd.dead.dlx";

    // COLAS PRINCIPALES
    public static final String QUEUE_CMD_EMAIL = "q.cmd.email";
    public static final String QUEUE_CMD_CREW = "q.cmd.crew";
    public static final String QUEUE_CMD_CERTIFICATE = "q.cmd.certificate";

    // COLAS DEAD LETTER QUEUE (DLQ)
    public static final String QUEUE_CMD_EMAIL_DLQ = "q.cmd.email.dlq";
    public static final String QUEUE_CMD_CREW_DLQ = "q.cmd.crew.dlq";
    public static final String QUEUE_CMD_CERTIFICATE_DLQ = "q.cmd.certificate.dlq";

    // ROUTING KEYS DIRECT
    public static final String ROUTING_KEY_EMAIL_SEND = "email.send";
    public static final String ROUTING_KEY_CREW_TICKET = "crew.ticket";
    public static final String ROUTING_KEY_CERTIFICATE_GEN = "certificate.gen";

    // ROUTING KEYS TOPIC
    public static final String ROUTING_KEY_EMAIL_TOPIC = "email.*";
    public static final String ROUTING_KEY_CREW_TOPIC = "crew.#";
    public static final String ROUTING_KEY_CERTIFICATE_TOPIC = "certificate.*";

    // ROUTING KEYS DLQ
    public static final String ROUTING_KEY_EMAIL_DLQ = "email.dlq";
    public static final String ROUTING_KEY_CREW_DLQ = "crew.dlq";
    public static final String ROUTING_KEY_CERTIFICATE_DLQ = "certificate.dlq";

    // ARGUMENTOS DLQ
    public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
}
