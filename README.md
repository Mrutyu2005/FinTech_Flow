\# 🏦 Fintech Wallet \& Transaction Platform



A full-stack, microservices-based financial application that simulates a secure digital wallet system. Users can register, manage wallet balances, and perform peer-to-peer fund transfers with transaction tracking.



\---



\## 🧩 System Architecture



```

User → Frontend (React)

&#x20;       ↓

&#x20;  Account Service  ←→  Transaction Service

&#x20;       ↓                     ↓

&#x20;  PostgreSQL DB       PostgreSQL DB

```



\---



\## 🏗️ Architecture Overview



This project follows a \*\*Microservices Architecture\*\* to ensure scalability, modularity, and separation of concerns.



\### 🔹 Services



\* \*\*Account Service\*\*



&#x20; \* User registration \& authentication

&#x20; \* Wallet balance management

&#x20; \* JWT generation \& validation



\* \*\*Transaction Service\*\*



&#x20; \* Handles fund transfers

&#x20; \* Maintains transaction history

&#x20; \* Communicates securely with Account Service



\* \*\*Frontend (SPA)\*\*



&#x20; \* User dashboard

&#x20; \* Transfer interface

&#x20; \* Transaction history view



\---



\## 🛠️ Tech Stack



\### Backend



\* Java 21

\* Spring Boot 3 (Web, Data JPA, Security)

\* PostgreSQL

\* JWT Authentication

\* Lombok

\* Swagger / OpenAPI



\### Frontend



\* React 18

\* Vite

\* Axios

\* React Router DOM

\* CSS



\---



\## ✨ Key Features



\* 🔐 Secure authentication using JWT

\* 💸 Peer-to-peer fund transfer system

\* 📊 Transaction history (sent \& received)

\* 🔄 Microservices-based architecture

\* 🛡️ Protection against IDOR vulnerabilities

\* ⚡ Responsive and interactive UI



\---



\## 🔗 Service Communication



The \*\*Transaction Service\*\* communicates with the \*\*Account Service\*\* using REST APIs.



\* JWT tokens are forwarded for authentication

\* Each request is validated before processing

\* Ensures secure inter-service communication



\---



\## 📡 API Endpoints (Sample)



\### Account Service



\* `POST /api/auth/register` → Register user

\* `POST /api/auth/login` → Login

\* `GET /api/account/balance` → Get wallet balance



\### Transaction Service



\* `POST /api/transactions/transfer` → Transfer funds

\* `GET /api/transactions/history` → View transactions



\---



\## ⚙️ Getting Started



\### Prerequisites



\* Java 21+

\* Node.js (v18+)

\* PostgreSQL

\* Maven (or use `mvnw`)



\---



\## 🗄️ Database Setup



```sql

CREATE DATABASE account\_db;

CREATE DATABASE transaction\_db;

```



\---



\## ▶️ Run the Application



\### 1️⃣ Start Backend Services



```bash

cd account-service

./mvnw spring-boot:run

```



```bash

cd transaction-service

./mvnw spring-boot:run

```



\---



\### 2️⃣ Start Frontend



```bash

cd fintech-frontend

npm install

npm run dev

```



\---



\## ⚠️ Run Order



1\. Start PostgreSQL

2\. Start Account Service

3\. Start Transaction Service

4\. Start Frontend



\---



\## 📁 Project Structure



```

fintech-system/

&#x20;├── account-service/

&#x20;├── transaction-service/

&#x20;├── fintech-frontend/

&#x20;├── README.md

&#x20;└── .gitignore

```



\---



\## 🧠 Design Decisions



\* Used microservices to separate core business logic

\* Implemented JWT for stateless authentication

\* Ensured secure service-to-service communication

\* Prevented unauthorized data access using validation logic



\---





\## 📷 Screenshots



\### Login

!\[Login](screenshots/login.png)



\### Signup

!\[Signup](screenshots/signup.png)



\### Dashboard

!\[Dashboard](screenshots/dashboard.png)



\### Transactions

!\[Transactions](screenshots/transactions.png)



\### Transfer

!\[Transfer](screenshots/transfer.png)



\### KYC Details

!\[KYC](screenshots/kyc details.png)



\---



\## 🚀 Future Enhancements



\* API Gateway (Spring Cloud Gateway)

\* Service Discovery (Eureka)

\* Docker \& containerization

\* Deployment on cloud (AWS / Render)

\* Role-based access control



\---



\## 📌 Conclusion



This project demonstrates practical implementation of microservices architecture, secure authentication, and scalable backend design suitable for real-world financial systems.



\---



