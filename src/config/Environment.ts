/**
 * Environment configuration for FP9900 POS Application
 * 
 * This file handles environment-specific configurations
 * and provides a centralized way to manage different environments
 */

export type Environment = 'development' | 'staging' | 'production';

export interface EnvironmentConfig {
  environment: Environment;
  apiBaseUrl: string;
  apiTimeout: number;
  syncInterval: number;
  maxRetryAttempts: number;
  cacheSize: number;
  maxConcurrentRequests: number;
  enableSslPinning: boolean;
  enableAuditLogging: boolean;
  logLevel: 'debug' | 'info' | 'warn' | 'error';
  printerTimeout: number;
  offlineStorageLimit: number;
  enableDataEncryption: boolean;
  enablePerformanceMonitoring: boolean;
  enableErrorReporting: boolean;
  certificatePins: string[];
  encryptionKey: string;
  terminalId: string;
  appVersion: string;
  buildNumber: string;
}

// Default configuration for development
const defaultConfig: EnvironmentConfig = {
  environment: 'development',
  apiBaseUrl: 'http://10.0.2.2:3000', // Android emulator
  apiTimeout: 30000,
  syncInterval: 300000, // 5 minutes
  maxRetryAttempts: 3,
  cacheSize: 1000,
  maxConcurrentRequests: 5,
  enableSslPinning: false,
  enableAuditLogging: true,
  logLevel: 'debug',
  printerTimeout: 10000,
  offlineStorageLimit: 100,
  enableDataEncryption: false,
  enablePerformanceMonitoring: true,
  enableErrorReporting: true,
  certificatePins: [],
  encryptionKey: 'development-key-not-secure',
  terminalId: 'FP9900_DEV_001',
  appVersion: '1.0.0',
  buildNumber: '1',
};

// Environment-specific configurations
const environmentConfigs: Record<Environment, Partial<EnvironmentConfig>> = {
  development: {
    environment: 'development',
    apiBaseUrl: 'http://10.0.2.2:3000', // Android emulator
    enableSslPinning: false,
    enableAuditLogging: true,
    logLevel: 'debug',
    enableDataEncryption: false,
    enablePerformanceMonitoring: true,
    enableErrorReporting: true,
    terminalId: 'FP9900_DEV_001',
  },
  
  staging: {
    environment: 'staging',
    apiBaseUrl: 'https://staging-api.yourcompany.com',
    enableSslPinning: true,
    enableAuditLogging: true,
    logLevel: 'info',
    enableDataEncryption: true,
    enablePerformanceMonitoring: true,
    enableErrorReporting: true,
    terminalId: 'FP9900_STAGING_001',
  },
  
  production: {
    environment: 'production',
    apiBaseUrl: 'https://api.yourcompany.com',
    enableSslPinning: true,
    enableAuditLogging: true,
    logLevel: 'warn',
    enableDataEncryption: true,
    enablePerformanceMonitoring: true,
    enableErrorReporting: true,
    terminalId: 'FP9900_PROD_001',
    // Production certificate pins (example)
    certificatePins: [
      'sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=',
      'sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=',
    ],
  },
};

class EnvironmentManager {
  private static instance: EnvironmentManager;
  private config: EnvironmentConfig;
  private currentEnvironment: Environment;

  private constructor() {
    this.currentEnvironment = this.detectEnvironment();
    this.config = this.loadConfiguration();
  }

  public static getInstance(): EnvironmentManager {
    if (!EnvironmentManager.instance) {
      EnvironmentManager.instance = new EnvironmentManager();
    }
    return EnvironmentManager.instance;
  }

  private detectEnvironment(): Environment {
    // In React Native, we can't access process.env directly
    // Instead, we'll use __DEV__ flag and other methods
    
    if (__DEV__) {
      return 'development';
    }
    
    // For production builds, you might want to use a different detection method
    // For example, checking a configuration file or build-time constants
    return 'production';
  }

  private loadConfiguration(): EnvironmentConfig {
    const baseConfig = { ...defaultConfig };
    const envConfig = environmentConfigs[this.currentEnvironment];
    
    return {
      ...baseConfig,
      ...envConfig,
    };
  }

