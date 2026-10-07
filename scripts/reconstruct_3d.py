#!/usr/bin/env python3
"""
OptiVision Photoréaliste - Expert 3D Parametric Engine (Wireframe & Bevel)
Génère des lunettes avec montures tubulaires 3D lisses (pour un rendu métal haut de gamme)
et des verres optiques galbés (Base 4-6 curvature) pour des reflets parfaits.
"""

import os
import sys
import json
import math
import argparse
import numpy as np

try:
    import trimesh
    import trimesh.transformations as tf
    from trimesh.visual.material import PBRMaterial
except ImportError:
    os.system("pip install trimesh numpy scipy Pillow")
    import trimesh
    import trimesh.transformations as tf
    from trimesh.visual.material import PBRMaterial


def generate_rim_contour(shape_type, width_m, height_m, num_points=64):
    """Génère un profil 2D haute résolution pour les verres."""
    angles = np.linspace(0, 2 * np.pi, num_points, endpoint=False)
    pts = []
    st = (shape_type or "CARRE").upper()

    rx = width_m / 2.0
    ry = height_m / 2.0

    for a in angles:
        ca, sa = math.cos(a), math.sin(a)
        if "ROND" in st or "ROUND" in st:
            x, y = rx * ca, ry * sa
        elif "AVIAT" in st:
            x = rx * math.copysign(abs(ca)**0.8, ca)
            y = ry * sa * (1.2 - 0.3 * math.sin(a)) if sa < 0 else ry * sa
        elif "CAT" in st or "PAPILLON" in st:
            x = rx * ca
            y = ry * sa + (ry * 0.25 * (ca**2) if sa > 0 and ca > 0 else 0)
        else: # RECTANGLE / CARRE
            n = 0.35 if "RECTANGLE" in st else 0.45
            x = rx * math.copysign(abs(ca)**n, ca)
            y = ry * math.copysign(abs(sa)**n, sa)
        pts.append([x, y, 0])

    return np.array(pts)


def create_tubular_rim(pts, thickness, material):
    """Crée une monture tubulaire 3D parfaite par concaténation de cylindres et sphères (Rendu Métal/Acier Luxe)."""
    meshes = []
    n = len(pts)
    for i in range(n):
        p1 = pts[i]
        p2 = pts[(i+1)%n]
        segment = p2 - p1
        length = np.linalg.norm(segment)
        if length < 1e-6: continue
        
        # Cylindre de liaison
        cyl = trimesh.creation.cylinder(radius=thickness/2, height=length, sections=16)
        
        z_axis = np.array([0,0,1])
        dir_vec = segment / length
        cross = np.cross(z_axis, dir_vec)
        angle = np.arccos(np.dot(z_axis, dir_vec))
        if np.linalg.norm(cross) > 1e-6:
            axis = cross / np.linalg.norm(cross)
            rot = trimesh.transformations.rotation_matrix(angle, axis)
            cyl.apply_transform(rot)
        elif dir_vec[2] < 0:
            cyl.apply_transform(trimesh.transformations.rotation_matrix(np.pi, [1,0,0]))
            
        cyl.apply_translation((p1 + p2)/2.0)
        meshes.append(cyl)
        
        # Sphère aux articulations pour un lissage parfait
        sph = trimesh.creation.icosphere(radius=thickness/2, subdivisions=2)
        sph.apply_translation(p1)
        meshes.append(sph)
    
    mesh = trimesh.util.concatenate(meshes)
    mesh.visual.material = material
    return mesh


