package com.library_management.borrowing;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/borrowings")
public class BorrowingController {
  private final BorrowingService borrowingService;

  public BorrowingController(BorrowingService borrowingService) {
    this.borrowingService = borrowingService;
  }

  @PostMapping
  public ResponseEntity<BorrowingResponse> borrowBook(
      @RequestBody BorrowBookRequest request,
      Authentication authentication
  ) {
    String username = authentication.getName();

    BorrowingResponse response = borrowingService.borrowBook(username, request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  @GetMapping("/me")
  public ResponseEntity<List<BorrowingResponse>> getMyBorrowings(Authentication authentication) {
    List<BorrowingResponse> responses = borrowingService.getMyBorrowings(authentication.getName());
    return ResponseEntity.ok(responses);
  }
}
