import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { analyticsApi } from '../services/api';
import type { AnalyticsDashboardResponse } from '../types/api';
import {
  Box,
  CheckCircle,
  XCircle,
  Clock,
  AlertTriangle,
  ArrowUpRight,
  ArrowDownRight,
  TrendingUp,
  ExternalLink,
} from 'lucide-react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  AreaChart,
  Area,
} from 'recharts';
import { cn, formatNumber, formatDate } from '../utils/cn';

interface StatCardProps {
  title: string;
  value: string | number;
  change?: string;
  changeType?: 'positive' | 'negative' | 'neutral';
  icon: React.ReactNode;
  href?: string;
}

function StatCard({ title, value, change, changeType = 'neutral', icon, href }: StatCardProps) {
  return (
    <Link
      to={href || '#'}
      className={cn(
        'card hover:shadow-md transition-shadow',
        href && 'cursor-pointer'
      )}
    >
      <div className="card-body">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-sm font-medium text-dark-500 dark:text-dark-400">{title}</p>
            <p className="mt-1 text-3xl font-bold text-dark-900 dark:text-white">{value}</p>
            {change && (
              <p className={cn(
                'mt-2 text-sm flex items-center gap-1',
                changeType === 'positive' ? 'text-green-600 dark:text-green-400' :
                changeType === 'negative' ? 'text-red-600 dark:text-red-400' :
                'text-dark-500 dark:text-dark-400'
              )}>
                {changeType === 'positive' && <ArrowUpRight className="h-3.5 w-3.5" />}
                {changeType === 'negative' && <ArrowDownRight className="h-3.5 w-3.5" />}
                {change}
              </p>
            )}
          </div>
          <div className="h-12 w-12 rounded-lg bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center text-primary-600 dark:text-primary-400">
            {icon}
          </div>
        </div>
      </div>
    </Link>
  );
}

