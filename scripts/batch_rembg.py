#!/usr/bin/env python3
"""
Batch Background Removal Script for Eyewear Photos.
Uses `rembg` (u2net model) to remove backgrounds, then centers and pads
the image onto a 1024x1024 square with a pure white background.

Usage:
    python scripts/batch_rembg.py --input ./raw_photos --output ./processed_photos
"""

import argparse
import os
import sys
from pathlib import Path
from PIL import Image

try:
    from rembg import remove, new_session
except ImportError:
    print("Error: `rembg` library is not installed. Install with: pip install rembg pillow", file=sys.stderr)
    sys.exit(1)


def process_image(input_path: Path, output_path: Path, session, target_size: int = 1024):
    """Remove background and format image to target_size square with white background."""
    print(f"Processing: {input_path.name} ...", end=" ")
    
    with open(input_path, 'rb') as f:
        input_bytes = f.read()

    # 1. Remove background with rembg
    output_bytes = remove(input_bytes, session=session)
    
    # 2. Open RGBA image
    from io import BytesIO
    rgba_img = Image.open(BytesIO(output_bytes)).convert("RGBA")

    # 3. Crop bounding box of non-transparent content
    bbox = rgba_img.getbbox()
    if bbox:
        cropped = rgba_img.crop(bbox)
    else:
        cropped = rgba_img

    # 4. Resize keeping aspect ratio to fit inside target_size with 5% padding
    max_dim = int(target_size * 0.90)
    w, h = cropped.size
    scale = min(max_dim / w, max_dim / h)
    new_w, new_h = max(1, int(w * scale)), max(1, int(h * scale))
    resized = cropped.resize((new_w, new_h), Image.Resampling.LANCZOS)

    # 5. Composite onto 1024x1024 square white background
    final_canvas = Image.new("RGBA", (target_size, target_size), (255, 255, 255, 255))
    offset_x = (target_size - new_w) // 2
    offset_y = (target_size - new_h) // 2
    final_canvas.paste(resized, (offset_x, offset_y), resized)

    # 6. Save as PNG
    final_rgb = final_canvas.convert("RGB")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    final_rgb.save(output_path, "PNG", quality=95)
    print(f"Done -> {output_path.name}")


def main():
    parser = argparse.ArgumentParser(description="Batch remove background for eyewear photos.")
    parser.add_argument("--input", "-i", required=True, help="Input directory containing raw frame photos")
    parser.add_argument("--output", "-o", required=True, help="Output directory for processed PNGs")
    parser.add_argument("--size", "-s", type=int, default=1024, help="Target square canvas size (default: 1024)")
    args = parser.parse_args()

    input_dir = Path(args.input)
    output_dir = Path(args.output)

    if not input_dir.exists() or not input_dir.is_dir():
        print(f"Error: Input directory {input_dir} does not exist.", file=sys.stderr)
        sys.exit(1)

    valid_extensions = {".png", ".jpg", ".jpeg", ".webp", ".bmp", ".tiff"}
    image_files = [f for f in input_dir.iterdir() if f.suffix.lower() in valid_extensions]

    if not image_files:
        print(f"No valid image files found in {input_dir}")
        return

    print(f"Initializing rembg session (u2net)...")
    session = new_session("u2net")

    print(f"Found {len(image_files)} images to process.")
    for img_path in image_files:
        out_name = f"{img_path.stem}_clean.png"
        process_image(img_path, output_dir / out_name, session, args.size)

    print("\nBatch background removal completed successfully!")


if __name__ == "__main__":
    main()
