import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, ScrollView, Alert } from 'react-native';
import { GlobalStyles, Colors, Spacing } from '../styles';
import Icon from 'react-native-vector-icons/MaterialIcons';

type Line = { label: string; qty: string; unitPrice: string };

interface InvoiceFormProps {
  onSubmit: (payload: any) => void;
}

export default function InvoiceForm({ onSubmit }: InvoiceFormProps) {
  const [issuerIFU, setIssuerIFU] = useState('');
  const [buyerName, setBuyerName] = useState('');
  const [buyerIFU, setBuyerIFU] = useState('');
  const [lines, setLines] = useState<Line[]>([{ label: '', qty: '1', unitPrice: '0' }]);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const addLine = () => {
    setLines([...lines, { label: '', qty: '1', unitPrice: '0' }]);
  };

  const removeLine = (idx: number) => {
    if (lines.length > 1) {
      const newLines = lines.filter((_, i) => i !== idx);
      setLines(newLines);
    }
  };

  const updateLine = (idx: number, patch: Partial<Line>) => {
    const copy = [...lines];
    copy[idx] = { ...copy[idx], ...patch };
    setLines(copy);
  };

  const computeTotal = () => {
    return lines.reduce((sum, l) => {
      const qty = parseFloat(l.qty || '0');
      const price = parseFloat(l.unitPrice || '0');
      return sum + (qty * price);
    }, 0);
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!issuerIFU.trim()) {
      newErrors.issuerIFU = 'IFU émetteur requis';
    }

    if (!buyerName.trim()) {
      newErrors.buyerName = 'Nom du client requis';
    }

    lines.forEach((line, idx) => {
      if (!line.label.trim()) {
        newErrors[`line_${idx}_label`] = 'Libellé requis';
      }
      if (!line.qty || parseFloat(line.qty) <= 0) {
        newErrors[`line_${idx}_qty`] = 'Quantité invalide';
      }
      if (!line.unitPrice || parseFloat(line.unitPrice) < 0) {
        newErrors[`line_${idx}_price`] = 'Prix invalide';
      }
    });

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = () => {
    if (!validateForm()) {
      Alert.alert('Erreur', 'Veuillez corriger les erreurs avant de continuer');
      return;
    }

    const payload = {
      issuerIFU: issuerIFU.trim(),
      buyerName: buyerName.trim(),
      buyerIFU: buyerIFU.trim() || undefined,
      items: lines.map(l => ({
        label: l.label.trim(),
        qty: parseFloat(l.qty || '0'),
        unitPrice: parseFloat(l.unitPrice || '0')
      })),
      total: computeTotal()
    };

    onSubmit(payload);
  };

  return (
    <ScrollView style={{ flex: 1 }} showsVerticalScrollIndicator={false}>
      <View style={GlobalStyles.card}>
        <Text style={GlobalStyles.header}>Nouvelle Facture</Text>
        
        {/* IFU Émetteur */}
        <View style={{ marginBottom: Spacing.md }}>
          <Text style={[GlobalStyles.text, { marginBottom: Spacing.sm }]}>IFU Émetteur *</Text>
          <TextInput
            value={issuerIFU}
            onChangeText={setIssuerIFU}
            placeholder="Numéro IFU de l'entreprise"
            style={[
              GlobalStyles.input,
              errors.issuerIFU ? GlobalStyles.inputError : {}
            ]}
            autoCapitalize="characters"
          />
          {errors.issuerIFU && (
            <Text style={[GlobalStyles.textError, { fontSize: 12, marginTop: Spacing.xs }]}>
              {errors.issuerIFU}
            </Text>
          )}
        </View>

        {/* Client */}
        <View style={{ marginBottom: Spacing.md }}>
          <Text style={[GlobalStyles.text, { marginBottom: Spacing.sm }]}>Client *</Text>
          <TextInput
            value={buyerName}
            onChangeText={setBuyerName}
            placeholder="Nom du client"
            style={[
              GlobalStyles.input,
              errors.buyerName ? GlobalStyles.inputError : {}
            ]}
          />
          {errors.buyerName && (
            <Text style={[GlobalStyles.textError, { fontSize: 12, marginTop: Spacing.xs }]}>
              {errors.buyerName}
            </Text>
          )}
        </View>

        {/* IFU Client (optionnel) */}
        <View style={{ marginBottom: Spacing.lg }}>
          <Text style={[GlobalStyles.text, { marginBottom: Spacing.sm }]}>IFU Client (optionnel)</Text>
          <TextInput
            value={buyerIFU}
            onChangeText={setBuyerIFU}
            placeholder="Numéro IFU du client"
            style={GlobalStyles.input}
            autoCapitalize="characters"
          />
        </View>

        {/* Articles */}
        <Text style={[GlobalStyles.text, { marginBottom: Spacing.md, fontWeight: 'bold' }]}>
          Articles
        </Text>
        
        {lines.map((line, idx) => (
          <View key={idx} style={[GlobalStyles.card, { marginBottom: Spacing.sm }]}>
            <View style={[GlobalStyles.row, GlobalStyles.spaceBetween, { marginBottom: Spacing.sm }]}>
              <Text style={GlobalStyles.text}>Article {idx + 1}</Text>
              {lines.length > 1 && (
                <TouchableOpacity onPress={() => removeLine(idx)}>
                  <Icon name="delete" size={24} color={Colors.error} />
                </TouchableOpacity>
              )}
            </View>

            <View style={{ marginBottom: Spacing.sm }}>
              <Text style={[GlobalStyles.text, { marginBottom: Spacing.xs }]}>Libellé *</Text>
              <TextInput
                value={line.label}
                onChangeText={(t) => updateLine(idx, { label: t })}
                placeholder="Description de l'article"
                style={[
                  GlobalStyles.input,
                  errors[`line_${idx}_label`] ? GlobalStyles.inputError : {}
                ]}
              />
              {errors[`line_${idx}_label`] && (
                <Text style={[GlobalStyles.textError, { fontSize: 12, marginTop: Spacing.xs }]}>
                  {errors[`line_${idx}_label`]}
                </Text>
              )}
            </View>

            <View style={[GlobalStyles.row, { gap: Spacing.sm }]}>
              <View style={{ flex: 1 }}>
                <Text style={[GlobalStyles.text, { marginBottom: Spacing.xs }]}>Qté *</Text>
                <TextInput
                  value={line.qty}
                  onChangeText={(t) => updateLine(idx, { qty: t })}
                  keyboardType="numeric"
                  style={[
                    GlobalStyles.input,
                    errors[`line_${idx}_qty`] ? GlobalStyles.inputError : {}
                  ]}
                />
                {errors[`line_${idx}_qty`] && (
                  <Text style={[GlobalStyles.textError, { fontSize: 12, marginTop: Spacing.xs }]}>
                    {errors[`line_${idx}_qty`]}
                  </Text>
                )}
              </View>

              <View style={{ flex: 2 }}>
                <Text style={[GlobalStyles.text, { marginBottom: Spacing.xs }]}>Prix unitaire *</Text>
                <TextInput
                  value={line.unitPrice}
                  onChangeText={(t) => updateLine(idx, { unitPrice: t })}
                  keyboardType="numeric"
                  style={[
                    GlobalStyles.input,
                    errors[`line_${idx}_price`] ? GlobalStyles.inputError : {}
                  ]}
                />
                {errors[`line_${idx}_price`] && (
                  <Text style={[GlobalStyles.textError, { fontSize: 12, marginTop: Spacing.xs }]}>
                    {errors[`line_${idx}_price`]}
                  </Text>
                )}
              </View>
            </View>

            <View style={{ marginTop: Spacing.sm }}>
              <Text style={[GlobalStyles.text, { textAlign: 'right', fontWeight: 'bold' }]}>
                Sous-total: {(parseFloat(line.qty || '0') * parseFloat(line.unitPrice || '0')).toFixed(2)} FCFA
              </Text>
            </View>
          </View>
        ))}

        {/* Bouton Ajouter ligne */}
        <TouchableOpacity
          style={[GlobalStyles.button, GlobalStyles.buttonSecondary, { marginBottom: Spacing.lg }]}
          onPress={addLine}
        >
          <Icon name="add" size={20} color={Colors.white} style={{ marginRight: Spacing.sm }} />
          <Text style={GlobalStyles.buttonText}>Ajouter un article</Text>
        </TouchableOpacity>

        {/* Total */}
        <View style={[GlobalStyles.card, { backgroundColor: Colors.primaryLight }]}>
          <Text style={[GlobalStyles.text, { fontSize: 18, fontWeight: 'bold', textAlign: 'center' }]}>
            Total: {computeTotal().toFixed(2)} FCFA
          </Text>
        </View>

        {/* Bouton Valider */}
        <TouchableOpacity
          style={[GlobalStyles.button, GlobalStyles.buttonSuccess, { marginTop: Spacing.lg }]}
          onPress={handleSubmit}
        >
          <Icon name="check" size={20} color={Colors.white} style={{ marginRight: Spacing.sm }} />
          <Text style={GlobalStyles.buttonText}>Valider & Certifier</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}
