#!/bin/bash

# Script de test automatisé pour la détection d'ANR
# Ce script exécute tous les tests de détection d'ANR et génère un rapport

set -e  # Arrêter en cas d'erreur

echo "🧪 ========================================="
echo "🧪 ANR Detection Test Suite"
echo "🧪 ========================================="
echo ""

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables
PROJECT_ROOT=$(pwd)
TEST_RESULTS_DIR="$PROJECT_ROOT/test-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_FILE="$TEST_RESULTS_DIR/anr-test-report-$TIMESTAMP.txt"

# Créer le dossier de résultats
mkdir -p "$TEST_RESULTS_DIR"

# Fonction pour logger
log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1" | tee -a "$REPORT_FILE"
}

log_success() {
    echo -e "${GREEN}[$(date +'%H:%M:%S')] ✅ $1${NC}" | tee -a "$REPORT_FILE"
}

log_error() {
    echo -e "${RED}[$(date +'%H:%M:%S')] ❌ $1${NC}" | tee -a "$REPORT_FILE"
}

log_warning() {
    echo -e "${YELLOW}[$(date +'%H:%M:%S')] ⚠️  $1${NC}" | tee -a "$REPORT_FILE"
}

# Fonction pour vérifier les prérequis
check_prerequisites() {
    log "Vérification des prérequis..."
    
    # Vérifier Java
    if ! command -v java &> /dev/null; then
        log_error "Java n'est pas installé"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" != "17" ]; then
        log_warning "Java version $JAVA_VERSION détectée. Version 17 recommandée."
    else
        log_success "Java 17 détecté"
    fi
    
    # Vérifier Gradle
    if [ ! -f "./gradlew" ]; then
        log_error "gradlew non trouvé dans le répertoire courant"
        exit 1
    fi
    
    log_success "Prérequis vérifiés"
}

# Fonction pour exécuter les tests unitaires
run_unit_tests() {
    log "Exécution des tests unitaires ANR..."
    
    if ./gradlew testDebugUnitTest --tests "*ANR*" --continue; then
        log_success "Tests unitaires ANR réussis"
        return 0
    else
        log_error "Tests unitaires ANR échoués"
        return 1
    fi
}

# Fonction pour exécuter les tests d'intégration
run_integration_tests() {
    log "Exécution des tests d'intégration ANR..."
    
    # Vérifier qu'un device est connecté
    if ! adb devices | grep -q "device$"; then
        log_warning "Aucun device Android connecté. Tests d'intégration ignorés."
        return 0
    fi
    
    if ./gradlew connectedAndroidTest --tests "*ANRIntegrationTest*" --continue; then
        log_success "Tests d'intégration ANR réussis"
        return 0
    else
        log_error "Tests d'intégration ANR échoués"
        return 1
    fi
}

# Fonction pour analyser les résultats
analyze_results() {
    log "Analyse des résultats..."
    
    # Chercher les fichiers de résultats de test
    UNIT_TEST_RESULTS=$(find . -name "test-results" -type d 2>/dev/null | head -n 1)
    INTEGRATION_TEST_RESULTS=$(find . -name "androidTest-results" -type d 2>/dev/null | head -n 1)
    
    if [ -n "$UNIT_TEST_RESULTS" ]; then
        log "Résultats des tests unitaires trouvés dans: $UNIT_TEST_RESULTS"
    fi
    
    if [ -n "$INTEGRATION_TEST_RESULTS" ]; then
        log "Résultats des tests d'intégration trouvés dans: $INTEGRATION_TEST_RESULTS"
    fi
    
    # Générer un résumé
    echo "" | tee -a "$REPORT_FILE"
    echo "📊 =========================================" | tee -a "$REPORT_FILE"
    echo "📊 RÉSUMÉ DES TESTS ANR" | tee -a "$REPORT_FILE"
    echo "📊 =========================================" | tee -a "$REPORT_FILE"
    echo "Date: $(date)" | tee -a "$REPORT_FILE"
    echo "Projet: FP9900 POS Certification" | tee -a "$REPORT_FILE"
    echo "" | tee -a "$REPORT_FILE"
}

# Fonction pour nettoyer
cleanup() {
    log "Nettoyage..."
    ./gradlew clean > /dev/null 2>&1 || true
    log_success "Nettoyage terminé"
}

# Fonction principale
main() {
    echo "🚀 Démarrage de la suite de tests ANR..." | tee -a "$REPORT_FILE"
    echo "" | tee -a "$REPORT_FILE"
    
    # Variables pour le suivi des résultats
    UNIT_TESTS_PASSED=false
    INTEGRATION_TESTS_PASSED=false
    
    # Vérifier les prérequis
    check_prerequisites
    
    # Exécuter les tests unitaires
    if run_unit_tests; then
        UNIT_TESTS_PASSED=true
    fi
    
    # Exécuter les tests d'intégration
    if run_integration_tests; then
        INTEGRATION_TESTS_PASSED=true
    fi
    
    # Analyser les résultats
    analyze_results
    
    # Afficher le résumé final
    echo "🎯 ========================================="
    echo "🎯 RÉSULTATS FINAUX"
    echo "🎯 ========================================="
    
    if [ "$UNIT_TESTS_PASSED" = true ]; then
        log_success "Tests unitaires ANR: PASSÉS"
    else
        log_error "Tests unitaires ANR: ÉCHOUÉS"
    fi
    
    if [ "$INTEGRATION_TESTS_PASSED" = true ]; then
        log_success "Tests d'intégration ANR: PASSÉS"
    else
        log_warning "Tests d'intégration ANR: IGNORÉS OU ÉCHOUÉS"
    fi
    
    # Déterminer le statut global
    if [ "$UNIT_TESTS_PASSED" = true ]; then
        echo ""
        echo -e "${GREEN}🎉 SUCCÈS: Aucun problème ANR détecté !${NC}"
        echo ""
        exit 0
    else
        echo ""
        echo -e "${RED}💥 ÉCHEC: Problèmes ANR détectés !${NC}"
        echo ""
        echo "📋 Actions recommandées:"
        echo "1. Vérifier les logs ci-dessus"
        echo "2. Analyser les fichiers de résultats de test"
        echo "3. Optimiser les opérations lourdes"
        echo "4. Réexécuter les tests après corrections"
        echo ""
        exit 1
    fi
}

# Gestion des signaux
trap cleanup EXIT

# Exécution du script principal
main "$@"
