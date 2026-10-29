package com.library_management.borrowing;

import com.library_management.common.response.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "Borrowing",
    description = "APIs for borrowing and returning books"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/borrowings")
public class BorrowingController {
  private final BorrowingService borrowingService;

  public BorrowingController(BorrowingService borrowingService) {
    this.borrowingService = borrowingService;
  }

  @Operation(
      summary = "Borrow a book",
      description = "Create a borowing record for the authenticated user."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "Book borrowed successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = BorrowingResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Book unavailable or borrowing limit exceeded",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiError.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Unauthenticated",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiError.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Book not found",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiError.class)
          )
      )
  })
  @PostMapping
  public ResponseEntity<BorrowingResponse> borrowBook(
      @RequestBody BorrowingRequest request,
      Authentication authentication
  ) {
    String username = authentication.getName();

    BorrowingResponse response = borrowingService.borrowBook(username, request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  @Operation(
      summary = "Get my borrowings",
      description = "Returns borrowing records of the authenticated user."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Borrowings retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthenticated"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @GetMapping("/me")
  public ResponseEntity<List<BorrowingResponse>> getMyBorrowings(Authentication authentication) {
    List<BorrowingResponse> responses = borrowingService.getMyBorrowings(authentication.getName());
    return ResponseEntity.ok(responses);
  }

  @Operation(
      summary = "Return a book",
      description = "Returns a borrowed book owned by the authenticated user."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Book returned successfully"),
      @ApiResponse(responseCode = "400", description = "Borrowing has already been returned"),
      @ApiResponse(responseCode = "401", description = "Unauthenticated"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Borrowing not found")
  })
  @PostMapping("/{id}/return")
  public ResponseEntity<BorrowingResponse> returnBook(
      @PathVariable Long id,
      Authentication authentication
  ) {
    String username = authentication.getName();
    BorrowingResponse response = borrowingService.returnBook(id, username);
    return ResponseEntity.ok(response);
  }
}
