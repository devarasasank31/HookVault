import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { endpointsApi, eventsApi } from '../services/api';
import type { EndpointResponse, EventResponse, PaginatedResponse } from '../types/api';
import { toast } from 'react-hot-toast';
import {
  Loader2,
  Copy,
  Check,
  RotateCcw,
  Trash2,
  Edit,
  ExternalLink,
  Send,
  Key,
  Eye,
  EyeOff,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { cn, formatDateTime } from '../utils/cn';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

const rotateSchema = z.object({
  confirm: z.literal('ROTATE', { errorMap: () => ({ message: 'Type ROTATE to confirm' }) }),
});

type RotateForm = z.infer<typeof rotateSchema>;

export function EndpointDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [endpoint, setEndpoint] = useState<EndpointResponse | null>(null);
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showSecret, setShowSecret] = useState(false);
  const [secret, setSecret] = useState<string | null>(null);
  const [rotateDialogOpen, setRotateDialogOpen] = useState(false);
  const [isRotating, setIsRotating] = useState(false);

  const {
    register: registerRotate,
    handleSubmit: handleRotateSubmit,
    reset: resetRotate,
    formState: { errors: rotateErrors },
  } = useForm<RotateForm>({
    resolver: zodResolver(rotateSchema),
  });

  const fetchEndpoint = async () => {
    if (!id) return;
    try {
      const response = await endpointsApi.get(id);
      setEndpoint(response.data.data!);
    } catch (error) {
      toast.error('Failed to load endpoint');
      navigate('/endpoints');
    }
  };

  const fetchEvents = async () => {
    if (!id) return;
    try {
      const response = await eventsApi.list({ endpointId: id, size: 20 });
      setEvents(response.data.data?.content || []);
    } catch (error) {
      console.error('Failed to load events', error);
    }
  };

  useEffect(() => {
    fetchEndpoint();
    fetchEvents();
  }, [id]);

  const handleRotate = async (data: RotateForm) => {
    if (!id) return;
    setIsRotating(true);
    try {
      const response = await endpointsApi.rotateSecret(id);
      const newSecret = response.data.data?.signingSecret;
      if (newSecret) {
        setSecret(newSecret);
        setShowSecret(true);
        toast.success('Signing secret rotated');
      }
      fetchEndpoint();
      setRotateDialogOpen(false);
    } catch (error) {
      toast.error('Failed to rotate secret');
    } finally {
      setIsRotating(false);
    }
  };

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text).then(() => toast.success(`${label} copied`));
  };

  if (isLoading) {
    return (
      <div className="space-y-6" role="status" aria-label="Loading endpoint">
        <div className="animate-pulse">
          <div className="h-8 w-48 bg-dark-200 dark:bg-dark-700 rounded mb-2" />
          <div className="h-4 w-64 bg-dark-200 dark:bg-dark-700 rounded" />
        </div>
        <div className="card">
          <div className="card-body h-64 bg-dark-50 dark:bg-dark-800/50" />
        </div>
      </div>
    );
  }

  if (!endpoint) return null;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
        <div className="flex items-start gap-4">
          <Link to="/endpoints" className="p-2 rounded-lg text-dark-500 hover:text-dark-700 hover:bg-dark-100 dark:hover:bg-dark-800 dark:hover:text-dark-300 mt-1">
            <ExternalLink className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-dark-900 dark:text-white">{endpoint.name}</h1>
            <p className="text-dark-500 dark:text-dark-400 mt-1">{endpoint.url}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Link to={`/endpoints/${endpoint.id}/edit`} className="btn-secondary">
            <Edit className="h-4 w-4" />
            Edit
          </Link>
          <button
            onClick={() => setRotateDialogOpen(true)}
            className="btn-outline"
          >
            <RotateCcw className="h-4 w-4" />
            Rotate Secret
          </button>
          <button
            onClick={() => {
              if (confirm(`Delete ${endpoint.name}? This cannot be undone.`)) {
                // Handle delete
              }
            }}
            className="btn-outline text-red-600 hover:text-red-700 border-red-300 hover:border-red-500"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Secret Display */}
      {showSecret && secret && (
        <div className="card border-yellow-200 dark:border-yellow-800 bg-yellow-50 dark:bg-yellow-900/20 animate-slide-up">
          <div className="card-body">
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1">
                <h3 className="font-medium text-yellow-800 dark:text-yellow-200 flex items-center gap-2">
                  <Key className="h-5 w-5" />
                  New Signing Secret
                </h3>
                <p className="text-sm text-yellow-700 dark:text-yellow-300 mt-1">
                  Save this secret now. It won't be shown again.
                </p>
                <div className="mt-3 flex items-center gap-2 flex-wrap">
                  <code className="flex-1 min-w-0 bg-white dark:bg-dark-800 px-3 py-2 rounded font-mono text-sm break-all">
                    {secret}
                  </code>
                  <button
                    onClick={() => copyToClipboard(secret, 'Secret')}
                    className="btn-secondary text-sm whitespace-nowrap"
                  >
                    Copy
                  </button>
                </div>
              </div>
              <button
                onClick={() => setShowSecret(false)}
                className="p-2 rounded-lg text-yellow-600 hover:bg-yellow-100 dark:hover:bg-yellow-900/30"
                aria-label="Dismiss secret"
              >
                <EyeOff className="h-5 w-5" />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Info Cards */}
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="card">
          <div className="card-body">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-dark-500 dark:text-dark-400">Status</p>
                <p className="text-2xl font-bold text-dark-900 dark:text-white capitalize">
                  {endpoint.active ? 'Active' : 'Inactive'}
                </p>
              </div>
              <div className={cn(
                'h-12 w-12 rounded-lg flex items-center justify-center',
                endpoint.active ? 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400' :
                'bg-dark-100 dark:bg-dark-800 text-dark-500 dark:text-dark-400'
              )}>
                {endpoint.active ? <Send className="h-6 w-6" /> : <EyeOff className="h-6 w-6" />}
              </div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-body">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-dark-500 dark:text-dark-400">Created</p>
                <p className="text-2xl font-bold text-dark-900 dark:text-white">
                  {formatDateTime(endpoint.createdAt).split(',')[0]}
                </p>
              </div>
              <div className="h-12 w-12 rounded-lg bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center text-blue-600 dark:text-blue-400">
                <Key className="h-6 w-6" />
              </div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-body">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-dark-500 dark:text-dark-400">Events (24h)</p>
                <p className="text-2xl font-bold text-dark-900 dark:text-white">{events.length}</p>
              </div>
              <div className="h-12 w-12 rounded-lg bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center text-purple-600 dark:text-purple-400">
                <Send className="h-6 w-6" />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Details & Recent Events */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Endpoint Details */}
        <div className="card">
          <div className="card-header">
            <h2 className="text-lg font-semibold text-dark-900 dark:text-white">Configuration</h2>
          </div>
          <div className="card-body space-y-4">
            <div>
              <p className="text-sm font-medium text-dark-500 dark:text-dark-400">URL</p>
              <div className="mt-1 flex items-center gap-2">
                <code className="flex-1 bg-dark-100 dark:bg-dark-800 px-3 py-2 rounded text-sm font-mono break-all">
                  {endpoint.url}
                </code>
                <button
                  onClick={() => copyToClipboard(endpoint.url, 'URL')}
                  className="btn-ghost text-sm"
                >
                  Copy
                </button>
              </div>
            </div>

            {endpoint.description && (
              <div>
                <p className="text-sm font-medium text-dark-500 dark:text-dark-400">Description</p>
                <p className="mt-1 text-dark-900 dark:text-white">{endpoint.description}</p>
              </div>
            )}

            {endpoint.headers && Object.keys(endpoint.headers).length > 0 && (
              <div>
                <p className="text-sm font-medium text-dark-500 dark:text-dark-400">Custom Headers</p>
                <div className="mt-1 space-y-1">
                  {Object.entries(endpoint.headers).map(([key, value]) => (
                    <div key={key} className="flex items-center gap-2 text-sm">
                      <span className="font-mono text-dark-900 dark:text-white">{key}</span>
                      <span className="text-dark-500 dark:text-dark-400">:</span>
                      <code className="bg-dark-100 dark:bg-dark-800 px-2 py-1 rounded text-dark-900 dark:text-white">
                        {value}
                      </code>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="pt-4 border-t border-dark-200 dark:border-dark-700">
              <p className="text-sm font-medium text-dark-500 dark:text-dark-400">Signing Secret</p>
              <p className="mt-1 text-sm text-dark-600 dark:text-dark-400">
                Used to verify webhook signatures. Rotate if compromised.
              </p>
            </div>
          </div>
        </div>

        {/* Recent Events */}
        <div className="card">
          <div className="card-header flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark-900 dark:text-white">Recent Events</h2>
            <Link to={`/events?endpointId=${endpoint.id}`} className="text-sm text-primary-600 hover:text-primary-500">
              View all
            </Link>
          </div>
          <div className="card-body p-0">
            {events.length === 0 ? (
              <div className="text-center py-8">
                <Send className="h-12 w-12 text-dark-300 dark:text-dark-600 mx-auto mb-4" />
                <p className="text-dark-500 dark:text-dark-400">No events yet</p>
                <p className="text-sm text-dark-400 dark:text-dark-500 mt-1">
                  Events will appear here when webhooks are received
                </p>
              </div>
            ) : (
              <div className="table-container">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Event Type</th>
                      <th>Status</th>
                      <th>Time</th>
                    </tr>
                  </thead>
                  <tbody>
                    {events.map((event) => (
                      <tr key={event.id}>
                        <td>
                          <Link to={`/events/${event.id}`} className="font-mono text-sm text-primary-600 hover:underline">
                            {event.eventType}
                          </Link>
                        </td>
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
                          {formatDateTime(event.createdAt)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Rotate Secret Dialog */}
      {rotateDialogOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setRotateDialogOpen(false)}>
          <div className="bg-white dark:bg-dark-900 rounded-xl shadow-xl max-w-md w-full" onClick={(e) => e.stopPropagation()}>
            <div className="p-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="h-10 w-10 rounded-full bg-yellow-100 dark:bg-yellow-900/30 flex items-center justify-center">
                  <RotateCcw className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
                </div>
                <h3 className="text-lg font-semibold text-dark-900 dark:text-white">Rotate Signing Secret</h3>
              </div>
              <p className="text-dark-600 dark:text-dark-400 mb-4">
                This will generate a new signing secret and invalidate the current one. Make sure to update your webhook verification code.
              </p>
              <p className="text-sm text-dark-500 dark:text-dark-400 mb-6">
                Type <code className="bg-dark-100 dark:bg-dark-800 px-1.5 py-0.5 rounded font-mono">ROTATE</code> to confirm:
              </p>
              <form onSubmit={handleRotateSubmit(handleRotate)} className="space-y-4">
                <div>
                  <input
                    {...registerRotate('confirm')}
                    type="text"
                    placeholder="ROTATE"
                    className={cn(
                      'input text-center font-mono',
                      rotateErrors.confirm && 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                    )}
                    autoFocus
                  />
                  {rotateErrors.confirm && (
                    <p className="mt-1 text-sm text-red-600 dark:text-red-400 text-center">{rotateErrors.confirm.message}</p>
                  )}
                </div>
                <div className="flex justify-end gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setRotateDialogOpen(false)}
                    className="btn-secondary"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn-warning"
                    disabled={isRotating}
                  >
                    {isRotating ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Rotating...
                      </>
                    ) : (
                      'Rotate Secret'
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}