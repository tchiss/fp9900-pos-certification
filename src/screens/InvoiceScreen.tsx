import React, { useState, useEffect } from 'react';
import { View, Text, Alert, ScrollView, StatusBar, ActivityIndicator, TouchableOpacity } from 'react-native';
import MaterialIcons from 'react-native-vector-icons/MaterialIcons';
import InvoiceForm from '../components/InvoiceForm';
import InvoicePreview from '../components/InvoicePreview';
import { certifyInvoice } from '../services/api';
import { enqueuePending, syncPending, getPending, setupAutoSync } from '../storage/offline';
import { GlobalStyles, Colors, Spacing } from '../styles';
import NetInfo from '@react-native-community/netinfo';
import MonitoringService from '../services/MonitoringService';
import PrinterService from '../services/PrinterService';
import Config from '../config/Config';

interface InvoiceScreenProps {
  onLogout: () => void;
  onNavigateToStats: () => void;
  onNavigateToConfig: () => void;
}

export default function InvoiceScreen({ onLogout, onNavigateToStats, onNavigateToConfig }: InvoiceScreenProps) {
  const [response, setResponse] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isOnline, setIsOnline] = useState(true);
  const [pendingCount, setPendingCount] = useState(0);

  useEffect(() => {
    // Check network status
    const unsubscribe = NetInfo.addEventListener(state => {
      setIsOnline(state.isConnected ?? false);
    });

    // Initialize auto-sync
    setupAutoSync();

    // Try to sync any pending invoices when the screen mounts
    syncPending().then(() => updatePendingCount());

    // Record app usage
    MonitoringService.recordAuditLog(
      'screen_view',
      'invoice_screen',
      'success',
      { timestamp: Date.now() }
    );

    return () => unsubscribe();
  }, []);

  const updatePendingCount = async () => {
    const pending = await getPending();
    setPendingCount(pending.length);
  };

  const onSubmit = async (payload: any) => {
    setIsLoading(true);
    const startTime = Date.now();
    
    try {
      // Record invoice certification attempt
      MonitoringService.recordAuditLog(
        'invoice_certification',
        'certification',
        'success',
        { payload: { issuerIFU: payload.issuerIFU, itemsCount: payload.items.length } }
      );

      const res = await certifyInvoice(payload);
      setResponse(res);
      
      // Record successful certification
      MonitoringService.recordAuditLog(
        'invoice_certified',
        'certification',
        'success',
        { 
          status: res.status,
          mecefCode: res.mecefCode,
          duration: Date.now() - startTime
        }
      );
      
      if (res.status === 'PENDING') {
        await enqueuePending(payload);
        Alert.alert(
          'Offline', 
          'Invoice saved and will be synchronized when connection is restored.',
          [{ text: 'OK', onPress: () => updatePendingCount() }]
        );
      } else {
        Alert.alert('Success', 'Invoice certified successfully!');
      }
    } catch (e: any) {
      await enqueuePending(payload);
      Alert.alert(
        'Erreur réseau', 
        'Facture mise en file pour synchronisation.',
        [{ text: 'OK', onPress: () => updatePendingCount() }]
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleBack = () => {
    setResponse(null);
    updatePendingCount();
  };

  const handleSyncNow = async () => {
    setIsLoading(true);
    try {
      const result = await syncPending();
      if (result.synced > 0) {
        Alert.alert('Succès', `${result.synced} facture(s) synchronisée(s) avec succès`);
        updatePendingCount();
      } else {
        Alert.alert('Info', 'Aucune facture en attente ou problème de connectivité');
      }
    } catch (error) {
      Alert.alert('Erreur', 'Impossible de synchroniser les factures en attente');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={GlobalStyles.container}>
      <StatusBar barStyle="dark-content" backgroundColor={Colors.background} />
      
      {/* Header */}
      <View style={[GlobalStyles.card, { marginBottom: 0 }]}>
        <View style={[GlobalStyles.row, GlobalStyles.spaceBetween, { alignItems: 'center' }]}>
          <View style={{ flex: 1 }}>
            <Text style={[GlobalStyles.text, { fontWeight: 'bold', fontSize: 18 }]}>
              FP9900 POS - DGI Certification
            </Text>
            <View style={[GlobalStyles.row, { marginTop: Spacing.xs }]}>
              <View style={[
                GlobalStyles.statusIndicator, 
                isOnline ? GlobalStyles.statusOnline : GlobalStyles.statusOffline
              ]} />
              <Text style={[GlobalStyles.caption, { color: isOnline ? Colors.success : Colors.error }]}>
                {isOnline ? 'Online' : 'Offline'}
              </Text>
            </View>
          </View>
          
          {/* Action buttons */}
          <View style={[GlobalStyles.row, { alignItems: 'center' }]}>
            <TouchableOpacity 
              style={[GlobalStyles.button, { marginRight: Spacing.sm, paddingHorizontal: 12 }]}
              onPress={onNavigateToStats}
            >
              <MaterialIcons name="analytics" size={16} color="#fff" />
            </TouchableOpacity>
            <TouchableOpacity 
              style={[GlobalStyles.button, { marginRight: Spacing.sm, paddingHorizontal: 12 }]}
              onPress={onNavigateToConfig}
            >
              <MaterialIcons name="settings" size={16} color="#fff" />
            </TouchableOpacity>
            <TouchableOpacity 
              style={[GlobalStyles.button, { backgroundColor: Colors.error, paddingHorizontal: 12 }]}
              onPress={onLogout}
            >
              <MaterialIcons name="logout" size={16} color="#fff" />
            </TouchableOpacity>
          </View>
        </View>
        
        {/* Pending count */}
        {pendingCount > 0 && (
          <TouchableOpacity 
            style={[GlobalStyles.button, GlobalStyles.buttonWarning, { paddingHorizontal: Spacing.md, marginTop: Spacing.sm }]}
            onPress={handleSyncNow}
            disabled={isLoading || !isOnline}
          >
            <MaterialIcons name="sync" size={16} color={Colors.white} style={{ marginRight: Spacing.xs }} />
            <Text style={[GlobalStyles.buttonText, { fontSize: 12 }]}>
              {pendingCount} en attente
            </Text>
          </TouchableOpacity>
        )}
      </View>

      {/* Main Content */}
      <View style={{ flex: 1 }}>
        {isLoading && (
          <View style={[GlobalStyles.card, { alignItems: 'center', padding: Spacing.lg }]}>
            <ActivityIndicator size="large" color={Colors.primary} />
            <Text style={[GlobalStyles.text, { marginTop: Spacing.md }]}>
              Traitement en cours...
            </Text>
          </View>
        )}
        
        {!response && !isLoading && (
          <InvoiceForm onSubmit={onSubmit} />
        )}
        
        {response && !isLoading && (
          <InvoicePreview response={response} onBack={handleBack} />
        )}
      </View>

      {/* Offline Banner */}
      {!isOnline && (
        <View style={[GlobalStyles.card, { backgroundColor: Colors.warning + '20', margin: Spacing.md }]}>
          <View style={[GlobalStyles.row, { alignItems: 'center' }]}>
            <MaterialIcons name="wifi-off" size={20} color={Colors.warning} style={{ marginRight: Spacing.sm }} />
            <Text style={[GlobalStyles.text, { color: Colors.warning, flex: 1 }]}>
              Mode hors ligne - Les factures seront synchronisées dès la reconnexion
            </Text>
          </View>
        </View>
      )}
    </View>
  );
}
