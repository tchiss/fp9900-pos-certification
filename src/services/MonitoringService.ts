import AsyncStorage from '@react-native-async-storage/async-storage';
import Config from '../config/Config';

export interface MetricData {
  timestamp: number;
  type: 'api_call' | 'printer_operation' | 'sync_operation' | 'error' | 'performance';
  category: string;
  value: number;
  metadata?: Record<string, any>;
}

export interface PerformanceMetrics {
  apiLatency: number[];
  certificationRate: number;
  printSuccessRate: number;
  syncSuccessRate: number;
  errorCount: number;
  lastUpdated: number;
}

export interface AuditLog {
  timestamp: number;
  userId?: string;
  terminalId?: string;
  action: string;
  resource: string;
  result: 'success' | 'failure';
  metadata?: Record<string, any>;
}

class MonitoringService {
  private static instance: MonitoringService;
  private metrics: MetricData[] = [];
  private auditLogs: AuditLog[] = [];
  private performanceMetrics: PerformanceMetrics = {
    apiLatency: [],
    certificationRate: 0,
    printSuccessRate: 0,
    syncSuccessRate: 0,
    errorCount: 0,
    lastUpdated: Date.now(),
  };

  private constructor() {
    this.loadStoredData();
  }

  public static getInstance(): MonitoringService {
    if (!MonitoringService.instance) {
      MonitoringService.instance = new MonitoringService();
    }
    return MonitoringService.instance;
  }

  private async loadStoredData(): Promise<void> {
    try {
      const [storedMetrics, storedAuditLogs, storedPerformance] = await Promise.all([
        AsyncStorage.getItem('monitoring_metrics'),
        AsyncStorage.getItem('audit_logs'),
        AsyncStorage.getItem('performance_metrics'),
      ]);

      if (storedMetrics) {
        this.metrics = JSON.parse(storedMetrics);
      }

      if (storedAuditLogs) {
        this.auditLogs = JSON.parse(storedAuditLogs);
      }

      if (storedPerformance) {
        this.performanceMetrics = JSON.parse(storedPerformance);
      }

      console.log('Monitoring data loaded:', {
        metrics: this.metrics.length,
        auditLogs: this.auditLogs.length,
      });
    } catch (error) {
      console.error('Failed to load monitoring data:', error);
    }
  }

  private async saveData(): Promise<void> {
    try {
      await Promise.all([
        AsyncStorage.setItem('monitoring_metrics', JSON.stringify(this.metrics)),
        AsyncStorage.setItem('audit_logs', JSON.stringify(this.auditLogs)),
        AsyncStorage.setItem('performance_metrics', JSON.stringify(this.performanceMetrics)),
      ]);
    } catch (error) {
      console.error('Failed to save monitoring data:', error);
    }
  }

  // Metric recording methods
  public recordMetric(
    type: MetricData['type'],
    category: string,
    value: number,
    metadata?: Record<string, any>
  ): void {
    const metric: MetricData = {
      timestamp: Date.now(),
      type,
      category,
      value,
      metadata,
    };

    this.metrics.push(metric);

    // Keep only recent metrics (last 1000 entries)
    if (this.metrics.length > 1000) {
      this.metrics = this.metrics.slice(-1000);
    }

    this.updatePerformanceMetrics(metric);
    this.saveData();
  }

  public recordApiCall(
    endpoint: string,
    method: string,
    latency: number,
    success: boolean,
    statusCode?: number
  ): void {
    this.recordMetric('api_call', endpoint, latency, {
      method,
      success,
      statusCode,
    });

    if (Config.enableAuditLogging) {
      this.recordAuditLog(
        'api_call',
        `${method} ${endpoint}`,
        success ? 'success' : 'failure',
        { latency, statusCode }
      );
    }
  }

  public recordPrinterOperation(
    operation: string,
    duration: number,
    success: boolean,
    error?: string
  ): void {
    this.recordMetric('printer_operation', operation, duration, {
      success,
      error,
    });

    if (Config.enableAuditLogging) {
      this.recordAuditLog(
        'printer_operation',
        operation,
        success ? 'success' : 'failure',
        { duration, error }
      );
    }
  }

  public recordSyncOperation(
    operation: string,
    itemsCount: number,
    success: boolean,
    duration?: number
  ): void {
    this.recordMetric('sync_operation', operation, itemsCount, {
      success,
      duration,
    });

    if (Config.enableAuditLogging) {
      this.recordAuditLog(
        'sync_operation',
        operation,
        success ? 'success' : 'failure',
        { itemsCount, duration }
      );
    }
  }

  public recordError(
    category: string,
    error: Error | string,
    context?: Record<string, any>
  ): void {
    const errorMessage = error instanceof Error ? error.message : error;
    
    this.recordMetric('error', category, 1, {
      error: errorMessage,
      context,
    });

    this.performanceMetrics.errorCount++;

    if (Config.enableAuditLogging) {
      this.recordAuditLog(
        'error',
        category,
        'failure',
        { error: errorMessage, context }
      );
    }
  }

