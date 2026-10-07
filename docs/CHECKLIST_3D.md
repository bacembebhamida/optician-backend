# Checklist Qualité Modèles 3D Lunettes (OptiVision)

Chaque modèle 3D GLB doit valider cette grille de contrôle avant publication sur le catalogue public.

---

## 📋 Grille d'Évaluation Qualité (Pass/Fail)

| Critère | Tolérance / Objectif | Statut | Méthode de vérification |
| :--- | :--- | :--- | :--- |
| **1. Cotations métriques** | Conforme à **±0.5 mm** des mesures réelles DB (`52□18-140`) | [ ] PASS | Blender (Panneau N -> Dimensions) vs Mesures au pied à coulisse |
| **2. Superposition Silhouette** | Écart visuel invisible (< 1px) sur photos orthogonales | [ ] PASS | Superposition photo 2D (50% opacité) en vue face/profil/dessus |
| **3. Fidélité des Couleurs PBR** | Aucun reflet "peint" (*baked reflection*) dans la Base Color | [ ] PASS | Inspection texture dans Blender sous Khronos PBR Neutral |
| **4. Transparence des Verres** | Verres transparents (IOR 1.5, Transmission 1) sans taches noires | [ ] PASS | Test d'éclairage 360° dans `<model-viewer>` |
| **5. Intégrité Géométrique 360°** | Aucun trou, maillage fermé, pas de collisions entre pièces | [ ] PASS | Orbiting complet à 360° |
| **6. Poids Fichier & Optimisation** | **< 5 Mo**, 30k–60k triangles, Draco + WebP | [ ] PASS | Execution de `./scripts/optimize_glb.sh` |
| **7. Fluidité Mobile** | ≥ 60 FPS sur smartphone milieu de gamme | [ ] PASS | Test Chrome DevTools / Mobile device audit |

---

## 🚀 Procédure de Validation Rapide

```bash
# 1. Vérifier la taille et la compression
./scripts/optimize_glb.sh monture_raw.glb monture_web.glb

# 2. Vérifier les en-têtes HTTP Spring Boot
curl -I http://localhost:8080/uploads/models/monture_web.glb
# Doit retourner: Cache-Control: public, max-age=31536000, immutable
```
