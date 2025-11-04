#!/bin/bash

# Script pour générer la commande curl exacte basée sur les données de l'app
# Basé sur les valeurs par défaut du formulaire et les données de test

# Variables depuis InvoiceFragment.createInvoiceFromForm()
EXTERNAL_NUM="${1:-Facture20250931}"
MACHINE_NUM="${2:-2335626441}"
ISSUER_NAME="${3:-Pos1 Les Grands Moulins}"
ISSUER_ID="${4:-2510076652}"
CUSTOMER_NAME="${5:-Bemba Thomas}"
CUSTOMER_ID="${6:-40842322909316563708}"

# Date ISO 8601 format (exact format used in app)
ISSUE_DATE=$(date -u +"%Y-%m-%dT%H:%M:%S.%3NZ")

# Calcul des totaux basés sur InvoiceLine:
# Line 1: Article1, quantity=5, unitPrice=10000 (cents), vatRate=5%
#   Total Price = 5 * 10000 = 50000 cents
#   VAT Amount = 50000 * 5 / 100 = 2500 cents
# Line 2: Article2, quantity=2, unitPrice=3500 (cents), vatRate=20%
#   Total Price = 2 * 3500 = 7000 cents
#   VAT Amount = 7000 * 20 / 100 = 1400 cents
# Total HT = 50000 + 7000 = 57000 cents
# Total VAT = 2500 + 1400 = 3900 cents
# Total TTC = 57000 + 3900 = 60900 cents

TOTAL_HT=57000
TOTAL_VAT=3900
TOTAL_TTC=60900

BASE_URL="https://api.fiv.dgi.kpsaccess.com"

echo "=== CURL COMMAND FOR CREATE INVOICE ==="
echo ""
echo "curl -X POST \\"
echo "  '$BASE_URL/api/invoices' \\"
echo "  -H 'Accept: */*' \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{"
echo "    \"externalNum\": \"$EXTERNAL_NUM\","
echo "    \"machineNum\": \"$MACHINE_NUM\","
echo "    \"issuer\": {"
echo "      \"num\": \"$ISSUER_ID\","
echo "      \"name\": \"$ISSUER_NAME\""
echo "    },"
echo "    \"customer\": {"
echo "      \"name\": \"$CUSTOMER_NAME\","
echo "      \"identityNumber\": \"$CUSTOMER_ID\""
echo "    },"
echo "    \"invoiceLines\": ["
echo "      {"
echo "        \"designation\": \"Article1\","
echo "        \"quantity\": 5,"
echo "        \"unitPrice\": 10000,"
echo "        \"vatRate\": 5"
echo "      },"
echo "      {"
echo "        \"designation\": \"Article2\","
echo "        \"quantity\": 2,"
echo "        \"unitPrice\": 3500,"
echo "        \"vatRate\": 20"
echo "      }"
echo "    ],"
echo "    \"totalHt\": $TOTAL_HT,"
echo "    \"totalVat\": $TOTAL_VAT,"
echo "    \"totalTtc\": $TOTAL_TTC,"
echo "    \"issueDate\": \"$ISSUE_DATE\""
echo "  }'"
echo ""
echo "=== FULL CERTIFICATION FLOW ==="
echo ""
echo "# Step 1: Create Invoice"
echo "INVOICE_ID=\$(curl -X POST \\"
echo "  '$BASE_URL/api/invoices' \\"
echo "  -H 'Accept: */*' \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{...}' | jq -r '.invoiceId')"
echo ""
echo "# Step 2: Fiscalize Invoice"
echo "curl -X POST \\"
echo "  '$BASE_URL/api/invoices/'\"\$INVOICE_ID\"\"/fiscalize' \\"
echo "  -H 'Accept: */*'"
echo ""
echo "# Step 3: Verify Invoice"
echo "curl -X GET \\"
echo "  '$BASE_URL/api/invoices/'\"\$INVOICE_ID\" \\"
echo "  -H 'Accept: */*'"
echo ""

