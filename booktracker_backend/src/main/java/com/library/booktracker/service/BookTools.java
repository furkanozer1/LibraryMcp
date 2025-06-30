package com.library.booktracker.service;

import com.library.booktracker.model.Book;
import com.library.booktracker.repository.BookRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class BookTools {

    private final BookRepository repo;

    public BookTools(BookRepository repo) {
        this.repo = repo;
    }

    /* ----------  READ  ---------- */

    public Flux<Book> listAllBooks() {
        return Mono.fromCallable(repo::findAll)
                .flatMapMany(Flux::fromIterable)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Book> getBookById(Long id) {
        return Mono.fromCallable(() -> repo.findById(id).orElse(null))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<Book> findBookByTitle(String title) {
        return Mono.fromCallable(() -> repo.findByTitle(title))
                .flatMapMany(Flux::fromIterable)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<Book> findBookByAuthor(String author) {
        return Mono.fromCallable(() -> repo.findByAuthor(author))
                .flatMapMany(Flux::fromIterable)
                .subscribeOn(Schedulers.boundedElastic());
    }

    /* ----------  WRITE  ---------- */

    public Mono<Book> createBook(String t, String a, String i) {
        return Mono.fromCallable(() -> repo.save(new Book(t, a, i)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Book> updateBook(Long id, String t, String a, String i) {
        return Mono.fromCallable(() ->
                        repo.findById(id).map(b -> {
                            b.setTitle(t); b.setAuthor(a); b.setIsbn(i);
                            return repo.save(b);
                        }).orElse(null))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteBook(Long id) {
        return Mono.fromRunnable(() -> repo.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
