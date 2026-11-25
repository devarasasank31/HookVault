import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { deadLettersApi } from '../services/api';
import type { DeadLetterResponse, PaginatedResponse } from '../types/api';
import { toast } from 'react-hot-toast';
import {
  Loader2,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
  CheckCircle,
  Trash2,
  AlertTriangle,
  ExternalLink,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { cn, formatDateTime } from '../utils/cn';

export function DeadLettersPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [deadLetters, setDeadLetters] = useState<DeadLetterResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const page = parseInt(searchParams.get('page') || '1', 10);
  const resolved = searchParams.get('resolved') === 'true';

  const fetchDeadLetters = async () => {
    setIsLoading(true);
    try {
      const response = await deadLettersApi.list({
        page: page - 1,
        size: 20,
        sort: 'createdAt,desc',
        resolved,
      });
      const data = response.data.data;
      if (data) {
        setDeadLetters(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      }
    } catch (error) {
      toast.error('Failed to load dead letters');
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDeadLetters();
  }, [page, resolved]);

  const handleReplay = async (id: string) => {
    setActionLoading(id);
    try {
      await deadLettersApi.replay(id);
      toast.success('Event replayed successfully');
      fetchDeadLetters();
    } catch (error) {
      toast.error('Failed to replay event');
    } finally {
      setActionLoading(null);
    }
  };

  const handleResolve = async (id: string) => {
    setActionLoading(id);
    try {
      await deadLettersApi.resolve(id);
      toast.success('Marked as resolved');
      fetchDeadLetters();
    } catch (error) {
      toast.error('Failed to resolve');
    } finally {
      setActionLoading(null);
    }
  };

  const handleDismiss = async (id: string) => {
    if (!confirm('Permanently dismiss this dead letter? This cannot be undone.')) return;
    setActionLoading(id);
    try {
      await deadLettersApi.dismiss(id);
      toast.success('Dead letter dismissed');
      fetchDeadLetters();
    } catch (error) {
      toast.error('Failed to dismiss');
    } finally {
      setActionLoading(null);
    }
  };

  const handlePageChange = (newPage: number) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('page', newPage.toString());
      return next;
    });
  };

  const toggleResolved = () => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      if (resolved) {
        next.delete('resolved');
      } else {
        next.set('resolved', 'true');
      }
      next.set('page', '1');
      return next;
    });
  };

  if (isLoading) {
    return (
      <div className="space-y-6" role="status" aria-label="Loading dead letters">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-dark-900 dark:text-white">Dead Letter Queue</h1>
            <p className="text-dark-500 dark:text-dark-400 mt-1">Manage permanently failed webhook deliveries</p>
          </div>
        </div>
        <div className="card">
          <div className="card-body">
            {[1, 2, 3].map((i) => (
              <div key={i} className="animate-pulse flex items-center gap-4 py-4 border-b border-dark-200 dark:border-dark-700">
                <div className="h-10 w-10 rounded-lg bg-dark-200 dark:bg-dark-700" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-1/4 bg-dark-200 dark:bg-dark-700 rounded" />
                  <div className="h-3 w-1/2 bg-dark-200 dark:bg-dark-700 rounded" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-dark-900 dark:text-white">Dead Letter Queue</h1>
          <p className="text-dark-500 dark:text-dark-400 mt-1">Manage permanently failed webhook deliveries</p>
        </div>
        <button
          onClick={toggleResolved}
          className={cn(
            'btn-secondary',
            resolved && 'bg-primary-100 text-primary-700 border-primary-300 dark:bg-primary-900/30 dark:text-primary-300 dark:border-primary-700'
          )}
        >
          {resolved ? <CheckCircle className="h-4 w-4" /> : <AlertTriangle className="h-4 w-4" />}
          {resolved ? 'Show Unresolved' : 'Show Resolved'}
        </button>
      </div>

      {/* Dead Letters Table */}
      <div className="card">
        <div className="card-body p-0">
          {deadLetters.length === 0 ? (
            <div className="text-center py-12">
              <AlertTriangle className="h-12 w-12 text-dark-300 dark:text-dark-600 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-dark-900 dark:text-white mb-2">
                {resolved ? 'No resolved dead letters' : 'No dead letters'}
              </h3>
              <p className="text-dark-500 dark:text-dark-400">
                {resolved ? 'All caught up! No resolved dead letters to show.' : 'Great! No permanently failed deliveries.'}
              </p>
            </div>
          ) : (
            <>
              <div className="table-container">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Event</th>
                      <th>Endpoint</th>
                      <th>Failure Reason</th>
                      <th>Attempts</th>
                      <th>Last Attempt</th>
                      <th>Status</th>
                      <th className="text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {deadLetters.map((dl) => (
                      <tr key={dl.id}>
                        <td>
                          <div>
                            <p className="font-mono text-sm text-primary-600 hover:underline cursor-pointer">
                              {dl.eventType}
                            </p>
                            <p className="text-xs text-dark-500 dark:text-dark-400 font-mono">
                              {dl.eventId.slice(0, 8)}...
                            </p>
                          </div>
                        </td>
                        <td className="text-dark-500 dark:text-dark-400 font-mono text-sm">
                          {dl.endpointId.slice(0, 8)}...
                        </td>
                        <td>
                          <p className="text-sm text-dark-600 dark:text-dark-300 max-w-xs truncate" title={dl.failureReason}>
                            {dl.failureReason || 'Unknown error'}
                          </p>
                        </td>
                        <td className="font-mono text-dark-900 dark:text-white">{dl.attemptCount}</td>
                        <td className="text-dark-500 dark:text-dark-400 text-sm">
                          {dl.lastAttemptAt ? formatDateTime(dl.lastAttemptAt) : '—'}
                        </td>
                        <td>
                          <span className={cn(
                            'badge',
                            dl.resolved ? 'badge-success' : 'badge-danger'
                          )}>
                            {dl.resolved ? 'Resolved' : 'Unresolved'}
                          </span>
                        </td>
                        <td className="text-right">
                          <div className="flex items-center justify-end gap-2">
                            {!dl.resolved && (
                              <>
                                <button
                                  onClick={() => handleReplay(dl.id)}
                                  disabled={actionLoading === dl.id}
                                  className="btn-ghost text-green-600 hover:text-green-700"
                                  aria-label={`Replay event ${dl.eventId.slice(0, 8)}`}
                                >
                                  {actionLoading === dl.id ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                  ) : (
                                    <RotateCcw className="h-4 w-4" />
                                  )}
                                </button>
                                <button
                                  onClick={() => handleResolve(dl.id)}
                                  disabled={actionLoading === dl.id}
                                  className="btn-ghost text-blue-600 hover:text-blue-700"
                                  aria-label={`Resolve dead letter ${dl.eventId.slice(0, 8)}`}
                                >
                                  <CheckCircle className="h-4 w-4" />
                                </button>
                              </>
                            )}
                            <button
                              onClick={() => handleDismiss(dl.id)}
                              disabled={actionLoading === dl.id}
                              className="btn-ghost text-red-600 hover:text-red-700"
                              aria-label={`Dismiss dead letter ${dl.eventId.slice(0, 8)}`}
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="px-4 py-3 border-t border-dark-200 dark:border-dark-700 flex items-center justify-between">
                  <p className="text-sm text-dark-500 dark:text-dark-400">
                    Page {page} of {totalPages} ({totalElements} total)
                  </p>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handlePageChange(page - 1)}
                      disabled={page === 1}
                      className="btn-ghost p-2"
                      aria-label="Previous page"
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handlePageChange(page + 1)}
                      disabled={page === totalPages}
                      className="btn-ghost p-2"
                      aria-label="Next page"
                    >
                      <ChevronRight className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}