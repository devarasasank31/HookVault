import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { eventsApi, deliveryAttemptsApi } from '../services/api';
import type { EventResponse, DeliveryAttemptResponse, PaginatedResponse } from '../types/api';
import { toast } from 'react-hot-toast';
import {
  Loader2,
  Copy,
  CheckCircle,
  XCircle,
  Clock,
  AlertTriangle,
  RotateCcw,
  ChevronDown,
  ChevronUp,
  ExternalLink,
  Download,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { cn, formatDateTime, formatNumber } from '../utils/cn';

interface DeliveryAttemptWithEvent extends DeliveryAttemptResponse {
  eventId: string;
}

export function EventDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [event, setEvent] = useState<EventResponse | null>(null);
  const [attempts, setAttempts] = useState<DeliveryAttemptWithEvent[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [expandedAttempt, setExpandedAttempt] = useState<string | null>(null);

  const fetchEvent = async () => {
    if (!id) return;
    try {
      const response = await eventsApi.get(id);
      setEvent(response.data.data!);
    } catch (error) {
      toast.error('Event not found');
      navigate('/events');
    }
  };

  const fetchAttempts = async () => {
    if (!id) return;
    try {
      const response = await deliveryAttemptsApi.getByEvent(id);
      const data = response.data.data;
      if (data) {
        setAttempts(data.content.map((a) => ({ ...a, eventId: id! })));
      }
    } catch (error) {
      console.error('Failed to load attempts', error);
    }
  };

  useEffect(() => {
    fetchEvent();
    fetchAttempts();
  }, [id]);

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text).then(() => toast.success(`${label} copied`));
  };

  const formatJson = (obj: unknown) => {
    try {
      return JSON.stringify(obj, null, 2);
    } catch {
      return String(obj);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'DELIVERED': return <CheckCircle className="h-5 w-5 text-green-600 dark:text-green-400" />;
      case 'FAILED': return <XCircle className="h-5 w-5 text-red-600 dark:text-red-400" />;
      case 'PENDING': return <Clock className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />;
      case 'RETRY_SCHEDULED': return <RotateCcw className="h-5 w-5 text-blue-600 dark:text-blue-400" />;
      case 'DEAD_LETTERED': return <AlertTriangle className="h-5 w-5 text-red-600 dark:text-red-400" />;
      default: return <Clock className="h-5 w-5 text-dark-500" />;
    }
  };

  const getAttemptStatusBadge = (status: string) => {
    return (
      <span className={cn(
        'badge',
        status === 'SUCCESS' && 'badge-success',
        status === 'FAILURE' && 'badge-danger',
        status === 'TIMEOUT' && 'badge-warning',
        status === 'IN_PROGRESS' && 'badge-info'
      )}>
        {status}
      </span>
    );
  };

  if (isLoading) {
    return (
      <div className="space-y-6" role="status" aria-label="Loading event">
        <div className="animate-pulse">
          <div className="h-8 w-48 bg-dark-200 dark:bg-dark-700 rounded mb-2" />
          <div className="h-4 w-64 bg-dark-200 dark:bg-dark-700 rounded" />
        </div>
        <div className="card">
          <div className="card-body h-96 bg-dark-50 dark:bg-dark-800/50" />
        </div>
      </div>
    );
  }

  if (!event) return null;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
        <div className="flex items-start gap-4">
          <Link to="/events" className="p-2 rounded-lg text-dark-500 hover:text-dark-700 hover:bg-dark-100 dark:hover:bg-dark-800 dark:hover:text-dark-300 mt-1">
            <ExternalLink className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-dark-900 dark:text-white">{event.eventType}</h1>
            <p className="text-dark-500 dark:text-dark-400 mt-1 font-mono text-sm">{event.id}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <span className={cn(
            'badge px-3 py-1',
            event.status === 'DELIVERED' && 'badge-success',
            event.status === 'FAILED' && 'badge-danger',
            event.status === 'PENDING' && 'badge-warning',
            event.status === 'RETRY_SCHEDULED' && 'badge-info',
            event.status === 'DEAD_LETTERED' && 'badge-danger'
          )}>
            {getStatusIcon(event.status)}
            {event.status}
          </span>
        </div>
      </div>

      {/* Event Info Cards */}
      <div className="grid gap-4 sm:grid-cols-4">
        <div className="card">
          <div className="card-body">
            <p className="text-sm font-medium text-dark-500 dark:text-dark-400">Endpoint</p>
            <p className="text-lg font-medium text-dark-900 dark:text-white font-mono mt-1">
              {event.endpointId.slice(0, 12)}...
            </p>
          </div>
        </div>
        <div className="card">
          <div className="card-body">
            <p className="text-sm font-medium text-dark-500 dark:text-dark-400">Created</p>
            <p className="text-lg font-medium text-dark-900 dark:text-white mt-1">
              {formatDateTime(event.createdAt)}
            </p>
          </div>
        </div>
        <div className="card">
          <div className="card-body">
            <p className="text-sm font-medium text-dark-500 dark:text-dark-400">
              {event.status === 'DELIVERED' ? 'Delivered' : 'Updated'}
            </p>
            <p className="text-lg font-medium text-dark-900 dark:text-white mt-1">
              {event.deliveredAt ? formatDateTime(event.deliveredAt) : formatDateTime(event.updatedAt)}
            </p>
          </div>
        </div>
        <div className="card">
          <div className="card-body">
            <p className="text-sm font-medium text-dark-500 dark:text-dark-400">Idempotency Key</p>
            <p className="text-lg font-medium text-dark-900 dark:text-white mt-1 font-mono">
              {event.idempotencyKey || '—'}
            </p>
          </div>
        </div>
      </div>

      {/* Payload */}
      <div className="card">
        <div className="card-header flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark-900 dark:text-white">Event Payload</h2>
          <button
            onClick={() => copyToClipboard(formatJson(event.payload), 'Payload')}
            className="btn-ghost text-sm"
          >
            <Copy className="h-4 w-4" />
            Copy JSON
          </button>
        </div>
        <div className="card-body">
          <pre className="bg-dark-100 dark:bg-dark-800 rounded-lg p-4 overflow-x-auto max-h-96">
            <code className="text-sm font-mono text-dark-900 dark:text-white">{formatJson(event.payload)}</code>
          </pre>
        </div>
      </div>

      {/* Delivery Attempts */}
      <div className="card">
        <div className="card-header">
          <h2 className="text-lg font-semibold text-dark-900 dark:text-white">Delivery Attempts</h2>
        </div>
        <div className="card-body p-0">
          {attempts.length === 0 ? (
            <div className="text-center py-8">
              <Clock className="h-12 w-12 text-dark-300 dark:text-dark-600 mx-auto mb-4" />
              <p className="text-dark-500 dark:text-dark-400">No delivery attempts yet</p>
            </div>
          ) : (
            <div className="space-y-0">
              {attempts.map((attempt, index) => (
                <div
                  key={attempt.id}
                  className={cn(
                    'border-b border-dark-200 dark:border-dark-700 last:border-0',
                    index === 0 && 'bg-green-50/50 dark:bg-green-900/10'
                  )}
                >
                  <button
                    onClick={() => setExpandedAttempt(attempt.id === expandedAttempt ? null : attempt.id)}
                    className="w-full px-6 py-4 flex items-center justify-between gap-4 hover:bg-dark-50 dark:hover:bg-dark-800/50 text-left"
                  >
                    <div className="flex items-center gap-4 flex-1 min-w-0">
                      <div className={cn(
                        'h-10 w-10 rounded-lg flex items-center justify-center flex-shrink-0',
                        attempt.status === 'SUCCESS' && 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400',
                        attempt.status === 'FAILURE' && 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
                        attempt.status === 'TIMEOUT' && 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400',
                        attempt.status === 'IN_PROGRESS' && 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
                      )}>
                        {attempt.status === 'SUCCESS' && <CheckCircle className="h-5 w-5" />}
                        {attempt.status === 'FAILURE' && <XCircle className="h-5 w-5" />}
                        {attempt.status === 'TIMEOUT' && <Clock className="h-5 w-5" />}
                        {attempt.status === 'IN_PROGRESS' && <RotateCcw className="h-5 w-5 animate-spin" />}
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-3">
                          <span className="font-mono text-sm text-dark-900 dark:text-white">
                            Attempt #{attempt.attemptNumber}
                          </span>
                          {getAttemptStatusBadge(attempt.status)}
                        </div>
                        <div className="flex items-center gap-4 text-sm text-dark-500 dark:text-dark-400 mt-1">
                          <span>{formatDateTime(attempt.attemptedAt)}</span>
                          {attempt.durationMs && (
                            <span>{attempt.durationMs}ms</span>
                          )}
                          {attempt.responseStatus && (
                            <span>HTTP {attempt.responseStatus}</span>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {attempt.status === 'SUCCESS' && (
                        <span className="text-green-600 dark:text-green-400 text-sm font-medium">
                          Delivered
                        </span>
                      )}
                      <ChevronDown
                        className={cn(
                          'h-5 w-5 text-dark-400 transition-transform',
                          expandedAttempt === attempt.id && 'rotate-180'
                        )}
                      />
                    </div>
                  </button>

                  {expandedAttempt === attempt.id && (
                    <div className="px-6 pb-6 bg-dark-50 dark:bg-dark-800/50 animate-slide-up">
                      <div className="grid gap-4 md:grid-cols-2">
                        {/* Request */}
                        <div className="space-y-3">
                          <h4 className="text-sm font-medium text-dark-500 dark:text-dark-400 flex items-center gap-2">
                            Request
                            <button
                              onClick={() => copyToClipboard(formatJson(attempt.requestBody), 'Request body')}
                              className="btn-ghost p-1"
                            >
                              <Copy className="h-3.5 w-3.5" />
                            </button>
                          </h4>
                          <div className="bg-dark-100 dark:bg-dark-800 rounded-lg p-3 max-h-64 overflow-auto">
                            <pre className="text-xs font-mono text-dark-900 dark:text-white">
                              {formatJson(attempt.requestHeaders)}
                            </pre>
                          </div>
                          <div className="bg-dark-100 dark:bg-dark-800 rounded-lg p-3 max-h-64 overflow-auto">
                            <pre className="text-xs font-mono text-dark-900 dark:text-white">
                              {formatJson(attempt.requestBody)}
                            </pre>
                          </div>
                        </div>

                        {/* Response */}
                        <div className="space-y-3">
                          <h4 className="text-sm font-medium text-dark-500 dark:text-dark-400 flex items-center gap-2">
                            Response
                            {attempt.responseBody && (
                              <button
                                onClick={() => copyToClipboard(attempt.responseBody, 'Response body')}
                                className="btn-ghost p-1"
                              >
                                <Copy className="h-3.5 w-3.5" />
                              </button>
                            )}
                          </h4>
                          {attempt.responseStatus ? (
                            <>
                              <div className="flex items-center gap-3">
                                <span className={cn(
                                  'px-3 py-1 rounded-full text-sm font-mono',
                                  attempt.responseStatus >= 200 && attempt.responseStatus < 300 && 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400',
                                  attempt.responseStatus >= 400 && 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
                                  (attempt.responseStatus >= 300 && attempt.responseStatus < 400) && 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400'
                                )}>
                                  HTTP {attempt.responseStatus}
                                </span>
                                {attempt.durationMs && (
                                  <span className="text-sm text-dark-500 dark:text-dark-400">{attempt.durationMs}ms</span>
                                )}
                              </div>
                              <div className="bg-dark-100 dark:bg-dark-800 rounded-lg p-3 max-h-64 overflow-auto">
                                <pre className="text-xs font-mono text-dark-900 dark:text-white">
                                  {formatJson(attempt.responseHeaders)}
                                </pre>
                              </div>
                              <div className="bg-dark-100 dark:bg-dark-800 rounded-lg p-3 max-h-64 overflow-auto">
                                <pre className="text-xs font-mono text-dark-900 dark:text-white">
                                  {attempt.responseBody || '(empty)'}
                                </pre>
                              </div>
                            </>
                          ) : attempt.errorMessage ? (
                            <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-3">
                              <pre className="text-sm text-red-700 dark:text-red-300 font-mono">
                                {attempt.errorMessage}
                              </pre>
                            </div>
                          ) : (
                            <p className="text-dark-500 dark:text-dark-400 text-sm">No response data</p>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}