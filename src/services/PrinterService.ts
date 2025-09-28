import { Platform, NativeModules } from 'react-native';
import MonitoringService from './MonitoringService';
import Config from '../config/Config';

export interface PrintJob {
  title?: string;
  lines: Array<{ 
    text: string; 
    align?: 'left' | 'center' | 'right'; 
    bold?: boolean;
    size?: 'small' | 'medium' | 'large';
  }>;
  qrData?: string;
  qrSize?: number;
  separator?: string;
}

export interface PrinterStatus {
  connected: boolean;
  paperAvailable: boolean;
  error?: string;
}

export interface IPrinter {
  init(): Promise<void>;
  print(job: PrintJob): Promise<void>;
  getStatus(): Promise<PrinterStatus>;
  disconnect(): Promise<void>;
}

export class PrinterError extends Error {
  constructor(
    message: string,
    public code?: string,
    public details?: any
  ) {
    super(message);
    this.name = 'PrinterError';
  }
}

class MockPrinter implements IPrinter {
  private isInitialized = false;
  private isConnected = false;

  async init(): Promise<void> {
    const startTime = Date.now();
    console.log('[MockPrinter] Initializing...');
    
    try {
      await new Promise(resolve => setTimeout(resolve, 1000));
      this.isInitialized = true;
      this.isConnected = true;
      
      const duration = Date.now() - startTime;
      console.log('[MockPrinter] Initialized successfully');
      
      MonitoringService.recordPrinterOperation(
        'printer_init',
        duration,
        true
      );
    } catch (error) {
      const duration = Date.now() - startTime;
      MonitoringService.recordPrinterOperation(
        'printer_init',
        duration,
        false,
        error instanceof Error ? error.message : 'Unknown error'
      );
      throw error;
    }
  }

  async print(job: PrintJob): Promise<void> {
    if (!this.isInitialized) {
      throw new PrinterError('Printer not initialized', 'NOT_INITIALIZED');
    }

    const startTime = Date.now();
    console.log('[MockPrinter] Printing job:', JSON.stringify(job, null, 2));
    
    try {
      // Simulate printing delay
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      // Simulate occasional errors
      if (Math.random() < 0.1) {
        const error = new PrinterError('Paper jam simulated', 'PAPER_JAM');
        const duration = Date.now() - startTime;
        
        MonitoringService.recordPrinterOperation(
          'printer_print',
          duration,
          false,
          error.message
        );
        
        throw error;
      }
      
      const duration = Date.now() - startTime;
      console.log('[MockPrinter] Print completed successfully');
      
      MonitoringService.recordPrinterOperation(
        'printer_print',
        duration,
        true
      );
    } catch (error) {
      const duration = Date.now() - startTime;
      
      MonitoringService.recordPrinterOperation(
        'printer_print',
        duration,
        false,
        error instanceof Error ? error.message : 'Unknown error'
      );
      
      throw error;
    }
  }

  async getStatus(): Promise<PrinterStatus> {
    return {
      connected: this.isConnected,
      paperAvailable: true,
    };
  }

  async disconnect(): Promise<void> {
    this.isConnected = false;
    this.isInitialized = false;
    console.log('[MockPrinter] Disconnected');
  }
}

class AndroidPrinterModule implements IPrinter {
  private native = NativeModules.FP9900Printer;
  private isInitialized = false;

  async init(): Promise<void> {
    try {
      if (!this.native) {
        throw new PrinterError('FP9900Printer native module not found', 'MODULE_NOT_FOUND');
      }
      
      console.log('[AndroidPrinter] Initializing...');
      await this.native.init();
      this.isInitialized = true;
      console.log('[AndroidPrinter] Initialized successfully');
    } catch (error) {
      console.error('[AndroidPrinter] Init error:', error);
      throw new PrinterError(
        'Failed to initialize printer',
        'INIT_ERROR',
        error
      );
    }
  }

  async print(job: PrintJob): Promise<void> {
    try {
      if (!this.isInitialized) {
        throw new PrinterError('Printer not initialized', 'NOT_INITIALIZED');
      }

      if (!this.native) {
        throw new PrinterError('FP9900Printer native module not found', 'MODULE_NOT_FOUND');
      }

      console.log('[AndroidPrinter] Printing job:', JSON.stringify(job, null, 2));
      
      // Format the job for the native module
      const formattedJob = {
        title: job.title || 'FACTURE',
        lines: job.lines.map(line => ({
          text: line.text,
          align: line.align || 'left',
          bold: line.bold || false,
          size: line.size || 'medium'
        })),
        qrData: job.qrData,
        qrSize: job.qrSize || 6,
        separator: job.separator || '─'
      };

      await this.native.print(JSON.stringify(formattedJob));
      console.log('[AndroidPrinter] Print completed successfully');
    } catch (error) {
      console.error('[AndroidPrinter] Print error:', error);
      
      if (error instanceof PrinterError) {
        throw error;
      }
      
      throw new PrinterError(
        'Failed to print document',
        'PRINT_ERROR',
        error
      );
    }
  }

