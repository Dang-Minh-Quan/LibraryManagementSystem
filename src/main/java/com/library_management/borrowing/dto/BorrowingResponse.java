package com.library_management.borrowing;

import java.time.LocalDateTime;

public class BorrowingResponse {
  private Long id;
  private Long bookId;
  private String status;
  private LocalDateTime borrowedAt;
  private LocalDateTime dueDate;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getBookId() {
    return bookId;
  }

  public void setBookId(Long bookId) {
    this.bookId = bookId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getBorrowedAt() {
    return borrowedAt;
  }

  public void setBorrowedAt(LocalDateTime borrowedAt) {
    this.borrowedAt = borrowedAt;
  }

  public LocalDateTime getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDateTime dueDate) {
    this.dueDate = dueDate;
  }
}
