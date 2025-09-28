import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Share,
} from 'react-native';
import MaterialIcons from 'react-native-vector-icons/MaterialIcons';
import TestService, { TestSuite, TestResult } from '../services/TestService';
import { GlobalStyles } from '../styles';

interface TestScreenProps {
  onBack: () => void;
}

const TestScreen: React.FC<TestScreenProps> = ({ onBack }) => {
  const [isRunning, setIsRunning] = useState(false);
  const [testSuite, setTestSuite] = useState<TestSuite | null>(null);
  const [quickTests, setQuickTests] = useState<TestResult[]>([]);

  const runAllTests = async () => {
    setIsRunning(true);
    try {
      const suite = await TestService.runAllTests();
      setTestSuite(suite);
    } catch (error) {
      Alert.alert('Error', 'Failed to run tests');
    } finally {
      setIsRunning(false);
    }
  };

  const runQuickTests = async () => {
    setIsRunning(true);
    try {
      const tests = await TestService.runQuickTests();
      setQuickTests(tests);
    } catch (error) {
      Alert.alert('Error', 'Failed to run quick tests');
    } finally {
      setIsRunning(false);
    }
  };

  const exportTestReport = async () => {
    if (!testSuite) {
      Alert.alert('Error', 'No test results to export');
      return;
    }

    try {
      const report = await TestService.generateTestReport(testSuite);
      await Share.share({
        message: report,
        title: 'FP9900 POS Test Report',
      });
    } catch (error) {
      Alert.alert('Error', 'Failed to export test report');
    }
  };

  const TestResultItem: React.FC<{ result: TestResult }> = ({ result }) => {
    const getStatusIcon = (status: string) => {
      switch (status) {
        case 'pass':
          return <MaterialIcons name="check-circle" size={20} color="#4CAF50" />;
        case 'fail':
          return <MaterialIcons name="error" size={20} color="#F44336" />;
        case 'skip':
          return <MaterialIcons name="skip-next" size={20} color="#FF9800" />;
        default:
          return <MaterialIcons name="help" size={20} color="#666" />;
      }
    };

    const getStatusColor = (status: string) => {
      switch (status) {
        case 'pass':
          return '#4CAF50';
        case 'fail':
          return '#F44336';
        case 'skip':
          return '#FF9800';
        default:
          return '#666';
      }
    };

    return (
      <View style={styles.testResultItem}>
        <View style={styles.testResultHeader}>
          {getStatusIcon(result.status)}
          <Text style={[styles.testResultName, { color: getStatusColor(result.status) }]}>
            {result.name}
          </Text>
          <Text style={styles.testResultDuration}>{result.duration}ms</Text>
        </View>
        
        <Text style={styles.testResultMessage}>{result.message}</Text>
        
        {result.details && (
          <View style={styles.testResultDetails}>
            <Text style={styles.testResultDetailsText}>
              {JSON.stringify(result.details, null, 2)}
            </Text>
          </View>
        )}
      </View>
    );
  };

  const TestSummary: React.FC<{ suite: TestSuite }> = ({ suite }) => (
    <View style={styles.summaryCard}>
      <Text style={styles.summaryTitle}>Test Summary</Text>
      
      <View style={styles.summaryStats}>
        <View style={styles.summaryStat}>
          <Text style={[styles.summaryStatValue, { color: '#4CAF50' }]}>
            {suite.passed}
          </Text>
          <Text style={styles.summaryStatLabel}>Passed</Text>
        </View>
        
        <View style={styles.summaryStat}>
          <Text style={[styles.summaryStatValue, { color: '#F44336' }]}>
            {suite.failed}
          </Text>
          <Text style={styles.summaryStatLabel}>Failed</Text>
        </View>
        
        <View style={styles.summaryStat}>
          <Text style={[styles.summaryStatValue, { color: '#FF9800' }]}>
            {suite.skipped}
          </Text>
          <Text style={styles.summaryStatLabel}>Skipped</Text>
        </View>
        
        <View style={styles.summaryStat}>
          <Text style={[styles.summaryStatValue, { color: '#2196F3' }]}>
            {suite.results.length}
          </Text>
          <Text style={styles.summaryStatLabel}>Total</Text>
        </View>
      </View>
      
      <Text style={styles.summaryDuration}>
        Total Duration: {suite.totalDuration}ms
      </Text>
    </View>
  );

  return (
    <View style={GlobalStyles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={onBack} style={styles.backButton}>
          <MaterialIcons name="arrow-back" size={24} color="#fff" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Application Tests</Text>
        <View style={styles.headerActions}>
          {testSuite && (
            <TouchableOpacity onPress={exportTestReport} style={styles.actionButton}>
              <MaterialIcons name="share" size={20} color="#fff" />
            </TouchableOpacity>
          )}
        </View>
      </View>

      <ScrollView style={styles.content}>
        {/* Test Controls */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Test Controls</Text>
          
          <TouchableOpacity
            style={[styles.testButton, styles.testButtonPrimary]}
            onPress={runQuickTests}
            disabled={isRunning}
          >
            {isRunning ? (
              <ActivityIndicator color="#fff" size="small" />
            ) : (
              <MaterialIcons name="flash-on" size={20} color="#fff" />
            )}
            <Text style={styles.testButtonText}>Run Quick Tests</Text>
          </TouchableOpacity>
          
          <TouchableOpacity
            style={[styles.testButton, styles.testButtonSecondary]}
            onPress={runAllTests}
            disabled={isRunning}
          >
            {isRunning ? (
              <ActivityIndicator color="#fff" size="small" />
            ) : (
              <MaterialIcons name="play-arrow" size={20} color="#fff" />
            )}
            <Text style={styles.testButtonText}>Run All Tests</Text>
          </TouchableOpacity>
        </View>

        {/* Quick Tests Results */}
        {quickTests.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Quick Test Results</Text>
            {quickTests.map((result, index) => (
              <TestResultItem key={index} result={result} />
            ))}
          </View>
        )}

        {/* Full Test Suite Results */}
        {testSuite && (
          <View style={styles.section}>
            <TestSummary suite={testSuite} />
            
            <Text style={styles.sectionTitle}>Detailed Results</Text>
            {testSuite.results.map((result, index) => (
              <TestResultItem key={index} result={result} />
            ))}
          </View>
        )}

        {/* Test Information */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>About Tests</Text>
          <View style={styles.infoCard}>
            <Text style={styles.infoText}>
              • <Text style={styles.infoBold}>Quick Tests:</Text> Basic functionality checks
            </Text>
            <Text style={styles.infoText}>
              • <Text style={styles.infoBold}>All Tests:</Text> Comprehensive application validation
            </Text>
            <Text style={styles.infoText}>
              • <Text style={styles.infoBold}>Config Tests:</Text> Configuration validation
            </Text>
            <Text style={styles.infoText}>
              • <Text style={styles.infoBold}>Auth Tests:</Text> Authentication system checks
            </Text>
            <Text style={styles.infoText}>
              • <Text style={styles.infoBold}>Security Tests:</Text> Security features validation
            </Text>
            <Text style={styles.infoText}>
              • <Text style={styles.infoBold}>Printer Tests:</Text> Printer service validation
            </Text>
            <Text style={styles.infoText}>
              • <Text style={styles.infoBold}>Storage Tests:</Text> Offline storage checks
            </Text>
          </View>
        </View>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
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
  testButton: {
    borderRadius: 8,
    paddingVertical: 12,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  testButtonPrimary: {
    backgroundColor: '#4CAF50',
  },
  testButtonSecondary: {
    backgroundColor: '#2196F3',
  },
  testButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
  },
  summaryCard: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 16,
    marginBottom: 16,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  summaryTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 16,
    textAlign: 'center',
  },
  summaryStats: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 16,
  },
  summaryStat: {
    alignItems: 'center',
  },
  summaryStatValue: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  summaryStatLabel: {
    fontSize: 12,
    color: '#666',
    marginTop: 4,
  },
  summaryDuration: {
    fontSize: 14,
    color: '#666',
    textAlign: 'center',
  },
  testResultItem: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 16,
    marginBottom: 12,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  testResultHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  testResultName: {
    fontSize: 16,
    fontWeight: '600',
    flex: 1,
    marginLeft: 8,
  },
  testResultDuration: {
    fontSize: 12,
    color: '#666',
  },
  testResultMessage: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
  },
  testResultDetails: {
    backgroundColor: '#f5f5f5',
    borderRadius: 4,
    padding: 8,
  },
  testResultDetailsText: {
    fontSize: 12,
    fontFamily: 'monospace',
    color: '#333',
  },
  infoCard: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 16,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  infoText: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
    lineHeight: 20,
  },
  infoBold: {
    fontWeight: 'bold',
    color: '#333',
  },
});

export default TestScreen;
