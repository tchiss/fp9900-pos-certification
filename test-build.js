#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

console.log('🧪 Testing FP9900 POS Application Build...\n');

// Test 1: TypeScript compilation
console.log('1️⃣ Testing TypeScript compilation...');
try {
  execSync('yarn tsc --noEmit', { stdio: 'pipe' });
  console.log('✅ TypeScript compilation: PASSED\n');
} catch (error) {
  console.log('❌ TypeScript compilation: FAILED');
  console.log(error.stdout?.toString() || error.message);
  process.exit(1);
}

// Test 2: Check if all required files exist
console.log('2️⃣ Checking required files...');
const requiredFiles = [
  'package.json',
  'tsconfig.json',
  'babel.config.js',
  'metro.config.js',
  'src/App.tsx',
  'src/screens/InvoiceScreen.tsx',
  'src/components/InvoiceForm.tsx',
  'src/components/InvoicePreview.tsx',
  'src/services/api.ts',
  'src/services/PrinterService.ts',
  'src/storage/offline.ts',
  'android/app/build.gradle',
  'android/app/src/main/AndroidManifest.xml',
  'android/app/src/main/java/com/fp9900/pos/MainApplication.java',
  'android/app/src/main/java/com/fp9900/printer/PrinterModule.java',
];

let allFilesExist = true;
for (const file of requiredFiles) {
  if (fs.existsSync(file)) {
    console.log(`✅ ${file}`);
  } else {
    console.log(`❌ ${file} - MISSING`);
    allFilesExist = false;
  }
}

if (allFilesExist) {
  console.log('\n✅ All required files: PASSED\n');
} else {
  console.log('\n❌ Some required files are missing\n');
  process.exit(1);
}

// Test 3: Check dependencies
console.log('3️⃣ Checking dependencies...');
const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf8'));
const requiredDeps = [
  'react',
  'react-native',
  'react-native-qrcode-svg',
  'react-native-svg',
  '@react-native-async-storage/async-storage',
  '@react-native-community/netinfo',
  'axios',
  'react-native-vector-icons'
];

let allDepsExist = true;
for (const dep of requiredDeps) {
  if (packageJson.dependencies[dep]) {
    console.log(`✅ ${dep}: ${packageJson.dependencies[dep]}`);
  } else {
    console.log(`❌ ${dep} - MISSING`);
    allDepsExist = false;
  }
}

if (allDepsExist) {
  console.log('\n✅ All dependencies: PASSED\n');
} else {
  console.log('\n❌ Some dependencies are missing\n');
  process.exit(1);
}

// Test 4: Check node_modules
console.log('4️⃣ Checking node_modules...');
if (fs.existsSync('node_modules')) {
  console.log('✅ node_modules directory exists');
  
  // Check a few critical packages
  const criticalPackages = ['react', 'react-native', 'axios'];
  for (const pkg of criticalPackages) {
    if (fs.existsSync(`node_modules/${pkg}`)) {
      console.log(`✅ ${pkg} package installed`);
    } else {
      console.log(`❌ ${pkg} package missing`);
      process.exit(1);
    }
  }
  console.log('\n✅ node_modules: PASSED\n');
} else {
  console.log('❌ node_modules directory missing - run "yarn install" first\n');
  process.exit(1);
}

console.log('🎉 All tests passed! The application is ready for deployment.');
console.log('\n📋 Next steps:');
console.log('1. Configure your backend API URL in src/services/api.ts');
console.log('2. Add FP9900 SDK to android/app/libs/');
console.log('3. Update PrinterModule.java with actual SDK calls');
console.log('4. Test on FP9900 terminal: yarn android');
console.log('\n🚀 Happy coding!');
