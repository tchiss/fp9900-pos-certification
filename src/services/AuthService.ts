import AsyncStorage from '@react-native-async-storage/async-storage';
import Config from '../config/Config';
import { api } from './api';

export interface LoginCredentials {
  username: string;
  password: string;
  terminalId?: string;
}

export interface AuthToken {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
  terminalId: string;
}

export interface User {
  id: string;
  username: string;
  terminalId: string;
  permissions: string[];
}

class AuthService {
  private static instance: AuthService;
  private currentToken: AuthToken | null = null;
  private currentUser: User | null = null;
  private refreshPromise: Promise<AuthToken> | null = null;

  private constructor() {
    this.initializeAuth();
  }

  public static getInstance(): AuthService {
    if (!AuthService.instance) {
      AuthService.instance = new AuthService();
    }
    return AuthService.instance;
  }

  private async initializeAuth(): Promise<void> {
    try {
      const storedToken = await AsyncStorage.getItem('auth_token');
      const storedUser = await AsyncStorage.getItem('auth_user');

      if (storedToken) {
        this.currentToken = JSON.parse(storedToken);
        
        // Check if token is expired
        if (this.currentToken && this.isTokenExpired(this.currentToken)) {
          await this.refreshToken();
        }
      }

      if (storedUser) {
        this.currentUser = JSON.parse(storedUser);
      }
    } catch (error) {
      console.error('Failed to initialize auth:', error);
      await this.logout();
    }
  }

  public async login(credentials: LoginCredentials): Promise<{ user: User; token: AuthToken }> {
    try {
      const response = await api.post('/auth/login', {
        username: credentials.username,
        password: credentials.password,
        terminalId: credentials.terminalId || 'FP9900_001',
      });

      const { user, token } = response.data;

      // Store tokens and user data
      await this.storeAuthData(user, token);

      return { user, token };
    } catch (error) {
      console.error('Login failed:', error);
      throw new Error('Login failed. Please check your credentials.');
    }
  }

  public async logout(): Promise<void> {
    try {
      // Call logout endpoint if token exists
      if (this.currentToken) {
        await api.post('/auth/logout', {}, {
          headers: { Authorization: `Bearer ${this.currentToken.accessToken}` }
        }).catch(() => {
          // Ignore logout API errors
        });
      }

      // Clear stored data
      await AsyncStorage.multiRemove(['auth_token', 'auth_user']);
      
      // Clear memory
      this.currentToken = null;
      this.currentUser = null;
      this.refreshPromise = null;

      console.log('User logged out successfully');
    } catch (error) {
      console.error('Logout failed:', error);
      throw error;
    }
  }

  public async refreshToken(): Promise<AuthToken> {
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    if (!this.currentToken?.refreshToken) {
      throw new Error('No refresh token available');
    }

    this.refreshPromise = this.performTokenRefresh();

    try {
      const newToken = await this.refreshPromise;
      return newToken;
    } finally {
      this.refreshPromise = null;
    }
  }

  private async performTokenRefresh(): Promise<AuthToken> {
    try {
      const response = await api.post('/auth/refresh', {
        refreshToken: this.currentToken?.refreshToken,
      });

      const { token } = response.data;
      await this.storeAuthData(this.currentUser!, token);

      return token;
    } catch (error) {
      console.error('Token refresh failed:', error);
      await this.logout();
      throw new Error('Session expired. Please login again.');
    }
  }

  private async storeAuthData(user: User, token: AuthToken): Promise<void> {
    this.currentToken = token;
    this.currentUser = user;

    await AsyncStorage.setItem('auth_token', JSON.stringify(token));
    await AsyncStorage.setItem('auth_user', JSON.stringify(user));
  }

  public getCurrentToken(): string | null {
    if (!this.currentToken || this.isTokenExpired(this.currentToken)) {
      return null;
    }
    return this.currentToken.accessToken;
  }

  public getCurrentUser(): User | null {
    return this.currentUser;
  }

  public isAuthenticated(): boolean {
    return !!(this.currentToken && !this.isTokenExpired(this.currentToken));
  }

  private isTokenExpired(token: AuthToken): boolean {
    return Date.now() >= token.expiresAt;
  }

  public async validateSession(): Promise<boolean> {
    if (!this.isAuthenticated()) {
      return false;
    }

    try {
      await api.get('/auth/validate', {
        headers: { Authorization: `Bearer ${this.currentToken!.accessToken}` }
      });
      return true;
    } catch (error) {
      console.error('Session validation failed:', error);
      
      // Try to refresh token
      try {
        await this.refreshToken();
        return true;
      } catch (refreshError) {
        await this.logout();
        return false;
      }
    }
  }

  public async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    if (!this.isAuthenticated()) {
      throw new Error('User not authenticated');
    }

    try {
      await api.post('/auth/change-password', {
        currentPassword,
        newPassword,
      }, {
        headers: { Authorization: `Bearer ${this.currentToken!.accessToken}` }
      });
    } catch (error) {
      console.error('Password change failed:', error);
      throw new Error('Failed to change password');
    }
  }

  public hasPermission(permission: string): boolean {
    return this.currentUser?.permissions.includes(permission) || false;
  }

  public getTerminalId(): string | null {
    return this.currentUser?.terminalId || this.currentToken?.terminalId || null;
  }
}

export default AuthService.getInstance();
