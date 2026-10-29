package com.library_management.common.exception;

import org.springframework.http.HttpStatus;

public class BorrowingException extends RuntimeException {
    private final HttpStatus status;

    public BorrowingException(HttpStatus status, String message) {
      super(message);
      this.status = status;
    }

    public HttpStatus getStatus() {
      return status;
    }
}
