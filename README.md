# Backend Pedidos360

Backend en microservicios (Java 17 + Spring Boot 3.3.5) con RabbitMQ (mensajería de pedidos), Kafka (streaming y analítica), PostgreSQL en Supabase y autenticación JWT con Microsoft Entra ID (IDaaS).

## Estructura

```
Backend-Pedidos360/
├── infra/
│   ├── docker-compose.rabbitmq.yml   # Clúster RabbitMQ de 2 nodos
│   ├── docker-compose.kafka.yml      # Zookeeper + 2 brokers Kafka
│   ├── docker-compose.kafka-ui.yml   # Kafka UI
│   ├── rabbitmq.conf
│   └── .env.example                  # Plantilla de variables (copiar a .env)
├── microservicio-pedidos/            # Puerto 8081: CRUD pedidos, RabbitMQ (+DLQ), admin RabbitMQ, Kafka
├── microservicio-analitica/          # Puerto 8082: consume eventos, calcula métricas, produce a Kafka
└── microservicio-kafka-admin/        # Puerto 8083: administra tópicos, configs y consumer groups
```

## Requisitos

- JDK 17, Maven 3.9+ (o el `mvnw` incluido en cada microservicio)
- Docker Desktop con Docker Compose v2
- Acceso a la base de datos Supabase (usuario y contraseña, pedirlos al equipo; **nunca subirlos a Git**)

## 1. Variables de entorno

Copia la plantilla y completa los valores reales:

```powershell
cd infra
copy .env.example .env
```

`infra/.env` (lo lee Docker Compose para RabbitMQ):

```
RABBIT_PASSWORD=<password-rabbit>
RABBIT_ERLANG_COOKIE=<texto-largo-aleatorio>
```

Variables que leen los microservicios (se definen en la terminal donde se ejecutan):

| Variable | Usada por | Obligatoria | Descripción |
|---|---|---|---|
| `DB_USER` | pedidos | Sí | Usuario Supabase (pooler) |
| `DB_PASSWORD` | pedidos | Sí | Contraseña Supabase |
| `RABBIT_PASSWORD` | pedidos | Sí | Misma que en `infra/.env` (usuario `admin`) |
| `DB_URL` | pedidos | No | Por defecto, el pooler de Supabase configurado en `application.yml` |
| `KAFKA_BOOTSTRAP_SERVERS` | los 3 | No | Por defecto `localhost:9092,localhost:9093` |
| `AZURE_ISSUER_URI`, `AZURE_TENANT_ID`, `AZURE_AUDIENCE` | los 3 | No | Por defecto, los del tenant del proyecto |
| `PORT` | los 3 | No | Sobrescribe el puerto |

PowerShell:

```powershell
$env:DB_USER = "<usuario>"
$env:DB_PASSWORD = "<password>"
$env:RABBIT_PASSWORD = "<password-rabbit>"
```

Linux/EC2:

```bash
export DB_USER="<usuario>" DB_PASSWORD="<password>" RABBIT_PASSWORD="<password-rabbit>"
```

## 2. Levantar la infraestructura (en este orden)

Desde `infra/`:

```powershell
# 1) RabbitMQ (clúster de 2 nodos)
docker compose -f docker-compose.rabbitmq.yml up -d

# 2) Kafka (Zookeeper + 2 brokers). Crea la red kafka_net
docker compose -f docker-compose.kafka.yml up -d

# 3) Kafka UI (requiere la red kafka_net del paso anterior)
docker compose -f docker-compose.kafka-ui.yml up -d

docker ps
```

| Servicio | URL |
|---|---|
| RabbitMQ Management (nodo 1 / 2) | http://localhost:15672 / http://localhost:15673 (usuario `admin`) |
| Kafka brokers | `localhost:9092`, `localhost:9093` |
| Kafka UI | http://localhost:8090 |

Los tópicos **no se crean solos** (auto-create desactivado): los crea el microservicio `kafka-admin` al arrancar.

