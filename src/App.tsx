import React, { useState, useEffect } from 'react';
import { SafeAreaView, StatusBar, View, ActivityIndicator } from 'react-native';
import InvoiceScreen from './screens/InvoiceScreen';
import LoginScreen from './screens/LoginScreen';
import StatsScreen from './screens/StatsScreen';
import ConfigScreen from './screens/ConfigScreen';
import TestScreen from './screens/TestScreen';
import AuthService from './services/AuthService';
import Config from './config/Config';
import MonitoringService from './services/MonitoringService';

type Screen = 'login' | 'invoice' | 'stats' | 'config' | 'tests';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('login');
  const [isLoading, setIsLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    initializeApp();
  }, []);

  const initializeApp = async () => {
    try {
      // Initialize configuration
      console.log('App initialized with config:', Config.getAllConfig());
      
      // Check authentication status
      const authenticated = await AuthService.validateSession();
      setIsAuthenticated(authenticated);
      
      if (authenticated) {
        setCurrentScreen('invoice');
      }
      
      // Initialize monitoring
      MonitoringService.recordAuditLog(
        'app_start',
        'application',
        'success',
        { timestamp: Date.now(), authenticated }
      );
      
    } catch (error) {
      console.error('Failed to initialize app:', error);
      MonitoringService.recordError('app_initialization', error as Error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLoginSuccess = () => {
    setIsAuthenticated(true);
    setCurrentScreen('invoice');
    MonitoringService.recordAuditLog(
      'user_login',
      'authentication',
      'success',
      { timestamp: Date.now() }
    );
  };

  const handleLogout = async () => {
    try {
      await AuthService.logout();
      setIsAuthenticated(false);
      setCurrentScreen('login');
      MonitoringService.recordAuditLog(
        'user_logout',
        'authentication',
        'success',
        { timestamp: Date.now() }
      );
    } catch (error) {
      console.error('Logout failed:', error);
      MonitoringService.recordError('logout', error as Error);
    }
  };

  const navigateToStats = () => {
    setCurrentScreen('stats');
  };

  const navigateToConfig = () => {
    setCurrentScreen('config');
  };

  const navigateToTests = () => {
    setCurrentScreen('tests');
  };

  const navigateBack = () => {
    setCurrentScreen('invoice');
  };

  if (isLoading) {
    return (
      <SafeAreaView style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <StatusBar />
        <ActivityIndicator size="large" color="#1976D2" />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <StatusBar barStyle="dark-content" backgroundColor="#fff" />
      <View style={{ flex: 1 }}>
        {currentScreen === 'login' && (
          <LoginScreen onLoginSuccess={handleLoginSuccess} />
        )}
        
        {currentScreen === 'invoice' && isAuthenticated && (
          <InvoiceScreen 
            onLogout={handleLogout}
            onNavigateToStats={navigateToStats}
            onNavigateToConfig={navigateToConfig}
          />
        )}
        
        {currentScreen === 'stats' && isAuthenticated && (
          <StatsScreen onBack={navigateBack} />
        )}
        
        {currentScreen === 'config' && isAuthenticated && (
          <ConfigScreen onBack={navigateBack} onNavigateToTests={navigateToTests} />
        )}
        
        {currentScreen === 'tests' && isAuthenticated && (
          <TestScreen onBack={navigateBack} />
        )}
      </View>
    </SafeAreaView>
  );
}
