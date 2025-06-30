package com.library.booktracker.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.booktracker.service.BookTools;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

@RestController
public class McpServerController {

    private final BookTools tools;
    private final ObjectMapper om = new ObjectMapper();

    public McpServerController(BookTools tools) {
        this.tools = tools;
    }

    @PostMapping(value = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> handleJsonRpc(@RequestBody Map<String, Object> request) {
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
    }

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

    private Mono<Map<String, Object>> handleToolsList(Object id) {
        return Mono.just(jsonRpcResult(id, Map.of(
                "tools", List.of(
                        tool("getAllBooks", "Get all books", Map.of(), List.of()),
                        
                        tool("getBookById", "Get book by ID", 
                            Map.of("id", Map.of("type", "number", "description", "Book ID")), 
                            List.of("id")),
                        
                        tool("createBook", "Create a new book", 
                            Map.of(
                                "title", Map.of("type", "string", "description", "Book title"),
                                "author", Map.of("type", "string", "description", "Book author"),
                                "isbn", Map.of("type", "string", "description", "Book ISBN")
                            ), 
                            List.of("title", "author", "isbn")),
                        
                        tool("updateBook", "Update an existing book", 
                            Map.of(
                                "id", Map.of("type", "number", "description", "Book ID"),
                                "title", Map.of("type", "string", "description", "Book title"),
                                "author", Map.of("type", "string", "description", "Book author"),
                                "isbn", Map.of("type", "string", "description", "Book ISBN")
                            ), 
                            List.of("id", "title", "author", "isbn")),
                        
                        tool("deleteBook", "Delete a book by ID", 
                            Map.of("id", Map.of("type", "number", "description", "Book ID")), 
                            List.of("id")),
                        
                        tool("findBookByTitle", "Find books by title", 
                            Map.of("title", Map.of("type", "string", "description", "Book title to search for")), 
                            List.of("title")),
                        
                        tool("findBookByAuthor", "Find books by author", 
                            Map.of("author", Map.of("type", "string", "description", "Author name to search for")), 
                            List.of("author"))
                )
        )));
    }

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

    private Object executeBookTool(String name, Map<String, Object> args) {
        try {
            return switch (name) {
                case "getAllBooks" -> {
                    var result = tools.listAllBooks().collectList().blockOptional();
                    yield result.orElse(List.of());
                }
                case "getBookById" -> {
                    Long id = extractLongParam(args, "id");
                    var result = tools.getBookById(id).blockOptional();
                    yield result.orElse(null);
                }
                case "createBook" -> {
                    String title = extractStringParam(args, "title");
                    String author = extractStringParam(args, "author");
                    String isbn = extractStringParam(args, "isbn");
                    var result = tools.createBook(title, author, isbn).blockOptional();
                    yield result.orElse(null);
                }
                case "updateBook" -> {
                    Long id = extractLongParam(args, "id");
                    String title = extractStringParam(args, "title");
                    String author = extractStringParam(args, "author");
                    String isbn = extractStringParam(args, "isbn");
                    var result = tools.updateBook(id, title, author, isbn).blockOptional();
                    yield result.orElse(null);
                }
                case "deleteBook" -> {
                    Long id = extractLongParam(args, "id");
                    tools.deleteBook(id).block();
                    yield Map.of("success", true, "message", "Book deleted successfully");
                }
                case "findBookByTitle" -> {
                    String title = extractStringParam(args, "title");
                    var result = tools.findBookByTitle(title).collectList().blockOptional();
                    yield result.orElse(List.of());
                }
                case "findBookByAuthor" -> {
                    String author = extractStringParam(args, "author");
                    var result = tools.findBookByAuthor(author).collectList().blockOptional();
                    yield result.orElse(List.of());
                }
                default -> throw new IllegalArgumentException("Unknown tool: " + name);
            };
        } catch (Exception e) {
            throw new RuntimeException("Error executing tool '" + name + "': " + e.getMessage(), e);
        }
    }

    /* ---------- Helper Methods ---------- */

    private Map<String, Object> jsonRpcResult(Object id, Object result) {
        Map<String, Object> map = new HashMap<>();
        map.put("jsonrpc", "2.0");
        if (id != null) map.put("id", id);
        map.put("result", result);
        return map;
    }

    private Map<String, Object> jsonRpcError(Object id, int code, String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("jsonrpc", "2.0");
        if (id != null) map.put("id", id);
        map.put("error", Map.of("code", code, "message", message));
        return map;
    }

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

    private Long extractLongParam(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for parameter '" + key + "': " + value);
        }
    }

    private String extractStringParam(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return value.toString();
    }
}