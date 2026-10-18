package com.library_management.book;

import com.library_management.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(BookResponse::from)
                .toList();
    }

    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        return BookResponse.from(book);
    }

    public BookResponse createBook(BookRequest request) {
        if (request.availableQuantity() > request.quantity()) {
            throw new IllegalArgumentException(
                    "availableQuantity must not exceed quantity");
        }

        Book book = Book.builder()
                .title(request.title())
                .author(request.author())
                .isbn(request.isbn())
                .category(request.category())
                .quantity(request.quantity())
                .availableQuantity(request.availableQuantity())
                .build();

        return BookResponse.from(bookRepository.save(book));
    }

    /**
     * Xóa sách theo id.
     * Lưu ý: Hiện tại chưa kiểm tra bản ghi mượn liên quan vì module Borrowing
     * chưa được triển khai. Nhóm cần thống nhất quy tắc trước khi bổ sung check này.
     */
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }
}
