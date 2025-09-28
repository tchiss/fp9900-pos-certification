# FP9900 POS - DGI Certification Application

A comprehensive React Native application for FP9900 POS terminal enabling invoice certification through DGI (Direction Générale des Impôts) APIs in Benin. This application provides offline capabilities, thermal printing, real-time synchronization, authentication, monitoring, and security features.

## 🎯 Features

### Core Functionality
- **Invoice input** with optimized touch interface
- **DGI certification** via backend API
- **Offline mode** with automatic synchronization
- **Thermal printing** on FP9900 terminal
- **Robust error handling** with automatic retry
- **Modern interface** adapted for POS terminals

### Authentication & Security
- **User Authentication**: Secure login system with token management
- **Session Management**: Automatic token refresh and session validation
- **SSL Pinning**: Certificate pinning for secure API communication
- **Data Encryption**: Encrypt sensitive data at rest
- **Audit Logging**: Comprehensive audit trail for compliance

### Monitoring & Analytics
- **Performance Metrics**: API latency, success rates, error tracking
- **Real-time Statistics**: Certification rates, print success rates, sync statistics
- **Error Monitoring**: Detailed error logging and reporting
- **Export Capabilities**: Export performance data and audit logs

### Configuration & Testing
- **Dynamic Configuration**: Runtime configuration management
- **Test Suite**: Comprehensive application testing framework
- **Quick Tests**: Basic functionality validation
- **Test Reports**: Detailed test results with export functionality

### Background Services
- **Sync Service**: Background synchronization of pending invoices
- **Network State Receiver**: Automatic sync when network becomes available
- **Headless Tasks**: JavaScript tasks running in background

## 🏗️ Architecture

### Frontend (React Native)
- **Components**: Touch user interface with authentication
- **Services**: 
  - API client with authentication and monitoring
  - Printing service with hardware integration
  - Offline storage with auto-sync
  - Authentication service with token management
  - Security service with SSL pinning and encryption
  - Monitoring service with performance tracking
  - Configuration service with runtime updates
  - Test service with comprehensive validation
- **Screens**: Login, Invoice, Statistics, Configuration, Tests
- **Styles**: Design adapted for POS terminals

### Backend
- **DGI API**: Communication with certification services
- **Database**: Invoice and metadata storage
- **Synchronization**: Queue management

### FP9900 Terminal
- **Native module**: Java bridge for printing
- **FP9900 SDK**: Hardware integration (to be configured)
- **Background Services**: 
  - SyncService for background synchronization
  - NetworkStateReceiver for network change detection
  - SyncTaskService for headless JavaScript tasks
- **Lifecycle Management**: Proper initialization and cleanup

## 🚀 Installation

### Prerequisites

- Node.js 16+
- React Native CLI
- Android Studio
- FP9900 terminal with Android
- FP9900 SDK (provided by manufacturer)

### 1. Clone the project

   ```bash
git clone <repository-url>
cd pos_certif_starter
   ```

### 2. Install dependencies

   ```bash
# JavaScript dependencies
yarn install
```

### 3. Android Configuration

#### 3.1. FP9900 SDK Configuration

1. Get FP9900 SDK from manufacturer
2. Add JAR files to `android/app/libs/`
3. Update `android/app/build.gradle`:

```gradle
dependencies {
    // FP9900 SDK
    implementation files('libs/fp9900-sdk.jar')
    // Other dependencies...
}
```

#### 3.2. Native Module Configuration

The `PrinterModule.java` module (in `android/app/src/main/java/com/fp9900/printer/`) contains TODOs to be completed with actual SDK calls:

     ```java
// Replace TODOs with FP9900 SDK calls
printerSDK = PrinterSDK.getInstance();
printerConnection = printerSDK.createConnection();
```

### 4. Backend API Configuration

Modify `src/services/api.ts`:

```typescript
export const BASE_URL = 'https://your-backend.example.com';
```

### 5. Android Permissions

Android permissions are already configured in `AndroidManifest.xml`:

- Bluetooth (printing)
- Network (API)
- Storage (offline cache)
- Location (Bluetooth)

## 📱 Deployment

### Development

   ```bash
# Start Metro
yarn start

# Android
   yarn android
```

### Production

#### 1. Build Android

```bash
# Debug
cd android && ./gradlew assembleDebug

# Release
cd android && ./gradlew assembleRelease
```

#### 2. Install on FP9900

1. Enable developer mode on terminal
2. Enable USB debugging
3. Install APK:

```bash
adb install app-release.apk
```

**Note**: Application ID is `com.fp9900.pos`

#### 3. Terminal Configuration

1. Configure network connection (WiFi/4G)
2. Test printing with test page
3. Configure DGI settings (IFU, etc.)

## 🔧 Configuration

### Environment Variables

Create `.env`:

```env
API_BASE_URL=https://your-backend.example.com
API_TIMEOUT=30000
API_RETRY_ATTEMPTS=3

# DGI Configuration
DGI_API_KEY=your-dgi-api-key
DGI_CERTIFICATE_PATH=path/to/certificate.pem

# Printer Configuration
PRINTER_TIMEOUT=10000
PRINTER_RETRY_ATTEMPTS=2
PRINTER_CONNECTION_TYPE=bluetooth

# Synchronization Configuration
SYNC_INTERVAL=60000
SYNC_BATCH_SIZE=10
SYNC_MAX_ATTEMPTS=3

# Development Configuration
DEBUG_MODE=true
LOG_LEVEL=debug
MOCK_PRINTER=false

# Security Configuration
ENABLE_SSL_PINNING=true
CERTIFICATE_PINNING_HASH=your-certificate-hash

# Performance Configuration
CACHE_SIZE=50
CACHE_TTL=3600000
MAX_CONCURRENT_REQUESTS=5
```

