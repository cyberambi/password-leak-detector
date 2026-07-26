/**
 * The backend's error envelope puts the real reason for a 400 in
 * `validationErrors` (per-field messages) and only a generic "Validation
 * failed" in the top-level `message`. Prefer the field-level detail so the
 * user actually learns what to fix instead of a dead-end generic error.
 */
export function extractErrorMessage(error, fallback) {
  const data = error?.response?.data;
  if (!data) {
    return fallback;
  }
  if (data.validationErrors && Object.keys(data.validationErrors).length > 0) {
    return Object.values(data.validationErrors).join(' ');
  }
  return data.message ?? fallback;
}
