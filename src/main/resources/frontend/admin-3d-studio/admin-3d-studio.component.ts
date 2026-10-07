import {
  Component,
  OnInit,
  Input,
  CUSTOM_ELEMENTS_SCHEMA,
  ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import '@google/model-viewer';

export interface QualityScores {
  overall: number;
  geometry: number;
  dimensions: number;
  materials: number;
  textures: number;
  symmetry: number;
  visualSimilarity: number;
  vtoFit: number;
}

export interface AssetVersion {
  id: number;
  version: number;
  modelUrl: string;
  format: string;
  status: 'DRAFT' | 'GENERATED' | 'NEEDS_REVIEW' | 'VALIDATED' | 'PUBLISHED' | 'REJECTED';
  qualityScore: number;
  createdAt: string;
}

@Component({
  selector: 'app-admin-3d-studio',
  standalone: true,
  imports: [CommonModule, FormsModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  changeDetectionStrategy: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-3d-studio.component.html',
  styleUrls: ['./admin-3d-studio.component.css']
})
export class Admin3dStudioComponent implements OnInit {
  @Input() variantId: string = '1';
  @Input() productName: string = 'Ray-Ban Wayfarer Classic';
  @Input() variantSku: string = 'RB2140-901-52';

  // 3D Quality Gate Status
  assetStatus: 'DRAFT' | 'GENERATED' | 'NEEDS_REVIEW' | 'VALIDATED' | 'PUBLISHED' | 'REJECTED' = 'NEEDS_REVIEW';

  // Quality Score Breakdown (/100)
  scores: QualityScores = {
    overall: 92,
    geometry: 94,
    dimensions: 96,
    materials: 92,
    textures: 88,
    symmetry: 98,
    visualSimilarity: 90,
    vtoFit: 92
  };

  // Dimensions physiques réelles
  lensWidth: number = 52;
  bridgeWidth: number = 18;
  templeLength: number = 140;
  totalWidth: number = 138;
  lensHeight: number = 42;

  // Calibration 3D Sliders
  scale: number = 1.0;
  positionX: number = 0.0;
  positionY: number = 0.0;
  positionZ: number = 0.0;
  rotationX: number = 0.0;
  rotationY: number = 0.0;
  rotationZ: number = 0.0;

  // Renders de comparaison visuelle (Original vs 3D)
  activeView: 'front' | 'three_quarter' | 'side' | 'back' = 'front';
  modelUrl: string = '/uploads/models/eyewear_3d_p1.glb';

  // Enregistrement des versions (v1, v2, v3)
  versions: AssetVersion[] = [
    { id: 3, version: 3, modelUrl: '/uploads/models/eyewear_3d_p1_v3.glb', format: 'GLB', status: 'NEEDS_REVIEW', qualityScore: 92, createdAt: '2026-10-07 10:40' },
    { id: 2, version: 2, modelUrl: '/uploads/models/eyewear_3d_p1_v2.glb', format: 'GLB', status: 'REJECTED', qualityScore: 68, createdAt: '2026-10-06 14:15' },
    { id: 1, version: 1, modelUrl: '/uploads/models/eyewear_3d_p1_v1.glb', format: 'GLB', status: 'DRAFT', qualityScore: 60, createdAt: '2026-10-05 09:30' }
  ];

  isSaving: boolean = false;
  statusMessage: string | null = null;

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {}

  /** Définir la vue de comparaison visuelle */
  setActiveView(view: 'front' | 'three_quarter' | 'side' | 'back'): void {
    this.activeView = view;
    this.cdr.markForCheck();
  }

  /** Appliquer les ajustements de calibration */
  saveCalibration(): void {
    this.isSaving = true;
    this.statusMessage = 'Enregistrement de la calibration 3D...';
    this.cdr.markForCheck();

    setTimeout(() => {
      this.isSaving = false;
      this.statusMessage = '✅ Calibration 3D enregistrée avec succès.';
      this.cdr.markForCheck();
    }, 800);
  }

  /** Valider la monture 3D */
  validateAsset(): void {
    this.assetStatus = 'VALIDATED';
    this.statusMessage = '✅ Modèle 3D validé par le contrôle qualité.';
    this.cdr.markForCheck();
  }

  /** Publier le modèle 3D sur le site public client */
  publishAsset(): void {
    this.assetStatus = 'PUBLISHED';
    this.statusMessage = '🚀 Modèle 3D publié et actif pour l\'essayage virtuel client !';
    this.cdr.markForCheck();
  }

  /** Rejeter le modèle 3D */
  rejectAsset(): void {
    const reason = prompt('Raison du rejet du modèle 3D :', 'Géométrie non conforme sur les branches.');
    if (reason) {
      this.assetStatus = 'REJECTED';
      this.statusMessage = `❌ Modèle 3D rejeté : ${reason}`;
      this.cdr.markForCheck();
    }
  }

  /** Relancer la génération 3D */
  regenerate3d(): void {
    this.statusMessage = '⚡ Régénération 3D initiée (Moteur Paramétrique Eyewear Engine)...';
    this.cdr.markForCheck();
  }

  /** Sélectionner une version historique */
  selectVersion(v: AssetVersion): void {
    this.modelUrl = v.modelUrl;
    this.assetStatus = v.status;
    this.scores.overall = v.qualityScore;
    this.cdr.markForCheck();
  }
}
