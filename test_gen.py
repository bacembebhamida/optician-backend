import numpy as np
import trimesh
import math

def create_wireframe_rim(pts, thickness=0.002):
    meshes = []
    n = len(pts)
    for i in range(n):
        p1 = pts[i]
        p2 = pts[(i+1)%n]
        segment = p2 - p1
        length = np.linalg.norm(segment)
        if length < 1e-6: continue
        
        # cylinder along Z, we need to rotate it to align with segment
        cyl = trimesh.creation.cylinder(radius=thickness/2, height=length, sections=12)
        
        # compute rotation axis and angle
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
        
        sph = trimesh.creation.icosphere(radius=thickness/2, subdivisions=1)
        sph.apply_translation(p1)
        meshes.append(sph)
    
    return trimesh.util.concatenate(meshes)

pts = np.array([[math.cos(a)*0.025, math.sin(a)*0.02, 0] for a in np.linspace(0, 2*math.pi, 36, endpoint=False)])
m = create_wireframe_rim(pts)
print("Mesh valid:", m.is_watertight, len(m.faces))
