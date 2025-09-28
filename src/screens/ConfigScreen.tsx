import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  Switch,
  TextInput,
} from 'react-native';
import MaterialIcons from 'react-native-vector-icons/MaterialIcons';
import Config from '../config/Config';
import AuthService from '../services/AuthService';
import MonitoringService from '../services/MonitoringService';
import { GlobalStyles } from '../styles';

interface ConfigScreenProps {
  onBack: () => void;
  onNavigateToTests: () => void;
}

const ConfigScreen: React.FC<ConfigScreenProps> = ({ onBack, onNavigateToTests }) => {
  const [config, setConfig] = useState(Config.getAllConfig());
  const [isEditing, setIsEditing] = useState(false);
  const [editedConfig, setEditedConfig] = useState(config);

  useEffect(() => {
    setConfig(Config.getAllConfig());
  }, []);

  const handleToggleSwitch = (key: keyof typeof config, value: boolean) => {
    const newConfig = { ...editedConfig, [key]: value };
    setEditedConfig(newConfig);
  };

  const handleTextChange = (key: keyof typeof config, value: string) => {
    const newConfig = { ...editedConfig, [key]: value };
    setEditedConfig(newConfig);
  };

  const handleSave = () => {
    Alert.alert(
      'Save Configuration',
      'Are you sure you want to save these configuration changes?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Save',
          onPress: () => {
            try {
              Config.updateConfig(editedConfig);
              setConfig(editedConfig);
              setIsEditing(false);
              
              MonitoringService.recordAuditLog(
                'config_updated',
                'configuration',
                'success',
                { changes: editedConfig }
              );
              
              Alert.alert('Success', 'Configuration saved successfully');
            } catch (error) {
              Alert.alert('Error', 'Failed to save configuration');
            }
          },
        },
      ]
    );
  };

  const handleReset = () => {
    Alert.alert(
      'Reset Configuration',
      'Are you sure you want to reset all configuration to defaults?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Reset',
          style: 'destructive',
          onPress: () => {
            Config.resetToDefaults();
            const defaultConfig = Config.getAllConfig();
            setConfig(defaultConfig);
            setEditedConfig(defaultConfig);
            setIsEditing(false);
            
            MonitoringService.recordAuditLog(
              'config_reset',
              'configuration',
              'success',
              { timestamp: Date.now() }
            );
            
            Alert.alert('Success', 'Configuration reset to defaults');
          },
        },
      ]
    );
  };

  const handleLogout = async () => {
    Alert.alert(
      'Logout',
      'Are you sure you want to logout?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Logout',
          style: 'destructive',
          onPress: async () => {
            try {
              await AuthService.logout();
              Alert.alert('Success', 'Logged out successfully');
            } catch (error) {
              Alert.alert('Error', 'Failed to logout');
            }
          },
        },
      ]
    );
  };

  const ConfigItem: React.FC<{
    title: string;
    description: string;
    value: any;
    type: 'switch' | 'text' | 'number';
    onValueChange?: (value: any) => void;
    editable?: boolean;
  }> = ({ title, description, value, type, onValueChange, editable = true }) => (
    <View style={styles.configItem}>
      <View style={styles.configHeader}>
        <Text style={styles.configTitle}>{title}</Text>
        <Text style={styles.configDescription}>{description}</Text>
      </View>
      
      <View style={styles.configValue}>
        {type === 'switch' && (
          <Switch
            value={Boolean(value)}
            onValueChange={onValueChange}
            disabled={!editable}
            trackColor={{ false: '#767577', true: '#81b0ff' }}
            thumbColor={value ? '#f5dd4b' : '#f4f3f4'}
          />
        )}
        
        {type === 'text' && (
          <TextInput
            style={[styles.textInput, !editable && styles.textInputDisabled]}
            value={String(value)}
            onChangeText={onValueChange}
            editable={editable}
            placeholder="Enter value"
          />
        )}
        
        {type === 'number' && (
          <TextInput
            style={[styles.textInput, !editable && styles.textInputDisabled]}
            value={String(value)}
            onChangeText={onValueChange}
            editable={editable}
            keyboardType="numeric"
            placeholder="Enter number"
          />
        )}
      </View>
    </View>
  );

  return (
    <View style={GlobalStyles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={onBack} style={styles.backButton}>
          <MaterialIcons name="arrow-back" size={24} color="#fff" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Configuration</Text>
        <View style={styles.headerActions}>
          {isEditing ? (
            <>
              <TouchableOpacity onPress={() => setIsEditing(false)} style={styles.actionButton}>
                <MaterialIcons name="close" size={20} color="#fff" />
              </TouchableOpacity>
              <TouchableOpacity onPress={handleSave} style={styles.actionButton}>
                <MaterialIcons name="save" size={20} color="#fff" />
              </TouchableOpacity>
            </>
          ) : (
            <TouchableOpacity onPress={() => setIsEditing(true)} style={styles.actionButton}>
              <MaterialIcons name="edit" size={20} color="#fff" />
            </TouchableOpacity>
          )}
        </View>
      </View>

      <ScrollView style={styles.content}>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>API Configuration</Text>
          
          <ConfigItem
            title="API Base URL"
            description="Backend API endpoint"
            value={isEditing ? editedConfig.API_BASE_URL : config.API_BASE_URL}
            type="text"
            onValueChange={(value) => handleTextChange('API_BASE_URL', value)}
            editable={isEditing}
          />
          
          <ConfigItem
            title="API Timeout"
            description="Request timeout in milliseconds"
            value={isEditing ? editedConfig.API_TIMEOUT : config.API_TIMEOUT}
            type="number"
            onValueChange={(value) => handleTextChange('API_TIMEOUT', value)}
            editable={isEditing}
          />
          
          <ConfigItem
            title="Max Retry Attempts"
            description="Maximum number of retry attempts"
            value={isEditing ? editedConfig.MAX_RETRY_ATTEMPTS : config.MAX_RETRY_ATTEMPTS}
            type="number"
            onValueChange={(value) => handleTextChange('MAX_RETRY_ATTEMPTS', value)}
            editable={isEditing}
          />
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Sync Configuration</Text>
          
          <ConfigItem
            title="Sync Interval"
            description="Auto-sync interval in milliseconds"
            value={isEditing ? editedConfig.SYNC_INTERVAL : config.SYNC_INTERVAL}
            type="number"
            onValueChange={(value) => handleTextChange('SYNC_INTERVAL', value)}
            editable={isEditing}
          />
          
          <ConfigItem
            title="Cache Size"
            description="Maximum cache size"
            value={isEditing ? editedConfig.CACHE_SIZE : config.CACHE_SIZE}
            type="number"
            onValueChange={(value) => handleTextChange('CACHE_SIZE', value)}
            editable={isEditing}
          />
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Security & Logging</Text>
          
          <ConfigItem
            title="Enable SSL Pinning"
            description="Enable SSL certificate pinning"
            value={isEditing ? editedConfig.ENABLE_SSL_PINNING : config.ENABLE_SSL_PINNING}
            type="switch"
            onValueChange={(value) => handleToggleSwitch('ENABLE_SSL_PINNING', value)}
            editable={isEditing}
          />
          
          <ConfigItem
            title="Enable Audit Logging"
            description="Enable audit logging for compliance"
            value={isEditing ? editedConfig.ENABLE_AUDIT_LOGGING : config.ENABLE_AUDIT_LOGGING}
            type="switch"
            onValueChange={(value) => handleToggleSwitch('ENABLE_AUDIT_LOGGING', value)}
            editable={isEditing}
          />
          
          <ConfigItem
            title="Log Level"
            description="Application log level"
            value={isEditing ? editedConfig.LOG_LEVEL : config.LOG_LEVEL}
            type="text"
            onValueChange={(value) => handleTextChange('LOG_LEVEL', value)}
            editable={isEditing}
          />
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>System Actions</Text>
          
          <TouchableOpacity 
            style={[styles.actionButton, { backgroundColor: '#FF9800' }]} 
            onPress={onNavigateToTests}
          >
            <MaterialIcons name="bug-report" size={20} color="#fff" />
            <Text style={styles.actionButtonText}>Run Tests</Text>
          </TouchableOpacity>
          
          <TouchableOpacity style={styles.actionButton} onPress={handleReset}>
            <MaterialIcons name="refresh" size={20} color="#fff" />
            <Text style={styles.actionButtonText}>Reset to Defaults</Text>
          </TouchableOpacity>
          
          <TouchableOpacity 
            style={[styles.actionButton, { backgroundColor: '#F44336' }]} 
            onPress={handleLogout}
          >
            <MaterialIcons name="logout" size={20} color="#fff" />
            <Text style={styles.actionButtonText}>Logout</Text>
          </TouchableOpacity>
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
  configItem: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 16,
    marginBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  configHeader: {
    flex: 1,
  },
  configTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  configDescription: {
    fontSize: 14,
    color: '#666',
    marginTop: 4,
  },
  configValue: {
    marginLeft: 16,
  },
  textInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 4,
    padding: 8,
    minWidth: 120,
    fontSize: 14,
  },
  textInputDisabled: {
    backgroundColor: '#f5f5f5',
    color: '#999',
  },
  actionButton: {
    backgroundColor: '#1976D2',
    borderRadius: 8,
    paddingVertical: 12,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  actionButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
  },
});

export default ConfigScreen;
