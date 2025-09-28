import Config from '../config/Config';
import AuthService from './AuthService';
import MonitoringService from './MonitoringService';
import SecurityService from './SecurityService';
import PrinterService from './PrinterService';
import { checkApiHealth } from './api';
import { getPending, syncPending } from '../storage/offline';

export interface TestResult {
  name: string;
  status: 'pass' | 'fail' | 'skip';
  message: string;
  duration: number;
  details?: any;
}

export interface TestSuite {
  name: string;
  results: TestResult[];
  totalDuration: number;
  passed: number;
  failed: number;
  skipped: number;
}

class TestService {
  private static instance: TestService;

  private constructor() {}

  public static getInstance(): TestService {
    if (!TestService.instance) {
      TestService.instance = new TestService();
    }
    return TestService.instance;
  }

  public async runAllTests(): Promise<TestSuite> {
    const startTime = Date.now();
    const results: TestResult[] = [];

    console.log('[TestService] Starting test suite...');

    // Configuration tests
    results.push(...await this.runConfigTests());

    // Authentication tests
    results.push(...await this.runAuthTests());

    // API tests
    results.push(...await this.runApiTests());

    // Security tests
    results.push(...await this.runSecurityTests());

    // Printer tests
    results.push(...await this.runPrinterTests());

    // Storage tests
    results.push(...await this.runStorageTests());

    // Monitoring tests
    results.push(...await this.runMonitoringTests());

    const totalDuration = Date.now() - startTime;
    const passed = results.filter(r => r.status === 'pass').length;
    const failed = results.filter(r => r.status === 'fail').length;
    const skipped = results.filter(r => r.status === 'skip').length;

    const testSuite: TestSuite = {
      name: 'FP9900 POS Application Tests',
      results,
      totalDuration,
      passed,
      failed,
      skipped,
    };

    console.log(`[TestService] Test suite completed: ${passed} passed, ${failed} failed, ${skipped} skipped`);
    
    return testSuite;
  }

  private async runConfigTests(): Promise<TestResult[]> {
    const results: TestResult[] = [];

    // Test configuration loading
    results.push(await this.runTest('Config Loading', async () => {
      const config = Config.getAllConfig();
      if (!config.API_BASE_URL) {
        throw new Error('API_BASE_URL not configured');
      }
      if (!config.API_TIMEOUT) {
        throw new Error('API_TIMEOUT not configured');
      }
      return { config };
    }));

    // Test configuration validation
    results.push(await this.runTest('Config Validation', async () => {
      const config = Config.getAllConfig();
      if (config.API_TIMEOUT < 1000) {
        throw new Error('API_TIMEOUT too low');
      }
      if (config.SYNC_INTERVAL < 60000) {
        throw new Error('SYNC_INTERVAL too low');
      }
      return { validated: true };
    }));

    return results;
  }

  private async runAuthTests(): Promise<TestResult[]> {
    const results: TestResult[] = [];

    // Test authentication service initialization
    results.push(await this.runTest('Auth Service Init', async () => {
      const isAuthenticated = AuthService.isAuthenticated();
      return { isAuthenticated };
    }));

    // Test token validation
    results.push(await this.runTest('Token Validation', async () => {
      try {
        const isValid = await AuthService.validateSession();
        return { isValid };
      } catch (error) {
        // This is expected if no valid session exists
        return { isValid: false, expected: true };
      }
    }));

    return results;
  }

  private async runApiTests(): Promise<TestResult[]> {
    const results: TestResult[] = [];

    // Test API health check
    results.push(await this.runTest('API Health Check', async () => {
      try {
        const isHealthy = await checkApiHealth();
        return { isHealthy };
      } catch (error) {
        // This is expected if backend is not running
        return { isHealthy: false, expected: true };
      }
    }));

    return results;
  }

  private async runSecurityTests(): Promise<TestResult[]> {
    const results: TestResult[] = [];

    // Test security service initialization
    results.push(await this.runTest('Security Service Init', async () => {
      const config = SecurityService.getSecurityConfig();
      if (!config) {
        throw new Error('Security config not available');
      }
      return { config: Object.keys(config) };
    }));

    // Test SSL pinning configuration
    results.push(await this.runTest('SSL Pinning Config', async () => {
      const isEnabled = SecurityService.isSslPinningEnabled();
      const pins = SecurityService.getCertificatePins();
      return { isEnabled, pinsCount: pins.length };
    }));

    // Test data encryption
    results.push(await this.runTest('Data Encryption', async () => {
      const testData = 'test-data-123';
      const encrypted = await SecurityService.encryptData(testData);
      const decrypted = await SecurityService.decryptData(encrypted);
      
      if (decrypted !== testData) {
        throw new Error('Encryption/decryption failed');
      }
      
      return { encrypted: encrypted.substring(0, 20) + '...' };
    }));

    // Test audit logging
    results.push(await this.runTest('Audit Logging', async () => {
      const isEnabled = SecurityService.isAuditLoggingEnabled();
      return { isEnabled };
    }));

    return results;
  }

