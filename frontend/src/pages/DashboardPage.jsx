import BreachCheckForm from '../components/BreachCheckForm';
import StrengthCheckForm from '../components/StrengthCheckForm';
import PasswordGeneratorForm from '../components/PasswordGeneratorForm';

export default function DashboardPage() {
  return (
    <div className="dashboard-page">
      <h1>Dashboard</h1>
      <div className="dashboard-grid">
        <BreachCheckForm />
        <StrengthCheckForm />
        <PasswordGeneratorForm />
      </div>
    </div>
  );
}
