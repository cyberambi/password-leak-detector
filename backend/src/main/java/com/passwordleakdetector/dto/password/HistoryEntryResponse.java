package com.passwordleakdetector.dto.password;

import com.passwordleakdetector.entity.PasswordHistoryEntry;

import java.time.Instant;

/** Metadata-only view of a saved entry - never includes the decrypted password. */
public record HistoryEntryResponse(
        Long id,
        String siteName,
        String siteUrl,
        String siteUsername,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {

    public static HistoryEntryResponse from(PasswordHistoryEntry entry) {
        return new HistoryEntryResponse(
                entry.getId(),
                entry.getSiteName(),
                entry.getSiteUrl(),
                entry.getSiteUsername(),
                entry.getNotes(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }
}
