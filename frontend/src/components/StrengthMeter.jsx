const LEVEL_COLORS = ['#d32f2f', '#f57c00', '#fbc02d', '#7cb342', '#2e7d32'];

export default function StrengthMeter({ score, label, feedback }) {
  const safeScore = Math.min(Math.max(score ?? 0, 0), 4);
  const color = LEVEL_COLORS[safeScore];

  return (
    <div className="strength-meter">
      <div className="strength-meter-track">
        {LEVEL_COLORS.map((levelColor, index) => (
          <div
            key={index}
            className="strength-meter-segment"
            style={{ backgroundColor: index <= safeScore ? color : '#e0e0e0' }}
          />
        ))}
      </div>
      <div className="strength-meter-label" style={{ color }}>
        {label}
      </div>
      {feedback && feedback.length > 0 && (
        <ul className="strength-meter-feedback">
          {feedback.map((item, index) => (
            <li key={index}>{item}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
