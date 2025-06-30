# What is an MCP Server? (Beginner-Friendly Guide)

## 1. Introduction: What is MCP?

**MCP** stands for **Model Context Protocol**. It is a new open standard that allows AI models (like Claude, ChatGPT, etc.) to connect to external tools, databases, and business logic in a secure, standardized way. Think of it as a bridge between AI and your backend code, so the AI can do more than just chat—it can fetch real data, run business logic, and interact with your app securely.

- **MCP Server:** A backend service that exposes your app's logic and data to AI models via the MCP protocol.
- **MCP Client:** A program (could be an AI model, another app, or a desktop tool) that connects to the MCP server to use its tools and data.

---

## 2. Why Use an MCP Server?

- **Let AI interact with your real data** (e.g., books in your database)
- **Expose business logic as tools** (e.g., search for a book, add a book)
- **Standardized, secure, and easy to connect**

---

## 3. How is MCP Implemented in This Project?

This project uses **Spring Boot** (a popular Java framework) and **Spring AI** (a library for AI integrations) to create an MCP server for a book tracker app.

### **Project Structure Overview**

```
booktracker_backend/
  ├── src/main/java/com/library/booktracker/
  │     ├── BooktrackerApplication.java         # Main Spring Boot app (MCP server)
  │     ├── controller/BookStreamController.java # REST endpoints for books
  │     ├── model/Book.java                    # Book entity (database model)
  │     ├── repository/BookRepository.java      # Database access (Spring Data JPA)
  │     ├── service/BookTools.java              # Business logic for books
  │     └── ...
  └── src/main/resources/
        ├── application.properties              # App configuration
        └── ...
```

### **How the MCP Server Works Here**
- The **Spring Boot app** runs a web server (usually on port 8080).
- It exposes endpoints (URLs) for CRUD operations on books (Create, Read, Update, Delete).
- With **Spring AI MCP integration**, these endpoints can be accessed by AI models (like Claude) using the MCP protocol.
- The AI can now "call" your backend tools (like searching for a book) as if they were built-in features.

---

## 4. Step-by-Step: How the Server is Built

### **A. Dependencies (What's Installed)**

- **Spring Boot:** The main Java framework for building web servers.
- **Spring Web:** Lets you create REST APIs (web endpoints).
- **Spring Data JPA:** Makes it easy to talk to databases (like PostgreSQL).
- **PostgreSQL Driver:** Allows Java to connect to a PostgreSQL database.
- **Spring AI (MCP):** Adds support for the Model Context Protocol, so AI models can connect.