### DGI Configuration

1. **Issuer IFU**: Fiscal identification number of the company
2. **Backend API**: URL and authentication keys
3. **Certificates**: SSL certificates for secure communication

### Printer Configuration

The printing module supports:

- **Thermal printing** 58mm/80mm
- **QR codes** for certification
- **Formatted text** (bold, alignment, size)
- **Automatic paper cutting**

## 🧪 Testing

### Unit Tests

```bash
yarn test
```

### Integration Tests

```bash
# API test
yarn test:api

# Printer test
yarn test:printer

# Offline test
yarn test:offline
```

### Terminal Testing

1. **Print test**: Use "Test Printer" button
2. **Certification test**: Create a test invoice
3. **Offline test**: Disconnect network and check synchronization

## 📊 Monitoring

### Logs

Logs are available via:

```bash
# Android
adb logcat | grep "FP9900POS"

# Metrics
yarn logs:metrics
```

### Key Metrics

- **Certification rate**: % of successfully certified invoices
- **Response time**: DGI API latency
- **Print errors**: Hardware issues
- **Offline sync**: Queue and retry status

## 🚨 Troubleshooting

### Common Issues

#### 1. "Printer not initialized" Error

```bash
# Check Bluetooth connection
adb shell dumpsys bluetooth

# Restart printing service
adb shell am force-stop com.fp9900.pos
```

#### 2. API Certification Error

```bash
# Check connectivity
curl -I https://your-backend.example.com/health

# Check backend logs
yarn logs:backend
```

#### 3. Offline Sync Issue

```bash
# Check local storage
adb shell run-as com.fp9900.pos ls /data/data/com.fp9900.pos/shared_prefs/

# Clear cache
yarn clean:cache
```

### Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| `PRINTER_NOT_FOUND` | Printing module not found | Check SDK installation |
| `NETWORK_ERROR` | Connectivity issue | Check network |
| `API_TIMEOUT` | DGI API timeout | Increase timeout |
| `VALIDATION_ERROR` | Invalid invoice data | Check data format |

## 🔒 Security

### Best Practices

1. **SSL Certificates**: Use HTTPS for all communications
2. **Authentication**: JWT tokens with expiration
3. **Encryption**: Encrypt sensitive data locally
4. **Audit**: Log all critical operations

### DGI Compliance

- **Timestamping**: All invoices timestamped
- **Traceability**: Complete audit trail
- **Integrity**: Digital signature of data
- **Archiving**: Data retention according to regulations

## 📈 Performance

### Optimizations

1. **Cache**: Cache frequent data
2. **Compression**: Compress network data
3. **Lazy loading**: Load on demand
4. **Background sync**: Background synchronization

### Monitoring

```bash
# Performance metrics
yarn perf:metrics

# Memory profiling
yarn perf:memory

# Network analysis
yarn perf:network
```

## 🧪 Testing

### Test Suite

The application includes a comprehensive test suite accessible through the Configuration screen:

- **Quick Tests**: Basic functionality validation
- **Full Test Suite**: Comprehensive application testing
- **Configuration Tests**: Configuration validation
- **Authentication Tests**: Authentication system checks
- **Security Tests**: Security features validation
- **Printer Tests**: Printer service validation
- **Storage Tests**: Offline storage checks
- **Monitoring Tests**: Performance monitoring validation

### Running Tests

```bash
# Run tests from the app
1. Open the app
2. Navigate to Configuration
3. Tap "Run Tests"
4. View results and export reports
```

## 🔐 Security Features

### Authentication
- Secure login system with token management
- Automatic token refresh and session validation
- Session timeout and logout functionality

### Data Protection
- SSL certificate pinning for API communication
- Data encryption at rest for sensitive information
- Secure key generation and management

### Audit Logging
- Comprehensive audit trail for all user actions
- Compliance-ready logging for regulatory requirements
- Export capabilities for audit reports

## 📊 Monitoring & Analytics

### Performance Metrics
- API response times and latency tracking
- Success/failure rates for all operations
- Error tracking and categorization
- Resource usage monitoring

### Real-time Statistics
- Certification success rates
- Print operation success rates
- Synchronization statistics
- Network connectivity status

### Export Capabilities
- Performance data export
- Audit log export
- Test result export
- Statistics report generation

## 🤝 Contributing

### Workflow

1. Fork the project
2. Create a feature branch
3. Commit changes
4. Create a Pull Request

### Standards

- **Code**: ESLint + Prettier
- **Tests**: Coverage > 80%
- **Documentation**: JSDoc for public functions
- **Commits**: Conventional Commits
- **Security**: Security tests must pass
- **Monitoring**: Performance metrics must be within acceptable ranges

## 📞 Support

### Contact

- **Email**: support@yourcompany.com
- **Documentation**: [docs.yourcompany.com](https://docs.yourcompany.com)
- **Issues**: [GitHub Issues](https://github.com/yourcompany/fp9900-pos/issues)

### SLA

- **Critical**: < 4h (business days)
- **Important**: < 24h
- **Standard**: < 72h

## 📄 License

Copyright © 2024 Your Company. All rights reserved.

---

## 📋 Deployment Checklist

- [ ] FP9900 SDK installed and configured
- [ ] Backend API accessible and tested
- [ ] SSL certificates configured
- [ ] Android permissions granted
- [ ] Print tests successful
- [ ] Certification tests successful
- [ ] Offline mode tested
- [ ] Monitoring configured
- [ ] User documentation provided
- [ ] Team training completed

## 🔄 Changelog

### v1.0.0 (2024-01-XX)
- ✨ First stable release
- 🎯 Complete DGI certification
- 📱 Optimized touch interface
- 🖨️ FP9900 thermal printing
- 🔄 Offline synchronization
- 🛡️ Robust error handling