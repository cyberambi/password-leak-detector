package com.passwordleakdetector.service;

import com.passwordleakdetector.dto.password.HistoryEntryDetailResponse;
import com.passwordleakdetector.dto.password.HistoryEntryRequest;
import com.passwordleakdetector.dto.password.HistoryEntryResponse;
import com.passwordleakdetector.entity.PasswordHistoryEntry;
import com.passwordleakdetector.entity.User;
import com.passwordleakdetector.exception.ResourceNotFoundException;
import com.passwordleakdetector.repository.PasswordHistoryRepository;
import com.passwordleakdetector.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * All queries here are scoped to the authenticated caller's own userId - never
 * trust an entry id alone, always look it up via findByIdAndUserId so one
 * user can never read/modify/delete another user's saved entries. A missing
 * or foreign-owned entry both surface as 404, not 403, to avoid confirming
 * that an id belongs to someone else.
 */
@Service
public class PasswordHistoryService {

    private final PasswordHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public PasswordHistoryService(PasswordHistoryRepository historyRepository,
                                   UserRepository userRepository,
                                   EncryptionService encryptionService) {
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public List<HistoryEntryResponse> list(Long userId) {
        return historyRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(HistoryEntryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public HistoryEntryDetailResponse getWithPassword(Long id, Long userId) {
        PasswordHistoryEntry entry = findOwned(id, userId);
        String password = encryptionService.decrypt(entry.getEncryptedPassword(), entry.getEncryptionIv());
        return toDetailResponse(entry, password);
    }

    @Transactional
    public HistoryEntryResponse create(Long userId, HistoryEntryRequest request) {
        User userRef = userRepository.getReferenceById(userId);
        EncryptionService.EncryptedValue encrypted = encryptionService.encrypt(request.password());

        PasswordHistoryEntry entry = new PasswordHistoryEntry();
        entry.setUser(userRef);
        applyRequest(entry, request, encrypted);

        entry = historyRepository.save(entry);
        return HistoryEntryResponse.from(entry);
    }

    @Transactional
    public HistoryEntryResponse update(Long id, Long userId, HistoryEntryRequest request) {
        PasswordHistoryEntry entry = findOwned(id, userId);
        EncryptionService.EncryptedValue encrypted = encryptionService.encrypt(request.password());
        applyRequest(entry, request, encrypted);
        entry = historyRepository.save(entry);
        return HistoryEntryResponse.from(entry);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        PasswordHistoryEntry entry = findOwned(id, userId);
        historyRepository.delete(entry);
    }

    private PasswordHistoryEntry findOwned(Long id, Long userId) {
        return historyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No saved password entry with id " + id));
    }

    private void applyRequest(PasswordHistoryEntry entry, HistoryEntryRequest request,
                               EncryptionService.EncryptedValue encrypted) {
        entry.setSiteName(request.siteName());
        entry.setSiteUrl(request.siteUrl());
        entry.setSiteUsername(request.siteUsername());
        entry.setNotes(request.notes());
        entry.setEncryptedPassword(encrypted.ciphertext());
        entry.setEncryptionIv(encrypted.iv());
    }

    private HistoryEntryDetailResponse toDetailResponse(PasswordHistoryEntry entry, String password) {
        return new HistoryEntryDetailResponse(
                entry.getId(),
                entry.getSiteName(),
                entry.getSiteUrl(),
                entry.getSiteUsername(),
                password,
                entry.getNotes(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }
}
