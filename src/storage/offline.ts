import AsyncStorage from '@react-native-async-storage/async-storage';
import NetInfo from '@react-native-community/netinfo';
import { certifyInvoice, InvoiceRequest, ApiError, withRetry } from '../services/api';
import MonitoringService from '../services/MonitoringService';
import Config from '../config/Config';
import SyncService from '../services/SyncService';

const KEYS = {
  PENDING_INVOICES: 'pending_invoices',
  SYNC_STATS: 'sync_stats',
  LAST_SYNC: 'last_sync',
  FAILED_SYNC: 'failed_sync',
};

export interface PendingInvoice extends InvoiceRequest {
  id: string;
  timestamp: number;
  attempts: number;
  lastAttempt?: number;
  error?: string;
}

export interface SyncStats {
  totalSynced: number;
  totalFailed: number;
  lastSyncTime?: number;
  averageSyncTime: number;
}

export interface SyncResult {
  synced: number;
  failed: number;
  total: number;
  errors: Array<{ id: string; error: string }>;
}

export async function enqueuePending(inv: InvoiceRequest): Promise<string> {
  try {
    const pendingInvoice: PendingInvoice = {
      ...inv,
      id: generateId(),
      timestamp: Date.now(),
      attempts: 0,
    };

    const raw = await AsyncStorage.getItem(KEYS.PENDING_INVOICES);
    const arr = raw ? JSON.parse(raw) as PendingInvoice[] : [];
    
    // Check for duplicates (same IFU + timestamp within 1 minute)
    const duplicate = arr.find(item => 
      item.issuerIFU === inv.issuerIFU && 
      Math.abs(item.timestamp - pendingInvoice.timestamp) < 60000
    );
    
    if (duplicate) {
      console.log('[OfflineStorage] Duplicate invoice detected, skipping');
      return duplicate.id;
    }
    
    arr.push(pendingInvoice);
    await AsyncStorage.setItem(KEYS.PENDING_INVOICES, JSON.stringify(arr));
    
    console.log(`[OfflineStorage] Invoice ${pendingInvoice.id} enqueued for sync`);
    return pendingInvoice.id;
  } catch (error) {
    console.error('[OfflineStorage] Failed to enqueue invoice:', error);
    throw new Error('Impossible d\'enregistrer la facture hors ligne');
  }
}

export async function getPending(): Promise<PendingInvoice[]> {
  try {
    const raw = await AsyncStorage.getItem(KEYS.PENDING_INVOICES);
    return raw ? JSON.parse(raw) as PendingInvoice[] : [];
  } catch (error) {
    console.error('[OfflineStorage] Failed to get pending invoices:', error);
    return [];
  }
}

export async function removePending(id: string): Promise<void> {
  try {
    const arr = await getPending();
    const filtered = arr.filter(item => item.id !== id);
    await AsyncStorage.setItem(KEYS.PENDING_INVOICES, JSON.stringify(filtered));
    console.log(`[OfflineStorage] Invoice ${id} removed from pending`);
  } catch (error) {
    console.error('[OfflineStorage] Failed to remove pending invoice:', error);
  }
}

export async function clearPending(): Promise<void> {
  try {
    await AsyncStorage.removeItem(KEYS.PENDING_INVOICES);
    await AsyncStorage.removeItem(KEYS.FAILED_SYNC);
    console.log('[OfflineStorage] All pending invoices cleared');
  } catch (error) {
    console.error('[OfflineStorage] Failed to clear pending invoices:', error);
  }
}

export async function updatePendingAttempt(id: string, error?: string): Promise<void> {
  try {
    const arr = await getPending();
    const item = arr.find(i => i.id === id);
    if (item) {
      item.attempts += 1;
      item.lastAttempt = Date.now();
      if (error) {
        item.error = error;
      }
      await AsyncStorage.setItem(KEYS.PENDING_INVOICES, JSON.stringify(arr));
    }
  } catch (error) {
    console.error('[OfflineStorage] Failed to update pending attempt:', error);
  }
}

