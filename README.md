# Astazou

> **Personal finance management platform** — track your bank accounts, transactions and credit cards in one place.

[🇧🇷 Leia em Português](./README.pt-BR.md)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Running with Docker Compose](#running-with-docker-compose)
  - [Running locally (development)](#running-locally-development)
- [Environment Variables](#environment-variables)
- [API Endpoints](#api-endpoints)
- [Project Structure](#project-structure)
- [License](#license)

---

## Overview

Astazou is a full-stack personal finance application that lets you manage your bank accounts, record income and expenses, organise credit card bills, and visualise spending analytics — all in a clean, responsive interface with dark/light mode and multi-language support (English / Portuguese).

---

## Features

- 🔐 **Authentication** — session-based authentication with configurable expiration time
- 🏦 **Bank Accounts** — create and manage multiple bank accounts with automatic balance tracking
- 💳 **Credit Cards** — register credit cards and import statements (Itaú PDF supported)
- 💸 **Transactions** — add, search, filter and paginate income/expense transactions
- 🔁 **Transfers** — convert transactions into transfers between accounts
- 📊 **Analytics** — monthly summaries and spending breakdown charts
- 🌐 **i18n** — interface available in English and Portuguese
- 🌙 **Dark/Light mode** — system-aware theme switching
- 📄 **OFX Import** — import bank statements via OFX files
- 🐍 **PDF Parser** — Python-powered Itaú credit card statement extractor

---

## Tech Stack

### Backend
| Technology | Version |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.2 |
| Spring Security | (included) |
| Spring Data JDBC | (included) |
| Flyway | (included) |
| PostgreSQL | latest |
| OpenFeign | Spring Cloud 2025.1.0 |
| OFX4J | 1.39 |
| Lombok | latest |
| Micrometer / Prometheus | (included) |

### Frontend
| Technology | Version |
|---|---|
| Next.js | 16 |
| React | 19 |
| TypeScript | 5 |
| Tailwind CSS | 3 |
| shadcn/ui | latest |
| pnpm | latest |

### Infrastructure
| Component | Technology |
|---|---|
| Reverse proxy | Traefik v3 |
| Container runtime | Docker / Docker Compose |
| Database | PostgreSQL |

---

## Architecture

```
┌─────────────────────────────────────────────┐
│                   Traefik                   │  :80 / :443
│              (Reverse Proxy)                │
└───────┬──────────────────────────┬──────────┘
        │                          │
        ▼                          ▼
┌───────────────┐        ┌──────────────────┐
│   Frontend    │        │    Backend       │
│  Next.js :3000│        │  Spring Boot     │
│               │◄──────►│  :8080           │
└───────────────┘        └────────┬─────────┘
                                  │
                         ┌────────▼─────────┐
                         │   PostgreSQL     │
                         │   :5432          │
                         └──────────────────┘
```

The backend exposes a REST API consumed by the Next.js frontend. Database migrations are managed by Flyway. Sensitive bank statement PDFs are processed by a Python script bundled inside the backend Docker image.

---

## Prerequisites

- [Docker](https://www.docker.com/) ≥ 24 and Docker Compose ≥ 2
- OR, for local development:
  - JDK 25+
  - Node.js 20+ and pnpm
  - PostgreSQL 15+
  - Python 3 (for PDF parsing)

---

## Getting Started

### Running with Docker Compose

1. **Clone the repository**
   ```bash
   git clone https://github.com/geovannyavelar/astazou.git
   cd astazou
   ```

2. **Create the environment file**
   ```bash
   cp .env.example .env
   ```
   Edit `.env` with your values (see [Environment Variables](#environment-variables)).

3. **Start all services**
   ```bash
   docker compose up -d
   ```

4. Access the application at the host you configured in `FRONTEND_HOST`.

---

### Running locally (development)

#### Backend

```bash
# Set the required environment variables (or use application.properties defaults)
export ASTAZOU_DATABASE_HOSTNAME=localhost
export ASTAZOU_DATABASE_PORT=5432
export ASTAZOU_DATABASE_NAME=astazou
export ASTAZOU_DATABASE_USERNAME=astazou
export ASTAZOU_DATABASE_PASSWORD=astazou

./gradlew bootRun
```

The API will be available at `http://localhost:8080`.

#### Frontend

```bash
cd ui
pnpm install
NEXT_PUBLIC_API_URL=http://localhost:8080 pnpm dev
```

The UI will be available at `http://localhost:3000`.

#### Python PDF parser (optional)

```bash
cd scripts/python
pip install -r requirements.txt
python parse_itau_history_pdf.py <path-to-pdf> <output.csv>
```

---

## Environment Variables

### Backend

| Variable | Default | Description |
|---|---|---|
| `ASTAZOU_DATABASE_HOSTNAME` | `localhost` | PostgreSQL host |
| `ASTAZOU_DATABASE_PORT` | `5432` | PostgreSQL port |
| `ASTAZOU_DATABASE_NAME` | `astazou` | Database name |
| `ASTAZOU_DATABASE_USERNAME` | `astazou` | Database user |
| `ASTAZOU_DATABASE_PASSWORD` | `astazou` | Database password |
| `ASTAZOU_CORS_ALLOWED_ORIGINS` | `*` | Allowed CORS origins |
| `ASTAZOU_SESSION_EXPIRATION_TIME` | `3600` | Session TTL in seconds |
| `ASTAZOU_PYTHON_INTERPRETER` | `python3` | Python interpreter path |
| `ASTAZOU_ITAU_PDF_PARSER_SCRIPT` | `scripts/python/parse_itau_history_pdf.py` | PDF parser script path |

### Frontend

| Variable | Description |
|---|---|
| `NEXT_PUBLIC_API_URL` | Base URL of the backend API |

### Docker Compose

| Variable | Description |
|---|---|
| `DB_HOST` | PostgreSQL host (for compose networking) |
| `DB_PORT` | PostgreSQL port |
| `DB_NAME` | Database name |
| `DB_USER` | Database user |
| `DB_PASSWORD` | Database password |
| `BACKEND_HOST` | Domain for the backend (Traefik routing) |
| `FRONTEND_HOST` | Domain for the frontend (Traefik routing) |
| `SPRING_PROFILES_ACTIVE` | Active Spring profiles |

---

## License

This project is licensed under the [MIT License](./LICENSE).

