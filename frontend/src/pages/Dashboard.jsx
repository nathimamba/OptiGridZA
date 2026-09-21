import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { apiFetch } from "../services/api";

const actionColors = {
    CHARGE: 'bg-sa-green/10 text-sa-green',
    DISCHARGE: 'bg-sa-red/10 text-sa-red',
    HOLD: 'bg-gray-100 text-gray-600',
    SOLAR_PRIORITY: 'bg-sa-gold/20 text-sa-blue',
}

export default function Dashboard() {
    const { user, logout } = useAuth();
    const [recommendation, setRecommendation] = useState(null);
    const [battery, setBattery] = useState(null);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!user?.companyId) return
        async function loadData() {
            try {
                const [rec, health] = await Promise.all([
                    apiFetch(`/api/v1/prediction/recommend/${user.companyId}`),
                    apiFetch(`/api/v1/simulation/battery/health/${user.companyId}`),
                ])
                setRecommendation(rec);
                setBattery(health);
            } catch (err) {
                setError('Could not load dashbaord data - check that a company and battery exist.');

            } finally {
                setLoading(false);
            }
        }
        loadData();
    }, [user])

    const healthColor = {
        GOOD: 'text-sa-green',
        WARNING: 'text-sa-gold',
        CRITICAL: 'text-sa-red',
    }[battery?.healthStatus] || 'text-gray-400';
    return (
        <div className="min-h-screen bg-brand-bg flex">
            <aside className="w-64 bg-sa-blue text-white flex flex-col p-6">
                <h1 className="text-xl font-bold mb-8">OptiGrid ZA</h1>
                <nav className="flex flex-col gap-2 flex-1">
                    <a className="px-3 py-2 rounded-lg bg-white/10">Dashboard</a>
                    <a className="px-3 py-2 rounded-lg hover:bg-white/10">Battery Health</a>
                    <a className="px-3 py-2 rounded-lg hover:bg-white/10">Savings</a>
                    <a className="px-3 py-2 rounded-lg hover:bg-white/10">Alerts</a>
                </nav>
                <button
                    onClick={logout}
                    className="text-sm text-white/70 hover:text-white text-left"
                >
                    Log Out
                </button>
            </aside>


            <main className="flex-1 p-8">
                <div className="flex items-center justify-between mb-8">
                    <h2 className="text-2xl font-bold text-brand-text">Dashboard</h2>
                    <span className="bg-sa-gold/20 text-sa-blue text-sm font-medium px-3 py-1 rounded-full">
                        {user?.role}
                    </span>
                </div>

                {error && (
                    <div className="bg-sa-red/10 text-sa-red rounded-xl p-4 mb-6 text-sm">
                        {error}
                    </div>
                )}


                <div className="grid grid-cols-4 gap-4 mb-8">
                    <div className="bg-brand-card rounded-2xl shadow-sm p-5">
                        <p className="text-sm text-gray-500">Battery SOC</p>
                        <p className="text-2xl font-bold text-brand-text mt-1">
                            {battery ? `${battery.soc}%` : '—'}
                        </p>
                    </div>
                    <div className="bg-brand-card rounded-2xl shadow-sm p-5">
                        <p className="text-sm text-gray-500">Load-Shedding Stage</p>
                        <p className="text-2xl font-bold text-brand-text mt-1">
                            {recommendation ? recommendation.loadSheddingStage : '—'}
                        </p>
                    </div>
                    <div className="bg-brand-card rounded-2xl shadow-sm p-5">
                        <p className="text-sm text-gray-500">Solar Forecast</p>
                        <p className="text-2xl font-bold text-brand-text mt-1">
                            {recommendation ? `${recommendation.solarForecastKwh} kWh` : '—'}
                        </p>
                    </div>
                    <div className="bg-brand-card rounded-2xl shadow-sm p-5">
                        <p className="text-sm text-gray-500">Estimated Savings</p>
                        <p className="text-2xl font-bold text-brand-text mt-1">
                            {recommendation ? `R${recommendation.estimatedSavingsRand.toFixed(2)}` : '—'}
                        </p>
                    </div>
                </div>

                <div className="bg-brand-card rounded-2xl shadow-sm p-6 mb-6">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="font-semibold text-brand-text">AI Recommendation</h3>
                        {recommendation && (
                            <span className={`text-xs font-medium px-3 py-1 rounded-full ${actionColors[recommendation.action] || ''}`}>
                                {recommendation.action}
                            </span>
                        )}
                    </div>

                    {loading && <p className="text-gray-400 text-sm">Loading recommendation...</p>}

                    {recommendation && (
                        <>
                            <div className="flex items-center gap-6 mb-4">
                                <div>
                                    <p className="text-sm text-gray-500">Confidence</p>
                                    <p className="text-lg font-bold text-brand-text">
                                        {(recommendation.confidence * 100).toFixed(0)}%
                                    </p>
                                </div>
                                <div>
                                    <p className="text-sm text-gray-500">Mode</p>
                                    <p className="text-lg font-bold text-brand-text">
                                        {recommendation.operatingMode}
                                    </p>
                                </div>
                            </div>
                            <p className="text-sm text-gray-600 leading-relaxed">
                                {recommendation.reasoning}
                            </p>
                        </>
                    )}
                </div>

                <div className="bg-brand-card rounded-2xl shadow-sm p-6">
                    <h3 className="font-semibold text-brand-text mb-4">Battery Health</h3>
                    {battery && (
                        <div className="flex items-center gap-8">
                            <div>
                                <p className="text-sm text-gray-500">Status</p>
                                <p className={`text-lg font-bold ${healthColor}`}>{battery.healthStatus}</p>
                            </div>
                            <div>
                                <p className="text-sm text-gray-500">Cycle Count</p>
                                <p className="text-lg font-bold text-brand-text">{battery.cycleCount}</p>
                            </div>
                            <div>
                                <p className="text-sm text-gray-500">Efficiency</p>
                                <p className="text-lg font-bold text-brand-text">{battery.efficiencyPct}%</p>
                            </div>
                        </div>
                    )}
                </div>
            </main>
        </div>)
}