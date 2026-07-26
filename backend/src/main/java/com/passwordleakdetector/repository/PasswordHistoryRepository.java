package com.passwordleakdetector.repository;

import com.passwordleakdetector.entity.PasswordHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntry, Long> {

    List<PasswordHistoryEntry> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<PasswordHistoryEntry> findByIdAndUserId(Long id, Long userId);
}
