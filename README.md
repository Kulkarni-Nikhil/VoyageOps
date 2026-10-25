# VoyageOps

VoyageOps is a backend-first travel planning platform built with Java and Spring Boot. It provides REST APIs for managing trips, destinations, activities, collaborative travel planning, expenses, and budgets, with an agentic AI assistant layered on top of the backend.

The project focuses on backend engineering concepts such as modular architecture, relational data modeling, caching, authorization-aware service operations, external information retrieval, and AI tool orchestration.

## Features

### Trip Planning
- Create and manage trips
- Configure trip dates, status, description, and budget
- Manage multiple destinations
- Plan activities within a trip

### Collaborative Trips
- Trip membership
- Role-based access to trip operations
- Shared trip planning between members

### Expense & Budget Management
- Add, update, and delete trip expenses
- Categorize expenses
- Track total spending
- Calculate remaining budget
- Calculate budget utilization percentage
- View spending by expense category

### Redis Caching
- Cache frequently requested trip-related data
- Cache expense summaries
- Invalidate cached data when relevant data changes
- Reduce unnecessary database queries

### Agentic AI Travel Assistant
VoyageOps includes an AI travel assistant built using Spring AI and Groq.

The assistant can:

- Understand natural-language travel questions
- Maintain conversation context
- Access VoyageOps trip data through application tools
- Retrieve trip details
- Retrieve planned activities
- Retrieve budget information
- Perform web research for current travel information
- Combine application data and web-search results into a contextual response

### AI Tool Calling

The AI assistant can invoke backend capabilities when required instead of relying entirely on model-generated knowledge.

Example:

```text
User
 │
 ▼
AI Travel Assistant
 │
 ├── get_trip_details
 │
 ├── get_trip_activities
 │
 ├── get_budget_status
 │
 └── web_search
       │
       ▼
   Live Web Information
```

This allows the assistant to distinguish between information stored inside VoyageOps and information that needs to be retrieved from the web.

## Architecture

```text
Client
  │
  ▼
REST Controllers
  │
  ▼
Application Services
  │
  ├───────────────┐
  ▼               ▼
PostgreSQL       Redis
  │
  ▼
Trip / Destination / Activity / Expense
                 │
                 ▼
          AI Travel Assistant
                 │
          ┌──────┴──────┐
          ▼             ▼
     Local Tools     Web Search
          │             │
          ▼             ▼
     Trip Data       Live Web
```

## Technology Stack

- Java 25
- Spring Boot 4.1
- Spring Data JPA
- Hibernate
- Spring AI
- Groq
- PostgreSQL
- Redis
- Docker
- Maven
- REST APIs
- Git / GitHub
- Postman

## Project Structure

```text
src/main/java/com/voyageops/app
│
├── ai
│   ├── application
│   ├── tool
│   ├── web
│   └── websearch
│
├── trip
├── expense
├── user
├── config
└── common
```

The AI web-search implementation is intentionally kept under:

```text
ai/websearch
```

rather than introducing unnecessary subpackages.

## AI Request Flow

For a question involving stored trip information:

```text
User Question
     ↓
Travel Assistant
     ↓
LLM determines required tool
     ↓
VoyageOps Tool
     ↓
Application Service
     ↓
PostgreSQL / Redis
     ↓
Tool Result
     ↓
LLM
     ↓
Natural Language Response
```

For current travel information:

```text
User Question
     ↓
Travel Assistant
     ↓
web_search tool
     ↓
Web Search
     ↓
Current Web Information
     ↓
LLM
     ↓
Contextual Response
```

## Example Queries

The assistant can handle questions such as:

```text
What is my current trip budget?

How much have I spent so far?

What activities have I planned?

What are some places I can visit near my destination?

What is the current weather there?

How does my planned itinerary look?
```

The assistant can combine multiple sources when required.

For example:

```text
"How is my Goa trip looking considering my budget,
planned activities and current weather?"
```

can require both application-level tool calls and web research.

## Running Locally

### Prerequisites

- Java 25
- Maven
- PostgreSQL
- Redis
- Docker
- Groq API key

### Environment Variables

Configure the required environment variables before starting the application.

```text
GROQ_API_KEY=<your-api-key>
```

Database and Redis configuration should be supplied through the application's configuration.

### Run

```bash
./mvnw spring-boot:run
```

or:

```bash
mvn spring-boot:run
```

## Testing APIs

REST APIs can be tested using Postman.

Typical operations include:

```text
Trips
Destinations
Activities
Expenses
Budget
AI Assistant
```

## Engineering Focus

VoyageOps was designed to demonstrate backend engineering rather than simply CRUD functionality.

Key engineering areas include:

- Layered/modular backend architecture
- REST API design
- Relational data modeling
- JPA/Hibernate
- Transaction boundaries
- Authorization-aware service operations
- Redis caching
- Cache invalidation
- External information retrieval
- AI tool calling
- AI conversation context
- Separation of application data from model knowledge
- Dockerized development environment

## Future Scope

Potential extensions include:

- OAuth2 login with Google/GitHub
- More specialized AI travel tools
- Automated itinerary optimization
- Collaborative real-time trip updates
- Advanced travel recommendation workflows
- Production deployment and observability