export async function syncPending(): Promise<SyncResult> {
  const startTime = Date.now();
  const result: SyncResult = { synced: 0, failed: 0, total: 0, errors: [] };

  try {
  const state = await NetInfo.fetch();
    if (!state.isConnected) {
      console.log('[OfflineStorage] No network connection, skipping sync');
      return result;
    }

    const pending = await getPending();
    result.total = pending.length;

    if (pending.length === 0) {
      console.log('[OfflineStorage] No pending invoices to sync');
      return result;
    }

    console.log(`[OfflineStorage] Starting sync of ${pending.length} pending invoices`);

    const remaining: PendingInvoice[] = [];

    for (const invoice of pending) {
      try {
        // Skip invoices that have failed too many times
        if (invoice.attempts >= 3) {
          console.log(`[OfflineStorage] Invoice ${invoice.id} has failed too many times, skipping`);
          result.errors.push({
            id: invoice.id,
            error: 'Trop de tentatives échouées'
          });
          result.failed++;
          continue;
        }

        // Use retry utility for better reliability
        await withRetry(
          () => certifyInvoice(invoice),
          2, // Max 2 retries per invoice
          1000 // 1 second delay
        );

        // Success - remove from pending
        await removePending(invoice.id);
        result.synced++;
        console.log(`[OfflineStorage] Invoice ${invoice.id} synced successfully`);

      } catch (error) {
        console.error(`[OfflineStorage] Failed to sync invoice ${invoice.id}:`, error);
        
        const errorMessage = (error as Error).message || 'Erreur de synchronisation';

        // Update attempt count
        await updatePendingAttempt(invoice.id, errorMessage);
        
        // Keep in pending list
        remaining.push(invoice);
        result.failed++;
        result.errors.push({
          id: invoice.id,
          error: errorMessage
        });
      }
    }

    // Update sync statistics
    await updateSyncStats(result.synced, result.failed, Date.now() - startTime);
    await AsyncStorage.setItem(KEYS.LAST_SYNC, Date.now().toString());

    console.log(`[OfflineStorage] Sync completed: ${result.synced} synced, ${result.failed} failed`);
    return result;

  } catch (error) {
    console.error('[OfflineStorage] Sync error:', error);
    throw new Error('Erreur lors de la synchronisation');
  }
}

export async function getSyncStats(): Promise<SyncStats> {
  try {
    const raw = await AsyncStorage.getItem(KEYS.SYNC_STATS);
    const lastSyncRaw = await AsyncStorage.getItem(KEYS.LAST_SYNC);
    
    const stats: SyncStats = raw ? JSON.parse(raw) : {
      totalSynced: 0,
      totalFailed: 0,
      averageSyncTime: 0,
    };
    
    if (lastSyncRaw) {
      stats.lastSyncTime = parseInt(lastSyncRaw);
    }
    
    return stats;
  } catch (error) {
    console.error('[OfflineStorage] Failed to get sync stats:', error);
    return {
      totalSynced: 0,
      totalFailed: 0,
      averageSyncTime: 0,
    };
  }
}

async function updateSyncStats(synced: number, failed: number, syncTime: number): Promise<void> {
  try {
    const stats = await getSyncStats();
    stats.totalSynced += synced;
    stats.totalFailed += failed;
    
    // Update average sync time (simple moving average)
    const totalSyncs = stats.totalSynced + stats.totalFailed;
    if (totalSyncs > 0) {
      stats.averageSyncTime = (stats.averageSyncTime * (totalSyncs - 1) + syncTime) / totalSyncs;
    }
    
    await AsyncStorage.setItem(KEYS.SYNC_STATS, JSON.stringify(stats));
  } catch (error) {
    console.error('[OfflineStorage] Failed to update sync stats:', error);
  }
}

export async function getPendingCount(): Promise<number> {
  const pending = await getPending();
  return pending.length;
}

export async function getFailedCount(): Promise<number> {
  const pending = await getPending();
  return pending.filter(item => item.attempts >= 3).length;
}

export async function retryFailed(): Promise<SyncResult> {
  try {
    const pending = await getPending();
    const failed = pending.filter(item => item.attempts >= 3);
    
    // Reset attempt counts for failed items
    for (const item of failed) {
      item.attempts = 0;
      item.error = undefined;
    }
    
    await AsyncStorage.setItem(KEYS.PENDING_INVOICES, JSON.stringify(pending));
    
    // Now sync them again
    return await syncPending();
  } catch (error) {
    console.error('[OfflineStorage] Failed to retry failed invoices:', error);
    throw new Error('Impossible de réessayer les factures échouées');
  }
}

function generateId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substr(2);
}

// Auto-sync when network becomes available
export function setupAutoSync(): () => void {
  console.log('[OfflineStorage] Setting up auto-sync');
  
  // Start auto-sync
  SyncService.startAutoSync();

  const unsubscribe = NetInfo.addEventListener(state => {
    if (state.isConnected) {
      console.log('[OfflineStorage] Network available, attempting auto-sync');
      syncPending().catch(error => {
        console.error('[OfflineStorage] Auto-sync failed:', error);
        MonitoringService.recordError('auto_sync', error as Error);
      });
    }
  });

  return unsubscribe;
}

// Background sync (call this periodically)
export async function backgroundSync(): Promise<void> {
  try {
    const pendingCount = await getPendingCount();
    if (pendingCount > 0) {
      console.log(`[OfflineStorage] Background sync: ${pendingCount} invoices pending`);
      const result = await syncPending();
      
      MonitoringService.recordSyncOperation(
        'background_sync',
        result.synced,
        true,
        undefined
      );
    }
  } catch (error) {
    console.error('[OfflineStorage] Background sync failed:', error);
    MonitoringService.recordError('background_sync', error as Error);
  }
}
