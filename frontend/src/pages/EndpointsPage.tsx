import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { endpointsApi } from '../services/api';
import type { EndpointResponse, PaginatedResponse } from '../types/api';
import { toast } from 'react-hot-toast';
import {
  Plus,
  Search,
  MoreHorizontal,
  Edit,
  Trash2,
  Key,
  Eye,
  CheckCircle,
  XCircle,
  Loader2,
} from 'lucide-react';
import { cn, formatDateTime } from '../utils/cn';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

const deleteSchema = z.object({
  confirmName: z.string(),
});

type DeleteForm = z.infer<typeof deleteSchema>;

export function EndpointsPage() {
  const [endpoints, setEndpoints] = useState<EndpointResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deletingEndpoint, setDeletingEndpoint] = useState<EndpointResponse | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const {
    register: registerDelete,
    handleSubmit: handleDeleteSubmit,
    reset: resetDelete,
    formState: { errors: deleteErrors },
  } = useForm<DeleteForm>({
    resolver: zodResolver(deleteSchema),
  });

  const fetchEndpoints = async () => {
    try {
      const response = await endpointsApi.list({ size: 100 });
      setEndpoints(response.data.data?.content || []);
    } catch (error) {
      toast.error('Failed to load endpoints');
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchEndpoints();
  }, []);

  const handleDelete = async (endpoint: EndpointResponse) => {
    setDeletingEndpoint(endpoint);
    setDeleteDialogOpen(true);
    resetDelete();
  };

  const confirmDelete = async (data: DeleteForm) => {
    if (!deletingEndpoint) return;

    if (data.confirmName !== deletingEndpoint.name) {
      toast.error('Endpoint name does not match');
      return;
    }

    setIsDeleting(true);
    try {
      await endpointsApi.delete(deletingEndpoint.id);
      toast.success('Endpoint deleted');
      setDeleteDialogOpen(false);
      fetchEndpoints();
    } catch (error) {
      toast.error('Failed to delete endpoint');
      console.error(error);
    } finally {
      setIsDeleting(false);
    }
  };

  const filteredEndpoints = endpoints.filter((endpoint) =>
    endpoint.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    endpoint.url.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (isLoading) {
    return (
      <div className="space-y-6" role="status" aria-label="Loading endpoints">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-dark-900 dark:text-white">Endpoints</h1>
            <p className="text-dark-500 dark:text-dark-400 mt-1">Manage your webhook endpoints</p>
          </div>
          <Link to="/endpoints/create" className="btn-primary">
            <Plus className="h-4 w-4" />
            Create Endpoint
          </Link>
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
          <h1 className="text-2xl font-bold text-dark-900 dark:text-white">Endpoints</h1>
          <p className="text-dark-500 dark:text-dark-400 mt-1">Manage your webhook endpoints</p>
        </div>
        <Link to="/endpoints/create" className="btn-primary">
          <Plus className="h-4 w-4" />
          Create Endpoint
        </Link>
      </div>

      {/* Search */}
      <div className="card">
        <div className="card-body">
          <div className="relative max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-dark-400" aria-hidden="true" />
            <input
              type="text"
              placeholder="Search endpoints..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="input pl-10"
              aria-label="Search endpoints"
            />
          </div>
        </div>
      </div>

      {/* Endpoints Table */}
      <div className="card">
        <div className="card-body p-0">
          {filteredEndpoints.length === 0 ? (
            <div className="text-center py-12">
              <XCircle className="h-12 w-12 text-dark-300 dark:text-dark-600 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-dark-900 dark:text-white mb-2">
                {searchQuery ? 'No matching endpoints' : 'No endpoints yet'}
              </h3>
              <p className="text-dark-500 dark:text-dark-400 mb-4">
                {searchQuery ? 'Try adjusting your search' : 'Create your first webhook endpoint to start receiving events'}
              </p>
              {!searchQuery && (
                <Link to="/endpoints/create" className="btn-primary inline-flex">
                  <Plus className="h-4 w-4" />
                  Create Endpoint
                </Link>
              )}
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>URL</th>
                    <th>Status</th>
                    <th>Events</th>
                    <th>Created</th>
                    <th className="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredEndpoints.map((endpoint) => (
                    <tr key={endpoint.id}>
                      <td>
                        <div>
                          <p className="font-medium text-dark-900 dark:text-white">{endpoint.name}</p>
                          <p className="text-xs text-dark-500 dark:text-dark-400 font-mono">
                            {endpoint.id.slice(0, 8)}...
                          </p>
                        </div>
                      </td>
                      <td>
                        <p className="text-sm text-dark-500 dark:text-dark-400 truncate max-w-xs" title={endpoint.url}>
                          {endpoint.url}
                        </p>
                      </td>
                      <td>
                        <span className={cn(
                          'badge',
                          endpoint.active ? 'badge-success' : 'badge-gray'
                        )}>
                          {endpoint.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td className="text-dark-500 dark:text-dark-400">—</td>
                      <td className="text-dark-500 dark:text-dark-400 text-sm">
                        {formatDateTime(endpoint.createdAt)}
                      </td>
                      <td className="text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Link
                            to={`/endpoints/${endpoint.id}`}
                            className="p-2 rounded-lg text-dark-500 hover:text-dark-700 hover:bg-dark-100 dark:hover:bg-dark-800 dark:hover:text-dark-300"
                            aria-label={`View ${endpoint.name}`}
                          >
                            <Eye className="h-4 w-4" />
                          </Link>
                          <Link
                            to={`/endpoints/${endpoint.id}`}
                            className="p-2 rounded-lg text-dark-500 hover:text-dark-700 hover:bg-dark-100 dark:hover:bg-dark-800 dark:hover:text-dark-300"
                            aria-label={`Edit ${endpoint.name}`}
                          >
                            <Edit className="h-4 w-4" />
                          </Link>
                          <button
                            onClick={() => handleDelete(endpoint)}
                            className="p-2 rounded-lg text-red-500 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20"
                            aria-label={`Delete ${endpoint.name}`}
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
          )}
        </div>
      </div>

      {/* Delete Confirmation Dialog */}
      {deleteDialogOpen && deletingEndpoint && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setDeleteDialogOpen(false)}>
          <div className="bg-white dark:bg-dark-900 rounded-xl shadow-xl max-w-md w-full" onClick={(e) => e.stopPropagation()}>
            <div className="p-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="h-10 w-10 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                  <Trash2 className="h-5 w-5 text-red-600 dark:text-red-400" />
                </div>
                <h3 className="text-lg font-semibold text-dark-900 dark:text-white">Delete Endpoint</h3>
              </div>
              <p className="text-dark-600 dark:text-dark-400 mb-4">
                Are you sure you want to delete <strong>{deletingEndpoint.name}</strong>? This action cannot be undone.
              </p>
              <p className="text-sm text-dark-500 dark:text-dark-400 mb-6">
                Type the endpoint name to confirm:
              </p>
              <form onSubmit={handleDeleteSubmit(confirmDelete)} className="space-y-4">
                <div>
                  <input
                    {...registerDelete('confirmName')}
                    type="text"
                    placeholder="Endpoint name"
                    className={cn(
                      'input',
                      deleteErrors.confirmName && 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                    )}
                    autoFocus
                  />
                  {deleteErrors.confirmName && (
                    <p className="mt-1 text-sm text-red-600 dark:text-red-400">{deleteErrors.confirmName.message}</p>
                  )}
                </div>
                <div className="flex justify-end gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setDeleteDialogOpen(false)}
                    className="btn-secondary"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn-danger"
                    disabled={isDeleting}
                  >
                    {isDeleting ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Deleting...
                      </>
                    ) : (
                      'Delete'
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