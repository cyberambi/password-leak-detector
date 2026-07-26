import { useState } from 'react';
import { generatePassword } from '../api/passwordApi';
import { extractErrorMessage } from '../api/errorMessage';

export default function PasswordGeneratorForm() {
  const [length, setLength] = useState(16);
  const [includeUppercase, setIncludeUppercase] = useState(true);
  const [includeLowercase, setIncludeLowercase] = useState(true);
  const [includeDigits, setIncludeDigits] = useState(true);
  const [includeSymbols, setIncludeSymbols] = useState(true);
  const [generated, setGenerated] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setCopied(false);
    setIsLoading(true);
    try {
      const data = await generatePassword({
        length: Number(length),
        includeUppercase,
        includeLowercase,
        includeDigits,
        includeSymbols,
      });
      setGenerated(data.password);
    } catch (err) {
      setGenerated('');
      setError(extractErrorMessage(err, 'Could not generate a password right now.'));
    } finally {
      setIsLoading(false);
    }
  };

  const handleCopy = async () => {
    if (!generated) return;
    await navigator.clipboard.writeText(generated);
    setCopied(true);
  };

  return (
    <div className="card">
      <h2>Generate a secure password</h2>
      <form onSubmit={handleSubmit}>
        <label>
          Length: {length}
          <input
            type="range"
            min="8"
            max="64"
            value={length}
            onChange={(event) => setLength(event.target.value)}
          />
        </label>
        <label>
          <input
            type="checkbox"
            checked={includeUppercase}
            onChange={(event) => setIncludeUppercase(event.target.checked)}
          />
          Uppercase (A-Z)
        </label>
        <label>
          <input
            type="checkbox"
            checked={includeLowercase}
            onChange={(event) => setIncludeLowercase(event.target.checked)}
          />
          Lowercase (a-z)
        </label>
        <label>
          <input
            type="checkbox"
            checked={includeDigits}
            onChange={(event) => setIncludeDigits(event.target.checked)}
          />
          Digits (0-9)
        </label>
        <label>
          <input
            type="checkbox"
            checked={includeSymbols}
            onChange={(event) => setIncludeSymbols(event.target.checked)}
          />
          Symbols (!@#$...)
        </label>
        <button type="submit" disabled={isLoading}>
          {isLoading ? 'Generating...' : 'Generate password'}
        </button>
      </form>
      {error && <p className="form-error">{error}</p>}
      {generated && (
        <div className="generated-password">
          <code>{generated}</code>
          <button type="button" onClick={handleCopy} className={copied ? 'pop' : ''}>
            {copied ? 'Copied!' : 'Copy'}
          </button>
        </div>
      )}
    </div>
  );
}
