package com.library_management.book;

import com.library_management.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * POST /api/books và DELETE /api/books/{id} theo thỏa thuận dành cho Admin.
 * Phân quyền chưa được tích hợp vì Security/JWT module chưa hoàn thiện.
 * Teammate Auth/Security cần bổ sung @PreAuthorize("hasRole('ADMIN')") hoặc
 * cấu hình SecurityFilterChain tương ứng khi module đó sẵn sàng.
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Book management API")
public class BookController {

    private final BookService bookService;

    @GetMapping
    @Operation(summary = "Get all books")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getAllBooks() {
        return ResponseEntity.ok(ApiResponse.ok(bookService.getAllBooks()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<ApiResponse<BookResponse>> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bookService.getBookById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new book (Admin only – authorization pending)")
    public ResponseEntity<ApiResponse<BookResponse>> createBook(
            @Valid @RequestBody BookRequest request) {
        BookResponse created = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Book created", created));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book (Admin only – authorization pending)")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.ok("Book deleted", null));
    }
}