  public getConfig(): EnvironmentConfig {
    return { ...this.config };
  }

  public getEnvironment(): Environment {
    return this.currentEnvironment;
  }

  public isDevelopment(): boolean {
    return this.currentEnvironment === 'development';
  }

  public isStaging(): boolean {
    return this.currentEnvironment === 'staging';
  }

  public isProduction(): boolean {
    return this.currentEnvironment === 'production';
  }

  public updateConfig(updates: Partial<EnvironmentConfig>): void {
    this.config = { ...this.config, ...updates };
  }

  public getApiBaseUrl(): string {
    return this.config.apiBaseUrl;
  }

  public getApiTimeout(): number {
    return this.config.apiTimeout;
  }

  public getSyncInterval(): number {
    return this.config.syncInterval;
  }

  public getMaxRetryAttempts(): number {
    return this.config.maxRetryAttempts;
  }

  public getCacheSize(): number {
    return this.config.cacheSize;
  }

  public getMaxConcurrentRequests(): number {
    return this.config.maxConcurrentRequests;
  }

  public isSslPinningEnabled(): boolean {
    return this.config.enableSslPinning;
  }

  public isAuditLoggingEnabled(): boolean {
    return this.config.enableAuditLogging;
  }

  public getLogLevel(): string {
    return this.config.logLevel;
  }

  public getPrinterTimeout(): number {
    return this.config.printerTimeout;
  }

  public getOfflineStorageLimit(): number {
    return this.config.offlineStorageLimit;
  }

  public isDataEncryptionEnabled(): boolean {
    return this.config.enableDataEncryption;
  }

  public isPerformanceMonitoringEnabled(): boolean {
    return this.config.enablePerformanceMonitoring;
  }

  public isErrorReportingEnabled(): boolean {
    return this.config.enableErrorReporting;
  }

  public getCertificatePins(): string[] {
    return [...this.config.certificatePins];
  }

  public getEncryptionKey(): string {
    return this.config.encryptionKey;
  }

  public getTerminalId(): string {
    return this.config.terminalId;
  }

  public getAppVersion(): string {
    return this.config.appVersion;
  }

  public getBuildNumber(): string {
    return this.config.buildNumber;
  }

  public getAppInfo(): {
    version: string;
    buildNumber: string;
    environment: Environment;
    terminalId: string;
  } {
    return {
      version: this.config.appVersion,
      buildNumber: this.config.buildNumber,
      environment: this.currentEnvironment,
      terminalId: this.config.terminalId,
    };
  }

  public validateConfiguration(): {
    isValid: boolean;
    errors: string[];
    warnings: string[];
  } {
    const errors: string[] = [];
    const warnings: string[] = [];

    // Validate required fields
    if (!this.config.apiBaseUrl) {
      errors.push('API base URL is required');
    }

    if (this.config.apiTimeout < 1000) {
      warnings.push('API timeout is very low (< 1 second)');
    }

    if (this.config.syncInterval < 60000) {
      warnings.push('Sync interval is very low (< 1 minute)');
    }

    if (this.config.maxRetryAttempts < 1) {
      errors.push('Max retry attempts must be at least 1');
    }

    if (this.config.cacheSize < 10) {
      warnings.push('Cache size is very low');
    }

    if (this.config.maxConcurrentRequests < 1) {
      errors.push('Max concurrent requests must be at least 1');
    }

    // Validate environment-specific settings
    if (this.isProduction()) {
      if (!this.config.enableSslPinning) {
        warnings.push('SSL pinning should be enabled in production');
      }

      if (!this.config.enableDataEncryption) {
        warnings.push('Data encryption should be enabled in production');
      }

      if (this.config.certificatePins.length === 0) {
        warnings.push('Certificate pins should be configured in production');
      }

      if (this.config.encryptionKey === 'development-key-not-secure') {
        errors.push('Default encryption key should not be used in production');
      }
    }

    return {
      isValid: errors.length === 0,
      errors,
      warnings,
    };
  }

  public exportConfiguration(): string {
    const config = this.getConfig();
    return JSON.stringify(config, null, 2);
  }
}

export default EnvironmentManager.getInstance();