def create_lens_mesh(pts, material, curvature=5.0):
    """Crée un verre optique avec courbure réaliste (Base Curve) pour générer de vrais reflets HDR."""
    n = len(pts)
    vertices = []
    
    # Centre avec courbure extérieure (Z positif)
    max_r = max(np.linalg.norm(p[:2]) for p in pts)
    z_center_front = curvature * (max_r ** 2)
    z_center_back = z_center_front - 0.001 # Épaisseur du verre
    
    vertices.append([0.0, 0.0, z_center_front])
    vertices.append([0.0, 0.0, z_center_back])
    
    for p in pts:
        x, y = p[0], p[1]
        z = curvature * (x**2 + y**2)
        vertices.append([x, y, z])
        vertices.append([x, y, z - 0.001])

    faces = []
    for i in range(1, n + 1):
        idx_front = 2 + (i - 1) * 2
        idx_back = idx_front + 1
        next_i = (i % n) + 1
        idx_next_front = 2 + (next_i - 1) * 2
        idx_next_back = idx_next_front + 1
        
        # Faces avant
        faces.append([0, idx_front, idx_next_front])
        # Faces arrière
        faces.append([1, idx_next_back, idx_back])

    mesh = trimesh.Trimesh(vertices=np.array(vertices), faces=np.array(faces))
    mesh.fix_normals()
    mesh.visual.material = material
    return mesh


def create_temple_mesh(length, material, frame_thickness):
    """Branches profilées et tubulaires au niveau des charnières."""
    arm_w = frame_thickness
    arm_l = length * 0.70
    
    meshes = []
    # segment principal
    cyl = trimesh.creation.cylinder(radius=arm_w/2, height=arm_l, sections=16)
    cyl.apply_transform(tf.rotation_matrix(np.pi/2, [1,0,0]))
    cyl.apply_translation([0, 0, -arm_l/2.0])
    meshes.append(cyl)
    
    # drop ear (spatule)
    tip_l = length * 0.30
    tip = trimesh.creation.cylinder(radius=arm_w/2, height=tip_l, sections=16)
    tip.apply_transform(tf.rotation_matrix(np.pi/2, [1,0,0]))
    tip.apply_transform(tf.rotation_matrix(np.radians(-30.0), [1,0,0]))
    tip.apply_translation([0, -0.005, -arm_l - tip_l/2.0 + 0.005])
    meshes.append(tip)
    
    mesh = trimesh.util.concatenate(meshes)
    mesh.visual.material = material
    return mesh


def extract_colors_from_image(img_path):
    """Extrait la couleur de la monture et l'applique."""
    if not img_path or not os.path.exists(img_path):
        return None, None
    try:
        from PIL import Image
        img = Image.open(img_path).convert("RGBA")
        img.thumbnail((150, 150))
        arr = np.array(img)
        mask = (arr[:, :, 3] > 50) & ~((arr[:, :, 0] > 230) & (arr[:, :, 1] > 230) & (arr[:, :, 2] > 230))
        valid_pixels = arr[mask]
        if len(valid_pixels) > 0:
            frame_color = np.median(valid_pixels[:, :3], axis=0).astype(int)
            lens_color = [31, 51, 40, 160] # Vert profond
            return [int(frame_color[0]), int(frame_color[1]), int(frame_color[2]), 255], lens_color
    except Exception as e:
        print(f"[OptiVision Engine] Image extraction note: {e}")
    return None, None


def apply_materials(extracted_frame_color=None, extracted_lens_color=None, frame_mat_type="METAL"):
    """Définit les paramètres PBR Physically Based Rendering."""
    if extracted_frame_color:
        base_color = extracted_frame_color
        r, g, b = base_color[0], base_color[1], base_color[2]
        is_metallic = (r > 180 and g > 140 and b < 110) or (r > 170 and g > 170 and b > 170)
    else:
        base_color = [212, 175, 55, 255] # Or par défaut
        is_metallic = True

    metallic = 0.95 if is_metallic or frame_mat_type == "METAL" else 0.1
    roughness = 0.2 if is_metallic or frame_mat_type == "METAL" else 0.4

    frame_material = PBRMaterial(
        name="FramePBR",
        baseColorFactor=base_color,
        metallicFactor=metallic,
        roughnessFactor=roughness,
        alphaMode="OPAQUE"
    )

    lens_color = extracted_lens_color if extracted_lens_color else [31, 51, 40, 180]
    glass_mat = PBRMaterial(
        name="LensesPBR",
        baseColorFactor=lens_color,
        metallicFactor=0.1,
        roughnessFactor=0.02,
        alphaMode="BLEND",
        doubleSided=True
    )
    
    return frame_material, glass_mat