  async getStatus(): Promise<PrinterStatus> {
    try {
      if (!this.native) {
        return {
          connected: false,
          paperAvailable: false,
          error: 'Native module not found'
        };
      }

      const status = await this.native.getStatus();
      return {
        connected: status?.connected ?? false,
        paperAvailable: status?.paperAvailable ?? false,
        error: status?.error
      };
    } catch (error) {
      console.error('[AndroidPrinter] Status error:', error);
      return {
        connected: false,
        paperAvailable: false,
        error: error instanceof Error ? error.message : 'Unknown error'
      };
    }
  }

  async disconnect(): Promise<void> {
    try {
      if (this.native) {
        await this.native.disconnect();
      }
      this.isInitialized = false;
      console.log('[AndroidPrinter] Disconnected');
    } catch (error) {
      console.error('[AndroidPrinter] Disconnect error:', error);
      throw new PrinterError(
        'Failed to disconnect printer',
        'DISCONNECT_ERROR',
        error
      );
    }
  }
}

// Create instance based on platform
let instance: IPrinter;
if (Platform.OS === 'android') {
  instance = new AndroidPrinterModule();
} else {
  instance = new MockPrinter();
}

export const PrinterService: IPrinter = instance;

// Utility functions for common print jobs
export const PrintUtils = {
  async printInvoice(invoiceData: {
    title: string;
    issuer: string;
    buyer: string;
    items: Array<{ label: string; qty: number; price: number }>;
    total: number;
    mecefCode?: string;
    qrData?: string;
  }): Promise<void> {
    const job: PrintJob = {
      title: invoiceData.title,
      lines: [
        { text: '═'.repeat(32), align: 'center' },
        { text: invoiceData.title, align: 'center', bold: true, size: 'large' },
        { text: '═'.repeat(32), align: 'center' },
        { text: '', align: 'left' },
        { text: `Émetteur: ${invoiceData.issuer}`, align: 'left' },
        { text: `Client: ${invoiceData.buyer}`, align: 'left' },
        { text: '', align: 'left' },
        { text: '─'.repeat(32), align: 'center' },
        { text: 'ARTICLES', align: 'center', bold: true },
        { text: '─'.repeat(32), align: 'center' },
      ],
      qrData: invoiceData.qrData,
      qrSize: 6
    };

    // Add items
    invoiceData.items.forEach(item => {
      job.lines.push(
        { text: item.label, align: 'left' },
        { text: `Qté: ${item.qty} × ${item.price.toFixed(2)} = ${(item.qty * item.price).toFixed(2)} FCFA`, align: 'right' }
      );
    });

    // Add total and certification info
    job.lines.push(
      { text: '─'.repeat(32), align: 'center' },
      { text: `TOTAL: ${invoiceData.total.toFixed(2)} FCFA`, align: 'center', bold: true, size: 'large' },
      { text: '', align: 'left' }
    );

    if (invoiceData.mecefCode) {
      job.lines.push(
        { text: 'CERTIFICATION DGI', align: 'center', bold: true },
        { text: `Code MECEF: ${invoiceData.mecefCode}`, align: 'center' },
        { text: '', align: 'left' }
      );
    }

    job.lines.push(
      { text: '═'.repeat(32), align: 'center' },
      { text: `Date: ${new Date().toLocaleDateString('fr-FR')}`, align: 'center' },
      { text: `Heure: ${new Date().toLocaleTimeString('fr-FR')}`, align: 'center' },
      { text: '', align: 'left' },
      { text: '', align: 'left' }
    );

    await PrinterService.print(job);
  },

  async printTestPage(): Promise<void> {
    const job: PrintJob = {
      title: 'TEST IMPRIMANTE',
      lines: [
        { text: '═'.repeat(32), align: 'center' },
        { text: 'TEST IMPRIMANTE FP9900', align: 'center', bold: true },
        { text: '═'.repeat(32), align: 'center' },
        { text: '', align: 'left' },
        { text: 'Ceci est un test d\'impression', align: 'left' },
        { text: 'pour vérifier le bon fonctionnement', align: 'left' },
        { text: 'de l\'imprimante thermique.', align: 'left' },
        { text: '', align: 'left' },
        { text: 'Date: ' + new Date().toLocaleString('fr-FR'), align: 'left' },
        { text: '─'.repeat(32), align: 'center' },
        { text: 'Test terminé avec succès !', align: 'center', bold: true },
        { text: '', align: 'left' },
        { text: '', align: 'left' }
      ]
    };

    await PrinterService.print(job);
  }
};

// Export default instance
export default PrinterService;
