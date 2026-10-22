package com.library_management.borrowing;

import com.library_management.book.Book;
import com.library_management.book.BookRepository;
import com.library_management.common.exception.BorrowingException;
import com.library_management.common.exception.ResourceNotFoundException;
import com.library_management.user.User;
import com.library_management.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BorrowingService {
  private final BorrowingRepository borrowingRepository;
  private final BookRepository bookRepository;
  private final UserRepository userRepository;

  public BorrowingService(BorrowingRepository borrowingRepository, BookRepository bookRepository,
      UserRepository userRepository) {
    this.borrowingRepository = borrowingRepository;
    this.bookRepository = bookRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public BorrowingResponse borrowBook(String username, BorrowBookRequest request) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() ->
            new ResourceNotFoundException("User not found"));

    Book book = bookRepository.findById(request.getBookId())
        .orElseThrow(() ->
            new ResourceNotFoundException("Book not found"));

    if(book.getAvailableQuantity() <= 0) {
      throw new BorrowingException(HttpStatus.BAD_REQUEST,"Book unavailable!");
    }

    long activeBorrowing =
        borrowingRepository.countByUserIdAndStatus(user.getId(), BorrowingStatus.BORROWING);

    if(activeBorrowing >= 5) {
      throw new BorrowingException(HttpStatus.BAD_REQUEST,"Borrowing limit exceeded!");
    }

    LocalDateTime now = LocalDateTime.now();
    Borrowing borrowing = new Borrowing();
    borrowing.setUser(user);
    borrowing.setBook(book);
    borrowing.setBorrowedAt(now);
    borrowing.setReturnedAt(null);
    borrowing.setStatus(BorrowingStatus.BORROWING);

    Borrowing saved = borrowingRepository.save(borrowing);

    book.setAvailableQuantity(book.getAvailableQuantity() - 1);

    return new BorrowingResponse(
        saved.getId(),
        book.getId(),
        saved.getStatus().name(),
        saved.getBorrowedAt(),
        saved.getDueDate()
    );
  }

  @Transactional(readOnly = true)
  public List<BorrowingResponse> getMyBorrowings(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() ->
            new ResourceNotFoundException("User not found"));

    List<Borrowing> borrowings = borrowingRepository.findByUserId(user.getId());

    return  borrowings.stream()
        .map(borrowing -> new BorrowingResponse(
            borrowing.getId(),
            borrowing.getBook().getId(),
            borrowing.getStatus().name(),
            borrowing.getBorrowedAt(),
            borrowing.getDueDate()
        ))
        .toList();
  }
}
