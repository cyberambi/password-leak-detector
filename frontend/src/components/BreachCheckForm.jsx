import { useState } from 'react';
import { checkBreach } from '../api/passwordApi';

export default function BreachCheckForm() {
  const [password, setPassword] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setResult(null);
    if (!password) {
      return;
    }
    setIsLoading(true);
    try {
      const data = await checkBreach(password);
      setResult(data);
    } catch {
      setError('Could not check this password right now. Please try again shortly.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="card">
      <h2>Check for breaches</h2>
      <p className="card-description">
        Your password is never sent in full - only the first 5 characters of its SHA-1 hash
        are sent to the Have I Been Pwned API (k-anonymity).
      </p>
      <form onSubmit={handleSubmit}>
        <input
          type="password"
          placeholder="Enter a password to check"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        <button type="submit" disabled={isLoading}>
          {isLoading ? 'Checking...' : 'Check breach'}
        </button>
      </form>
      {error && <p className="form-error">{error}</p>}
      {result && (
        <p className={result.breached ? 'result-bad' : 'result-good'}>
          {result.breached
            ? `This password has appeared in ${result.occurrences.toLocaleString()} known breaches. Avoid using it.`
            : 'Good news - this password was not found in any known breach.'}
        </p>
      )}
    </div>
  );
}
