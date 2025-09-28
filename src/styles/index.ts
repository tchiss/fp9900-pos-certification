import { StyleSheet } from 'react-native';
import { Colors } from './colors';
import { Typography } from './typography';
import { Spacing, BorderRadius, TouchTarget } from './spacing';

export { Colors, Typography, Spacing, BorderRadius, TouchTarget };

export const GlobalStyles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  
  screen: {
    flex: 1,
    padding: Spacing.md,
  },
  
  card: {
    backgroundColor: Colors.surface,
    borderRadius: BorderRadius.md,
    padding: Spacing.md,
    marginVertical: Spacing.sm,
    elevation: 2,
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  
  input: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: BorderRadius.sm,
    padding: Spacing.md,
    fontSize: Typography.body1.fontSize,
    backgroundColor: Colors.white,
    minHeight: TouchTarget.minHeight,
  },
  
  inputFocused: {
    borderColor: Colors.primary,
    borderWidth: 2,
  },
  
  inputError: {
    borderColor: Colors.error,
  },
  
  button: {
    backgroundColor: Colors.primary,
    borderRadius: BorderRadius.md,
    padding: Spacing.md,
    minHeight: TouchTarget.minHeight,
    justifyContent: 'center',
    alignItems: 'center',
  },
  
  buttonSecondary: {
    backgroundColor: Colors.secondary,
  },
  
  buttonSuccess: {
    backgroundColor: Colors.success,
  },
  
  buttonWarning: {
    backgroundColor: Colors.warning,
  },
  
  buttonError: {
    backgroundColor: Colors.error,
  },
  
  buttonDisabled: {
    backgroundColor: Colors.textDisabled,
  },
  
  buttonText: {
    color: Colors.white,
    fontSize: Typography.button.fontSize,
    fontWeight: Typography.button.fontWeight,
  },
  
  buttonTextSecondary: {
    color: Colors.white,
  },
  
  text: {
    color: Colors.textPrimary,
    fontSize: Typography.body1.fontSize,
  },
  
  textSecondary: {
    color: Colors.textSecondary,
  },
  
  textError: {
    color: Colors.error,
  },
  
  textSuccess: {
    color: Colors.success,
  },
  
  header: {
    ...Typography.h2,
    color: Colors.textPrimary,
    marginBottom: Spacing.lg,
    textAlign: 'center',
  },
  
  row: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  
  spaceBetween: {
    justifyContent: 'space-between',
  },
  
  centered: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  
  statusIndicator: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: Spacing.sm,
  },
  
  statusOnline: {
    backgroundColor: Colors.success,
  },
  
  statusOffline: {
    backgroundColor: Colors.error,
  },
  
  statusPending: {
    backgroundColor: Colors.warning,
  },
  
  terminal: {
    fontFamily: 'monospace',
    fontSize: 14,
  },
  
  caption: {
    fontSize: 12,
    lineHeight: 16,
  },
});
