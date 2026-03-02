# Astazou

> **Plataforma de gestão de finanças pessoais** — acompanhe suas contas bancárias, transações e cartões de crédito em um só lugar.

[🇺🇸 Read in English](./README.md)

---

## Sumário

- [Visão Geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Como Executar](#como-executar)
  - [Com Docker Compose](#com-docker-compose)
  - [Localmente (desenvolvimento)](#localmente-desenvolvimento)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Endpoints da API](#endpoints-da-api)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Licença](#licença)

---

## Visão Geral

Astazou é uma aplicação fullstack de finanças pessoais que permite gerenciar contas bancárias, registrar receitas e despesas, organizar faturas de cartões de crédito e visualizar análises de gastos — tudo em uma interface limpa e responsiva com suporte a modo escuro/claro e múltiplos idiomas (Inglês / Português).

---

## Funcionalidades

- 🔐 **Autenticação** — autenticação baseada em sessão com tempo de expiração configurável
- 🏦 **Contas Bancárias** — crie e gerencie múltiplas contas com rastreamento automático de saldo
- 💳 **Cartões de Crédito** — cadastre cartões e importe extratos (suporte a PDF do Itaú)
- 💸 **Transações** — adicione, pesquise, filtre e pagine transações de receita/despesa
- 🔁 **Transferências** — converta transações em transferências entre contas
- 📊 **Análises** — resumos mensais e gráficos de gastos por categoria
- 🌐 **i18n** — interface disponível em Inglês e Português
- 🌙 **Modo Escuro/Claro** — alternância de tema com detecção automática do sistema
- 📄 **Importação OFX** — importe extratos bancários em formato OFX
- 🐍 **Parser de PDF** — extrator de extratos do Itaú em Python

---

## Tecnologias

### Backend
| Tecnologia | Versão |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.2 |
| Spring Security | (incluso) |
| Spring Data JDBC | (incluso) |
| Flyway | (incluso) |
| PostgreSQL | latest |
| OpenFeign | Spring Cloud 2025.1.0 |
| OFX4J | 1.39 |
| Lombok | latest |
| Micrometer / Prometheus | (incluso) |

### Frontend
| Tecnologia | Versão |
|---|---|
| Next.js | 16 |
| React | 19 |
| TypeScript | 5 |
| Tailwind CSS | 3 |
| shadcn/ui | latest |
| pnpm | latest |

### Infraestrutura
| Componente | Tecnologia |
|---|---|
| Proxy reverso | Traefik v3 |
| Container runtime | Docker / Docker Compose |
| Banco de dados | PostgreSQL |

---

## Arquitetura

```
┌─────────────────────────────────────────────┐
│                   Traefik                    │  :80 / :443
│             (Proxy Reverso)                  │
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

O backend expõe uma API REST consumida pelo frontend Next.js. As migrações de banco de dados são gerenciadas pelo Flyway. PDFs de extratos bancários são processados por um script Python embutido na imagem Docker do backend.

---

## Pré-requisitos

- [Docker](https://www.docker.com/) ≥ 24 e Docker Compose ≥ 2
- OU, para desenvolvimento local:
  - JDK 25+
  - Node.js 20+ e pnpm
  - PostgreSQL 15+
  - Python 3 (para o parser de PDF)

---

## Como Executar

### Com Docker Compose

1. **Clone o repositório**
   ```bash
   git clone https://github.com/geovannyavelar/astazou.git
   cd astazou
   ```

2. **Crie o arquivo de variáveis de ambiente**
   ```bash
   cp .env.example .env
   ```
   Edite o arquivo `.env` com seus valores (veja [Variáveis de Ambiente](#variáveis-de-ambiente)).

3. **Inicie todos os serviços**
   ```bash
   docker compose up -d
   ```

4. Acesse a aplicação pelo domínio configurado em `FRONTEND_HOST`.

---

### Localmente (desenvolvimento)

#### Backend

```bash
# Configure as variáveis de ambiente (ou use os valores padrão do application.properties)
export ASTAZOU_DATABASE_HOSTNAME=localhost
export ASTAZOU_DATABASE_PORT=5432
export ASTAZOU_DATABASE_NAME=astazou
export ASTAZOU_DATABASE_USERNAME=astazou
export ASTAZOU_DATABASE_PASSWORD=astazou

./gradlew bootRun
```

A API estará disponível em `http://localhost:8080`.

#### Frontend

```bash
cd ui
pnpm install
NEXT_PUBLIC_API_URL=http://localhost:8080 pnpm dev
```

A interface estará disponível em `http://localhost:3000`.

#### Parser de PDF em Python (opcional)

```bash
cd scripts/python
pip install -r requirements.txt
python parse_itau_history_pdf.py <caminho-do-pdf> <saida.csv>
```

---

## Variáveis de Ambiente

### Backend

| Variável | Padrão | Descrição |
|---|---|---|
| `ASTAZOU_DATABASE_HOSTNAME` | `localhost` | Host do PostgreSQL |
| `ASTAZOU_DATABASE_PORT` | `5432` | Porta do PostgreSQL |
| `ASTAZOU_DATABASE_NAME` | `astazou` | Nome do banco de dados |
| `ASTAZOU_DATABASE_USERNAME` | `astazou` | Usuário do banco de dados |
| `ASTAZOU_DATABASE_PASSWORD` | `astazou` | Senha do banco de dados |
| `ASTAZOU_CORS_ALLOWED_ORIGINS` | `*` | Origens permitidas pelo CORS |
| `ASTAZOU_SESSION_EXPIRATION_TIME` | `3600` | TTL da sessão em segundos |
| `ASTAZOU_PYTHON_INTERPRETER` | `python3` | Caminho do interpretador Python |
| `ASTAZOU_ITAU_PDF_PARSER_SCRIPT` | `scripts/python/parse_itau_history_pdf.py` | Caminho do script de parsing |

### Frontend

| Variável | Descrição |
|---|---|
| `NEXT_PUBLIC_API_URL` | URL base da API do backend |

### Docker Compose

| Variável | Descrição |
|---|---|
| `DB_HOST` | Host do PostgreSQL (para rede interna do compose) |
| `DB_PORT` | Porta do PostgreSQL |
| `DB_NAME` | Nome do banco de dados |
| `DB_USER` | Usuário do banco de dados |
| `DB_PASSWORD` | Senha do banco de dados |
| `BACKEND_HOST` | Domínio do backend (roteamento Traefik) |
| `FRONTEND_HOST` | Domínio do frontend (roteamento Traefik) |
| `SPRING_PROFILES_ACTIVE` | Profiles ativos do Spring |

---

## Endpoints da API

| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/auth/login` | Autenticar e iniciar sessão |
| `POST` | `/auth/logout` | Encerrar a sessão atual |
| `GET` | `/bank-accounts` | Listar contas bancárias |
| `POST` | `/bank-accounts` | Criar conta bancária |
| `PUT` | `/bank-accounts/{id}` | Atualizar conta bancária |
| `GET` | `/transactions/{accountId}` | Listar transações de uma conta |
| `POST` | `/transactions` | Criar transação |
| `POST` | `/transactions/ofx/{accountId}` | Importar extrato OFX |
| `GET` | `/transactions/{accountId}/monthly-summary` | Resumo mensal |
| `GET` | `/credit-cards` | Listar cartões de crédito |
| `POST` | `/credit-cards` | Cadastrar cartão de crédito |
| `GET` | `/credit-cards/{cardId}` | Detalhes do cartão de crédito |
| `GET` | `/credit-cards/{cardId}/transactions` | Listar transações do cartão |
| `POST` | `/credit-cards/{cardId}/transactions/pdf` | Importar extrato PDF do Itaú |

---

## Estrutura do Projeto

```
astazou/
├── src/                        # Backend Spring Boot
│   └── main/
│       ├── java/dev/avelar/astazou/
│       │   ├── controller/     # Controllers REST
│       │   ├── service/        # Regras de negócio
│       │   ├── model/          # Entidades do domínio
│       │   ├── repository/     # Repositórios Spring Data JDBC
│       │   ├── dto/            # DTOs de requisição/resposta
│       │   ├── config/         # Configurações do Spring
│       │   └── scheduler/      # Tarefas agendadas
│       └── resources/
│           ├── application.properties
│           └── db/migration/   # Migrações Flyway
├── ui/                         # Frontend Next.js
│   ├── app/                    # Páginas (App Router)
│   ├── components/             # Componentes React
│   ├── lib/                    # Utilitários, contexto de auth, i18n
│   └── hooks/                  # Custom React hooks
├── scripts/
│   └── python/                 # Parser de extrato PDF do Itaú
├── Dockerfile                  # Imagem Docker do backend
├── docker-compose.yml          # Deploy full-stack
└── build.gradle                # Configuração Gradle
```

---

## Licença

Este projeto é privado. Todos os direitos reservados.

