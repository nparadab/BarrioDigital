package cl.duoc.barriodigital.requests.messaging.producer;

import cl.duoc.barriodigital.requests.model.Request;

/**
 * Interfaz que aísla las operaciones de publicación de eventos de negocio hacia mensajería asíncrona.
 * Garantiza que la lógica de negocio no se acople a detalles de infraestructura RabbitMQ.
 */
public interface RequestEventProducer {

    void publishEmailNotification(String eventType, Request request, String messageText);

    void publishCrewTicket(Request request, String assignedCrew);

    void publishCertificateGeneration(String documentType, Request request);
}
