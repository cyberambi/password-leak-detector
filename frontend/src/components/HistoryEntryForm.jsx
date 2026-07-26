import { useState } from 'react';
import { extractErrorMessage } from '../api/errorMessage';

const EMPTY = { siteName: '', siteUrl: '', siteUsername: '', password: '', notes: '' };

export default function HistoryEntryForm({ initialValues, onSubmit, onCancel, submitLabel }) {
  const [values, setValues] = useState({ ...EMPTY, ...initialValues });
  const [error, setError] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const handleChange = (field) => (event) => {
    setValues((prev) => ({ ...prev, [field]: event.target.value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    if (!values.siteName || !values.password) {
      setError('Site name and password are required.');
      return;
    }
    setIsSaving(true);
    try {
      await onSubmit(values);
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not save this entry. Please try again.'));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <form className="history-entry-form" onSubmit={handleSubmit}>
      <input
        type="text"
        placeholder="Site name (required)"
        value={values.siteName}
        onChange={handleChange('siteName')}
      />
      <input
        type="text"
        placeholder="Site URL (optional)"
        value={values.siteUrl ?? ''}
        onChange={handleChange('siteUrl')}
      />
      <input
        type="text"
        placeholder="Username / email on this site (optional)"
        value={values.siteUsername ?? ''}
        onChange={handleChange('siteUsername')}
      />
      <input
        type="text"
        placeholder="Password (required)"
        value={values.password}
        onChange={handleChange('password')}
      />
      <textarea
        placeholder="Notes (optional)"
        value={values.notes ?? ''}
        onChange={handleChange('notes')}
      />
      {error && <p className="form-error">{error}</p>}
      <div className="history-entry-form-actions">
        <button type="submit" disabled={isSaving}>
          {isSaving ? 'Saving...' : submitLabel}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
