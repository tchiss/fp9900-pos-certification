#!/usr/bin/env node

/**
 * Build and Test Script for FP9900 POS Application
 * 
 * This script performs comprehensive validation of the application
 * including TypeScript compilation, dependency checks, and test execution
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Colors for console output
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m',
};

function log(message, color = colors.reset) {
  console.log(`${color}${message}${colors.reset}`);
}

function logStep(step, message) {
  log(`\n${colors.cyan}[${step}]${colors.reset} ${message}`);
}

function logSuccess(message) {
  log(`${colors.green}✓${colors.reset} ${message}`);
}

function logError(message) {
  log(`${colors.red}✗${colors.reset} ${message}`);
}

function logWarning(message) {
  log(`${colors.yellow}⚠${colors.reset} ${message}`);
}

function logInfo(message) {
  log(`${colors.blue}ℹ${colors.reset} ${message}`);
}

// Check if a file exists
function fileExists(filePath) {
  return fs.existsSync(path.resolve(filePath));
}

// Check if a directory exists
function dirExists(dirPath) {
  return fs.existsSync(path.resolve(dirPath)) && fs.statSync(path.resolve(dirPath)).isDirectory();
}

// Run a command and return the result
function runCommand(command, options = {}) {
  try {
    const result = execSync(command, { 
      encoding: 'utf8', 
      stdio: 'pipe',
      ...options 
    });
    return { success: true, output: result };
  } catch (error) {
    return { success: false, error: error.message, output: error.stdout || error.stderr };
  }
}

// Check project structure
function checkProjectStructure() {
  logStep('STRUCTURE', 'Checking project structure...');
  
  const requiredFiles = [
    'package.json',
    'tsconfig.json',
    'babel.config.js',
    'metro.config.js',
    'index.js',
    'app.json',
    '.yarnrc.yml',
  ];
  
  const requiredDirs = [
    'src',
    'src/screens',
    'src/components',
    'src/services',
    'src/config',
    'src/storage',
    'src/styles',
    'android',
    'android/app',
    'android/app/src/main/java/com/fp9900/pos',
    'android/app/src/main/java/com/fp9900/printer',
  ];
  
  let allFilesExist = true;
  let allDirsExist = true;
  
  // Check files
  for (const file of requiredFiles) {
    if (fileExists(file)) {
      logSuccess(`File exists: ${file}`);
    } else {
      logError(`Missing file: ${file}`);
      allFilesExist = false;
    }
  }
  
  // Check directories
  for (const dir of requiredDirs) {
    if (dirExists(dir)) {
      logSuccess(`Directory exists: ${dir}`);
    } else {
      logError(`Missing directory: ${dir}`);
      allDirsExist = false;
    }
  }
  
  return allFilesExist && allDirsExist;
}

// Check dependencies
function checkDependencies() {
  logStep('DEPENDENCIES', 'Checking dependencies...');
  
  if (!fileExists('package.json')) {
    logError('package.json not found');
    return false;
  }
  
  const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf8'));
  const requiredDeps = [
    'react',
    'react-native',
    '@react-native-community/netinfo',
    '@react-native-async-storage/async-storage',
    'react-native-vector-icons',
    'react-native-qrcode-svg',
    'react-native-svg',
  ];
  
  let allDepsExist = true;
  
  for (const dep of requiredDeps) {
    if (packageJson.dependencies[dep] || packageJson.devDependencies[dep]) {
      logSuccess(`Dependency found: ${dep}`);
    } else {
      logError(`Missing dependency: ${dep}`);
      allDepsExist = false;
    }
  }
  
  // Check if node_modules exists
  if (dirExists('node_modules')) {
    logSuccess('node_modules directory exists');
  } else {
    logWarning('node_modules directory not found - run yarn install');
    allDepsExist = false;
  }
  
  return allDepsExist;
}

// Check TypeScript compilation
function checkTypeScript() {
  logStep('TYPESCRIPT', 'Checking TypeScript compilation...');
  
  if (!fileExists('tsconfig.json')) {
    logError('tsconfig.json not found');
    return false;
  }
  
  const result = runCommand('yarn tsc --noEmit');
  
  if (result.success) {
    logSuccess('TypeScript compilation successful');
    return true;
  } else {
    logError('TypeScript compilation failed');
    log(result.output);
    return false;
  }
}

// Check Android configuration
function checkAndroidConfig() {
  logStep('ANDROID', 'Checking Android configuration...');
  
  const androidFiles = [
    'android/build.gradle',
    'android/app/build.gradle',
    'android/app/src/main/AndroidManifest.xml',
    'android/app/src/main/java/com/fp9900/pos/MainActivity.java',
    'android/app/src/main/java/com/fp9900/pos/MainApplication.java',
    'android/app/src/main/java/com/fp9900/printer/PrinterModule.java',
    'android/app/src/main/java/com/fp9900/printer/PrinterPackage.java',
    'android/app/src/main/java/com/fp9900/pos/SyncService.java',
    'android/app/src/main/java/com/fp9900/pos/SyncTaskService.java',
    'android/app/src/main/java/com/fp9900/pos/NetworkStateReceiver.java',
  ];
  
  let allFilesExist = true;
  
  for (const file of androidFiles) {
    if (fileExists(file)) {
      logSuccess(`Android file exists: ${file}`);
    } else {
      logError(`Missing Android file: ${file}`);
      allFilesExist = false;
    }
  }
  
  return allFilesExist;
}

// Check React Native configuration
function checkReactNativeConfig() {
  logStep('REACT-NATIVE', 'Checking React Native configuration...');
  
  const configFiles = [
    'babel.config.js',
    'metro.config.js',
    'index.js',
    'app.json',
  ];
  
  let allFilesExist = true;
  
  for (const file of configFiles) {
    if (fileExists(file)) {
      logSuccess(`Config file exists: ${file}`);
    } else {
      logError(`Missing config file: ${file}`);
      allFilesExist = false;
    }
  }
  
  // Check app.json content
  if (fileExists('app.json')) {
    try {
      const appJson = JSON.parse(fs.readFileSync('app.json', 'utf8'));
      if (appJson.name && appJson.displayName) {
        logSuccess('app.json configuration valid');
      } else {
        logError('app.json missing required fields');
        allFilesExist = false;
      }
    } catch (error) {
      logError('Invalid app.json format');
      allFilesExist = false;
    }
  }
  
  return allFilesExist;
}

// Check source code structure
function checkSourceCode() {
  logStep('SOURCE', 'Checking source code structure...');
  
  const sourceFiles = [
    'src/App.tsx',
    'src/screens/InvoiceScreen.tsx',
    'src/screens/LoginScreen.tsx',
    'src/screens/StatsScreen.tsx',
    'src/screens/ConfigScreen.tsx',
    'src/screens/TestScreen.tsx',
    'src/components/InvoiceForm.tsx',
    'src/components/InvoicePreview.tsx',
    'src/services/api.ts',
    'src/services/AuthService.ts',
    'src/services/MonitoringService.ts',
    'src/services/PrinterService.ts',
    'src/services/SecurityService.ts',
    'src/services/SyncService.ts',
    'src/services/TestService.ts',
    'src/config/Config.ts',
    'src/config/Environment.ts',
    'src/storage/offline.ts',
    'src/styles/index.ts',
  ];
  
  let allFilesExist = true;
  
  for (const file of sourceFiles) {
    if (fileExists(file)) {
      logSuccess(`Source file exists: ${file}`);
    } else {
      logError(`Missing source file: ${file}`);
      allFilesExist = false;
    }
  }
  
  return allFilesExist;
}

// Run linting
function runLinting() {
  logStep('LINTING', 'Running linting...');
  
  const result = runCommand('yarn lint', { stdio: 'pipe' });
  
  if (result.success) {
    logSuccess('Linting passed');
    return true;
  } else {
    logWarning('Linting failed or not configured');
    log(result.output);
    return false;
  }
}

// Check build readiness
function checkBuildReadiness() {
  logStep('BUILD', 'Checking build readiness...');
  
  // Check if we can run React Native commands
  const result = runCommand('yarn react-native --version', { stdio: 'pipe' });
  
  if (result.success) {
    logSuccess('React Native CLI available');
    logInfo(`React Native version: ${result.output.trim()}`);
    return true;
  } else {
    logError('React Native CLI not available');
    log(result.output);
    return false;
  }
}

// Generate build report
function generateBuildReport(results) {
  logStep('REPORT', 'Generating build report...');
  
  const report = {
    timestamp: new Date().toISOString(),
    results: results,
    summary: {
      total: Object.keys(results).length,
      passed: Object.values(results).filter(r => r === true).length,
      failed: Object.values(results).filter(r => r === false).length,
    }
  };
  
  const reportPath = 'build-report.json';
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
  
  logSuccess(`Build report saved to: ${reportPath}`);
  
  // Print summary
  log(`\n${colors.bright}Build Summary:${colors.reset}`);
  log(`${colors.green}Passed: ${report.summary.passed}${colors.reset}`);
  log(`${colors.red}Failed: ${report.summary.failed}${colors.reset}`);
  log(`${colors.blue}Total: ${report.summary.total}${colors.reset}`);
  
  const successRate = (report.summary.passed / report.summary.total) * 100;
  log(`${colors.cyan}Success Rate: ${successRate.toFixed(1)}%${colors.reset}`);
  
  return report;
}

// Main function
function main() {
  log(`${colors.bright}${colors.cyan}FP9900 POS Application Build & Test Script${colors.reset}`);
  log(`${colors.cyan}================================================${colors.reset}`);
  
  const results = {};
  
  // Run all checks
  results.projectStructure = checkProjectStructure();
  results.dependencies = checkDependencies();
  results.typeScript = checkTypeScript();
  results.androidConfig = checkAndroidConfig();
  results.reactNativeConfig = checkReactNativeConfig();
  results.sourceCode = checkSourceCode();
  results.linting = runLinting();
  results.buildReadiness = checkBuildReadiness();
  
  // Generate report
  const report = generateBuildReport(results);
  
  // Exit with appropriate code
  if (report.summary.failed === 0) {
    log(`\n${colors.green}${colors.bright}All checks passed! Application is ready for build.${colors.reset}`);
    process.exit(0);
  } else {
    log(`\n${colors.red}${colors.bright}Some checks failed. Please fix the issues before building.${colors.reset}`);
    process.exit(1);
  }
}

// Run the script
if (require.main === module) {
  main();
}

module.exports = {
  checkProjectStructure,
  checkDependencies,
  checkTypeScript,
  checkAndroidConfig,
  checkReactNativeConfig,
  checkSourceCode,
  runLinting,
  checkBuildReadiness,
  generateBuildReport,
};
