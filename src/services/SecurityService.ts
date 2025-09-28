import Config from '../config/Config';
import MonitoringService from './MonitoringService';
import AsyncStorage from '@react-native-async-storage/async-storage';

export interface SecurityConfig {
  enableSslPinning: boolean;
  enableAuditLogging: boolean;
  enableDataEncryption: boolean;
  certificatePins: string[];
  encryptionKey: string;
}

export interface AuditEvent {
  timestamp: number;
  userId?: string;
  terminalId?: string;
  action: string;
  resource: string;
  result: 'success' | 'failure';
  ipAddress?: string;
  userAgent?: string;
  metadata?: Record<string, any>;
}

class SecurityService {
  private static instance: SecurityService;
  private securityConfig: SecurityConfig;

  private constructor() {
    this.securityConfig = {
      enableSslPinning: Config.enableSslPinning,
      enableAuditLogging: Config.enableAuditLogging,
      enableDataEncryption: true,
      certificatePins: [
        // TODO: Add actual certificate pins for production
        'sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=',
      ],
      encryptionKey: 'default-encryption-key', // TODO: Generate secure key
    };
  }

  public static getInstance(): SecurityService {
    if (!SecurityService.instance) {
      SecurityService.instance = new SecurityService();
    }
    return SecurityService.instance;
  }

  // SSL Pinning
  public isSslPinningEnabled(): boolean {
    return this.securityConfig.enableSslPinning;
  }

  public getCertificatePins(): string[] {
    return this.securityConfig.certificatePins;
  }

  public validateCertificate(certificate: string): boolean {
    if (!this.isSslPinningEnabled()) {
      return true; // Skip validation if pinning is disabled
    }

    try {
      // TODO: Implement actual certificate validation
      // This would involve comparing the certificate hash with the pinned certificates
      const isValid = this.securityConfig.certificatePins.includes(certificate);
      
      if (!isValid) {
        MonitoringService.recordAuditLog(
          'ssl_certificate_validation',
          'security',
          'failure',
          { certificate: certificate.substring(0, 20) + '...' }
        );
      }
      
      return isValid;
    } catch (error) {
      MonitoringService.recordError('ssl_validation', error as Error);
      return false;
    }
  }

  // Data Encryption
  public isDataEncryptionEnabled(): boolean {
    return this.securityConfig.enableDataEncryption;
  }

  public async encryptData(data: string): Promise<string> {
    if (!this.isDataEncryptionEnabled()) {
      return data;
    }

    try {
      // TODO: Implement actual encryption using a secure library
      // For now, we'll use a simple base64 encoding as a placeholder
      const encrypted = Buffer.from(data).toString('base64');
      
      MonitoringService.recordAuditLog(
        'data_encrypted',
        'security',
        'success',
        { dataLength: data.length }
      );
      
      return encrypted;
    } catch (error) {
      MonitoringService.recordError('data_encryption', error as Error);
      throw new Error('Failed to encrypt data');
    }
  }

  public async decryptData(encryptedData: string): Promise<string> {
    if (!this.isDataEncryptionEnabled()) {
      return encryptedData;
    }

    try {
      // TODO: Implement actual decryption
      // For now, we'll use a simple base64 decoding as a placeholder
      const decrypted = Buffer.from(encryptedData, 'base64').toString();
      
      MonitoringService.recordAuditLog(
        'data_decrypted',
        'security',
        'success',
        { dataLength: decrypted.length }
      );
      
      return decrypted;
    } catch (error) {
      MonitoringService.recordError('data_decryption', error as Error);
      throw new Error('Failed to decrypt data');
    }
  }

  // Audit Logging
  public isAuditLoggingEnabled(): boolean {
    return this.securityConfig.enableAuditLogging;
  }

  public async recordAuditEvent(event: AuditEvent): Promise<void> {
    if (!this.isAuditLoggingEnabled()) {
      return;
    }

    try {
      const auditLog = {
        ...event,
        timestamp: event.timestamp || Date.now(),
        terminalId: event.terminalId || 'FP9900_001', // TODO: Get from Config
      };

      // Store audit log
      const existingLogs = await this.getAuditLogs();
      existingLogs.push(auditLog);

      // Keep only last 1000 audit logs
      if (existingLogs.length > 1000) {
        existingLogs.splice(0, existingLogs.length - 1000);
      }

      await AsyncStorage.setItem('audit_logs', JSON.stringify(existingLogs));

      console.log('[SecurityService] Audit event recorded:', event.action);
    } catch (error) {
      console.error('[SecurityService] Failed to record audit event:', error);
      MonitoringService.recordError('audit_logging', error as Error);
    }
  }

  public async getAuditLogs(): Promise<AuditEvent[]> {
    try {
      const logs = await AsyncStorage.getItem('audit_logs');
      return logs ? JSON.parse(logs) : [];
    } catch (error) {
      console.error('[SecurityService] Failed to get audit logs:', error);
      return [];
    }
  }

  public async exportAuditLogs(): Promise<string> {
    try {
      const logs = await this.getAuditLogs();
      const exportData = {
        exportDate: new Date().toISOString(),
        terminalId: 'FP9900_001', // TODO: Get from Config
        totalLogs: logs.length,
        logs: logs,
      };

      return JSON.stringify(exportData, null, 2);
    } catch (error) {
      console.error('[SecurityService] Failed to export audit logs:', error);
      throw new Error('Failed to export audit logs');
    }
  }

  public async clearAuditLogs(): Promise<void> {
    try {
      await AsyncStorage.removeItem('audit_logs');
      
      MonitoringService.recordAuditLog(
        'audit_logs_cleared',
        'security',
        'success',
        { timestamp: Date.now() }
      );
      
      console.log('[SecurityService] Audit logs cleared');
    } catch (error) {
      console.error('[SecurityService] Failed to clear audit logs:', error);
      throw new Error('Failed to clear audit logs');
    }
  }

  // Security Configuration
  public updateSecurityConfig(config: Partial<SecurityConfig>): void {
    this.securityConfig = { ...this.securityConfig, ...config };
    
    MonitoringService.recordAuditLog(
      'security_config_updated',
      'security',
      'success',
      { changes: Object.keys(config) }
    );
    
    console.log('[SecurityService] Security configuration updated');
  }

  public getSecurityConfig(): SecurityConfig {
    return { ...this.securityConfig };
  }

  // Security Validation
  public validateSecurityRequirements(): {
    isValid: boolean;
    issues: string[];
  } {
    const issues: string[] = [];

    if (this.securityConfig.enableSslPinning && this.securityConfig.certificatePins.length === 0) {
      issues.push('SSL pinning enabled but no certificate pins configured');
    }

    if (this.securityConfig.enableDataEncryption && this.securityConfig.encryptionKey === 'default-encryption-key') {
      issues.push('Data encryption enabled but using default encryption key');
    }

    if (this.securityConfig.enableAuditLogging && !Config.enableAuditLogging) {
      issues.push('Audit logging enabled in security config but disabled in app config');
    }

    return {
      isValid: issues.length === 0,
      issues,
    };
  }

  // Generate secure encryption key
  public generateEncryptionKey(): string {
    // TODO: Implement secure key generation
    // This should use a cryptographically secure random number generator
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < 32; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  }

  // Hash sensitive data
  public hashData(data: string): string {
    // TODO: Implement secure hashing (SHA-256)
    // For now, we'll use a simple hash function
    let hash = 0;
    for (let i = 0; i < data.length; i++) {
      const char = data.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    return hash.toString(16);
  }
}

export default SecurityService.getInstance();
