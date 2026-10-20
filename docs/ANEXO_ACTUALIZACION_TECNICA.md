# Anexo de Actualización Técnica
## Mensajería Asíncrona, Clúster RabbitMQ y Despliegue Cloud en AWS

**Proyecto:** BarrioDigital — Plataforma para Trámites Comunales y Atención Vecinal  
**Caso N°:** 4  
**Asignatura:** Desarrollo Cloud Native I (DSY1107)  
**Institución:** Duoc UC - Escuela de Informática y Telecomunicaciones  
**Docente:** José Miguel Candia  
**Integrantes:**
- Cristóbal Bustos Reyes
- Nicolás Parada Bahamondes
- Marcelo Mancilla Valdivia  
**Fecha de Actualización:** 20 de octubre de 2026  
**Repositorio Oficial:** [https://github.com/nparadab/BarrioDigital.git](https://github.com/nparadab/BarrioDigital.git)  
**Endpoint de Producción (AWS EC2):** `https://100.58.58.154:4200`  
**Consola Web RabbitMQ Clúster:** `http://100.58.58.154:15672` (Usuario: `guest` | Contraseña: `guest`)  

---

### 1. Propósito y Alcance

El presente anexo complementa el Informe Técnico de Implementación incorporando los avances y evidencias correspondientes a la **mensajería asíncrona empresarial**, la **alta disponibilidad de RabbitMQ en clúster** y la **orquestación completa en la nube AWS**.

A diferencia de las etapas tempranas de desarrollo en entornos locales, la solución descrita a continuación se encuentra **100% desplegada, configurada y validada en una instancia AWS EC2 en producción (IP `100.58.58.154`)**, dando estricto cumplimiento a los cuatro indicadores de evaluación:

1. **Clúster RabbitMQ de Alta Disponibilidad:** Dos nodos RabbitMQ independientes integrados activamente en un clúster.
2. **Colas de Trabajo y Dead Letter Queues (DLQ):** Tres colas operativas con sus correspondientes colas de cartas muertas para tratamiento de fallos.
3. **Exchanges Direct y Topic:** Configuración, vinculación (bindings) y enrutamiento especializado de eventos comunales.
4. **Despliegue y Orquestación Multi-Contenedor en la Nube:** Ejecución de 8 microservicios interconectados mediante `docker-compose` en AWS EC2.

---

### 2. Resumen de Cumplimiento de Criterios de Evaluación

| Criterio Evaluado | Estado | Implementación y Evidencia en Producción (AWS EC2) |
| :--- | :---: | :--- |
| **1. Clúster RabbitMQ de dos nodos** | **Cumplido (100%)** | Dos nodos en contenedores (`bd-rabbitmq` y `bd-rabbitmq-node2`) integrados mediante Erlang Cookie compartida y descubrimiento `classic_config`. Ambos nodos reportan estado `Running Nodes` en `cluster_status`. |
| **2. Tres colas de mensajes con sus DLQ** | **Cumplido (100%)** | Colas `q.cmd.email`, `q.cmd.crew` y `q.cmd.certificate`, cada una configurada con argumentos `x-dead-letter-exchange` hacia sus respectivas DLQ (`.dlq`). Consumidores activos conectados (`consumerCount: 1`). |
| **3. Exchanges Direct y Topic** | **Cumplido (100%)** | Exchanges configurados: `cmd.direct` (enrutamiento unívoco por routing key exacta), `cmd.topic` (enrutamiento jerárquico por patrones wildcard `*` y `#`), y `cmd.dead.dlx` (enrutador de contingencia para DLQ). |
| **4. Ejecución en la nube con `docker-compose`** | **Cumplido (100%)** | Despliegue en AWS EC2 (`100.58.58.154`) levantando 8 contenedores: 2 RabbitMQ, 4 microservicios backend Spring Boot (Requests, Catalog, Notify, RabbitAdmin), 1 BFF Spring Boot y 1 Frontend Angular Nginx SSL. |

---

### 3. Arquitectura de Mensajería Asíncrona y Topología

La mensajería asíncrona de BarrioDigital fue diseñada bajo el principio de desacoplamiento guiado por eventos (Event-Driven Architecture), separando el ciclo transaccional del trámite del procesamiento de tareas secundarias (envío de correos, despacho de cuadrillas técnicas y emisión de certificados).

```
                      ┌───────────────────────────────────────────────┐
                      │    ms-barriodigital-requests (Publisher)      │
                      └──────────────────────┬────────────────────────┘
                                             │ RabbitTemplate
                                             ▼
                 ┌────────────────────────────────────────────────────────┐
                 │             EXCHANGES (RabbitMQ Clúster)               │
                 │  ┌──────────────────────┐    ┌──────────────────────┐  │
                 │  │   cmd.direct         │    │   cmd.topic          │  │
                 │  └──────────┬───────────┘    └──────────┬───────────┘  │
                 └─────────────┼───────────────────────────┼──────────────┘
                               │ email.send                │ crew.# / cert.*
                               ▼                           ▼
                 ┌────────────────────────────────────────────────────────┐
                 │                  COLAS PRINCIPALES                     │
                 │   q.cmd.email          q.cmd.crew    q.cmd.certificate │
                 └─────────────┬───────────────────────────┬──────────────┘
                               │ NACK (Requeue=false)      │
                               ▼                           ▼
                 ┌────────────────────────────────────────────────────────┐
                 │       cmd.dead.dlx (Dead Letter Exchange)              │
                 └─────────────────────────┬──────────────────────────────┘
                                           │ email.dlq / crew.dlq / cert.dlq
                                           ▼
                 ┌────────────────────────────────────────────────────────┐
                 │               DEAD LETTER QUEUES (DLQ)                 │
                 │ q.cmd.email.dlq    q.cmd.crew.dlq    q.cmd.certificate.│
                 │                                                   dlq  │
                 └─────────────────────────┬──────────────────────────────┘
                                           │
                                           ▼
                 ┌────────────────────────────────────────────────────────┐
                 │       ms-barriodigital-notify (Listeners & DLQ)        │
                 └────────────────────────────────────────────────────────┘
```

#### 3.1 Nodos del Clúster RabbitMQ
- **Nodo 1 (`bd-rabbitmq` / `rabbit@rabbitmq`):** Nodo primario. Puerto AMQP 5672, Puerto Web Management 15672.
- **Nodo 2 (`bd-rabbitmq-node2` / `rabbit@rabbitmq2`):** Nodo de alta disponibilidad. Puerto AMQP 5673, Puerto Web Management 15673.
- **Mecanismo de Clustering:** Definido en `infra/mq/rabbitmq.conf` mediante `cluster_formation.peer_discovery_backend = classic_config` con cookie de Erlang compartida (`BARRIODIGITAL_MQ_COOKIE_SECRET`) y sincronización automática de estado y metadatos.

#### 3.2 Matriz de Exchanges, Colas y Enrutamiento

| Exchange | Tipo | Routing Key | Cola Destino | Dead Letter Exchange | Routing Key DLQ | Cola DLQ Final |
| :--- | :---: | :--- | :--- | :--- | :--- | :--- |
| **`cmd.direct`** | `direct` | `email.send` | `q.cmd.email` | `cmd.dead.dlx` | `email.dlq` | `q.cmd.email.dlq` |
| **`cmd.direct`** | `direct` | `crew.ticket` | `q.cmd.crew` | `cmd.dead.dlx` | `crew.dlq` | `q.cmd.crew.dlq` |
| **`cmd.direct`** | `direct` | `certificate.gen`| `q.cmd.certificate` | `cmd.dead.dlx` | `certificate.dlq` | `q.cmd.certificate.dlq` |
| **`cmd.topic`** | `topic` | `email.*` | `q.cmd.email` | `cmd.dead.dlx` | `email.dlq` | `q.cmd.email.dlq` |
| **`cmd.topic`** | `topic` | `crew.#` | `q.cmd.crew` | `cmd.dead.dlx` | `crew.dlq` | `q.cmd.crew.dlq` |
| **`cmd.topic`** | `topic` | `certificate.*` | `q.cmd.certificate` | `cmd.dead.dlx` | `certificate.dlq` | `q.cmd.certificate.dlq` |
| **`cmd.dead.dlx`** | `direct` | `*.dlq` | *(Específica)* | N/A | N/A | Colas `.dlq` de auditoría |

---

### 4. Tratamiento de Errores y Estrategia Dead Letter Queue (DLQ)

Para evitar la pérdida silenciosa de mensajes o el bloqueo en bucle infinito de las colas de trabajo (poison message problem), se diseñó la siguiente estrategia de resiliencia:

1. **Configuración en Declaración de Cola:** Cada cola principal declara mediante argumentos AMQP su pasarela de descarte:
   ```java
   QueueBuilder.durable("q.cmd.email")
       .withArgument("x-dead-letter-exchange", "cmd.dead.dlx")
       .withArgument("x-dead-letter-routing-key", "email.dlq")
       .build();
   ```
2. **Rechazo Explícito (NACK sin requeue):** Cuando el consumidor `EmailNotificationConsumer` detecta un fallo de negocio, payload malformado o error irrecuperable, rechaza explícitamente el mensaje invocando:
   ```java
   channel.basicNack(deliveryTag, false, false); // requeue = false
   ```
3. **Enrutamiento Automático a DLQ:** El broker RabbitMQ, al recibir el NACK con `requeue=false`, desvía inmediatamente el mensaje al exchange `cmd.dead.dlx`, el cual lo deposita en `q.cmd.email.dlq`.
4. **Consumidor de Auditoría (`DeadLetterQueueConsumer`):** Escucha las tres colas DLQ, extrae las cabeceras estándar de fallo `x-death` (motivo de rechazo, cola de origen, marca de tiempo y contador de reintentos), persiste la traza forense en los logs del contenedor y confirma el mensaje (`basicAck`), garantizando trazabilidad inmutable sin saturar el almacenamiento del broker.

---

### 5. Microservicio Administrador de RabbitMQ (`ms-rabbitmq-admin`)

El microservicio `ms-rabbitmq-admin` (Puerto 8084) proporciona control programático sobre el broker a través de la API de administración de Spring AMQP:

#### 5.1 Inicialización Eager de Topología (`RabbitAdminConfig.java`)
Al iniciar la aplicación, un componente `ApplicationRunner` (`topologyInitializer`) declara proactivamente todos los exchanges, colas principales, DLQs y bindings sin requerir interacción manual previa:
```java
@Bean
public ApplicationRunner topologyInitializer(AmqpAdmin amqpAdmin) {
    return args -> {
        // Declara exchanges direct, topic y dlx
        // Declara las 6 colas y sus bindings
    };
}
```

#### 5.2 Catálogo de Operaciones REST Expuestas

| Operación | Método | Endpoint Directo / Vía BFF | Descripción |
| :--- | :---: | :--- | :--- |
| **Resumen Topología** | `GET` | `/api/admin/overview` | Retorna el estado ONLINE de RabbitMQ y las colas registradas. |
| **Crear Cola** | `POST` | `/api/admin/queues` | Crea dinámicamente una nueva cola con nombre y argumentos. |
| **Consultar Cola** | `GET` | `/api/admin/queues/{name}` | Obtiene métricas en vivo: cantidad de mensajes y consumidores. |
| **Purgar Cola** | `POST` | `/api/admin/queues/{name}/purge` | Vacía los mensajes acumulados en una cola de prueba. |
| **Eliminar Cola** | `DELETE` | `/api/admin/queues/{name}` | Elimina una cola temporal (`ifUnused`, `ifEmpty`). |
| **Crear Exchange** | `POST` | `/api/admin/exchanges` | Declara un exchange de tipo `direct`, `topic`, `fanout` o `headers`. |
| **Eliminar Exchange** | `DELETE`| `/api/admin/exchanges/{name}`| Elimina un exchange temporal del broker. |
| **Crear Binding** | `POST` | `/api/admin/bindings` | Enlaza una cola a un exchange con una routing key específica. |
| **Eliminar Binding** | `DELETE`| `/api/admin/bindings` | Desvincula una regla de enrutamiento existente. |

---

### 6. Evidencias de Ejecución y Validación en la Nube (AWS EC2)

A continuación se presentan las evidencias directas obtenidas sobre la instancia en producción **AWS EC2 (`100.58.58.154`)**:

#### 6.1 Orquestación de 8 Contenedores en AWS (`docker ps`)
```text
[ec2-user@ip-10-0-8-174 BarrioDigital]$ docker ps
CONTAINER ID   IMAGE                               COMMAND                  STATUS                    PORTS
3063c6fc35db   barriodigital-frontend              "/docker-entrypoint…"    Up 15 minutes             80/tcp, 0.0.0.0:4200->443/tcp
f2f4712a020f   barriodigital-ms-bff                "java -jar app.jar"      Up 15 minutes             0.0.0.0:8080->8080/tcp
989095eb28a1   rabbitmq:3.13-management-alpine     "docker-entrypoint.s…"   Up 15 minutes (healthy)   0.0.0.0:5673->5672/tcp, 0.0.0.0:15673->15672/tcp (bd-rabbitmq-node2)
bc15180c5a93   barriodigital-ms-requests            "java -jar app.jar"      Up 15 minutes             0.0.0.0:8081->8081/tcp
c7779d63c125   barriodigital-ms-rabbitmq-admin     "java -jar app.jar"      Up 15 minutes             0.0.0.0:8084->8084/tcp
2f92e3d56b12   barriodigital-ms-notify              "java -jar app.jar"      Up 15 minutes             0.0.0.0:8083->8083/tcp
70c90d6497de   rabbitmq:3.13-management-alpine     "docker-entrypoint.s…"   Up 15 minutes (healthy)   0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp (bd-rabbitmq)
35f83272a026   barriodigital-ms-catalog             "java -jar app.jar"      Up 15 minutes             0.0.0.0:8082->8082/tcp
```

#### 6.2 Estado del Clúster RabbitMQ (`cluster_status`)
```text
[ec2-user@ip-10-0-8-174 BarrioDigital]$ docker exec bd-rabbitmq rabbitmqctl cluster_status
Cluster status of node rabbit@rabbitmq ...
Basics
Cluster name: rabbit@rabbitmq
Total CPU cores available cluster-wide: 4

Disk Nodes
rabbit@rabbitmq
rabbit@rabbitmq2

Running Nodes
rabbit@rabbitmq
rabbit@rabbitmq2

Versions
rabbit@rabbitmq: RabbitMQ 3.13.7 on Erlang 26.2.5.16
rabbit@rabbitmq2: RabbitMQ 3.13.7 on Erlang 26.2.5.16

Maintenance status
Node: rabbit@rabbitmq, status: not under maintenance
Node: rabbit@rabbitmq2, status: not under maintenance

Alarms
(none)

Network Partitions
(none)

Listeners
Node: rabbit@rabbitmq, port: 15672, protocol: http (Management UI)
Node: rabbit@rabbitmq, port: 5672, protocol: amqp
Node: rabbit@rabbitmq, port: 25672, protocol: clustering
Node: rabbit@rabbitmq2, port: 15672, protocol: http (Management UI)
Node: rabbit@rabbitmq2, port: 5672, protocol: amqp
Node: rabbit@rabbitmq2, port: 25672, protocol: clustering
```

#### 6.3 Verificación de Colas de Trabajo y DLQs en la Nube
```text
[ec2-user@ip-10-0-8-174 BarrioDigital]$ docker exec bd-rabbitmq rabbitmqctl list_queues name messages consumers
Timeout: 60.0 seconds ...
Listing queues for vhost / ...
name	messages	consumers
q.cmd.email	0	1
q.cmd.email.dlq	0	1
q.cmd.crew	0	1
q.cmd.crew.dlq	0	1
q.cmd.certificate	0	1
q.cmd.certificate.dlq	0	1
```
*Evidencia:* Se observan las 3 colas de trabajo junto a sus 3 DLQs con `1` consumidor activo conectado permanentemente para cada una.

#### 6.4 Verificación de Exchanges Direct y Topic en la Nube
```text
[ec2-user@ip-10-0-8-174 BarrioDigital]$ docker exec bd-rabbitmq rabbitmqctl list_exchanges name type
Listing exchanges for vhost / ...
name	type
cmd.direct	direct
cmd.topic	topic
cmd.dead.dlx	direct
amq.direct	direct
amq.topic	topic
amq.fanout	fanout
amq.headers	headers
```

#### 6.5 Demostración en Vivo de Derivación a DLQ (Poison Pill)
Se ejecuta la simulación de un mensaje defectuoso a través del controlador de pruebas:
```bash
curl -X POST http://localhost:8083/api/notify/simulate/poison-pill
```
**Respuesta del Microservicio:**
```json
{
  "status": "POISON_PILL_SENT",
  "queue": "q.cmd.email",
  "message": "Mensaje defectuoso inyectado intencionalmente para forzar NACK sin requeue y validar derivación a DLQ",
  "timestamp": "2026-10-20T02:30:15.112Z"
}
```
**Registro de Auditoría capturado en los logs de `ms-notify`:**
```text
[Notify] [ERROR] Poison pill detectada en EmailNotificationConsumer. Payload: {"corrupt":true}. Forzando basicNack(requeue=false)...
[Notify] [WARN] DeadLetterQueueConsumer interceptó mensaje en DLQ [q.cmd.email.dlq].
[Notify] [INFO] Header x-death: {reason=rejected, queue=q.cmd.email, count=1, time=Tue Oct 20 02:30:15 UTC 2026}.
[Notify] [INFO] Traza de auditoría registrada exitosamente. Mensaje confirmado en DLQ con basicAck.
```

---

### 7. Guion y Comandos para Demostración Docente

Para la revisión presencial o remota con el docente, se deben ejecutar los siguientes pasos demostrativos:

1. **Mostrar el Clúster de 2 Nodos en AWS:**
   - **Vía Web:** Abrir `http://100.58.58.154:15672` (Credenciales: `guest`/`guest`). En la pestaña **Overview** mostrar ambos nodos en verde (`rabbit@rabbitmq` y `rabbit@rabbitmq2`).
   - **Vía Consola:** Ejecutar `docker exec bd-rabbitmq rabbitmqctl cluster_status`.
2. **Demostrar las 3 Colas de Trabajo y 3 DLQs:**
   - En la pestaña **Queues** de la consola web, señalar las parejas de colas (`q.cmd.email`/`q.cmd.email.dlq`, etc.) y sus tasas de consumo.
   - En consola: `docker exec bd-rabbitmq rabbitmqctl list_queues name messages consumers`.
3. **Demostrar los Exchanges Direct y Topic:**
   - En la pestaña **Exchanges**, abrir `cmd.direct` y mostrar los bindings exactos (`email.send`, `crew.ticket`, `certificate.gen`).
   - Abrir `cmd.topic` y mostrar los patrones comodín (`email.*`, `crew.#`, `certificate.*`).
4. **Demostrar la Derivación a DLQ en Vivo:**
   - Inyectar el fallo con: `curl -X POST http://localhost:8083/api/notify/simulate/poison-pill`.
   - Inspeccionar los logs del consumidor en tiempo real: `docker logs --tail 20 -f bd-ms-notify`.
5. **Demostrar Operaciones de Administración (Creación/Eliminación Dinámica):**
   - Crear una cola de prueba vía API:
     ```bash
     curl -X POST http://localhost:8084/api/admin/queues -H "Content-Type: application/json" -d '{"name":"cola.prueba.docente","durable":true}'
     ```
   - Comprobar su aparición inmediata en la interfaz web de RabbitMQ.
   - Eliminar la cola de prueba:
     ```bash
     curl -X DELETE http://localhost:8084/api/admin/queues/cola.prueba.docente
     ```

---

### 8. Referencias de Código Fuente en el Repositorio

- **Orquestación Multi-Contenedor:** [docker-compose.yml](file:///docker-compose.yml)
- **Configuración del Clúster RabbitMQ:** [infra/mq/rabbitmq.conf](file:///infra/mq/rabbitmq.conf)
- **Topología Eager y API de Administración:** [BarrioDigital-RabbitAdmin-main/.../RabbitAdminConfig.java](file:///BarrioDigital-RabbitAdmin-main/src/main/java/cl/duoc/barriodigital/admin/config/RabbitAdminConfig.java)
- **Controlador REST de Gestión de Colas:** [BarrioDigital-RabbitAdmin-main/.../QueueAdminController.java](file:///BarrioDigital-RabbitAdmin-main/src/main/java/cl/duoc/barriodigital/admin/controller/QueueAdminController.java)
- **Declaración de Colas, DLQ y Exchanges:** [BarrioDigital-Notify-main/.../RabbitMQConfig.java](file:///BarrioDigital-Notify-main/src/main/java/cl/duoc/barriodigital/notify/config/RabbitMQConfig.java)
- **Consumidores con NACK y Confirmación Manual:** [BarrioDigital-Notify-main/.../EmailNotificationConsumer.java](file:///BarrioDigital-Notify-main/src/main/java/cl/duoc/barriodigital/notify/consumer/email/EmailNotificationConsumer.java)
- **Auditoría Forense de Mensajes en DLQ:** [BarrioDigital-Notify-main/.../DeadLetterQueueConsumer.java](file:///BarrioDigital-Notify-main/src/main/java/cl/duoc/barriodigital/notify/consumer/dlq/DeadLetterQueueConsumer.java)
- **Simulador de Fallos y Poison Pill:** [BarrioDigital-Notify-main/.../NotifySimulationController.java](file:///BarrioDigital-Notify-main/src/main/java/cl/duoc/barriodigital/notify/controller/NotifySimulationController.java)
