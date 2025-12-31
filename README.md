# Customer Fee Service

A backend service for managing customer monthly fee collection at HDBank, implementing standardized API design, distributed scheduling, event-driven architecture, and SOLID principles.

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Architecture](#architecture)
- [Troubleshooting](#troubleshooting)

---

## Overview

This service handles:
- Customer management and fee configuration
- Multiple fee calculation strategies (Fixed, Tiered Balance, Percentage)
- Automated monthly fee charging with distributed locking
- Event-driven architecture using Kafka
- Redis caching for performance optimization
- Idempotent job processing across multiple instances

**Key Features:**
-  RESTful API with standardized response format
-  Strategy Pattern for flexible fee calculation
-  Distributed scheduler with PostgreSQL advisory locks
-  Kafka event publishing for fee charged events
-  Redis caching for frequently accessed data
-  Global exception handling with detailed error codes
-  Bean validation for all request DTOs

---

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Programming Language |
| Spring Boot | 3.5.9 | Application Framework |
| Gradle | 8.x | Build Tool |
| PostgreSQL | 16+ | Primary Database |
| Redis | 7.x | Caching Layer |
| Apache Kafka | 3.x | Event Streaming |
| Flyway | Latest | Database Migration |
| Docker | Latest | Containerization |
| Swagger/OpenAPI | 3.0 | API Documentation |

---

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** - [Download](https://adoptium.net/)
- **Docker & Docker Compose** - [Download](https://www.docker.com/products/docker-desktop)
- **Git** - [Download](https://git-scm.com/downloads)
- **Gradle 8.x** (optional, wrapper included)

### Verify Installation

```bash
java -version   # Should show Java 21
docker --version
docker-compose --version
```

---

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/your-username/customer-fee-service.git
cd customer-fee-service
```

### 2. Start Infrastructure Services

Use Docker Compose to start PostgreSQL, Redis, and Kafka (KRaft mode - no Zookeeper needed):

```bash
docker-compose up -d
```

**Services Started:**
- PostgreSQL: `localhost:25432` (mapped from container 5432)
- Redis: `localhost:26379` (mapped from container 6379)
- Kafka: `localhost:9092` (KRaft mode - Zookeeper-less)
- Kafka UI: `localhost:8090` (for monitoring topics)

**Verify services are running:**

```bash
docker-compose ps
```

All services should show status as "Up" or "Up (healthy)".

### 3. Configure Application Properties

The application uses Spring profiles. For local development, create or update:

**File:** `src/main/resources/application-local.properties`

```properties
# Database (match docker-compose.yml settings)
spring.datasource.url=jdbc:postgresql://localhost:25432/customer_fee_db
spring.datasource.username=postgres
spring.datasource.password=123456

# Server
server.port=8080

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=26379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=customer-fee-service-group
```

The application will automatically use `application-local.properties` when `spring.profiles.active=local` (default).

### 4. Run Database Migrations

Database schema is automatically created via Flyway on application startup. To run migrations manually:

```bash
./gradlew flywayMigrate
```

**Migration scripts location:** `src/main/resources/db/migration/`

### 5. Build the Application

```bash
./gradlew clean build
```

### 6. Run the Application

```bash
./gradlew bootRun
```

Or run the JAR directly:

```bash
java -jar build/libs/customer-fee-service-0.0.1-SNAPSHOT.jar
```

**Application will start on:** `http://localhost:8080`

---

## Configuration

### Application Properties

Main configuration file: `src/main/resources/application.properties`

Key configurations:

```properties
# Server
server.port=${SERVER_PORT}

# Database
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}

# Redis Cache
spring.data.redis.host=localhost
spring.data.redis.port=26379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=customer-fee-service-group

# Scheduler (Test mode - runs every 2 minutes)
# PrepareScheduler: 0 */2 * * * ?
# ExecuteScheduler: 0 */2 * * * ?
```

### Kafka Topics

| Topic | Purpose |
|-------|---------|
| `payment.fee.charged.v1` | Main topic for fee charged events |
| `payment.fee.charged.retry.v1` | Retry topic (optional) |
| `payment.fee.charged.dlq.v1` | Dead Letter Queue (optional) |

### Redis Cache Keys

| Cache | Key Pattern | TTL |
|-------|------------|-----|
| Fee Types | `feeTypes::{code}` | 1 day |
| Fee Configs | `feeConfigs::{id}` | 30 min |
| Active Config | `feeConfigs::customerId: {id}:active` | 30 min |

---

## Running the Application

### Development Mode

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Production Mode

```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar customer-fee-service.jar
```

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

---

## API Documentation

### Swagger UI

Access interactive API documentation:

**URL:** `http://localhost:8080/swagger-ui.html`

### OpenAPI Spec

**JSON:** `http://localhost:8080/api-docs`

### API Base URL

```
http://localhost:8080/api/v1
```

### Key Endpoints

#### Customer Management

```bash
# Create Customer
POST /api/v1/customers
Content-Type: application/json

{
  "customer_name": "Nguyen Van A",
  "account_number": "0123456789",
  "account_balance": 1000000.00
}

# Get Customer
GET /api/v1/customers/{id}

# Get All Customers (with pagination)
GET /api/v1/customers?page=0&size=10
```

#### Fee Configuration

```bash
# Create Fee Config
POST /api/v1/fee-configs
Content-Type: application/json

{
  "customer_id": 1,
  "fee_type_id": 1,
  "monthly_fee_amount": 50000.00,
  "currency": "VND",
  "effective_from": "2025-01-01"
}

# Get Active Config for Customer
GET /api/v1/fee-configs/customer/{customerId}

# Fee Preview
POST /api/v1/fee-configs/preview
Content-Type: application/json

{
  "customer_id": 1,
  "calculation_params": {
    "account_balance": 5000000
  }
}
```

#### Fee Types

```bash
# Get All Fee Types
GET /api/v1/fee-types

# Get Fee Type by Code
GET /api/v1/fee-types/code/FIXED_MONTHLY
```

### Response Format

All API responses follow this standard format:

```json
{
  "success": true,
  "message": "Success",
  "data": { ... },
  "timestamp": "2025-12-30T10:30:00"
}
```

**Error Response:**

```json
{
  "success": false,
  "message": "Validation failed",
  "error_code": "01",
  "error_id": "uuid-here",
  "timestamp": "2025-12-30T10:30:00"
}
```

---

## Testing

### Run All Tests

```bash
./gradlew test
```

### Run Integration Tests

```bash
./gradlew integrationTest
```

### Manual Testing

See detailed test cases in:
- `CLAUDE/MANUAL_TEST_CASES.md`
- `CLAUDE/TESTING_AND_DEMO_GUIDE.md`

### Testing Kafka

**Check topics:**

```bash
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

**Consume messages:**

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment.fee.charged.v1 \
  --from-beginning
```

**Or use kcat:**

```bash
kcat -b localhost:9092 -C -t payment.fee.charged.v1
```

### Testing Redis

**Connect to Redis:**

```bash
docker exec -it customer-fee-redis redis-cli
```

**Or connect from host:**

```bash
redis-cli -h localhost -p 26379
```

**Or use Redis Commander UI:** `http://localhost:8081`

**Check cached data:**

```redis
KEYS *
GET "feeTypes::FIXED_MONTHLY"
TTL "feeConfigs::1"
```

---

## Architecture

### System Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTP/REST
       ▼
┌─────────────────────────────────┐
│   Customer Fee Service          │
│  ┌──────────────────────────┐  │
│  │  Controller Layer        │  │
│  └──────────┬───────────────┘  │
│             │                   │
│  ┌──────────▼───────────────┐  │
│  │  Service Layer           │  │
│  │  - Strategy Pattern      │  │
│  └──────────┬───────────────┘  │
│             │                   │
│  ┌──────────▼───────────────┐  │
│  │  Repository Layer        │  │
│  └──────────┬───────────────┘  │
└─────────────┼───────────────────┘
              │
      ┌───────┴──────┐
      ▼              ▼
┌──────────┐   ┌──────────┐
│PostgreSQL│   │  Redis   │
└──────────┘   └──────────┘
      │
      ▼
┌──────────┐
│  Kafka   │
└──────────┘
```

### Strategy Pattern for Fee Calculation

```
FeeCalculationStrategy (Interface)
         │
         ├── FixedMonthlyFeeStrategy
         ├── TieredBalanceFeeStrategy
         └── PercentageBalanceFeeStrategy
```

### Scheduler Flow

**PrepareScheduler (Daily 00:30):**
1. Get all ACTIVE customers
2. Check if customer has active fee config
3. Create fee job with status = NEW
4. Use idempotency_key: `{customerId}_{billingMonth}`

**ExecuteScheduler (Daily 01:00):**
1. Get all jobs with status = NEW
2. Set status = IN_PROGRESS (with distributed lock)
3. Calculate fee using strategy pattern
4. Charge fee and create attempt log
5. Set status = DONE or FAILED
6. Publish Kafka event if successful

### Package Structure

```
com.hdbank.customer_fee_service
├── config/          # Configuration classes
├── controller/      # REST Controllers
├── dto/            # Data Transfer Objects
├── entity/         # JPA Entities
├── repository/     # Spring Data Repositories
├── service/        # Business Logic
│   └── strategy/   # Strategy Pattern for fee calculation
├── scheduler/      # Scheduled Jobs
├── kafka/          # Kafka Producer/Consumer
└── exception/      # Custom Exceptions & Handler
```

---

## Troubleshooting

### Common Issues

#### 1. Application fails to start - Port already in use

```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9
```

#### 2. Database connection refused

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Check logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

#### 3. Kafka connection timeout

```bash
# Check Kafka is running (KRaft mode)
docker-compose ps kafka

# Restart Kafka
docker-compose restart kafka
```

#### 4. Redis connection refused

```bash
# Check Redis is running
docker-compose ps redis

# Test connection
redis-cli -h localhost -p 26379 ping

# Check logs
docker-compose logs redis
```

#### 5. Flyway migration fails

```bash
# Clean and re-run migrations
./gradlew flywayClean flywayMigrate
```

### Checking Logs

**Application logs:**

```bash
tail -f logs/application.log
```

**Docker container logs:**

```bash
docker-compose logs -f [service-name]
docker-compose logs -f postgres
docker-compose logs -f kafka
```

### Reset Everything

```bash
# Stop all services
docker-compose down -v

# Remove volumes (WARNING: deletes all data)
docker volume prune

# Restart fresh
docker-compose up -d
./gradlew clean build bootRun
```

---

## License

Internal use only - HDBank IT Department

---

**Generated with [Claude Code](https://claude.com/claude-code)**
