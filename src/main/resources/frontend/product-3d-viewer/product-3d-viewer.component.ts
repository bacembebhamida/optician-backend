import {
  Component,
  Input,
  OnInit,
  CUSTOM_ELEMENTS_SCHEMA,
  ChangeDetectionStrategy,
  ElementRef,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';

// Importer le package @google/model-viewer pour déclarer l'élément personnalisé web
import '@google/model-viewer';

@Component({
  selector: 'app-product-3d-viewer',
  standalone: true,
  imports: [CommonModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  changeDetectionStrategy: ChangeDetectionStrategy.OnPush,
  templateUrl: './product-3d-viewer.component.html',
  styleUrls: ['./product-3d-viewer.component.css']
})
export class Product3dViewerComponent implements OnInit {
  /** URL du fichier .glb optimisé (Servi par Spring Boot avec Cache-Control) */
  @Input({ required: true }) modelUrl!: String;

  /** Image 2D de prévisualisation (Poster) affichée avant l'interaction / chargement du 3D */
  @Input() posterUrl: string = 'assets/images/placeholder-glasses.jpg';

  /** Description d'accessibilité (SEO / Screen Readers) */
  @Input() altText: string = 'Modèle 3D 360° de la monture optique';

  /** Cotations métriques réelles (mm) pour l'affichage de la fiche technique */
  @Input() lensWidth?: number;
  @Input() bridgeWidth?: number;
  @Input() templeLength?: number;

  @ViewChild('modelViewerRef') modelViewerRef!: ElementRef;

  isLoading: boolean = true;
  isLowEndDevice: boolean = false;
  hasError: boolean = false;

  ngOnInit(): void {
    this.checkDeviceCapability();
  }

  onModelLoaded(): void {
    this.isLoading = false;
    // Si appareil mobile d'entrée de gamme, ajuster le matériau des verres si nécessaire
    if (this.isLowEndDevice && this.modelViewerRef?.nativeElement) {
      this.applyMobileOptimizations();
    }
  }

  onModelError(event: any): void {
    console.error('Erreur lors du chargement du modèle 3D GLB:', event);
    this.isLoading = false;
    this.hasError = true;
  }

  private checkDeviceCapability(): void {
    // Détection sommaire des mobiles d'entrée de gamme (RAM faible ou GPU mobile)
    const nav = navigator as any;
    if (nav.deviceMemory && nav.deviceMemory <= 4) {
      this.isLowEndDevice = true;
    }
  }

  private applyMobileOptimizations(): void {
    const viewer = this.modelViewerRef.nativeElement;
    if (viewer && viewer.model) {
      // Si la transmission cause du lag, ajuster l'alpha du verre
      const lensMaterial = viewer.model.materials.find((m: any) =>
        m.name.toLowerCase().includes('lens') || m.name.toLowerCase().includes('verre')
      );
      if (lensMaterial && lensMaterial.pbrMetallicRoughness) {
        lensMaterial.pbrMetallicRoughness.setBaseColorFactor([0.9, 0.9, 0.95, 0.2]); // Opacité 20%
      }
    }
  }
}
