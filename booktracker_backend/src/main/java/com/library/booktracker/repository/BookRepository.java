package com.library.booktracker.repository;

import com.library.booktracker.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// This interface gives us CRUD methods for Book entity
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    // Custom query methods for MCP tools
    java.util.List<Book> findByTitle(String title);

    java.util.List<Book> findByAuthor(String author);
}
