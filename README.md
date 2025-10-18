# FP9900 POS Certification System

A comprehensive Android Point of Sale (POS) application with DGI (Direction Générale des Impôts) invoice certification capabilities. This system enables merchants to create, fiscalize, and manage invoices while maintaining compliance with fiscal regulations.

## 🚀 Features

### Core Functionality
- **Invoice Creation**: Create invoices with customer and issuer information
- **DGI Certification**: Three-step certification process (Create → Fiscalize → Verify)
- **Offline Support**: Store invoices locally when offline and sync when connected
- **Thermal Printing**: Print certified invoices using DSpread SDK
- **Real-time Sync**: Automatic synchronization of pending invoices

### Technical Features
- **MVVM Architecture**: Clean separation of concerns with ViewModel pattern
- **Data Binding**: Reactive UI with Android Data Binding
- **Background Processing**: Non-blocking initialization and API calls
- **Error Handling**: Comprehensive error management with specific DGI rejection codes
- **Material Design 3**: Modern UI following Material Design guidelines

### API Integration
- **DGI API Integration**: Complete integration with fiscal certification API
- **Multi-step Certification**: Handles create, fiscalize, and verify operations
- **Error Code Handling**: Specific handling for REJ001-REJ030 rejection codes
- **Timeout Management**: Configurable timeouts for network operations

## 🛠️ Technical Requirements

### Development Environment
- **Java**: Version 17 (JDK 17)
- **Gradle**: Version 8.0
- **Android Gradle Plugin**: Version 8.1.4
- **Android Studio**: Latest stable version recommended

### Target Platform
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Compile SDK**: API 34

### Dependencies
- **OkHttp**: 3.10.0 (HTTP client)
- **Retrofit**: 2.4.0 (REST client)
- **Gson**: 2.8.6 (JSON serialization)
- **MVVMHabit**: 4.0.0 (MVVM framework)
- **Material Design**: 1.0.0 (UI components)
- **Lifecycle**: 2.0.0 (Android Architecture Components)

## 📦 Installation & Setup

### Prerequisites
1. **Java 17**: Ensure JAVA_HOME is set to JDK 17
   ```bash
   export JAVA_HOME=/path/to/jdk-17
   ```

2. **Android SDK**: Install Android SDK with API levels 24-34

3. **Gradle**: The project uses Gradle Wrapper (version 8.0)

### Build Instructions

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd fp9900-pos-certification
   ```

2. **Set Java version**:
   ```bash
   export JAVA_HOME=/path/to/jdk-17
   ```

3. **Build the project**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```

### Android Studio Setup

1. **Open project** in Android Studio
2. **Sync Gradle** files
3. **Connect device** or start emulator
4. **Run** the application (▶️ button)

## 🏗️ Project Structure

```
pos_android_app/
├── src/main/java/com/dspread/pos/
│   ├── managers/           # Core business logic managers
│   │   ├── ApiManager.java      # DGI API integration
│   │   ├── PrinterManager.java  # Thermal printing
│   │   └── StorageManager.java  # Local storage & sync
│   ├── models/             # Data models
│   │   ├── InvoiceData.java
│   │   ├── InvoiceLine.java
│   │   ├── Issuer.java
│   │   ├── Customer.java
│   │   └── Response models...
│   ├── ui/invoice/         # Invoice UI components
│   │   ├── InvoiceFragment.java
│   │   └── InvoiceViewModel.java
│   └── utils/              # Utility classes
│       ├── TRACE.java           # Logging utility
│       └── InvoiceTestData.java # Test data generator
├── src/main/res/
│   ├── layout/             # UI layouts
│   ├── values/             # Colors, strings, styles
│   └── drawable/           # Icons and images
└── libs/                   # External libraries
    ├── dspread_pos_sdk_7.7.1.aar
    ├── dspread_print_sdk-1.7.4-beta.aar
    └── mvvmhabit-release.aar
```

## 🔧 Configuration

### API Configuration
The application is configured to use the DGI certification API:
- **Base URL**: `https://api.invoice.fisc.kpsaccess.com:9443`
- **Timeout**: 30 seconds
- **Endpoints**:
  - `/api/invoice/create` - Create invoice
  - `/api/invoice/fiscalize` - Fiscalize invoice
  - `/api/invoice/verify` - Verify certification

### Printer Configuration
- **SDK**: DSpread Thermal Printer SDK
- **Connection**: Bluetooth/USB
- **Paper Width**: 58mm standard

## 📱 Usage

### Creating an Invoice
1. **Navigate** to the Invoice tab
2. **Fill in** invoice details:
   - Invoice Number
   - Machine Number
   - Issuer information
   - Customer information
3. **Add items** using "Add Item" button
4. **Click** "Certify Invoice" to start certification

### Certification Process
The system follows a three-step process:
1. **Create**: Submit invoice data to DGI
2. **Fiscalize**: Process fiscal validation
3. **Verify**: Confirm certification and get QR code

### Offline Mode
- Invoices are stored locally when offline
- Automatic sync when connection is restored
- View pending invoices in the sync status

## 🐛 Troubleshooting

### Common Issues

1. **ANR (Application Not Responding)**:
   - Ensure Java 17 is used (not Java 23)
   - Check that managers initialize in background threads

2. **Build Errors**:
   - Verify JAVA_HOME points to JDK 17
   - Clean and rebuild: `./gradlew clean assembleDebug`

3. **API Connection Issues**:
   - Check network connectivity
   - Verify API endpoint configuration
   - Check timeout settings

### Debug Information
- **Logs**: Use `TRACE.i()`, `TRACE.e()` for logging
- **Tag**: Filter logs by "POS_LOG" tag
- **Crash Reports**: Integrated Bugly crash reporting

## 🔒 Security

- **Signing**: Debug and release builds are signed
- **Keystore**: Uses app.keystore for signing
- **API Security**: HTTPS communication with DGI API
- **Data Protection**: Local storage with encryption

## 📄 License

This project is licensed under the terms specified in the LICENSE file.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📞 Support

For technical support or questions:
- Check the troubleshooting section
- Review the logcat output
- Ensure all requirements are met

---

**Version**: 8.0.6 (Build 106)  
**Last Updated**: October 2024  
**Compatibility**: Android 7.0+ (API 24+)
