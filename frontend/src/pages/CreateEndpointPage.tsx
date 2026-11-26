import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { endpointsApi } from '../services/api';
import { toast } from 'react-hot-toast';
import { Loader2, ArrowLeft, CheckCircle, Plus } from 'lucide-react';
import { Link } from 'react-router-dom';
import { cn } from '../utils/cn';

const createEndpointSchema = z.object({
  name: z.string().min(1, 'Name is required').max(255, 'Name must be less than 255 characters'),
  url: z.string().url('Invalid URL').max(2048, 'URL too long'),
  description: z.string().max(1000, 'Description too long').optional(),
  headers: z.record(z.string()).optional(),
});

type CreateEndpointForm = z.infer<typeof createEndpointSchema>;

export function CreateEndpointPage() {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [showSecret, setShowSecret] = useState(false);
  const [secret, setSecret] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
    setValue,
  } = useForm<CreateEndpointForm>({
    resolver: zodResolver(createEndpointSchema),
    defaultValues: {
      name: '',
      url: '',
      description: '',
      headers: {},
    },
  });

  const onSubmit = async (data: CreateEndpointForm) => {
    setIsLoading(true);
    try {
      const response = await endpointsApi.create(data);
      const createdEndpoint = response.data.data!;
      setSecret(createdEndpoint.signingSecret || null);
      setShowSecret(true);
      toast.success('Endpoint created successfully');
    } catch (error: unknown) {
      const axiosError = error as { response?: { data?: { message?: string } } };
      toast.error(axiosError.response?.data?.message || 'Failed to create endpoint');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Link to="/endpoints" className="p-2 rounded-lg text-dark-500 hover:text-dark-700 hover:bg-dark-100 dark:hover:bg-dark-800 dark:hover:text-dark-300">
          <ArrowLeft className="h-5 w-5" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-dark-900 dark:text-white">Create Endpoint</h1>
          <p className="text-dark-500 dark:text-dark-400 mt-1">Register a new webhook endpoint to receive events</p>
        </div>
      </div>

      {showSecret && secret && (
        <div className="card border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-900/20 animate-slide-up">
          <div className="card-body">
            <div className="flex items-start gap-3">
              <CheckCircle className="h-5 w-5 text-green-600 dark:text-green-400 mt-0.5 flex-shrink-0" />
              <div className="flex-1">
                <h3 className="font-medium text-green-800 dark:text-green-200">Endpoint created successfully!</h3>
                <p className="text-sm text-green-700 dark:text-green-300 mt-1">
                  Your signing secret is shown below. <strong>Save it now - it won't be shown again.</strong>
                </p>
                <div className="mt-3 flex items-center gap-2">
                  <code className="flex-1 bg-white dark:bg-dark-800 px-3 py-2 rounded font-mono text-sm break-all">
                    {secret}
                  </code>
                  <button
                    onClick={() => navigator.clipboard.writeText(secret).then(() => toast.success('Copied!'))}
                    className="btn-secondary text-sm"
                  >
                    Copy
                  </button>
                </div>
                <button
                  onClick={() => {
                    setShowSecret(false);
                    navigate('/endpoints');
                  }}
                  className="mt-3 text-sm text-green-700 dark:text-green-300 hover:underline"
                >
                  I've saved the secret, continue to endpoints
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Form */}
      <div className="card">
        <div className="card-body">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div>
              <label htmlFor="name" className="label">
                Name <span className="text-red-500">*</span>
              </label>
              <input
                {...register('name')}
                id="name"
                type="text"
                className={cn(
                  'input',
                  errors.name && 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                )}
                placeholder="Stripe Webhooks"
                disabled={isLoading || showSecret}
              />
              {errors.name && (
                <p className="mt-1.5 text-sm text-red-600 dark:text-red-400" role="alert">
                  {errors.name.message}
                </p>
              )}
              <p className="mt-1 text-sm text-dark-500 dark:text-dark-400">
                A descriptive name for this endpoint
              </p>
            </div>

            <div>
              <label htmlFor="url" className="label">
                URL <span className="text-red-500">*</span>
              </label>
              <input
                {...register('url')}
                id="url"
                type="url"
                className={cn(
                  'input',
                  errors.url && 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                )}
                placeholder="https://your-app.com/webhooks"
                disabled={isLoading || showSecret}
              />
              {errors.url && (
                <p className="mt-1.5 text-sm text-red-600 dark:text-red-400" role="alert">
                  {errors.url.message}
                </p>
              )}
              <p className="mt-1 text-sm text-dark-500 dark:text-dark-400">
                The HTTPS URL that will receive webhook events. Only HTTPS URLs are allowed for security.
              </p>
            </div>

            <div>
              <label htmlFor="description" className="label">
                Description
              </label>
              <textarea
                {...register('description')}
                id="description"
                rows={3}
                className="input resize-none"
                placeholder="Optional description of this endpoint"
                disabled={isLoading || showSecret}
              />
              <p className="mt-1 text-sm text-dark-500 dark:text-dark-400">
                Optional description to help you identify this endpoint
              </p>
            </div>

            <div>
              <label className="label">Custom Headers (Optional)</label>
              <div className="space-y-2" id="headers-container">
                <HeadersFieldArray />
              </div>
              <button
                type="button"
                onClick={() => {
                  // Simple approach: just add a new header field
                  const container = document.getElementById('headers-container');
                  if (container) {
                    const count = container.children.length;
                    const div = document.createElement('div');
                    div.className = 'flex gap-2';
                    div.innerHTML = `
                      <input type="text" name="headers[${count}][key]" placeholder="Header name" className="input" />
                      <input type="text" name="headers[${count}][value]" placeholder="Header value" className="input" />
                      <button type="button" className="btn-ghost text-red-500" onclick="this.parentElement.remove()">Remove</button>
                    `;
                    container.appendChild(div);
                  }
                }}
                className="btn-outline text-sm"
              >
                <Plus className="h-4 w-4" />
                Add Header
              </button>
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-dark-200 dark:border-dark-700">
              <Link to="/endpoints" className="btn-secondary">
                <ArrowLeft className="h-4 w-4" />
                Cancel
              </Link>
              <button
                type="submit"
                className="btn-primary"
                disabled={isLoading || showSecret}
              >
                {isLoading ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Creating...
                  </>
                ) : (
                  'Create Endpoint'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

// Simple headers field array component
function HeadersFieldArray() {
  return (
    <div className="flex gap-2">
      <input
        type="text"
        name="headers[0][key]"
        placeholder="Header name (e.g., X-Custom-Header)"
        className="input"
      />
      <input
        type="text"
        name="headers[0][value]"
        placeholder="Header value"
        className="input"
      />
    </div>
  );
}