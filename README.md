# AI Inventory Management Agent

An intelligent, production-quality, AI-powered Inventory Management system designed for manufacturing operations. This portal allows administrators to monitor stock levels, query real-time data using a built-in LLM agent, run inventory audits, track low stock warnings, and calculate valuation trends.

---

## Technical Stack

- **Backend:** Spring Boot 3, Java 21, Maven, Spring Web, Spring Data MongoDB, Validation, Lombok
- **Frontend:** HTML5, CSS3, Bootstrap 5, JavaScript (Vanilla, serving directly from Spring Boot static resources)
- **Database:** MongoDB
- **AI Engine:** Groq API (`llama-3.3-70b-versatile` model)
- **Deployment:** Docker, Docker Compose

---

## Core Features

1. **Dashboard Analytics:** Live counters for Total SKUs, Healthy Items, Low Stock Warnings, Out of Stock Items, and Total valuation.
2. **Product Catalog CRUD:** Comprehensive product listing with search, category filters, stock status alerts, clickable header sorting, and pagination.
3. **AI Chat Analyst:** A ChatGPT-like analyst panel. The agent analyzes your live MongoDB data, generates recommendations, suggests supplier actions, recommends specific quantities, and alerts you to supply chain risks.
4. **Audit Reporting:** Clean reports for Low Stock, Out of Stock, printable PDF exports, and one-click AI analysis.
5. **Security:** Simple admin session verification (`admin` / `admin123`).
6. **Design:** Sleek toggleable Dark/Light themes with glassmorphism controls and animations.

---

## Directory Structure

```
.
├── backend/                  # Spring Boot application code
│   ├── src/                  # Source folders (Java & Static UI assets)
│   ├── Dockerfile            # Container configuration
│   └── pom.xml               # Dependency mappings
├── frontend/                 # Root aligned copy of UI templates (login, dashboard, etc.)
├── css/                      # Root aligned copy of custom styles
├── js/                       # Root aligned copy of client-side scripts
├── docker-compose.yml        # Orchestration configurations
└── README.md                 # System documentation (This file)
```

---

## Database Schema (MongoDB)

Collection Name: `products`

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | String | Primary Key |
| `productName` | String | Name of product (Required) |
| `sku` | String | Unique Stock Keeping Unit code (Required) |
| `category` | String | Category tag (Required) |
| `quantity` | Integer | Stock units count (>= 0) |
| `minimumStock` | Integer | Safety inventory margin (>= 0) |
| `price` | Double | Purchase unit price (> 0.00) |
| `warehouseLocation`| String | Specific shelf/aisle location |
| `description` | String | Technical specs / details |
| `supplier` | String | Manufacturer name |
| `supplierEmail` | String | Manufacturer contact email |
| `lastUpdated` | LocalDateTime | Timestamp of last modification |
| `status` | String | Calculated state: `HEALTHY` \| `LOW_STOCK` \| `OUT_OF_STOCK` |

---

## Getting Started

### 1. Setup Groq API Credentials
Go to [console.groq.com](https://console.groq.com/) to create a free API key.

Add the key to `backend/src/main/resources/application.properties`:
```properties
groq.api.key=YOUR_GROQ_API_KEY
```
*Note: If the key is left blank, the application will still compile and run, but the AI Chat Analyst will output a friendly warning prompting you to add the key.*

---

### 2. Running Locally

#### Step A: Spin up local MongoDB
Ensure you have a MongoDB instance running locally on the default port `27017`.
- Connect string: `mongodb://localhost:27017/inventory_db`

#### Step B: Start Spring Boot
Navigate to the `backend` folder and run the Maven execution script:
```powershell
# Windows PowerShell:
cd backend
.\mvnw.cmd clean spring-boot:run

# Linux / macOS Bash:
cd backend
chmod +x mvnw
./mvnw clean spring-boot:run
```

Once running, open your browser and navigate to:
👉 **[http://localhost:8080/login.html](http://localhost:8080/login.html)**

- **Default Username:** `admin`
- **Default Password:** `admin123`

---

### 3. Running via Docker Compose

You can build and spin up the complete infrastructure (MongoDB + Spring Boot Backend) using Docker Compose. Make sure Docker is running on your machine first.

In the workspace root directory:

**Windows PowerShell:**
```powershell
$env:GROQ_API_KEY="your_groq_api_key_here"
docker-compose up --build
```

**Linux / macOS Terminal:**
```bash
GROQ_API_KEY="your_groq_api_key_here" docker-compose up --build
```

Access the UI at: **[http://localhost:8080/login.html](http://localhost:8080/login.html)**

---

## API Endpoints Reference

### Authentication
- `POST /api/login` - Accepts `LoginRequest` (JSON) -> starts session.
- `POST /api/logout` - Terminates active session.
- `GET /api/session-check` - Confirms status of current session.

### Products Catalog
- `GET /api/products` - Returns paginated, searchable, sortable list of SKUs.
  - Parameters: `search` (String), `category` (String), `status` (String), `page` (int), `size` (int), `sortBy` (String), `direction` (`asc`\|`desc`).
- `GET /api/products/{id}` - Retrieve a single product.
- `POST /api/products` - Create new product (JSON validated).
- `PUT /api/products/{id}` - Modify existing product.
- `DELETE /api/products/{id}` - Remove a product record.
- `GET /api/products/low-stock` - Returns all products where `quantity <= minimumStock` and `quantity > 0`.
- `GET /api/products/out-of-stock` - Returns all products where `quantity <= 0`.
- `POST /api/products/import-sample` - Triggers database re-seeding.

### Analytics & AI
- `GET /api/dashboard` - Return high-level metrics for dashboard cards and category distribution.
- `POST /api/chat` - Call Groq API system. Accepts user question and returns markdown response.

---

## User Interface Screenshots

Below are placeholders representing key views of the application:

#### 1. Administration Portal
`[Screenshot Placeholder: Light/Dark theme glassmorphic login card with input validation alerts]`

#### 2. Operations Dashboard
`[Screenshot Placeholder: Dashboard screen featuring metric counters, inventory health progress ring, category bar chart, and recent updates feed]`

#### 3. Inventory Catalog Grid
`[Screenshot Placeholder: Product management table displaying pagination, search bars, category filters, and action buttons]`

#### 4. AI Analyst Conversation Room
`[Screenshot Placeholder: Chat window displaying styled markdown tables, quick suggestion chips, and recommendations from the Groq Agent]`
