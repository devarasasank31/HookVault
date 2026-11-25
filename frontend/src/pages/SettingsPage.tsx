import { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { toast } from 'react-hot-toast';
import { User, Mail, Lock, Bell, Shield, Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { cn } from '../utils/cn';

const profileSchema = z.object({
  fullName: z.string().max(255).optional(),
  email: z.string().email('Invalid email address'),
});

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Current password is required'),
  newPassword: z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string(),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});

type ProfileForm = z.infer<typeof profileSchema>;
type PasswordForm = z.infer<typeof passwordSchema>;

export function SettingsPage() {
  const { user, logout, refreshUser } = useAuth();
  const [activeTab, setActiveTab] = useState<'profile' | 'security' | 'notifications'>('profile');
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  const profileForm = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      fullName: user?.fullName || '',
      email: user?.email || '',
    },
  });

  const passwordForm = useForm<PasswordForm>({
    resolver: zodResolver(passwordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const handleProfileSubmit = async (data: ProfileForm) => {
    setIsSavingProfile(true);
    try {
      // In a real app, call API to update profile
      await new Promise(resolve => setTimeout(resolve, 500));
      toast.success('Profile updated');
      refreshUser();
    } catch (error) {
      toast.error('Failed to update profile');
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handlePasswordSubmit = async (data: PasswordForm) => {
    setIsChangingPassword(true);
    try {
      // In a real app, call API to change password
      await new Promise(resolve => setTimeout(resolve, 500));
      toast.success('Password changed');
      passwordForm.reset();
    } catch (error) {
      toast.error('Failed to change password');
    } finally {
      setIsChangingPassword(false);
    }
  };

  const tabs = [
    { id: 'profile', label: 'Profile', icon: User },
    { id: 'security', label: 'Security', icon: Shield },
    { id: 'notifications', label: 'Notifications', icon: Bell },
  ];

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-dark-900 dark:text-white">Settings</h1>
        <p className="text-dark-500 dark:text-dark-400 mt-1">Manage your account settings</p>
      </div>

      {/* Tabs */}
      <div className="card">
        <div className="border-b border-dark-200 dark:border-dark-700">
          <nav className="flex gap-1 px-4" aria-label="Settings tabs">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as typeof activeTab)}
                className={cn(
                  'px-4 py-3 text-sm font-medium border-b-2 -mb-px transition-colors',
                  activeTab === tab.id
                    ? 'border-primary-500 text-primary-600 dark:text-primary-400'
                    : 'border-transparent text-dark-500 hover:text-dark-700 dark:text-dark-400 dark:hover:text-dark-200'
                )}
                aria-selected={activeTab === tab.id}
              >
                <tab.icon className="h-4 w-4 inline mr-2" aria-hidden="true" />
                {tab.label}
              </button>
            ))}
          </nav>
        </div>

        <div className="card-body">
          {/* Profile Tab */}
          {activeTab === 'profile' && (
            <form onSubmit={profileForm.handleSubmit(handleProfileSubmit)} className="space-y-6">
              <div className="flex items-center gap-4">
                <div className="h-20 w-20 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                  <User className="h-10 w-10 text-primary-600 dark:text-primary-400" />
                </div>
                <div>
                  <h3 className="text-lg font-medium text-dark-900 dark:text-white">
                    {user?.fullName || 'User'}
                  </h3>
                  <p className="text-dark-500 dark:text-dark-400">{user?.email}</p>
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="fullName" className="label">Full Name</label>
                  <input
                    {...profileForm.register('fullName')}
                    id="fullName"
                    type="text"
                    className="input"
                    placeholder="John Doe"
                  />
                </div>
                <div>
                  <label htmlFor="email" className="label">Email</label>
                  <input
                    {...profileForm.register('email')}
                    id="email"
                    type="email"
                    className="input"
                    disabled
                  />
                  <p className="mt-1 text-sm text-dark-500 dark:text-dark-400">
                    Email cannot be changed. Contact support if needed.
                  </p>
                </div>
              </div>

              <div className="pt-4 border-t border-dark-200 dark:border-dark-700 flex justify-end">
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={isSavingProfile}
                >
                  {isSavingProfile ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Saving...
                    </>
                  ) : (
                    'Save Changes'
                  )}
                </button>
              </div>
            </form>
          )}

          {/* Security Tab */}
          {activeTab === 'security' && (
            <form onSubmit={passwordForm.handleSubmit(handlePasswordSubmit)} className="space-y-6">
              <div>
                <h3 className="text-lg font-medium text-dark-900 dark:text-white mb-4">Change Password</h3>
                <p className="text-dark-500 dark:text-dark-400 mb-6">
                  Your password must be at least 8 characters long.
                </p>

                <div>
                  <label htmlFor="currentPassword" className="label">Current Password</label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-dark-400" />
                    <input
                      {...passwordForm.register('currentPassword')}
                      id="currentPassword"
                      type="password"
                      className="input pl-10"
                      autoComplete="current-password"
                    />
                  </div>
                  {passwordForm.formState.errors.currentPassword && (
                    <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
                      {passwordForm.formState.errors.currentPassword.message}
                    </p>
                  )}
                </div>

                <div>
                  <label htmlFor="newPassword" className="label">New Password</label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-dark-400" />
                    <input
                      {...passwordForm.register('newPassword')}
                      id="newPassword"
                      type="password"
                      className="input pl-10"
                      autoComplete="new-password"
                    />
                  </div>
                  {passwordForm.formState.errors.newPassword && (
                    <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
                      {passwordForm.formState.errors.newPassword.message}
                    </p>
                  )}
                </div>

                <div>
                  <label htmlFor="confirmPassword" className="label">Confirm New Password</label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-dark-400" />
                    <input
                      {...passwordForm.register('confirmPassword')}
                      id="confirmPassword"
                      type="password"
                      className="input pl-10"
                      autoComplete="new-password"
                    />
                  </div>
                  {passwordForm.formState.errors.confirmPassword && (
                    <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
                      {passwordForm.formState.errors.confirmPassword.message}
                    </p>
                  )}
                </div>
              </div>

              <div className="pt-4 border-t border-dark-200 dark:border-dark-700 flex justify-end">
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={isChangingPassword}
                >
                  {isChangingPassword ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Changing...
                    </>
                  ) : (
                    'Change Password'
                  )}
                </button>
              </div>
            </form>
          )}

          {/* Notifications Tab */}
          {activeTab === 'notifications' && (
            <div className="space-y-6">
              <div>
                <h3 className="text-lg font-medium text-dark-900 dark:text-white mb-4">Email Notifications</h3>
                <p className="text-dark-500 dark:text-dark-400 mb-6">
                  Configure when you want to receive email notifications.
                </p>

                <div className="space-y-4">
                  {[
                    { id: 'delivery-failures', label: 'Delivery Failures', description: 'Notify me when webhook deliveries fail' },
                    { id: 'dead-letters', label: 'Dead Letter Queue', description: 'Notify me when events enter the dead letter queue' },
                    { id: 'retry-exhausted', label: 'Retries Exhausted', description: 'Notify me when all retry attempts are exhausted' },
                    { id: 'weekly-digest', label: 'Weekly Digest', description: 'Receive a weekly summary of webhook activity' },
                  ].map((item) => (
                    <label key={item.id} className="flex items-center justify-between p-4 bg-dark-50 dark:bg-dark-800/50 rounded-lg cursor-pointer hover:bg-dark-100 dark:hover:bg-dark-700 transition-colors">
                      <div>
                        <p className="font-medium text-dark-900 dark:text-white">{item.label}</p>
                        <p className="text-sm text-dark-500 dark:text-dark-400">{item.description}</p>
                      </div>
                      <input
                        type="checkbox"
                        defaultChecked={item.id !== 'weekly-digest'}
                        className="h-5 w-5 rounded border-dark-300 text-primary-600 focus:ring-primary-500"
                      />
                    </label>
                  ))}
                </div>
              </div>

              <div className="pt-4 border-t border-dark-200 dark:border-dark-700">
                <h3 className="text-lg font-medium text-dark-900 dark:text-white mb-4">Danger Zone</h3>
                <p className="text-dark-500 dark:text-dark-400 mb-4">
                  Irreversible actions. Proceed with caution.
                </p>
                <button
                  onClick={logout}
                  className="btn-danger"
                >
                  <Loader2 className="h-4 w-4" />
                  Sign Out Everywhere
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}