export function DashboardPage() {
  const [dashboard, setDashboard] = useState<AnalyticsDashboardResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await analyticsApi.getDashboard();
        setDashboard(response.data.data!);
      } catch (err) {
        setError('Failed to load dashboard');
        console.error(err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchDashboard();
  }, []);

  if (isLoading) {
    return (
      <div className="space-y-6" role="status" aria-label="Loading dashboard">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="card animate-pulse">
            <div className="card-body h-24" />
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="card">
        <div className="card-body text-center py-12">
          <AlertTriangle className="h-12 w-12 text-red-500 mx-auto mb-4" />
          <h2 className="text-lg font-medium text-dark-900 dark:text-white mb-2">Failed to load dashboard</h2>
          <p className="text-dark-500 dark:text-dark-400">{error}</p>
        </div>
      </div>
    );
  }

  if (!dashboard) {
    return null;
  }

  const stats = [
    {
      title: 'Total Events',
      value: formatNumber(dashboard.totalEvents),
      icon: <Box className="h-6 w-6" />,
      href: '/events',
    },
    {
      title: 'Delivered',
      value: formatNumber(dashboard.totalDelivered),
      change: `${dashboard.successRate.toFixed(1)}% success rate`,
      changeType: 'positive' as const,
      icon: <CheckCircle className="h-6 w-6" />,
      href: '/events?status=DELIVERED',
    },
    {
      title: 'Failed',
      value: formatNumber(dashboard.totalFailed),
      change: `${dashboard.totalDeadLettered} dead-lettered`,
      changeType: 'negative' as const,
      icon: <XCircle className="h-6 w-6" />,
      href: '/events?status=FAILED',
    },
    {
      title: 'Pending',
      value: formatNumber(dashboard.totalPending),
      change: `${dashboard.totalRetries} retries`,
      changeType: 'neutral' as const,
      icon: <Clock className="h-6 w-6" />,
      href: '/events?status=PENDING',
    },
  ];

  const recentEvents = dashboard.recentEvents.slice(0, 5);
  const volumeData = dashboard.volumeOverTime.slice(-7);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-dark-900 dark:text-white">Dashboard</h1>
          <p className="text-dark-500 dark:text-dark-400 mt-1">
            Overview of your webhook delivery performance
          </p>
        </div>
        <Link to="/endpoints/create" className="btn-primary">
          <Plus className="h-4 w-4" />
          Create Endpoint
        </Link>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat, index) => (
          <StatCard key={index} {...stat} />
        ))}
      </div>

      {/* Charts Row */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Volume Over Time */}
        <div className="card">
          <div className="card-header">
            <h2 className="text-lg font-semibold text-dark-900 dark:text-white">Delivery Volume (Last 7 Days)</h2>
          </div>
          <div className="card-body">
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={volumeData}>
                  <defs>
                    <linearGradient id="colorDelivered" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                    </linearGradient>
                    <linearGradient id="colorFailed" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                  <XAxis
                    dataKey="date"
                    tickFormatter={(value) => formatDate(value)}
                    stroke="#94a3b8"
                    fontSize={12}
                    tick={{ fill: '#64748b' }}
                  />
                  <YAxis stroke="#94a3b8" fontSize={12} tick={{ fill: '#64748b' }} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1e293b',
                      border: '1px solid #334155',
                      borderRadius: '8px',
                      color: '#f8fafc',
                    }}
                    formatter={(value: number) => [formatNumber(value), 'Events']}
                  />
                  <Area
                    type="monotone"
                    dataKey="delivered"
                    stroke="#10b981"
                    fillOpacity={1}
                    fill="url(#colorDelivered)"
                  />
                  <Area
                    type="monotone"
                    dataKey="failed"
                    stroke="#ef4444"
                    fillOpacity={1}
                    fill="url(#colorFailed)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
            <div className="flex items-center gap-4 mt-4 text-sm">
              <span className="flex items-center gap-1.5">
                <span className="h-3 w-3 rounded bg-green-500" />
                Delivered
              </span>
              <span className="flex items-center gap-1.5">
                <span className="h-3 w-3 rounded bg-red-500" />
                Failed
              </span>
            </div>
          </div>
        </div>

        {/* Success Rate Trend */}
        <div className="card">
          <div className="card-header">
            <h2 className="text-lg font-semibold text-dark-900 dark:text-white">Success Rate Trend</h2>
          </div>
          <div className="card-body">
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={volumeData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                  <XAxis
                    dataKey="date"
                    tickFormatter={(value) => formatDate(value)}
                    stroke="#94a3b8"
                    fontSize={12}
                    tick={{ fill: '#64748b' }}
                  />
                  <YAxis
                    stroke="#94a3b8"
                    fontSize={12}
                    tick={{ fill: '#64748b' }}
                    domain={[0, 100]}
                    tickFormatter={(value) => `${value}%`}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1e293b',
                      border: '1px solid #334155',
                      borderRadius: '8px',
                      color: '#f8fafc',
                    }}
                    formatter={(value: number) => [`${value.toFixed(1)}%`, 'Success Rate']}
                  />
                  <Line
                    type="monotone"
                    dataKey="total"
                    stroke="#0ea5e9"
                    strokeWidth={2}
                    dot={false}
                    activeDot={{ r: 6 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>

      {/* Endpoint Performance & Recent Events */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Endpoint Performance */}
        <div className="card">
          <div className="card-header flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark-900 dark:text-white">Endpoint Performance</h2>
            <Link to="/endpoints" className="text-sm text-primary-600 hover:text-primary-500 flex items-center gap-1">
              View all
              <ExternalLink className="h-3.5 w-3.5" />
            </Link>
          </div>
          <div className="card-body p-0">
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Endpoint</th>
                    <th className="text-right">Events</th>
                    <th className="text-right">Success Rate</th>
                    <th className="text-right">Avg Time</th>
                  </tr>
                </thead>
                <tbody>
                  {dashboard.endpointPerformance.slice(0, 5).map((endpoint) => (
                    <tr key={endpoint.endpointId}>
                      <td className="font-medium text-dark-900 dark:text-white">{endpoint.endpointName}</td>
                      <td className="text-right text-dark-500 dark:text-dark-400">{formatNumber(endpoint.totalEvents)}</td>
                      <td className="text-right">
                        <span className={cn(
                          'font-medium',
                          endpoint.successRate >= 95 ? 'text-green-600 dark:text-green-400' :
                          endpoint.successRate >= 80 ? 'text-yellow-600 dark:text-yellow-400' :
                          'text-red-600 dark:text-red-400'
                        )}>
                          {endpoint.successRate.toFixed(1)}%
                        </span>
                      </td>
                      <td className="text-right text-dark-500 dark:text-dark-400">
                        {endpoint.averageResponseTimeMs}ms
                      </td>
                    </tr>
                  ))}
                  {dashboard.endpointPerformance.length === 0 && (
                    <tr>
                      <td colSpan={4} className="text-center py-8 text-dark-500 dark:text-dark-400">
                        No endpoints yet. <Link to="/endpoints/create" className="text-primary-600 hover:underline">Create one</Link>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Recent Events */}
        <div className="card">
          <div className="card-header flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark-900 dark:text-white">Recent Events</h2>
            <Link to="/events" className="text-sm text-primary-600 hover:text-primary-500 flex items-center gap-1">
              View all
              <ExternalLink className="h-3.5 w-3.5" />
            </Link>
          </div>
          <div className="card-body p-0">
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Event</th>
                    <th>Endpoint</th>
                    <th>Status</th>
                    <th>Time</th>
                  </tr>
                </thead>
                <tbody>
                  {recentEvents.map((event) => (
                    <tr key={event.id}>
                      <td>
                        <div>
                          <p className="font-mono text-sm text-dark-900 dark:text-white">{event.eventType}</p>
                          <p className="text-xs text-dark-500 dark:text-dark-400 font-mono">
                            {event.id.slice(0, 8)}...
                          </p>
                        </div>
                      </td>
                      <td className="text-dark-500 dark:text-dark-400">{event.endpointName}</td>
                      <td>
                        <span className={cn(
                          'badge',
                          event.status === 'DELIVERED' && 'badge-success',
                          event.status === 'FAILED' && 'badge-danger',
                          event.status === 'PENDING' && 'badge-warning',
                          event.status === 'RETRY_SCHEDULED' && 'badge-info',
                          event.status === 'DEAD_LETTERED' && 'badge-danger'
                        )}>
                          {event.status}
                        </span>
                      </td>
                      <td className="text-dark-500 dark:text-dark-400 text-sm">
                        {formatDate(event.createdAt)}
                      </td>
                    </tr>
                  ))}
                  {recentEvents.length === 0 && (
                    <tr>
                      <td colSpan={4} className="text-center py-8 text-dark-500 dark:text-dark-400">
                        No events yet. <Link to="/events" className="text-primary-600 hover:underline">View events</Link>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}