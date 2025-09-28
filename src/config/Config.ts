import { Platform } from 'react-native';

// Default configuration values
const DEFAULT_CONFIG = {
  API_BASE_URL: 'http://localhost:3000',
  API_TIMEOUT: 30000,
  SYNC_INTERVAL: 300000, // 5 minutes
  MAX_RETRY_ATTEMPTS: 3,
  CACHE_SIZE: 1000,
  MAX_CONCURRENT_REQUESTS: 5,
  ENABLE_SSL_PINNING: false,
  ENABLE_AUDIT_LOGGING: true,
  LOG_LEVEL: 'info' as 'debug' | 'info' | 'warn' | 'error',
  PRINTER_TIMEOUT: 10000,
  OFFLINE_STORAGE_LIMIT: 100,
};

class Config {
  private static instance: Config;
  private config: typeof DEFAULT_CONFIG;

  private constructor() {
    this.config = { ...DEFAULT_CONFIG };
    this.loadEnvironmentConfig();
  }

  public static getInstance(): Config {
    if (!Config.instance) {
      Config.instance = new Config();
    }
    return Config.instance;
  }

  private loadEnvironmentConfig(): void {
    try {
      // In React Native, we can't directly access process.env
      // Instead, we'll use a different approach for environment variables
      
      // For development, we can use __DEV__ flag
      if (__DEV__) {
        this.config.API_BASE_URL = 'http://10.0.2.2:3000'; // Android emulator
        this.config.LOG_LEVEL = 'debug';
      }

      // Platform-specific configurations
      if (Platform.OS === 'android') {
        this.config.API_BASE_URL = 'http://10.0.2.2:3000';
      } else if (Platform.OS === 'ios') {
        this.config.API_BASE_URL = 'http://localhost:3000';
      }

      console.log('Configuration loaded:', {
        API_BASE_URL: this.config.API_BASE_URL,
        LOG_LEVEL: this.config.LOG_LEVEL,
        PLATFORM: Platform.OS,
      });
    } catch (error) {
      console.warn('Failed to load environment config:', error);
    }
  }

  // Getters for configuration values
  public get apiBaseUrl(): string {
    return this.config.API_BASE_URL;
  }

  public get apiTimeout(): number {
    return this.config.API_TIMEOUT;
  }

  public get syncInterval(): number {
    return this.config.SYNC_INTERVAL;
  }

  public get maxRetryAttempts(): number {
    return this.config.MAX_RETRY_ATTEMPTS;
  }

  public get cacheSize(): number {
    return this.config.CACHE_SIZE;
  }

  public get maxConcurrentRequests(): number {
    return this.config.MAX_CONCURRENT_REQUESTS;
  }

  public get enableSslPinning(): boolean {
    return this.config.ENABLE_SSL_PINNING;
  }

  public get enableAuditLogging(): boolean {
    return this.config.ENABLE_AUDIT_LOGGING;
  }

  public get logLevel(): string {
    return this.config.LOG_LEVEL;
  }

  public get printerTimeout(): number {
    return this.config.PRINTER_TIMEOUT;
  }

  public get offlineStorageLimit(): number {
    return this.config.OFFLINE_STORAGE_LIMIT;
  }

  // Method to update configuration at runtime
  public updateConfig(updates: Partial<typeof DEFAULT_CONFIG>): void {
    this.config = { ...this.config, ...updates };
    console.log('Configuration updated:', updates);
  }

  // Method to get all configuration
  public getAllConfig(): typeof DEFAULT_CONFIG {
    return { ...this.config };
  }

  // Method to reset to defaults
  public resetToDefaults(): void {
    this.config = { ...DEFAULT_CONFIG };
    this.loadEnvironmentConfig();
  }
}

export default Config.getInstance();
