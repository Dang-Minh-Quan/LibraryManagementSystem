package com.library_management.borrowing;

import com.library_management.book.Book;
import com.library_management.book.BookRepository;
import com.library_management.common.exception.BorrowingException;
import com.library_management.common.exception.ResourceNotFoundException;
import com.library_management.user.User;
import com.library_management.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BorrowingServiceTest {
  @Mock
  private BorrowingRepository borrowingRepository;

  @Mock
  private BookRepository bookRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private BorrowingService borrowingService;

  private User user;
  private Book book;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId(1L);

    book = new Book();
    book.setId(5L);
    book.setAvailableQuantity(3);
  }

  @Test
  void borrowBook_success() {
    when(userRepository.findByUsername("dangminhquan"))
        .thenReturn(Optional.of(user));

    when(bookRepository.findById(5L))
        .thenReturn(Optional.of(book));

    when(borrowingRepository.countByUserIdAndStatus(
        1L, BorrowingStatus.BORROWING))
        .thenReturn(0L);

    when(borrowingRepository.save(any(Borrowing.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    BorrowingRequest request = new BorrowingRequest();
    request.setBookId(5L);

    BorrowingResponse response = borrowingService.borrowBook(
        "dangminhquan", request);

    assertThat(response.getBookId()).isEqualTo(5L);
    assertThat(response.getStatus()).isEqualTo("BORROWING");
    assertThat(book.getAvailableQuantity()).isEqualTo(2);

    verify(borrowingRepository).save(any(Borrowing.class));
  }

  @Test
  void borrowBook_bookNotFound() {
    when(userRepository.findByUsername("dangminhquan"))
        .thenReturn(Optional.of(user));

    when(bookRepository.findById(5L))
        .thenReturn(Optional.empty());

    BorrowingRequest request = new BorrowingRequest();
    request.setBookId(5L);

    assertThatThrownBy(() ->
        borrowingService.borrowBook(
            "dangminhquan", request))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(borrowingRepository, never()).save(any());
    assertThat(book.getAvailableQuantity()).isEqualTo(3);
  }

  @Test
  void borrowBook_bookUnavailable() {
    when(userRepository.findByUsername("dangminhquan"))
        .thenReturn(Optional.of(user));

    book.setAvailableQuantity(0);

    when(bookRepository.findById(5L))
        .thenReturn(Optional.of(book));

    BorrowingRequest request = new BorrowingRequest();
    request.setBookId(5L);

    assertThatThrownBy(() ->
        borrowingService.borrowBook(
            "dangminhquan", request))
        .isInstanceOf(BorrowingException.class);

    verify(borrowingRepository, never()).save(any());
    assertThat(book.getAvailableQuantity()).isZero();
  }

  @Test
  void borrowBook_borrowingLimitExceeded() {
    when(userRepository.findByUsername("dangminhquan"))
        .thenReturn(Optional.of(user));

    when(bookRepository.findById(5L))
        .thenReturn(Optional.of(book));

    when(borrowingRepository.countByUserIdAndStatus(
        1L, BorrowingStatus.BORROWING))
        .thenReturn(5L);

    BorrowingRequest request = new BorrowingRequest();
    request.setBookId(5L);

    assertThatThrownBy(() ->
        borrowingService.borrowBook(
            "dangminhquan", request))
        .isInstanceOf(BorrowingException.class);

    verify(borrowingRepository, never()).save(any());
    assertThat(book.getAvailableQuantity()).isEqualTo(3);
  }

  @Test
  void returnBook_success() {
    when(userRepository.findByUsername("dangminhquan"))
        .thenReturn(Optional.of(user));

    Borrowing borrowing = new Borrowing();
    borrowing.setId(10L);
    borrowing.setUser(user);
    borrowing.setBook(book);
    borrowing.setStatus(BorrowingStatus.BORROWING);

    when(borrowingRepository.findById(10L))
        .thenReturn(Optional.of(borrowing));

    book.setAvailableQuantity(2);

    BorrowingResponse response =
        borrowingService.returnBook(10L, "dangminhquan");

    assertThat(response.getStatus()).isEqualTo("RETURNED");
    assertThat(borrowing.getReturnedAt()).isNotNull();
    assertThat(book.getAvailableQuantity()).isEqualTo(3);
  }

  @Test
  void returnBook_borrowingNotFound() {
    when(borrowingRepository.findById(10L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() ->
        borrowingService.returnBook(10L, "dangminhquan"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void returnBook_wrongUser() {
    when(userRepository.findByUsername("dangminhquan"))
        .thenReturn(Optional.of(user));

    User anotherUser = new User();
    anotherUser.setId(2L);

    Borrowing borrowing = new Borrowing();
    borrowing.setId(10L);
    borrowing.setUser(anotherUser);
    borrowing.setBook(book);
    borrowing.setStatus(BorrowingStatus.BORROWING);

    when(borrowingRepository.findById(10L))
        .thenReturn(Optional.of(borrowing));

    assertThatThrownBy(() ->
        borrowingService.returnBook(10L, "dangminhquan"))
        .isInstanceOf(BorrowingException.class);

    assertThat(borrowing.getStatus())
        .isEqualTo(BorrowingStatus.BORROWING);
  }

  @Test
  void returnBook_alreadyReturned() {
    when(userRepository.findByUsername("dangminhquan"))
        .thenReturn(Optional.of(user));

    Borrowing borrowing = new Borrowing();
    borrowing.setId(10L);
    borrowing.setUser(user);
    borrowing.setBook(book);
    borrowing.setStatus(BorrowingStatus.RETURNED);

    when(borrowingRepository.findById(10L))
        .thenReturn(Optional.of(borrowing));

    assertThatThrownBy(() ->
        borrowingService.returnBook(10L, "dangminhquan"))
        .isInstanceOf(BorrowingException.class);

    assertThat(book.getAvailableQuantity()).isEqualTo(3);
  }
}
