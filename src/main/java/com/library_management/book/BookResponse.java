package com.library_management.book;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        String category,
        Integer quantity,
        Integer availableQuantity
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getCategory(),
                book.getQuantity(),
                book.getAvailableQuantity()
        );
    }
}