## 3. Levantar los microservicios

Orden recomendado: **kafka-admin primero** (crea los tópicos), luego analítica y pedidos. Cada uno en su propia terminal.

```powershell
cd microservicio-kafka-admin ; .\mvnw.cmd spring-boot:run
cd microservicio-analitica   ; .\mvnw.cmd spring-boot:run
cd microservicio-pedidos     ; .\mvnw.cmd spring-boot:run   # requiere las variables DB_* y RABBIT_PASSWORD
```

Verificar: `http://localhost:808X/actuator/health` (X = 1, 2, 3).

## 4. Tópicos Kafka gestionados

| Tópico | Particiones | Réplicas | Cleanup | Retención |
|---|---|---|---|---|
| `pedidos.eventos` | 3 | 2 | delete | 7 días |
| `pedidos.analitica` | 3 | 2 | compact,delete | 30 días |
| `pedidos.eventos.DLT` | 1 | 2 | delete | 14 días |
| `pedidos.analitica.DLT` | 1 | 2 | delete | 14 días |

Consumer groups: `pedidos-metricas-group` (pedidos), `analitica-metricas-group` y el de auditoría (analitica). Los errores de consumo se reintentan con backoff exponencial (3 intentos) y luego van al `.DLT`.

## 5. Endpoints principales

Todos requieren `Authorization: Bearer <JWT de Entra ID>`.

**microservicio-pedidos (8081)**
- `GET /pedidos`, `GET /pedidos/{id}`, `POST /pedidos`, `DELETE /pedidos/{id}` (borrar solo ADMIN)
- `GET /api/analitica/clientes`, `GET /api/analitica/clientes/{cliente}`
- RabbitMQ admin (ADMIN): `POST /api/rabbitmq/admin/queues`, `GET|DELETE /api/rabbitmq/admin/queues/{name}`, `POST /api/rabbitmq/admin/exchanges`, `POST /api/rabbitmq/admin/bindings`
- Pruebas: `POST /api/rabbitmq/admin/test/simular-exito`, `POST /api/rabbitmq/admin/test/simular-fallo-dlq`

**microservicio-analitica (8082)**
- `GET /api/analitica/clientes`, `GET /api/analitica/clientes/{cliente}`

**microservicio-kafka-admin (8083, rol ADMIN)** prefijo `/api/kafka/admin`
- `GET /cluster`, `GET /topics`, `GET /topics/{name}`
- `POST /topics`, `POST /topics/bootstrap`
- `PUT /topics/{name}/config`, `PUT /topics/{name}/partitions`, `DELETE /topics/{name}`
- `GET /consumer-groups`, `GET /consumer-groups/{groupId}`, `DELETE /consumer-groups/{groupId}`

## 6. Compilar y probar

```powershell
cd microservicio-pedidos
.\mvnw.cmd clean package          # compila y ejecuta tests
.\mvnw.cmd -DskipTests package    # solo empaqueta -> target/*.jar
```

## 7. Seguridad (qué NO subir a Git)

`.env`, `*.pem`, `target/`, `.vscode/`. Solo se sube `infra/.env.example` con **valores de ejemplo**, nunca credenciales reales.

## 8. Problemas frecuentes

| Síntoma | Causa / solución |
|---|---|
| `Could not resolve placeholder 'DB_USER'` | Faltan variables de entorno en esa terminal |
| `kafka_net not found` al levantar Kafka UI | Levantar primero `docker-compose.kafka.yml` |
| Tópicos no existen | Arrancar `kafka-admin` o llamar `POST /api/kafka/admin/topics/bootstrap` |
| `Connection to node -1 could not be established` | Brokers caídos: `docker ps`, revisar `docker logs kafka-1` |
| 401 / 403 | JWT ausente, expirado, audiencia incorrecta o sin el rol requerido |
| Puerto ocupado | `netstat -ano \| findstr :8081` y detener el proceso |