  // Audit logging
  public recordAuditLog(
    action: string,
    resource: string,
    result: AuditLog['result'],
    metadata?: Record<string, any>
  ): void {
    if (!Config.enableAuditLogging) {
      return;
    }

    const auditLog: AuditLog = {
      timestamp: Date.now(),
      userId: 'system', // TODO: Get from AuthService
      terminalId: 'FP9900_001', // TODO: Get from Config
      action,
      resource,
      result,
      metadata,
    };

    this.auditLogs.push(auditLog);

    // Keep only recent audit logs (last 500 entries)
    if (this.auditLogs.length > 500) {
      this.auditLogs = this.auditLogs.slice(-500);
    }

    this.saveData();
  }

  // Performance metrics calculation
  private updatePerformanceMetrics(metric: MetricData): void {
    switch (metric.type) {
      case 'api_call':
        this.performanceMetrics.apiLatency.push(metric.value);
        // Keep only last 100 API latencies
        if (this.performanceMetrics.apiLatency.length > 100) {
          this.performanceMetrics.apiLatency = this.performanceMetrics.apiLatency.slice(-100);
        }
        break;
    }

    this.performanceMetrics.lastUpdated = Date.now();
  }

  public calculateCertificationRate(): number {
    const certificationMetrics = this.metrics.filter(
      m => m.type === 'api_call' && m.category.includes('certify') && m.metadata?.success
    );
    const totalCertificationCalls = this.metrics.filter(
      m => m.type === 'api_call' && m.category.includes('certify')
    );

    if (totalCertificationCalls.length === 0) return 0;
    
    this.performanceMetrics.certificationRate = 
      (certificationMetrics.length / totalCertificationCalls.length) * 100;
    
    return this.performanceMetrics.certificationRate;
  }

  public calculatePrintSuccessRate(): number {
    const printMetrics = this.metrics.filter(
      m => m.type === 'printer_operation' && m.metadata?.success
    );
    const totalPrintOperations = this.metrics.filter(
      m => m.type === 'printer_operation'
    );

    if (totalPrintOperations.length === 0) return 0;
    
    this.performanceMetrics.printSuccessRate = 
      (printMetrics.length / totalPrintOperations.length) * 100;
    
    return this.performanceMetrics.printSuccessRate;
  }

  public calculateSyncSuccessRate(): number {
    const syncMetrics = this.metrics.filter(
      m => m.type === 'sync_operation' && m.metadata?.success
    );
    const totalSyncOperations = this.metrics.filter(
      m => m.type === 'sync_operation'
    );

    if (totalSyncOperations.length === 0) return 0;
    
    this.performanceMetrics.syncSuccessRate = 
      (syncMetrics.length / totalSyncOperations.length) * 100;
    
    return this.performanceMetrics.syncSuccessRate;
  }

  // Data retrieval methods
  public getMetrics(type?: MetricData['type']): MetricData[] {
    if (type) {
      return this.metrics.filter(m => m.type === type);
    }
    return [...this.metrics];
  }

  public getAuditLogs(): AuditLog[] {
    return [...this.auditLogs];
  }

  public getPerformanceMetrics(): PerformanceMetrics {
    return {
      ...this.performanceMetrics,
      certificationRate: this.calculateCertificationRate(),
      printSuccessRate: this.calculatePrintSuccessRate(),
      syncSuccessRate: this.calculateSyncSuccessRate(),
    };
  }

  public getAverageApiLatency(): number {
    if (this.performanceMetrics.apiLatency.length === 0) return 0;
    
    const sum = this.performanceMetrics.apiLatency.reduce((a, b) => a + b, 0);
    return sum / this.performanceMetrics.apiLatency.length;
  }

  // Data export and cleanup
  public async exportData(): Promise<{
    metrics: MetricData[];
    auditLogs: AuditLog[];
    performanceMetrics: PerformanceMetrics;
  }> {
    return {
      metrics: this.getMetrics(),
      auditLogs: this.getAuditLogs(),
      performanceMetrics: this.getPerformanceMetrics(),
    };
  }

  public async clearOldData(olderThanDays: number = 30): Promise<void> {
    const cutoffTime = Date.now() - (olderThanDays * 24 * 60 * 60 * 1000);
    
    this.metrics = this.metrics.filter(m => m.timestamp > cutoffTime);
    this.auditLogs = this.auditLogs.filter(a => a.timestamp > cutoffTime);
    
    await this.saveData();
    console.log(`Cleared data older than ${olderThanDays} days`);
  }

  public async clearAllData(): Promise<void> {
    this.metrics = [];
    this.auditLogs = [];
    this.performanceMetrics = {
      apiLatency: [],
      certificationRate: 0,
      printSuccessRate: 0,
      syncSuccessRate: 0,
      errorCount: 0,
      lastUpdated: Date.now(),
    };

    await AsyncStorage.multiRemove([
      'monitoring_metrics',
      'audit_logs',
      'performance_metrics',
    ]);

    console.log('All monitoring data cleared');
  }
}

export default MonitoringService.getInstance();
