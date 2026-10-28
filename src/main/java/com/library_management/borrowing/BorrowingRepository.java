package com.library_management.borrowing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {
  List<Borrowing> findByUserId(Long userId);
  List<Borrowing> findByUserIdAndStatus(Long userId, BorrowingStatus status);
  long countByUserIdAndStatus(Long userId, BorrowingStatus status);
}
