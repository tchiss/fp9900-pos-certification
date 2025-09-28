import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  RefreshControl,
  TouchableOpacity,
  Alert,
  Share,
} from 'react-native';
import MaterialIcons from 'react-native-vector-icons/MaterialIcons';
import MonitoringService, { PerformanceMetrics } from '../services/MonitoringService';
import { GlobalStyles } from '../styles';

interface StatsScreenProps {
  onBack: () => void;
}

const StatsScreen: React.FC<StatsScreenProps> = ({ onBack }) => {
  const [stats, setStats] = useState<PerformanceMetrics | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const performanceMetrics = MonitoringService.getPerformanceMetrics();
      setStats(performanceMetrics);
      setLastUpdated(new Date());
    } catch (error) {
      console.error('Failed to load stats:', error);
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await loadStats();
    setRefreshing(false);
  };

  const formatPercentage = (value: number): string => {
    return `${value.toFixed(1)}%`;
  };

  const formatLatency = (value: number): string => {
    return `${value.toFixed(0)}ms`;
  };

  const formatCount = (value: number): string => {
    return value.toLocaleString();
  };

  const handleExportData = async () => {
    try {
      const data = await MonitoringService.exportData();
      const exportText = `Performance Report
Generated: ${new Date().toLocaleString()}

API Performance:
- Average Latency: ${formatLatency(stats?.apiLatency ? stats.apiLatency.reduce((a, b) => a + b, 0) / stats.apiLatency.length : 0)}
- Total API Calls: ${formatCount(stats?.apiLatency.length || 0)}

Success Rates:
- Certification Rate: ${formatPercentage(stats?.certificationRate || 0)}
- Print Success Rate: ${formatPercentage(stats?.printSuccessRate || 0)}
- Sync Success Rate: ${formatPercentage(stats?.syncSuccessRate || 0)}

Errors:
- Total Errors: ${formatCount(stats?.errorCount || 0)}

Last Updated: ${stats?.lastUpdated ? new Date(stats.lastUpdated).toLocaleString() : 'Never'}`;

      await Share.share({
        message: exportText,
        title: 'FP9900 Performance Report',
      });
    } catch (error) {
      console.error('Failed to export data:', error);
      Alert.alert('Error', 'Failed to export performance data');
    }
  };

  const handleClearData = () => {
    Alert.alert(
      'Clear All Data',
      'Are you sure you want to clear all monitoring data? This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Clear',
          style: 'destructive',
          onPress: async () => {
            try {
              await MonitoringService.clearAllData();
              await loadStats();
              Alert.alert('Success', 'All monitoring data has been cleared');
            } catch (error) {
              Alert.alert('Error', 'Failed to clear monitoring data');
            }
          },
        },
      ]
    );
  };

  if (!stats) {
    return (
      <View style={[GlobalStyles.container, styles.centered]}>
        <Text style={GlobalStyles.text}>Loading statistics...</Text>
      </View>
    );
  }

  const StatCard: React.FC<{
    title: string;
    value: string;
    icon: string;
    color: string;
  }> = ({ title, value, icon, color }) => (
    <View style={[styles.statCard, { borderLeftColor: color }]}>
      <View style={styles.statHeader}>
        <MaterialIcons name={icon} size={24} color={color} />
        <Text style={styles.statTitle}>{title}</Text>
      </View>
      <Text style={[styles.statValue, { color }]}>{value}</Text>
    </View>
  );

  return (
    <View style={GlobalStyles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={onBack} style={styles.backButton}>
          <MaterialIcons name="arrow-back" size={24} color="#fff" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Performance Statistics</Text>
        <View style={styles.headerActions}>
          <TouchableOpacity onPress={handleExportData} style={styles.actionButton}>
            <MaterialIcons name="share" size={20} color="#fff" />
          </TouchableOpacity>
          <TouchableOpacity onPress={handleClearData} style={styles.actionButton}>
            <MaterialIcons name="delete" size={20} color="#fff" />
          </TouchableOpacity>
        </View>
      </View>

      <ScrollView
        style={styles.content}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
      >
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>API Performance</Text>
          <View style={styles.statsGrid}>
            <StatCard
              title="Average Latency"
              value={formatLatency(
                stats.apiLatency.reduce((a, b) => a + b, 0) / (stats.apiLatency.length || 1)
              )}
              icon="speed"
              color="#4CAF50"
            />
            <StatCard
              title="Total API Calls"
              value={formatCount(stats.apiLatency.length)}
              icon="api"
              color="#2196F3"
            />
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Success Rates</Text>
          <View style={styles.statsGrid}>
            <StatCard
              title="Certification"
              value={formatPercentage(stats.certificationRate)}
              icon="verified"
              color="#4CAF50"
            />
            <StatCard
              title="Print Success"
              value={formatPercentage(stats.printSuccessRate)}
              icon="print"
              color="#FF9800"
            />
            <StatCard
              title="Sync Success"
              value={formatPercentage(stats.syncSuccessRate)}
              icon="sync"
              color="#9C27B0"
            />
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>System Health</Text>
          <View style={styles.statsGrid}>
            <StatCard
              title="Total Errors"
              value={formatCount(stats.errorCount)}
              icon="error"
              color="#F44336"
            />
            <StatCard
              title="Last Updated"
              value={stats.lastUpdated ? new Date(stats.lastUpdated).toLocaleTimeString() : 'Never'}
              icon="update"
              color="#607D8B"
            />
          </View>
        </View>

        {lastUpdated && (
          <View style={styles.footer}>
            <Text style={styles.footerText}>
              Last refreshed: {lastUpdated.toLocaleString()}
            </Text>
          </View>
        )}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  centered: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#1976D2',
    elevation: 4,
  },
  backButton: {
    padding: 8,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#fff',
    flex: 1,
    textAlign: 'center',
    marginHorizontal: 16,
  },
  headerActions: {
    flexDirection: 'row',
  },
  actionButton: {
    padding: 8,
    marginLeft: 8,
  },
  content: {
    flex: 1,
    padding: 16,
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  statCard: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 16,
    marginBottom: 12,
    width: '48%',
    borderLeftWidth: 4,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  statHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  statTitle: {
    fontSize: 14,
    color: '#666',
    marginLeft: 8,
    flex: 1,
  },
  statValue: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  footer: {
    alignItems: 'center',
    paddingVertical: 16,
  },
  footerText: {
    fontSize: 12,
    color: '#999',
  },
});

export default StatsScreen;