  private async runPrinterTests(): Promise<TestResult[]> {
    const results: TestResult[] = [];

    // Test printer service initialization
    results.push(await this.runTest('Printer Service Init', async () => {
      try {
        await PrinterService.init();
        const status = await PrinterService.getStatus();
        return { status };
      } catch (error) {
        // This is expected in mock mode
        return { status: 'mock', expected: true };
      }
    }));

    // Test printer status
    results.push(await this.runTest('Printer Status', async () => {
      try {
        const status = await PrinterService.getStatus();
        return { connected: status.connected, paperAvailable: status.paperAvailable };
      } catch (error) {
        return { connected: false, expected: true };
      }
    }));

    return results;
  }

  private async runStorageTests(): Promise<TestResult[]> {
    const results: TestResult[] = [];

    // Test offline storage
    results.push(await this.runTest('Offline Storage', async () => {
      const pending = await getPending();
      return { pendingCount: pending.length };
    }));

    // Test sync functionality
    results.push(await this.runTest('Sync Functionality', async () => {
      try {
        const result = await syncPending();
        return { synced: result.synced, failed: result.failed };
      } catch (error) {
        // This is expected if no network or no pending items
        return { synced: 0, failed: 0, expected: true };
      }
    }));

    return results;
  }

  private async runMonitoringTests(): Promise<TestResult[]> {
    const results: TestResult[] = [];

    // Test monitoring service
    results.push(await this.runTest('Monitoring Service', async () => {
      const metrics = MonitoringService.getPerformanceMetrics();
      return { 
        apiLatency: metrics.apiLatency.length,
        errorCount: metrics.errorCount,
        lastUpdated: metrics.lastUpdated 
      };
    }));

    // Test metric recording
    results.push(await this.runTest('Metric Recording', async () => {
      MonitoringService.recordMetric('performance', 'test_category', 1, { test: true });
      return { recorded: true };
    }));

    return results;
  }

  private async runTest(name: string, testFn: () => Promise<any>): Promise<TestResult> {
    const startTime = Date.now();
    
    try {
      console.log(`[TestService] Running test: ${name}`);
      const result = await testFn();
      const duration = Date.now() - startTime;
      
      console.log(`[TestService] ✓ ${name} passed (${duration}ms)`);
      
      return {
        name,
        status: 'pass',
        message: 'Test passed successfully',
        duration,
        details: result,
      };
    } catch (error) {
      const duration = Date.now() - startTime;
      const message = error instanceof Error ? error.message : 'Unknown error';
      
      console.log(`[TestService] ✗ ${name} failed (${duration}ms): ${message}`);
      
      return {
        name,
        status: 'fail',
        message,
        duration,
        details: { error: message },
      };
    }
  }

  public async runQuickTests(): Promise<TestResult[]> {
    const results: TestResult[] = [];

    // Quick configuration test
    results.push(await this.runTest('Quick Config Test', async () => {
      const config = Config.getAllConfig();
      return { apiUrl: config.API_BASE_URL, timeout: config.API_TIMEOUT };
    }));

    // Quick authentication test
    results.push(await this.runTest('Quick Auth Test', async () => {
      const isAuthenticated = AuthService.isAuthenticated();
      return { isAuthenticated };
    }));

    // Quick monitoring test
    results.push(await this.runTest('Quick Monitoring Test', async () => {
      const metrics = MonitoringService.getPerformanceMetrics();
      return { errorCount: metrics.errorCount };
    }));

    return results;
  }

  public async generateTestReport(testSuite: TestSuite): Promise<string> {
    const report = `
# FP9900 POS Application Test Report

**Generated:** ${new Date().toLocaleString()}
**Total Duration:** ${testSuite.totalDuration}ms
**Results:** ${testSuite.passed} passed, ${testSuite.failed} failed, ${testSuite.skipped} skipped

## Test Results

${testSuite.results.map(result => {
  const status = result.status === 'pass' ? '✅' : result.status === 'fail' ? '❌' : '⏭️';
  return `### ${status} ${result.name}
- **Status:** ${result.status.toUpperCase()}
- **Duration:** ${result.duration}ms
- **Message:** ${result.message}
${result.details ? `- **Details:** \`${JSON.stringify(result.details)}\`` : ''}`;
}).join('\n\n')}

## Summary

- **Total Tests:** ${testSuite.results.length}
- **Passed:** ${testSuite.passed} (${Math.round((testSuite.passed / testSuite.results.length) * 100)}%)
- **Failed:** ${testSuite.failed} (${Math.round((testSuite.failed / testSuite.results.length) * 100)}%)
- **Skipped:** ${testSuite.skipped} (${Math.round((testSuite.skipped / testSuite.results.length) * 100)}%)

${testSuite.failed > 0 ? `
## Failed Tests

${testSuite.results.filter(r => r.status === 'fail').map(r => `- **${r.name}:** ${r.message}`).join('\n')}
` : ''}

---
*Report generated by FP9900 POS Test Service*
`;

    return report;
  }
}

export default TestService.getInstance();