**You'll see these in your `pom.xml` (project file):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
</dependency>
```

### **B. Prerequisites (What You Need Installed)**
- **Java 17+** (or whatever version your project uses)
- **Maven** (for building the project)
- **PostgreSQL** (the database)

### **C. How the Code is Organized**
- **Book.java:** Defines what a Book is (title, author, etc.)
- **BookRepository.java:** Handles saving/finding books in the database.
- **BookTools.java:** Contains business logic (e.g., find books by title).
- **BookStreamController.java:** Exposes REST endpoints for books.
- **BooktrackerApplication.java:** The main entry point; starts the server.

### **D. How MCP is Enabled**
- The `spring-ai-mcp-server-spring-boot-starter` dependency automatically exposes your REST endpoints as MCP tools.
- When you run the server, it advertises its tools (endpoints) to any MCP client (like Claude Desktop).

---

## 5. How to Run the MCP Server

1. **Install prerequisites:** Java, Maven, PostgreSQL.
2. **Configure your database:** Set your DB connection in `src/main/resources/application.properties`.
3. **Build the project:**
   ```sh
   ./mvnw clean package
   ```
4. **Run the server:**
   ```sh
   ./mvnw spring-boot:run
   ```
   or
   ```sh
   java -jar target/booktracker-*.jar
   ```
5. **The server will start on port 8080 (by default).**
6. **Connect an MCP client (like Claude Desktop) to your server's address.**

---

## 6. How Does the AI Use the MCP Server?
- The AI (via an MCP client) connects to your server.
- It "sees" the available tools (like `findBookByTitle`, `addBook`, etc.).
- When you ask the AI to do something ("Find me all books by Tolkien"), it calls the right tool on your server.
- Your server runs the logic, fetches data from the database, and returns the result to the AI.

---

## 7. Key Takeaways
- **MCP server** = a backend that exposes your app's logic and data to AI models in a standard way.
- **Spring Boot + Spring AI** makes it easy to build an MCP server in Java.
- **Your project** is a working example: it exposes book-tracking tools to any AI that speaks MCP.
- **You control what the AI can do** by controlling which endpoints/tools you expose.

---

## 8. Further Reading & Resources
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/index.html)
- [Model Context Protocol (MCP) Spec](https://github.com/modelcontext/protocol)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)

---

## 9. FAQ

**Q: Do I need to know AI to run an MCP server?**  
A: No! You just need to know how to run a Spring Boot app. The AI connects to you.

**Q: Is my data safe?**  
A: You control what endpoints/tools are exposed. Only those are accessible to the AI.

**Q: Can I add more tools?**  
A: Yes! Add more endpoints or business logic, and they'll be available to the AI (as supported by Spring AI MCP).

---

## Line-by-Line Explanation: `McpServerController.java`

This section explains the main MCP server controller in detail, so you understand exactly how it works and how it implements the MCP JSON-RPC protocol.

### **Class Overview**
- The `McpServerController` is a Spring `@RestController` that exposes a single `/mcp` POST endpoint.
- It handles all MCP protocol requests using the JSON-RPC 2.0 standard.
- It routes requests based on the `method` field in the JSON payload, not the URL.
- It uses a `BookTools` service to perform book-related operations.

### **Imports and Class Declaration**
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.booktracker.service.BookTools;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.*;

@RestController
public class McpServerController {
```
- **Imports**: Bring in required classes for JSON handling, Spring Web, and reactive programming.
- **@RestController**: Marks this class as a REST controller in Spring.
- **public class McpServerController**: Declares the controller class.

### **Fields and Constructor**
```java
    private final BookTools tools;
    private final ObjectMapper om = new ObjectMapper();

    public McpServerController(BookTools tools) {
        this.tools = tools;
    }
```
- **BookTools tools**: Service for book operations (CRUD, search, etc.).
- **ObjectMapper om**: Used to convert Java objects to JSON strings.
- **Constructor**: Injects the `BookTools` dependency.

### **Main Endpoint: /mcp**
```java
    @PostMapping(value = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> handleJsonRpc(@RequestBody Map<String, Object> request) {
        // ...
    }
```
- **@PostMapping**: Exposes a POST endpoint at `/mcp` that accepts and returns JSON.
- **handleJsonRpc**: Main handler for all MCP JSON-RPC requests.
- **Mono<Map<String, Object>>**: Returns a reactive (async) response with a JSON object.

### **Request Validation and Routing**
```java
        // Validate JSON-RPC version
        String jsonrpc = (String) request.get("jsonrpc");
        if (!"2.0".equals(jsonrpc)) {
            Object id = request.get("id");
            return Mono.just(jsonRpcError(id, -32600, "Invalid Request - jsonrpc must be 2.0"));
        }

        String method = (String) request.get("method");
        Object id = request.get("id");
        Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", Map.of());

        // Handle null method
        if (method == null) {
            return Mono.just(jsonRpcError(id, -32600, "Invalid Request - method is required"));
        }

        switch (method) {
            case "initialize":
                return handleInitialize(id, params);
            case "tools/list":
                return handleToolsList(id);
            case "tools/call":
                return handleToolsCall(id, params);
            default:
                return Mono.just(jsonRpcError(id, -32601, "Method not found: " + method));
        }
```
- **Validate JSON-RPC version**: Ensures the request uses JSON-RPC 2.0.
- **Extract method, id, params**: Reads the method name, request id, and parameters from the request.
- **Null check for method**: Returns an error if the method is missing.
- **Switch on method**: Routes to the correct handler based on the method name (`initialize`, `tools/list`, `tools/call`).
- **Default**: Returns a JSON-RPC error for unknown methods.

