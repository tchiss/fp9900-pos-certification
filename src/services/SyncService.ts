import { AppRegistry } from 'react-native';
import NetInfo from '@react-native-community/netinfo';
import { syncPending, getPending } from '../storage/offline';
import MonitoringService from './MonitoringService';
import Config from '../config/Config';

class SyncService {
  private static instance: SyncService;
  private syncInterval: NodeJS.Timeout | null = null;
  private isRunning = false;

  private constructor() {
    this.setupNetworkListener();
  }

  public static getInstance(): SyncService {
    if (!SyncService.instance) {
      SyncService.instance = new SyncService();
    }
    return SyncService.instance;
  }

  private setupNetworkListener(): void {
    NetInfo.addEventListener(state => {
      if (state.isConnected && !this.isRunning) {
        console.log('[SyncService] Network connected, starting auto-sync');
        this.startAutoSync();
      } else if (!state.isConnected && this.isRunning) {
        console.log('[SyncService] Network disconnected, stopping auto-sync');
        this.stopAutoSync();
      }
    });
  }

  public startAutoSync(): void {
    if (this.isRunning) {
      return;
    }

    this.isRunning = true;
    console.log('[SyncService] Starting auto-sync with interval:', Config.syncInterval);

    // Immediate sync
    this.performSync();

    // Set up interval sync
    this.syncInterval = setInterval(() => {
      this.performSync();
    }, Config.syncInterval);

    MonitoringService.recordAuditLog(
      'auto_sync_started',
      'sync_service',
      'success',
      { interval: Config.syncInterval }
    );
  }

  public stopAutoSync(): void {
    if (!this.isRunning) {
      return;
    }

    this.isRunning = false;

    if (this.syncInterval) {
      clearInterval(this.syncInterval);
      this.syncInterval = null;
    }

    console.log('[SyncService] Auto-sync stopped');

    MonitoringService.recordAuditLog(
      'auto_sync_stopped',
      'sync_service',
      'success',
      { timestamp: Date.now() }
    );
  }

  public async performSync(): Promise<void> {
    try {
      const pendingInvoices = await getPending();
      
      if (pendingInvoices.length === 0) {
        console.log('[SyncService] No pending invoices to sync');
        return;
      }

      console.log(`[SyncService] Syncing ${pendingInvoices.length} pending invoices`);

      const startTime = Date.now();
      const result = await syncPending();
      const duration = Date.now() - startTime;

      if (result.synced > 0) {
        console.log(`[SyncService] Successfully synced ${result.synced} invoices`);
        
        MonitoringService.recordSyncOperation(
          'auto_sync',
          result.synced,
          true,
          duration
        );

        MonitoringService.recordAuditLog(
          'invoices_synced',
          'sync_service',
          'success',
          { 
            synced: result.synced,
            failed: result.failed,
            duration 
          }
        );
      }

      if (result.failed > 0) {
        console.warn(`[SyncService] Failed to sync ${result.failed} invoices`);
        
        MonitoringService.recordSyncOperation(
          'auto_sync_failed',
          result.failed,
          false,
          duration
        );
      }

    } catch (error) {
      console.error('[SyncService] Sync failed:', error);
      
      MonitoringService.recordError('auto_sync', error as Error);
      
      MonitoringService.recordAuditLog(
        'sync_error',
        'sync_service',
        'failure',
        { error: error instanceof Error ? error.message : 'Unknown error' }
      );
    }
  }

  public async manualSync(): Promise<{ synced: number; failed: number }> {
    console.log('[SyncService] Manual sync requested');
    
    try {
      const startTime = Date.now();
      const result = await syncPending();
      const duration = Date.now() - startTime;

      MonitoringService.recordSyncOperation(
        'manual_sync',
        result.synced,
        true,
        duration
      );

      MonitoringService.recordAuditLog(
        'manual_sync',
        'sync_service',
        'success',
        { 
          synced: result.synced,
          failed: result.failed,
          duration 
        }
      );

      return result;
    } catch (error) {
      console.error('[SyncService] Manual sync failed:', error);
      
      MonitoringService.recordError('manual_sync', error as Error);
      
      MonitoringService.recordAuditLog(
        'manual_sync_error',
        'sync_service',
        'failure',
        { error: error instanceof Error ? error.message : 'Unknown error' }
      );

      throw error;
    }
  }

  public isAutoSyncRunning(): boolean {
    return this.isRunning;
  }

  public async getSyncStatus(): Promise<{
    isRunning: boolean;
    pendingCount: number;
    lastSyncTime?: number;
  }> {
    const pendingInvoices = await getPending();
    
    return {
      isRunning: this.isRunning,
      pendingCount: pendingInvoices.length,
      lastSyncTime: Date.now(), // TODO: Store actual last sync time
    };
  }
}

// Register headless task for background sync
AppRegistry.registerHeadlessTask('syncPendingInvoices', () => {
  console.log('[SyncService] Headless task registered');
  
  return async (taskData: any) => {
    console.log('[SyncService] Headless task started with data:', taskData);
    
    try {
      const syncService = SyncService.getInstance();
      await syncService.performSync();
      
      console.log('[SyncService] Headless task completed successfully');
    } catch (error) {
      console.error('[SyncService] Headless task failed:', error);
      MonitoringService.recordError('headless_sync', error as Error);
    }
  };
});

export default SyncService.getInstance();