def build_expert_eyewear(shape_type, lens_w, bridge_w, temple_l, img_path=None):
    """Construit un modèle de lunettes très haute-fidélité."""
    scene = trimesh.Scene()
    
    w_m = lens_w / 1000.0
    h_m = (lens_w * 0.85) / 1000.0
    b_m = bridge_w / 1000.0
    t_m = temple_l / 1000.0
    
    st = (shape_type or "CARRE").upper()
    is_metal = "AVIAT" in st or "ROND" in st
    thickness = 0.0025 if is_metal else 0.0045
    
    frame_color, lens_color = extract_colors_from_image(img_path)
    frame_mat, glass_mat = apply_materials(frame_color, lens_color, "METAL" if is_metal else "ACETATE")
    
    pts = generate_rim_contour(shape_type, w_m, h_m)
    center_offset = (w_m / 2.0) + (b_m / 2.0)
    
    for side, sign in [("Left", -1), ("Right", 1)]:
        # 1. MONTURE TUBULAIRE HAUTE RÉSOLUTION
        rim = create_tubular_rim(pts, thickness, frame_mat)
        rot_yaw = tf.rotation_matrix(sign * np.radians(4.0), [0, 1, 0])
        rim.apply_transform(rot_yaw)
        rim.apply_translation([sign * center_offset, 0, 0])
        scene.add_geometry(rim, node_name=f"Frame_Rim_{side}")
        
        # 2. VERRE GALBÉ (BASE CURVE)
        lens = create_lens_mesh(pts, glass_mat, curvature=3.5 if is_metal else 2.0)
        lens.apply_transform(rot_yaw)
        lens.apply_translation([sign * center_offset, 0, 0.0005])
        scene.add_geometry(lens, node_name=f"Lens_{side}")
        
        # 3. BRANCHES
        temple = create_temple_mesh(t_m, frame_mat, thickness)
        hinge_x = sign * (center_offset + w_m / 2.0 + thickness / 2.0)
        temple_rot = tf.rotation_matrix(sign * np.radians(-2.5), [0, 1, 0])
        temple.apply_transform(temple_rot)
        temple.apply_translation([hinge_x, h_m / 4.0, 0])
        scene.add_geometry(temple, node_name=f"Frame_Temple_{side}")

    # 4. PONT TUBULAIRE
    bridge = trimesh.creation.cylinder(radius=thickness/2 * 0.8, height=b_m * 1.2, sections=16)
    bridge.apply_transform(tf.rotation_matrix(np.pi / 2, [0, 1, 0]))
    bridge.apply_translation([0, h_m / 6.0, 0.001])
    bridge.visual.material = frame_mat
    scene.add_geometry(bridge, node_name="Frame_Bridge")

    if "AVIAT" in st:
        top_bar = trimesh.creation.cylinder(radius=thickness/2 * 0.8, height=b_m * 1.5, sections=16)
        top_bar.apply_transform(tf.rotation_matrix(np.pi / 2, [0, 1, 0]))
        top_bar.apply_translation([0, h_m / 2.2, 0.001])
        top_bar.visual.material = frame_mat
        scene.add_geometry(top_bar, node_name="Frame_TopBar")

    return scene


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", help="Image input path")
    parser.add_argument("--output", required=True)
    parser.add_argument("--shape", default="CARRE")
    parser.add_argument("--lens-width", type=int, default=53)
    parser.add_argument("--bridge-width", type=int, default=20)
    parser.add_argument("--temple-length", type=int, default=145)

    args = parser.parse_args()
    
    print(f"[OptiVision Engine] Rendu Photoréaliste... -> {args.output}")

    scene = build_expert_eyewear(
        shape_type=args.shape,
        lens_w=args.lens_width, 
        bridge_w=args.bridge_width, 
        temple_l=args.temple_length, 
        img_path=args.input
    )

    glb_data = scene.export(file_type="glb")
    with open(args.output, "wb") as f:
        f.write(glb_data)

    report_data = {
        "outputGlb": args.output,
        "statusDetails": "Modèle Wireframe Tubulaire HD + Verres Galbés"
    }
    with open(args.output + ".json", "w") as rf:
        json.dump(report_data, rf)

    print("[OptiVision Engine] Terminé !")

if __name__ == "__main__":
    main()
