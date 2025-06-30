package com.library.booktracker.controller;

import com.library.booktracker.model.Book;
import com.library.booktracker.service.BookTools;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
public class BookStreamController {

    private final BookTools tools;

    public BookStreamController(BookTools tools) {
        this.tools = tools;
    }

    /* ----------  Hot event sink  ---------- */
    private final Sinks.Many<Book> sink =
            Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);

    /* ----------  SSE endpoint  ---------- */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Book> stream() {
        // merge ping heartbeat to keep LB connections alive
        return Flux.merge(
                sink.asFlux(),
                Flux.interval(Duration.ofSeconds(15)).map(i -> new Book("ping", "", "")));
    }

    /* ----------  CRUD endpoints (reactive) ---------- */

    @GetMapping
    public Flux<Book> all() { return tools.listAllBooks(); }

    @GetMapping("/{id}")
    public Flux<Book> byId(@PathVariable Long id) { return tools.getBookById(id).flux(); }

    @PostMapping
    public Flux<Book> create(@RequestBody Book b) {
        return tools.createBook(b.getTitle(), b.getAuthor(), b.getIsbn())
                .doOnNext(sink::tryEmitNext)
                .flux();
    }

    @PutMapping("/{id}")
    public Flux<Book> update(@PathVariable Long id, @RequestBody Book b) {
        return tools.updateBook(id, b.getTitle(), b.getAuthor(), b.getIsbn())
                .doOnNext(sink::tryEmitNext)
                .flux();
    }

    @DeleteMapping("/{id}")
    public Flux<Void> delete(@PathVariable Long id) {
        return tools.deleteBook(id)
                .doOnTerminate(() -> sink.tryEmitNext(
                        new Book("deleted#" + id, "", "")))
                .flux();
    }
}
