# 🚀 Quick Start Guide

Get the Customer Fee Service running in **5 minutes**!

## Prerequisites

✅ Java 21
✅ Docker & Docker Compose
✅ Git

## Steps

### 1️⃣ Clone & Navigate

```bash
git clone <repo-url>
cd customer-fee-service
```

### 2️⃣ Start Infrastructure

```bash
docker-compose up -d
```

Wait 30 seconds for services to initialize.

### 3️⃣ Configure Application (if needed)

Application uses `application-local.properties` by default (already configured for Docker services).

If you need custom settings, edit: `src/main/resources/application-local.properties`

### 4️⃣ Run Application

```bash
./gradlew bootRun
```

### 5️⃣ Verify

Open browser: `http://localhost:8080/swagger-ui.html`

## Test APIs

### Create Customer

```bash
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{
    "customer_name": "Test User",
    "account_number": "1234567890",
    "account_balance": 1000000
  }'
```

### Get All Customers

```bash
curl http://localhost:8080/api/v1/customers?page=0&size=10
```

## Management UIs

- **Swagger API Docs:** http://localhost:8080/swagger-ui.html
- **Kafka UI:** http://localhost:8090
- **Redis Commander:** http://localhost:8081

## Stop Services

```bash
# Stop application: Ctrl+C

# Stop infrastructure:
docker-compose down
```

## Need Help?

See full documentation: [README.md](README.md)
