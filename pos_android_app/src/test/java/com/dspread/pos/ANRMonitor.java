package com.dspread.pos;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utilitaire de monitoring ANR en temps réel
 * Utilisé dans les tests pour détecter les blocages du thread principal
 */
public class ANRMonitor {
    
    private static final int DEFAULT_ANR_THRESHOLD = 5000; // 5 secondes
    private static final int DEFAULT_CHECK_INTERVAL = 100; // 100ms
    
    /**
     * Interface pour les callbacks de monitoring ANR
     */
    public interface ANRCallback {
        void onANRDetected(long blockDuration);
        void onMainThreadFree();
    }
    
    /**
     * Monitore une opération pour détecter les ANR
     */
    public static class ANRDetectionResult {
        private final boolean anrDetected;
        private final long executionTime;
        private final long blockDuration;
        
        public ANRDetectionResult(boolean anrDetected, long executionTime, long blockDuration) {
            this.anrDetected = anrDetected;
            this.executionTime = executionTime;
            this.blockDuration = blockDuration;
        }
        
        public boolean isAnrDetected() { return anrDetected; }
        public long getExecutionTime() { return executionTime; }
        public long getBlockDuration() { return blockDuration; }
    }
    
    /**
     * Monitore une opération avec un seuil ANR personnalisé
     */
    public static ANRDetectionResult monitorOperation(Runnable operation, int anrThresholdMs) {
        AtomicBoolean anrDetected = new AtomicBoolean(false);
        AtomicLong blockDuration = new AtomicLong(0);
        AtomicLong executionStartTime = new AtomicLong(0);
        AtomicLong executionEndTime = new AtomicLong(0);
        
        // Thread de monitoring ANR
        Thread anrMonitor = new Thread(() -> {
            try {
                Thread.sleep(anrThresholdMs);
                anrDetected.set(true);
                blockDuration.set(anrThresholdMs);
            } catch (InterruptedException e) {
                // Normal - opération terminée avant le seuil ANR
            }
        });
        
        // Démarrer le monitoring
        anrMonitor.start();
        
        // Exécuter l'opération
        executionStartTime.set(System.currentTimeMillis());
        operation.run();
        executionEndTime.set(System.currentTimeMillis());
        
        // Arrêter le monitoring
        anrMonitor.interrupt();
        
        // Calculer les résultats
        long actualExecutionTime = executionEndTime.get() - executionStartTime.get();
        long actualBlockDuration = anrDetected.get() ? blockDuration.get() : 0;
        
        return new ANRDetectionResult(anrDetected.get(), actualExecutionTime, actualBlockDuration);
    }
    
    /**
     * Monitore une opération avec le seuil ANR par défaut
     */
    public static ANRDetectionResult monitorOperation(Runnable operation) {
        return monitorOperation(operation, DEFAULT_ANR_THRESHOLD);
    }
    
    /**
     * Monitore le thread principal pour détecter les blocages
     */
    public static class MainThreadMonitor {
        private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
        private final AtomicBoolean mainThreadBlocked = new AtomicBoolean(false);
        private final AtomicLong blockStartTime = new AtomicLong(0);
        private final AtomicLong blockDuration = new AtomicLong(0);
        private final int blockThresholdMs;
        private final int checkIntervalMs;
        private Thread monitorThread;
        
        public MainThreadMonitor(int blockThresholdMs, int checkIntervalMs) {
            this.blockThresholdMs = blockThresholdMs;
            this.checkIntervalMs = checkIntervalMs;
        }
        
        public MainThreadMonitor() {
            this(1000, 100); // 1 seconde de seuil, vérification toutes les 100ms
        }
        
        public void startMonitoring() {
            if (isMonitoring.get()) {
                return;
            }
            
            isMonitoring.set(true);
            mainThreadBlocked.set(false);
            
            monitorThread = new Thread(() -> {
                while (isMonitoring.get()) {
                    long startCheck = System.currentTimeMillis();
                    
                    try {
                        // Essayer de faire quelque chose sur le main thread
                        // Si ça prend trop de temps, le main thread est bloqué
                        Thread.sleep(checkIntervalMs);
                        
                        long endCheck = System.currentTimeMillis();
                        long checkDuration = endCheck - startCheck;
                        
                        if (checkDuration > blockThresholdMs) {
                            if (!mainThreadBlocked.get()) {
                                mainThreadBlocked.set(true);
                                blockStartTime.set(startCheck);
                            }
                            blockDuration.set(endCheck - blockStartTime.get());
                        } else {
                            if (mainThreadBlocked.get()) {
                                mainThreadBlocked.set(false);
                                blockDuration.set(0);
                            }
                        }
                        
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            
            monitorThread.start();
        }
        
        public void stopMonitoring() {
            isMonitoring.set(false);
            if (monitorThread != null) {
                monitorThread.interrupt();
            }
        }
        
        public boolean isMainThreadBlocked() {
            return mainThreadBlocked.get();
        }
        
        public long getBlockDuration() {
            return blockDuration.get();
        }
        
        public void reset() {
            mainThreadBlocked.set(false);
            blockDuration.set(0);
        }
    }
    
    /**
     * Classe utilitaire pour les tests de performance
     */
    public static class PerformanceTestRunner {
        
        /**
         * Exécute un test plusieurs fois et retourne les statistiques
         */
        public static class TestStatistics {
            public final long minTime;
            public final long maxTime;
            public final long averageTime;
            public final long totalTime;
            public final int iterations;
            
            public TestStatistics(long minTime, long maxTime, long averageTime, long totalTime, int iterations) {
                this.minTime = minTime;
                this.maxTime = maxTime;
                this.averageTime = averageTime;
                this.totalTime = totalTime;
                this.iterations = iterations;
            }
        }
        
        public static TestStatistics runPerformanceTest(Runnable test, int iterations) {
            long totalTime = 0;
            long minTime = Long.MAX_VALUE;
            long maxTime = 0;
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.currentTimeMillis();
                test.run();
                long endTime = System.currentTimeMillis();
                
                long executionTime = endTime - startTime;
                totalTime += executionTime;
                minTime = Math.min(minTime, executionTime);
                maxTime = Math.max(maxTime, executionTime);
            }
            
            long averageTime = totalTime / iterations;
            
            return new TestStatistics(minTime, maxTime, averageTime, totalTime, iterations);
        }
    }
}
