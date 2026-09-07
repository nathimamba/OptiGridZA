import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const GATEWAY_URL = 'http://localhost:8080'

export default function Login() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const response = await fetch(`${GATEWAY_URL}/api/v1/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email, password }),
            })

            if (!response.ok) {
                throw new Error('Invalid email or password');
            }

            const data = await response.json();
            localStorage.setItem('token', data.token);
            navigate('/dashboard');
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }
    return (
        <div className="min-h-screen bg-brand-bg flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-sa-blue">OptiGrid ZA</h1>
          <p className="text-sm text-gray-500 mt-1">Energy Management Platform</p>
        </div>

      
        <div className="bg-brand-card rounded-2xl shadow-lg p-8">
          <h2 className="text-xl font-semibold text-brand-text mb-6">Log In</h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">
                Email
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full px-4 py-2.5 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-sa-blue"
                placeholder="you@company.co.za"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">
                Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full px-4 py-2.5 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-sa-blue"
                placeholder="••••••••"
              />
            </div>

            {error && (
              <p className="text-sa-red text-sm">{error}</p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-sa-blue text-white font-medium py-2.5 rounded-lg hover:opacity-90 transition disabled:opacity-50"
            >
              {loading ? 'Logging in...' : 'Log In'}
            </button>
          </form>

          <p className="text-center text-sm text-gray-500 mt-6">
            Don't have an account?{' '}
            <a href="/register" className="text-sa-blue font-medium hover:underline">
              Register
            </a>
          </p>
        </div>
      </div>
    </div>
    )
}