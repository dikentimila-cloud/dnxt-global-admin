import { create } from 'zustand';

export interface User {
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  permissions: string[];
  mustChangePassword: boolean;
}

interface StoreState {
  token: string | null;
  user: User | null;
  sidebarOpen: boolean;
  setAuth: (token: string, user: User) => void;
  logout: () => void;
  hasPermission: (permission: string) => boolean;
  toggleSidebar: () => void;
}

export const useStore = create<StoreState>((set, get) => ({
  token: localStorage.getItem('gadmin_token'),
  user: localStorage.getItem('gadmin_user')
    ? JSON.parse(localStorage.getItem('gadmin_user')!)
    : null,
  sidebarOpen: true,

  setAuth: (token: string, user: User) => {
    localStorage.setItem('gadmin_token', token);
    localStorage.setItem('gadmin_user', JSON.stringify(user));
    set({ token, user });
  },

  logout: () => {
    localStorage.removeItem('gadmin_token');
    localStorage.removeItem('gadmin_user');
    set({ token: null, user: null });
  },

  hasPermission: (permission: string) => {
    const user = get().user;
    if (!user) return false;
    return user.permissions.includes(permission);
  },

  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
}));
