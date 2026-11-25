import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { eventsApi } from '../services/api';
import type { EventResponse, PaginatedResponse } from '../types/api';
import { toast } from 'react-hot-toast';
import {
  Search,
  Filter,
  Loader2,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { cn, formatDateTime } from '../utils/cn';

const STATUS_OPTIONS = [
  { value: '', label: 'All Statuses' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'RETRY_SCHEDULED', label: 'Retry Scheduled' },
  { value: 'DEAD_LETTERED', label: 'Dead Lettered' },
];

export function EventsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const page = parseInt(searchParams.get('page') || '1', 10);
  const status = searchParams.get('status') || '';
  const search = searchParams.get('search') || '';
  const endpointId = searchParams.get('endpointId') || '';

  const fetchEvents = async () => {
    setIsLoading(true);
    try {
      const response = await eventsApi.list({
        page: page - 1,
        size: 20,
        sort: 'createdAt,desc',
        status: status || undefined,
        endpointId: endpointId || undefined,
      });
      const data = response.data.data;
      if (data) {
        setEvents(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      }
    } catch (error) {
      toast.error('Failed to load events');
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
  }, [page, status, search, endpointId]);

  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const newSearch = formData.get('search') as string;
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      if (newSearch) {
        next.set('search', newSearch);
      } else {
        next.delete('search');
      }
      next.set('page', '1');
      return next;
    });
  };

  const handleStatusChange = (newStatus: string) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      if (newStatus) {
        next.set('status', newStatus);
      } else {
        next.delete('status');
      }
      next.set('page', '1');
      return next;
    });
  };

  const handlePageChange = (newPage: number) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('page', newPage.toString());
      return next;
    });
  };

  const getStatusBadge = (status: string) => {
    return (
      <span className={cn(
        'badge',
        status === 'DELIVERED' && 'badge-success',
        status === 'FAILED' && 'badge-danger',
        status === 'PENDING' && 'badge-warning',
        status === 'RETRY_SCHEDULED' && 'badge-info',
        status === 'DEAD_LETTERED' && 'badge-danger'
      )}>
        {status}
      </span>
    );
  };

  if (isLoading) {
    return (
      <div className="space-y-6" role="status" aria-label="Loading events">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-dark-900 dark:text-white">Events</h1>
            <p className="text-dark-500 dark:text-dark-400 mt-1">Browse and search webhook events</p>
          </div>
        </div>
        <div className="card">
          <div className="card-body">
            {[1, 2, 3, 4, 5].map((i) => (
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
          <h1 className="text-2xl font-bold text-dark-900 dark:text-white">Events</h1>
          <p className="text-dark-500 dark:text-dark-400 mt-1">Browse and search webhook events</p>
        </div>
      </div>

      {/* Filters */}
      <div className="card">
        <div className="card-body">
          <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-dark-400" aria-hidden="true" />
              <input
                name="search"
                type="text"
                placeholder="Search by event ID, type..."
                value={search}
                className="input pl-10"
              />
            </div>
            <select
              name="status"
              value={status}
              onChange={(e) => handleStatusChange(e.target.value)}
              className="input w-full sm:w-48"
            >
              {STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </form>
        </div>
      </div>

      {/* Events Table */}
      <div className="card">
        <div className="card-body p-0">
          {events.length === 0 ? (
            <div className="text-center py-12">
              <Search className="h-12 w-12 text-dark-300 dark:text-dark-600 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-dark-900 dark:text-white mb-2">No events found</h3>
              <p className="text-dark-500 dark:text-dark-400">
                {search || status ? 'Try adjusting your filters' : 'Events will appear here when webhooks are received'}
              </p>
            </div>
          ) : (
            <>
              <div className="table-container">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Event ID</th>
                      <th>Type</th>
                      <th>Endpoint</th>
                      <th>Status</th>
                      <th>Created</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {events.map((event) => (
                      <tr key={event.id}>
                        <td>
                          <Link to={`/events/${event.id}`} className="font-mono text-sm text-primary-600 hover:underline">
                            {event.id.slice(0, 12)}...
                          </Link>
                        </td>
                        <td className="font-medium text-dark-900 dark:text-white">{event.eventType}</td>
                        <td className="text-dark-500 dark:text-dark-400">
                          {event.endpointId.slice(0, 8)}...
                        </td>
                        <td>{getStatusBadge(event.status)}</td>
                        <td className="text-dark-500 dark:text-dark-400 text-sm">
                          {formatDateTime(event.createdAt)}
                        </td>
                        <td className="text-right">
                          <Link
                            to={`/events/${event.id}`}
                            className="p-2 rounded-lg text-dark-500 hover:text-dark-700 hover:bg-dark-100 dark:hover:bg-dark-800 dark:hover:text-dark-300"
                            aria-label={`View event ${event.id.slice(0, 8)}`}
                          >
                            <ChevronRight className="h-4 w-4" />
                          </Link>
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