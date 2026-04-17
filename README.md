# 🏦 Fintech Wallet & Transaction Platform

A full-stack, microservices-based financial application that simulates a secure digital wallet system. The platform allows users to register, manage wallet balances, and perform peer-to-peer fund transfers with a complete transaction history.

---

## 🧩 System Architecture

```
User → Frontend (React)
        ↓
   Account Service  ↔  Transaction Service
        ↓                     ↓
   PostgreSQL DB       PostgreSQL DB
```

---

## 🏗️ Architecture Overview

The system is designed using a **microservices architecture**, ensuring separation of concerns, scalability, and maintainability.

### 🔹 Core Services

**Account Service**

* Handles user registration and authentication
* Manages wallet balances
* Generates and validates JWT tokens

**Transaction Service**

* Processes fund transfers between users
* Maintains transaction history
* Communicates securely with the Account Service

**Frontend (Single Page Application)**

* Interactive user dashboard
* Fund transfer interface
* Transaction history visualization

---

## 🛠️ Tech Stack

### Backend

* Java 21
* Spring Boot 3 (Web, Data JPA, Security)
* PostgreSQL
* JWT (stateless authentication)
* Lombok
* Swagger / OpenAPI

### Frontend

* React 18
* Vite
* Axios
* React Router DOM
* CSS

---

## ✨ Key Features

* Secure authentication using JWT
* Peer-to-peer fund transfer system
* Transaction history (sent and received)
* Microservices-based backend architecture
* Protection against unauthorized data access (IDOR prevention)
* Responsive and user-friendly interface

---

## 🔗 Service Communication

The **Transaction Service** interacts with the **Account Service** via REST APIs.

* JWT tokens are propagated for authentication
* Requests are validated before execution
* Ensures secure inter-service communication

---

## 📡 API Endpoints (Sample)

### Account Service

* `POST /api/auth/register` — Register a new user
* `POST /api/auth/login` — Authenticate user
* `GET /api/account/balance` — Retrieve wallet balance

### Transaction Service

* `POST /api/transactions/transfer` — Transfer funds
* `GET /api/transactions/history` — Fetch transaction history

---

## ⚙️ Getting Started

### Prerequisites

* Java 21+
* Node.js (v18+)
* PostgreSQL
* Maven (or use `mvnw`)

---

## 🗄️ Database Setup

```sql
CREATE DATABASE account_db;
CREATE DATABASE transaction_db;
```

---

## ▶️ Running the Application

### 1. Start Backend Services

```bash
cd account-service
./mvnw spring-boot:run
```

```bash
cd transaction-service
./mvnw spring-boot:run
```

---

### 2. Start Frontend

```bash
cd fintech-frontend
npm install
npm run dev
```

---

## ⚠️ Recommended Run Order

1. Start PostgreSQL
2. Start Account Service
3. Start Transaction Service
4. Start Frontend

---

## 📁 Project Structure

```
fintech-system/
├── account-service/
├── transaction-service/
├── fintech-frontend/
├── screenshots/
├── README.md
└── .gitignore
```

---

## 🧠 Design Considerations

* Adopted microservices for modularity and scalability
* Implemented JWT for stateless and secure authentication
* Ensured secure communication between services
* Applied backend validation to prevent unauthorized data access

---

## 📷 Screenshots

### Signup

![Signup](screenshots/signup.png)

### Login

![Login](screenshots/login.png)

### KYC Details

![KYC](screenshots/kyc_details.png)

### Dashboard

![Dashboard](screenshots/dashboard.png)

### Transfer Funds

![Transfer](screenshots/transfer.png)

### Transaction History

![Transactions](screenshots/transactions.png)

---

## 🚀 Future Improvements

* API Gateway (Spring Cloud Gateway)
* Service Discovery (Eureka)
* Docker containerization
* Cloud deployment (AWS / Render)
* Role-based access control

---

## 📌 Summary

This project demonstrates a practical implementation of microservices architecture with secure authentication and transaction handling. It reflects real-world backend design principles applicable to financial systems.

---
