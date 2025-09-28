import React, { useState } from 'react';
import { View, Text, TouchableOpacity, Alert, ActivityIndicator } from 'react-native';
import QRCode from 'react-native-qrcode-svg';
import { PrinterService } from '../services/PrinterService';
import { GlobalStyles, Colors, Spacing } from '../styles';
import Icon from 'react-native-vector-icons/MaterialIcons';

interface InvoicePreviewProps {
  response: any;
  onBack: () => void;
}

export default function InvoicePreview({ response, onBack }: InvoicePreviewProps) {
  const [isPrinting, setIsPrinting] = useState(false);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CERTIFIED':
        return Colors.success;
      case 'PENDING':
        return Colors.warning;
      case 'REJECTED':
        return Colors.error;
      default:
        return Colors.gray;
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'CERTIFIED':
        return 'check-circle';
      case 'PENDING':
        return 'schedule';
      case 'REJECTED':
        return 'error';
      default:
        return 'help';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'CERTIFIED':
        return 'Certifiée';
      case 'PENDING':
        return 'En attente de synchronisation';
      case 'REJECTED':
        return 'Rejetée';
      default:
        return 'Statut inconnu';
    }
  };

  const handlePrint = async () => {
    setIsPrinting(true);
    try {
      await PrinterService.init();
      await PrinterService.print({
        title: 'FACTURE CERTIFIÉE DGI',
        lines: [
          { text: `Statut: ${getStatusText(response.status)}`, align: 'center', bold: true },
          { text: `Code MECEF: ${response.mecefCode || 'N/A'}`, align: 'center' },
          { text: `Date: ${new Date().toLocaleDateString('fr-FR')}`, align: 'center' },
          { text: '─'.repeat(32), align: 'center' },
        ],
        qrData: response.qrData,
      });
      Alert.alert('Succès', 'Facture imprimée avec succès');
    } catch (error) {
      console.error('Erreur impression:', error);
      Alert.alert('Erreur', 'Impossible d\'imprimer la facture');
    } finally {
      setIsPrinting(false);
    }
  };

  return (
    <View style={{ flex: 1 }}>
      <View style={GlobalStyles.card}>
        <View style={[GlobalStyles.row, GlobalStyles.spaceBetween, { marginBottom: Spacing.lg }]}>
          <Text style={GlobalStyles.header}>Résultat de Certification</Text>
          <TouchableOpacity onPress={onBack}>
            <Icon name="arrow-back" size={24} color={Colors.textSecondary} />
          </TouchableOpacity>
        </View>

        {/* Status */}
        <View style={[GlobalStyles.card, { backgroundColor: Colors.lightGray, marginBottom: Spacing.lg }]}>
          <View style={[GlobalStyles.row, GlobalStyles.centered, { marginBottom: Spacing.sm }]}>
            <Icon 
              name={getStatusIcon(response.status)} 
              size={24} 
              color={getStatusColor(response.status)} 
              style={{ marginRight: Spacing.sm }}
            />
            <Text style={[GlobalStyles.text, { fontWeight: 'bold', color: getStatusColor(response.status) }]}>
              {getStatusText(response.status)}
            </Text>
          </View>
        </View>

        {/* Code MECEF */}
        {response.mecefCode && (
          <View style={[GlobalStyles.card, { marginBottom: Spacing.lg }]}>
            <Text style={[GlobalStyles.text, { marginBottom: Spacing.sm, fontWeight: 'bold' }]}>
              Code MECEF
            </Text>
            <Text style={[GlobalStyles.terminal, { 
              backgroundColor: Colors.black, 
              color: Colors.white, 
              padding: Spacing.md, 
              borderRadius: Spacing.sm,
              textAlign: 'center'
            }]}>
              {response.mecefCode}
            </Text>
          </View>
        )}

        {/* QR Code */}
        {response.qrData && (
          <View style={[GlobalStyles.card, { alignItems: 'center', marginBottom: Spacing.lg }]}>
            <Text style={[GlobalStyles.text, { marginBottom: Spacing.md, fontWeight: 'bold' }]}>
              Code QR de Certification
            </Text>
            <QRCode 
              value={response.qrData} 
              size={200}
              backgroundColor={Colors.white}
              color={Colors.black}
            />
            <Text style={[GlobalStyles.caption, { marginTop: Spacing.sm, textAlign: 'center' }]}>
              Scannez ce code pour vérifier la certification
            </Text>
          </View>
        )}

        {/* Warnings */}
        {response.warnings && response.warnings.length > 0 && (
          <View style={[GlobalStyles.card, { backgroundColor: Colors.warning + '20', marginBottom: Spacing.lg }]}>
            <Text style={[GlobalStyles.text, { marginBottom: Spacing.sm, fontWeight: 'bold' }]}>
              Avertissements
            </Text>
            {response.warnings.map((warning: string, idx: number) => (
              <Text key={idx} style={[GlobalStyles.text, { marginBottom: Spacing.xs }]}>
                • {warning}
              </Text>
            ))}
          </View>
        )}

        {/* Reasons for rejection */}
        {response.reasons && response.reasons.length > 0 && (
          <View style={[GlobalStyles.card, { backgroundColor: Colors.error + '20', marginBottom: Spacing.lg }]}>
            <Text style={[GlobalStyles.text, { marginBottom: Spacing.sm, fontWeight: 'bold' }]}>
              Raisons du rejet
            </Text>
            {response.reasons.map((reason: string, idx: number) => (
              <Text key={idx} style={[GlobalStyles.text, { marginBottom: Spacing.xs }]}>
                • {reason}
              </Text>
            ))}
          </View>
        )}

        {/* Actions */}
        <View style={[GlobalStyles.row, { gap: Spacing.sm }]}>
          {response.status === 'CERTIFIED' && (
            <TouchableOpacity
              style={[GlobalStyles.button, { flex: 1 }]}
              onPress={handlePrint}
              disabled={isPrinting}
            >
              {isPrinting ? (
                <ActivityIndicator color={Colors.white} size="small" />
              ) : (
                <>
                  <Icon name="print" size={20} color={Colors.white} style={{ marginRight: Spacing.sm }} />
                  <Text style={GlobalStyles.buttonText}>Imprimer</Text>
                </>
              )}
            </TouchableOpacity>
          )}
          
          <TouchableOpacity
            style={[GlobalStyles.button, GlobalStyles.buttonSecondary, { flex: 1 }]}
            onPress={onBack}
          >
            <Icon name="refresh" size={20} color={Colors.white} style={{ marginRight: Spacing.sm }} />
            <Text style={GlobalStyles.buttonText}>Nouvelle Facture</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}
