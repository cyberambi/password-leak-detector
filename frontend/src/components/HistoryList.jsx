import { useState } from 'react';
import { getHistoryEntry } from '../api/passwordApi';

export default function HistoryList({ entries, onEdit, onDelete }) {
  const [revealed, setRevealed] = useState({});
  const [revealingId, setRevealingId] = useState(null);

  const handleReveal = async (id) => {
    if (revealed[id] !== undefined) {
      setRevealed((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
      return;
    }
    setRevealingId(id);
    try {
      const detail = await getHistoryEntry(id);
      setRevealed((prev) => ({ ...prev, [id]: detail.password }));
    } finally {
      setRevealingId(null);
    }
  };

  if (entries.length === 0) {
    return <p>No saved passwords yet. Add one below.</p>;
  }

  return (
    <table className="history-table">
      <thead>
        <tr>
          <th>Site</th>
          <th>Username</th>
          <th>Password</th>
          <th>Notes</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {entries.map((entry) => (
          <tr key={entry.id}>
            <td>
              {entry.siteUrl ? (
                <a href={entry.siteUrl} target="_blank" rel="noreferrer">
                  {entry.siteName}
                </a>
              ) : (
                entry.siteName
              )}
            </td>
            <td>{entry.siteUsername}</td>
            <td>
              <code>{revealed[entry.id] ?? '••••••••'}</code>{' '}
              <button type="button" onClick={() => handleReveal(entry.id)} disabled={revealingId === entry.id}>
                {revealed[entry.id] !== undefined ? 'Hide' : 'Reveal'}
              </button>
            </td>
            <td>{entry.notes}</td>
            <td>
              <button type="button" onClick={() => onEdit(entry.id)}>
                Edit
              </button>
              <button type="button" onClick={() => onDelete(entry.id)}>
                Delete
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
