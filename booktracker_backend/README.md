# Book Tracker Backend

A Spring Boot application that provides both REST API and MCP (Model Context Protocol) server functionality for managing books.

## Features

- **REST API**: Traditional HTTP endpoints for book management
- **MCP Server**: Protocol-compliant server for AI tool integration
- **PostgreSQL Database**: Persistent storage for book data
- **CRUD Operations**: Complete book management functionality

## Prerequisites

- Java 21+
- PostgreSQL database running on localhost:5432
- Database named `library_db`
- PostgreSQL user: `postgres` with password: `password`

## Running the Application

```bash
# Using Maven wrapper
./mvnw.cmd spring-boot:run

# Or if Maven is installed
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## REST API Endpoints

### Books Management

- `GET /api/books` - Get all books
- `GET /api/books/{id}` - Get book by ID
- `POST /api/books` - Create new book
- `PUT /api/books/{id}` - Update existing book
- `DELETE /api/books/{id}` - Delete book

### Example Book JSON

```json
{
  "title": "The Great Gatsby",
  "author": "F. Scott Fitzgerald",
  "isbn": "978-0-7432-7356-5"
}
```

## MCP Server Endpoints

The MCP server provides the same functionality as REST API but follows the MCP protocol specification.

### MCP Protocol Endpoints

- `POST /mcp/initialize` - Initialize MCP server connection
- `POST /mcp/tools/list` - List available tools
- `POST /mcp/tools/call` - Execute tool calls

### Available MCP Tools

1. **getAllBooks** - Get all books from database
2. **getBookById** - Get book by ID
   - Parameters: `id` (number)
3. **createBook** - Create new book
   - Parameters: `title` (string), `author` (string), `isbn` (string)
4. **updateBook** - Update existing book
   - Parameters: `id` (number), `title` (string), `author` (string), `isbn` (string)
5. **deleteBook** - Delete book by ID
   - Parameters: `id` (number)
6. **findBookByTitle** - Find books by title
   - Parameters: `title` (string)
7. **findBookByAuthor** - Find books by author
   - Parameters: `author` (string)

### MCP Tool Call Example

```json
{
  "name": "createBook",
  "arguments": {
    "title": "1984",
    "author": "George Orwell",
    "isbn": "978-0-452-28423-4"
  }
}
```

## Database Setup

Create PostgreSQL database:

```sql
CREATE DATABASE library_db;
```

The application will automatically create the `book` table with the following structure:

- `id` (BIGINT, Primary Key, Auto-increment)
- `title` (VARCHAR)
- `author` (VARCHAR)
- `isbn` (VARCHAR)

## Configuration

Database configuration in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/library_db
spring.datasource.username=postgres
spring.datasource.password=password
```

## API Documentation

When running, visit `http://localhost:8080/swagger-ui.html` for interactive API documentation.

## Architecture

- **Controller Layer**: REST endpoints (`BookController`) and MCP endpoints (`McpServerController`)
- **Service Layer**: Business logic (`BookTools`)
- **Repository Layer**: Data access (`BookRepository`)
- **Model Layer**: Entity definitions (`Book`)

## Error Handling

The application includes basic error handling:

- Invalid book IDs return `null`
- MCP tool errors are wrapped in error responses
- Database connection issues are logged

## Development

The application uses Spring Boot DevTools for hot reloading during development.
