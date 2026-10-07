#!/usr/bin/env bash
# ==============================================================================
# Script d'optimisation GLB pour Lunettes Optiques (Draco + WebP + Verification)
# Usage: ./scripts/optimize_glb.sh input_model.glb output_model.glb
# ==============================================================================

set -eo pipefail

if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <input_glb> <output_glb>"
    exit 1
fi

INPUT_GLB="$1"
OUTPUT_GLB="$2"
MAX_SIZE_BYTES=$((5 * 1024 * 1024)) # 5 MB threshold

if [ ! -f "$INPUT_GLB" ]; then
    echo "Erreur: Le fichier d'entrée '$INPUT_GLB' n'existe pas."
    exit 1
fi

# Vérifier si gltf-transform est installé
if ! command -v gltf-transform &> /dev/null; then
    echo "Erreur: gltf-transform CLI n'est pas installé."
    echo "Installez-le avec: npm install -g @gltf-transform/cli"
    exit 1
fi

echo "=== Optimisation de $INPUT_GLB ==="
INPUT_SIZE=$(stat -c%s "$INPUT_GLB" 2>/dev/null || stat -f%z "$INPUT_GLB")
echo "Taille initiale: $(awk "BEGIN {printf \"%.2f MB\", $INPUT_SIZE/1048576}")"

# Run gltf-transform optimize command with Draco geometry compression & WebP textures
gltf-transform optimize "$INPUT_GLB" "$OUTPUT_GLB" \
    --compress draco \
    --texture-compress webp

OUTPUT_SIZE=$(stat -c%s "$OUTPUT_GLB" 2>/dev/null || stat -f%z "$OUTPUT_GLB")
echo "Taille optimisée: $(awk "BEGIN {printf \"%.2f MB\", $OUTPUT_SIZE/1048576}")"

# Calculate reduction percentage
REDUCTION=$(awk "BEGIN {printf \"%.1f%%\", (1 - $OUTPUT_SIZE/$INPUT_SIZE)*100}")
echo "Taux de réduction: $REDUCTION"

# Verify 5 MB constraint
if [ "$OUTPUT_SIZE" -gt "$MAX_SIZE_BYTES" ]; then
    echo "❌ ATTENTION: Le fichier optimisé dépasse la limite recommandée de 5 Mo !"
    exit 1
else
    echo "✅ Fichier GLB conforme (< 5 Mo)."
fi
