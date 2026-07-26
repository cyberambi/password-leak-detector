import { useState } from 'react';
import { analyzeStrength } from '../api/passwordApi';
import StrengthMeter from './StrengthMeter';

export default function StrengthCheckForm() {
  const [password, setPassword] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    if (!password) {
      setResult(null);
      return;
    }
    setIsLoading(true);
    try {
      const data = await analyzeStrength(password);
      setResult(data);
    } catch {
      setError('Could not analyze this password right now. Please try again shortly.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="card">
      <h2>Analyze strength</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="password"
          placeholder="Enter a password to analyze"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        <button type="submit" disabled={isLoading}>
          {isLoading ? 'Analyzing...' : 'Analyze strength'}
        </button>
      </form>
      {error && <p className="form-error">{error}</p>}
      {result && <StrengthMeter score={result.score} label={result.label} feedback={result.feedback} />}
    </div>
  );
}