### **Handlers for Each Method**
#### **Initialize**
```java
    private Mono<Map<String, Object>> handleInitialize(Object id, Map<String, Object> params) {
        return Mono.just(jsonRpcResult(id, Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(
                        "tools", Map.of()
                ),
                "serverInfo", Map.of(
                        "name", "booktracker-mcp-server",
                        "version", "1.0.0"
                )
        )));
    }
```
- **handleInitialize**: Responds to the `initialize` method, returning protocol version, capabilities, and server info.

#### **List Tools**
```java
    private Mono<Map<String, Object>> handleToolsList(Object id) {
        return Mono.just(jsonRpcResult(id, Map.of(
                "tools", List.of(
                    // ... tool definitions ...
                )
        )));
    }
```
- **handleToolsList**: Responds to `tools/list`, returning a list of available tools (functions) with their schemas.

#### **Call Tool**
```java
    private Mono<Map<String, Object>> handleToolsCall(Object id, Map<String, Object> params) {
        String name = (String) params.get("name");
        Map<String, Object> args = (Map<String, Object>) params.getOrDefault("arguments", Map.of());

        if (name == null) {
            return Mono.just(jsonRpcError(id, -32602, "Invalid params - tool name is required"));
        }

        return Mono.fromCallable(() -> {
            Object result = executeBookTool(name, args);
            return jsonRpcResult(id, Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", om.writeValueAsString(result)
                ))
            ));
        })
        .onErrorResume(IllegalArgumentException.class, e ->
            Mono.just(jsonRpcError(id, -32602, "Invalid tool name: " + e.getMessage())))
        .onErrorResume(Exception.class, e ->
            Mono.just(jsonRpcError(id, -32603, "Internal error: " + e.getMessage())))
        .subscribeOn(Schedulers.boundedElastic());
    }
```
- **handleToolsCall**: Responds to `tools/call`, executes the requested tool, and returns the result.
- **Error handling**: Returns JSON-RPC errors for invalid tool names or internal errors.

### **Tool Definitions**
```java
    private Map<String, Object> tool(String name, String description, Map<String, Object> properties, List<String> required) {
        return Map.of(
            "name", name,
            "description", description,
            "inputSchema", Map.of(
                "type", "object",
                "properties", properties,
                "required", required
            )
        );
    }
```
- **tool**: Helper to define a tool's name, description, input schema (parameters), and required fields.

### **Book Tool Execution**
```java
    private Object executeBookTool(String name, Map<String, Object> args) {
        try {
            return switch (name) {
                case "getAllBooks" -> { ... }
                case "getBookById" -> { ... }
                case "createBook" -> { ... }
                case "updateBook" -> { ... }
                case "deleteBook" -> { ... }
                case "findBookByTitle" -> { ... }
                case "findBookByAuthor" -> { ... }
                default -> throw new IllegalArgumentException("Unknown tool: " + name);
            };
        } catch (Exception e) {
            throw new RuntimeException("Error executing tool '" + name + "': " + e.getMessage(), e);
        }
    }
```
- **executeBookTool**: Runs the requested tool by name, using the `BookTools` service. Throws errors for unknown tools or missing/invalid parameters.

### **Parameter Extraction Helpers**
```java
    private Long extractLongParam(Map<String, Object> args, String key) { ... }
    private String extractStringParam(Map<String, Object> args, String key) { ... }
```
- **extractLongParam / extractStringParam**: Safely extract and validate parameters from the arguments map.

### **JSON-RPC Response Helpers**
```java
    private Map<String, Object> jsonRpcResult(Object id, Object result) { ... }
    private Map<String, Object> jsonRpcError(Object id, int code, String message) { ... }
```
- **jsonRpcResult**: Builds a JSON-RPC success response, including the result.
- **jsonRpcError**: Builds a JSON-RPC error response, including error code and message. Handles null `id` safely.

---

**In summary:**
- The controller exposes a single `/mcp` endpoint for all MCP protocol requests.
- It validates and routes JSON-RPC requests by method.
- It defines and executes book-related tools.
- It handles errors and builds proper JSON-RPC responses.
- All logic is organized for clarity, extensibility, and MCP compliance.

---

**You now understand what an MCP server is, how it's built in this project, and how it connects your backend to AI models!** 