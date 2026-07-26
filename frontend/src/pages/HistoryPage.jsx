import { useEffect, useState } from 'react';
import HistoryEntryForm from '../components/HistoryEntryForm';
import HistoryList from '../components/HistoryList';
import {
  createHistoryEntry,
  deleteHistoryEntry,
  getHistoryEntry,
  listHistory,
  updateHistoryEntry,
} from '../api/passwordApi';

export default function HistoryPage() {
  const [entries, setEntries] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [mode, setMode] = useState('list'); // 'list' | 'add' | 'edit'
  const [editingEntry, setEditingEntry] = useState(null);

  const loadEntries = async () => {
    setIsLoading(true);
    try {
      const data = await listHistory();
      setEntries(data);
    } catch {
      setError('Could not load your saved passwords.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadEntries();
  }, []);

  const handleAdd = async (values) => {
    await createHistoryEntry(values);
    setMode('list');
    await loadEntries();
  };

  const handleEdit = async (id) => {
    const detail = await getHistoryEntry(id);
    setEditingEntry(detail);
    setMode('edit');
  };

  const handleUpdate = async (values) => {
    await updateHistoryEntry(editingEntry.id, values);
    setMode('list');
    setEditingEntry(null);
    await loadEntries();
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this saved password? This cannot be undone.')) {
      return;
    }
    await deleteHistoryEntry(id);
    await loadEntries();
  };

  return (
    <div className="history-page">
      <h1>Password history</h1>
      {error && <p className="form-error">{error}</p>}
      {isLoading ? (
        <p>Loading...</p>
      ) : (
        <>
          <HistoryList entries={entries} onEdit={handleEdit} onDelete={handleDelete} />

          {mode === 'list' && (
            <button type="button" onClick={() => setMode('add')}>
              Add new entry
            </button>
          )}

          {mode === 'add' && (
            <div className="card">
              <h2>Add a saved password</h2>
              <HistoryEntryForm onSubmit={handleAdd} onCancel={() => setMode('list')} submitLabel="Save" />
            </div>
          )}

          {mode === 'edit' && editingEntry && (
            <div className="card">
              <h2>Edit saved password</h2>
              <HistoryEntryForm
                initialValues={editingEntry}
                onSubmit={handleUpdate}
                onCancel={() => {
                  setMode('list');
                  setEditingEntry(null);
                }}
                submitLabel="Update"
              />
            </div>
          )}
        </>
      )}
    </div>
  );
}
