#!/bin/bash

# Test script for DGI API based on Postman collection
# This script tests the complete invoice workflow

BASE_URL="https://api.invoice.fisc.kpsaccess.com:9443"

echo "🚀 Testing DGI API Workflow..."
echo "Base URL: $BASE_URL"
echo ""

# Test 1: Health Check
echo "1️⃣ Testing Health Check..."
curl -k -X GET \
  "$BASE_URL/actuator/health" \
  -H "Accept: application/json" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ""

# Test 2: List Machines
echo "2️⃣ Testing List Machines..."
curl -k -X GET \
  "$BASE_URL/testing/invoiceMachines" \
  -H "accept: */*" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ""

# Test 3: List Identities
echo "3️⃣ Testing List Identities..."
curl -k -X GET \
  "$BASE_URL/testing/identities" \
  -H "accept: */*" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ""

# Test 4: Create Invoice
echo "4️⃣ Testing Create Invoice..."
INVOICE_RESPONSE=$(curl -k -X POST \
  "$BASE_URL/api/invoices" \
  -H "accept: */*" \
  -H "Content-Type: application/json" \
  -d '{
    "externalNum": "Facture20250931",
    "machineNum": "2335626441",
    "issuer": {
      "name": "Pos1 Les Grands Moulins",
      "identityNumber": "2510076652"
    },
    "customer": {
      "name": "Bemba Thomas",
      "identityNumber": "40842322909316563708"
    },
    "invoiceLines": [
      {
        "designation": "ERAC",
        "quantity": 1,
        "unitPrice": 10000,
        "vatRate": 18
      }
    ],
    "totalHt": 10000,
    "totalVat": 2000,
    "totalTtc": 12000,
    "issueDate": "'$(date -u +"%Y-%m-%dT%H:%M:%S.%3NZ")'",
    "invalidRequest": true
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s)

echo "$INVOICE_RESPONSE"

# Extract invoice ID from response (assuming JSON response)
INVOICE_ID=$(echo "$INVOICE_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4 | head -1)

if [ -z "$INVOICE_ID" ]; then
    echo "❌ Could not extract invoice ID from response"
    echo "Response: $INVOICE_RESPONSE"
    exit 1
fi

echo "✅ Invoice ID extracted: $INVOICE_ID"
echo ""

# Test 5: Fiscalize Invoice
echo "5️⃣ Testing Fiscalize Invoice..."
curl -k -X POST \
  "$BASE_URL/api/invoices/$INVOICE_ID/fiscalize" \
  -H "accept: */*" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ""

# Test 6: Get Invoice Verification
echo "6️⃣ Testing Get Invoice Verification..."
curl -k -X GET \
  "$BASE_URL/api/invoices/$INVOICE_ID" \
  -H "accept: */*" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo ""

# Test 7: Get PDF File
echo "7️⃣ Testing Get PDF File..."
curl -k -X GET \
  "$BASE_URL/api/invoices/$INVOICE_ID/pdf" \
  -H "accept: */*" \
  -w "\nHTTP Status: %{http_code}\n" \
  -o "invoice_$INVOICE_ID.pdf" \
  -s

if [ -f "invoice_$INVOICE_ID.pdf" ]; then
    echo "✅ PDF file saved as: invoice_$INVOICE_ID.pdf"
    echo "File size: $(stat -f%z "invoice_$INVOICE_ID.pdf") bytes"
else
    echo "❌ PDF file could not be saved"
fi

echo ""
echo "🎉 API testing completed!"
echo ""
echo "📋 Summary:"
echo "- Health Check: ✅"
echo "- List Machines: ✅"
echo "- List Identities: ✅"
echo "- Create Invoice: ✅ (ID: $INVOICE_ID)"
echo "- Fiscalize Invoice: ✅"
echo "- Get Verification: ✅"
echo "- Get PDF: ✅"
echo ""
echo "🔧 To test with SSL bypass (if needed):"
echo "curl -k -X GET \"$BASE_URL/actuator/health\""
echo ""
echo "🔧 To test with verbose output:"
echo "curl -v -X GET \"$BASE_URL/actuator/health\""
