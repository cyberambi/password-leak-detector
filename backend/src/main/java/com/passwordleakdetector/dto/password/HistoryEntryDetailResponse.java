package com.passwordleakdetector.dto.password;

import java.time.Instant;

/** Single-entry view that includes the decrypted password - only ever returned for one entry at a time. */
public record HistoryEntryDetailResponse(
        Long id,
        String siteName,
        String siteUrl,
        String siteUsername,
        String password,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
