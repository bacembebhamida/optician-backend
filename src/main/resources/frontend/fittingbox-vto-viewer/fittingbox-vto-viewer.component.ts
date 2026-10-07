import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  ElementRef,
  ViewChild,
  CUSTOM_ELEMENTS_SCHEMA,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  AfterViewInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import '@google/model-viewer';

// We dynamically load Three.js and MediaPipe via typical Angular module paths.
// The user should ensure `npm install three @mediapipe/tasks-vision` is run.
import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { FaceLandmarker, FilesetResolver, DrawingUtils } from '@mediapipe/tasks-vision';

export interface FrameVariant {
  id: number;
  colorName: string;
  colorHex: string;
  material: 'acetate' | 'metal' | 'titanium' | 'crystal';
  modelUrl: string;
  thumbnailUrl: string;
  price: number;
}

export interface FrameDimensions {
  lensWidth: number;
  bridgeWidth: number;
  templeLength: number;
  totalWidth: number;
  lensHeight: number;
  fitCategory: 'S' | 'M' | 'L';
}

@Component({
  selector: 'app-fittingbox-vto-viewer',
  standalone: true,
  imports: [CommonModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  changeDetectionStrategy: ChangeDetectionStrategy.OnPush,
  templateUrl: './fittingbox-vto-viewer.component.html',
  styleUrls: ['./fittingbox-vto-viewer.component.css']
})
export class FittingboxVtoViewerComponent implements OnInit, AfterViewInit, OnDestroy {
  activeMode: 'studio3d' | 'livevto' = 'studio3d';

  @Input() selectedVariant!: FrameVariant;
  @Input() variants: FrameVariant[] = [];
  @Input() dimensions: FrameDimensions = {
    lensWidth: 52, bridgeWidth: 18, templeLength: 140, totalWidth: 138, lensHeight: 42, fitCategory: 'M'
  };
  @Input() productName: string = 'LUNETTES PRO';
  @Input() brandName: string = 'OptiVision';

  @ViewChild('videoElement') videoElement!: ElementRef<HTMLVideoElement>;
  @ViewChild('vtoCanvas') canvasElement!: ElementRef<HTMLCanvasElement>;

  isCameraActive: boolean = false;
  isCameraLoading: boolean = false;
  cameraError: string | null = null;
  activeModelFace: string | null = null;

  demoFaces = [
    { id: 'female_1', name: 'Sophie', url: 'assets/vto/models/face_female_1.jpg' }
  ];

  scaleFactor: number = 1.0;
  verticalOffset: number = 0;
  horizontalOffset: number = 0;

  private mediaStream: MediaStream | null = null;
  
  // MediaPipe & Three.js Core
  private faceLandmarker!: FaceLandmarker;
  private renderer!: THREE.WebGLRenderer;
  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private eyewearModelGroup: THREE.Group | null = null;
  private animationFrameId: number | null = null;
  private isProcessingVideo: boolean = false;

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    if (!this.selectedVariant && this.variants.length > 0) {
      this.selectedVariant = this.variants[0];
    }
  }

  ngAfterViewInit(): void {
    // Pre-load MediaPipe tasks vision
    this.initMediaPipe();
  }

  ngOnDestroy(): void {
    this.stopCamera();
    if (this.animationFrameId) cancelAnimationFrame(this.animationFrameId);
    if (this.renderer) this.renderer.dispose();
  }

  async initMediaPipe() {
    try {
      const vision = await FilesetResolver.forVisionTasks(
        "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm"
      );
      this.faceLandmarker = await FaceLandmarker.createFromOptions(vision, {
        baseOptions: {
          modelAssetPath: "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task",
          delegate: "GPU"
        },
        outputFaceBlendshapes: true,
        runningMode: "VIDEO",
        numFaces: 1
      });
      console.log("MediaPipe FaceLandmarker Loaded Successfully");
    } catch (err) {
      console.error("Error loading MediaPipe:", err);
    }
  }

  initThreeJS() {
    if (this.renderer) return;

    const canvas = this.canvasElement.nativeElement;
    this.renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
    this.renderer.setSize(canvas.clientWidth, canvas.clientHeight);
    this.renderer.setPixelRatio(window.devicePixelRatio);

    this.scene = new THREE.Scene();
    
    // Virtual Camera setup matching webcam roughly
    this.camera = new THREE.PerspectiveCamera(45, canvas.clientWidth / canvas.clientHeight, 0.1, 1000);
    this.camera.position.z = 10;

    // Lighting (PBR Environment simulation)
    const ambientLight = new THREE.AmbientLight(0xffffff, 1.2);
    this.scene.add(ambientLight);
    const dirLight = new THREE.DirectionalLight(0xffffff, 1.5);
    dirLight.position.set(0, 5, 10);
    this.scene.add(dirLight);

    this.loadEyewearModel(this.selectedVariant?.modelUrl || '/uploads/models/procedural_rectangle.glb');
  }

  loadEyewearModel(url: string) {
    if (this.eyewearModelGroup) {
      this.scene.remove(this.eyewearModelGroup);
    }

    const loader = new GLTFLoader();
    loader.load(url, (gltf) => {
      this.eyewearModelGroup = gltf.scene;
      // Pre-scale based on physical DB dimension vs virtual units
      const baseScale = (this.dimensions.totalWidth / 1000) * 8.5; // Ajustement relatif au fov
      this.eyewearModelGroup.scale.set(baseScale, baseScale, baseScale);
      
      // Pivot à la racine du nez (pont)
      const box = new THREE.Box3().setFromObject(this.eyewearModelGroup);
      const center = box.getCenter(new THREE.Vector3());
      this.eyewearModelGroup.position.x += (this.eyewearModelGroup.position.x - center.x);
      this.eyewearModelGroup.position.y += (this.eyewearModelGroup.position.y - center.y);
      this.eyewearModelGroup.position.z += (this.eyewearModelGroup.position.z - center.z);
      
      this.scene.add(this.eyewearModelGroup);
      this.cdr.markForCheck();
    });
  }

  setMode(mode: 'studio3d' | 'livevto'): void {
    this.activeMode = mode;
    if (mode === 'livevto') {
      this.startCamera();
    } else {
      this.stopCamera();
    }
    this.cdr.markForCheck();
  }

  selectVariant(variant: FrameVariant): void {
    this.selectedVariant = variant;
    if (this.activeMode === 'livevto' && this.isCameraActive) {
       this.loadEyewearModel(variant.modelUrl);
    }
    this.cdr.markForCheck();
  }

  async startCamera(): Promise<void> {
    this.isCameraLoading = true;
    this.cameraError = null;
    this.activeModelFace = null;
    this.cdr.markForCheck();

    try {
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { width: { ideal: 1280 }, height: { ideal: 720 }, facingMode: 'user' }
      });

      const video = this.videoElement.nativeElement;
      video.srcObject = this.mediaStream;
      await video.play();
      
      this.isCameraActive = true;
      this.isCameraLoading = false;
      this.cdr.markForCheck();

      // Start 3D & Tracking Loop
      setTimeout(() => {
        this.initThreeJS();
        this.isProcessingVideo = true;
        this.predictWebcam();
      }, 500);

    } catch (err: any) {
      console.warn('Camera Error:', err);
      this.isCameraLoading = false;
      this.isCameraActive = false;
      this.cameraError = 'Accès caméra refusé. Utilisez le mode Studio 360.';
      this.cdr.markForCheck();
    }
  }

  stopCamera(): void {
    this.isProcessingVideo = false;
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(track => track.stop());
      this.mediaStream = null;
    }
    this.isCameraActive = false;
  }

  /**
   * MediaPipe -> Three.js Core Tracking Pipeline
   */
  async predictWebcam() {
    if (!this.isProcessingVideo) return;
    
    const video = this.videoElement.nativeElement;
    let startTimeMs = performance.now();
    
    if (this.faceLandmarker && video.currentTime > 0) {
      const results = this.faceLandmarker.detectForVideo(video, startTimeMs);
      
      if (results.faceLandmarks && results.faceLandmarks.length > 0 && this.eyewearModelGroup) {
        const landmarks = results.faceLandmarks[0];
        
        // MediaPipe Nose bridge landmark is usually index 168
        const noseBridge = landmarks[168];
        const leftEyeOuter = landmarks[33];
        const rightEyeOuter = landmarks[263];

        // 1. Calculate Positioning (Mapping normalized [0,1] to Three.js Virtual Space [-5, 5])
        // Le repère Three.js a x vers la droite, y vers le haut. Vidéo est inversée en miroir.
        const aspect = video.videoWidth / video.videoHeight;
        const vX = (0.5 - noseBridge.x) * 10 * aspect; 
        const vY = -(noseBridge.y - 0.5) * 10;
        const vZ = -noseBridge.z * 10; // depth

        this.eyewearModelGroup.position.set(
          vX + this.horizontalOffset, 
          vY + this.verticalOffset, 
          vZ + 2 // Offset vers l'avant du nez
        );

        // 2. Calculate Rotation (Yaw, Pitch, Roll)
        // Vector pointing from left eye to right eye
        const dX = rightEyeOuter.x - leftEyeOuter.x;
        const dY = rightEyeOuter.y - leftEyeOuter.y;
        const roll = Math.atan2(dY, dX);
        const yaw = Math.atan2(noseBridge.z - leftEyeOuter.z, leftEyeOuter.x - noseBridge.x);
        
        // Very basic pitch based on z-depth gradient of face
        const pitch = Math.atan2(landmarks[152].z - noseBridge.z, landmarks[152].y - noseBridge.y);

        this.eyewearModelGroup.rotation.set(pitch + 0.1, -yaw, -roll);

        // 3. User Scaling adjustment
        const s = this.scaleFactor * ((this.dimensions.totalWidth / 1000) * 8.5);
        this.eyewearModelGroup.scale.set(s, s, s);
      }
      
      // Render the Three.js GLB over the video
      this.renderer.render(this.scene, this.camera);
    }

    this.animationFrameId = requestAnimationFrame(() => this.predictWebcam());
  }

  resetAdjustments(): void {
    this.scaleFactor = 1.0;
    this.verticalOffset = 0;
    this.horizontalOffset = 0;
    this.cdr.markForCheck();
  }
}
