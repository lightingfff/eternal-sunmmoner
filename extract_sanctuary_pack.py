#!/usr/bin/env python3
import os
import subprocess
import json

BASE_DIR = "app/src/main/assets/sanctuary_pack"
SRC_IMG = "public/assets/aistudio/B__t_i_nguy_n_Pixel_Art_Th_nh___a_Tri_u_H_i_c6ed3c87.png"

folders = ["tiles", "roads", "vegetation", "props", "buildings", "waterfalls"]
for f in folders:
    os.makedirs(os.path.join(BASE_DIR, f), exist_ok=True)

manifest = {
    "version": "1.0",
    "tile_size": 64,
    "categories": {
        "tiles": [],
        "roads": [],
        "vegetation": [],
        "props": [],
        "buildings": [],
        "waterfalls": []
    }
}

def crop_asset(category, name, x, y, w, h, transparent_color=None):
    out_path = os.path.join(BASE_DIR, category, f"{name}.png")
    rel_path = f"sanctuary_pack/{category}/{name}.png"
    
    cmd = [
        "convert", SRC_IMG,
        "-crop", f"{w}x{h}+{x}+{y}",
        "+repage"
    ]
    if transparent_color:
        cmd.extend(["-fuzz", "12%", "-transparent", transparent_color])
    cmd.append(out_path)
    
    subprocess.run(cmd, check=True)
    
    item_info = {
        "id": name,
        "path": rel_path,
        "width": w,
        "height": h,
        "crop_x": x,
        "crop_y": y,
        "anchor_x": w // 2,
        "anchor_y": h - 8 if category in ["vegetation", "props", "buildings"] else h // 2
    }
    manifest["categories"][category].append(item_info)
    print(f"Extracted {name} ({w}x{h}) -> {out_path}")

# 1. GROUND TILES (64x64)
ground_crops = [
    ("tiles", "tile_grass_1", 832, 16, 64, 64),
    ("tiles", "tile_grass_2", 896, 16, 64, 64),
    ("tiles", "tile_grass_3", 832, 80, 64, 64),
    ("tiles", "tile_grass_4", 896, 80, 64, 64),
    ("tiles", "tile_grass_5", 832, 144, 64, 64),
    ("tiles", "tile_grass_6", 896, 144, 64, 64),
    ("tiles", "tile_grass_7", 832, 208, 64, 64),
    ("tiles", "tile_grass_8", 896, 208, 64, 64),
    ("tiles", "tile_stone_1", 960, 16, 64, 64),
    ("tiles", "tile_stone_2", 1024, 16, 64, 64),
    ("tiles", "tile_stone_3", 1088, 16, 64, 64),
    ("tiles", "tile_stone_4", 960, 80, 64, 64),
    ("tiles", "tile_stone_5", 1024, 80, 64, 64),
    ("tiles", "tile_stone_6", 1088, 80, 64, 64),
    ("tiles", "tile_garden_1", 960, 144, 64, 64),
    ("tiles", "tile_garden_2", 1024, 144, 64, 64),
    ("tiles", "tile_moss_stone_1", 960, 208, 64, 64),
    ("tiles", "tile_moss_stone_2", 1024, 208, 64, 64),
    ("tiles", "tile_water_1", 1088, 144, 64, 64),
    ("tiles", "tile_water_2", 1088, 208, 64, 64),
]

for args in ground_crops:
    crop_asset(*args)

# 2. ROAD TILESET (64x64)
road_crops = [
    ("roads", "road_straight_h", 832, 272, 64, 64),
    ("roads", "road_straight_v", 896, 272, 64, 64),
    ("roads", "road_corner_tl", 960, 272, 64, 64),
    ("roads", "road_corner_tr", 1024, 272, 64, 64),
    ("roads", "road_corner_bl", 1088, 272, 64, 64),
    ("roads", "road_corner_br", 1152, 272, 64, 64),
    ("roads", "road_t_junction", 832, 336, 64, 64),
    ("roads", "road_cross", 896, 336, 64, 64),
    ("roads", "road_border_stone", 960, 336, 64, 64),
]

for args in road_crops:
    crop_asset(*args)

# 3. VEGETATION PACK
veg_crops = [
    ("vegetation", "tree_oak_large", 832, 400, 112, 128, "#0B0A1A"),
    ("vegetation", "tree_sakura_large", 948, 400, 112, 128, "#0B0A1A"),
    ("vegetation", "tree_pine_large", 1064, 400, 96, 128, "#0B0A1A"),
    ("vegetation", "tree_mystic_plum", 1164, 400, 96, 128, "#0B0A1A"),
    ("vegetation", "tree_small_oak", 832, 532, 64, 80, "#0B0A1A"),
    ("vegetation", "tree_small_sakura", 900, 532, 64, 80, "#0B0A1A"),
    ("vegetation", "bush_flowering_pink", 968, 532, 56, 56, "#0B0A1A"),
    ("vegetation", "bush_green_dense", 1028, 532, 56, 56, "#0B0A1A"),
    ("vegetation", "flower_cluster_lavender", 1088, 532, 40, 40, "#0B0A1A"),
    ("vegetation", "flower_cluster_rose", 1132, 532, 40, 40, "#0B0A1A"),
]

for args in veg_crops:
    crop_asset(*args)

# 4. ENVIRONMENT PROPS
prop_crops = [
    ("props", "fountain_sanctuary_water", 1264, 616, 96, 112, "#0B0A1A"),
    ("props", "stone_stairs_grand", 1192, 16, 80, 112, "#0B0A1A"),
    ("props", "rock_boulder_large", 1088, 592, 56, 44, "#0B0A1A"),
    ("props", "rock_boulder_medium", 1148, 592, 44, 36, "#0B0A1A"),
    ("props", "street_lantern_gold", 1272, 336, 32, 72, "#0B0A1A"),
    ("props", "stone_pillar_gothic", 1024, 704, 40, 96, "#0B0A1A"),
    ("props", "brazier_fire_stand", 1068, 704, 32, 80, "#0B0A1A"),
    ("props", "crystal_mana_cluster", 1376, 816, 36, 68, "#0B0A1A"),
    ("props", "fence_wooden_gate", 1376, 416, 64, 48, "#0B0A1A"),
]

for args in prop_crops:
    crop_asset(*args)

# 5. SANCTUARY ARCHITECTURE BUILDINGS
bldg_crops = [
    ("buildings", "building_central_plaza", 12, 536, 150, 140, "#0B0A1A"),
    ("buildings", "building_summoning_hall", 166, 536, 148, 140, "#0B0A1A"),
    ("buildings", "building_training_hall", 318, 536, 148, 140, "#0B0A1A"),
    ("buildings", "building_dorms_residential", 470, 536, 148, 140, "#0B0A1A"),
    ("buildings", "building_armory_shop", 622, 536, 148, 140, "#0B0A1A"),
    ("buildings", "building_dining_hall", 12, 708, 150, 140, "#0B0A1A"),
    ("buildings", "building_synthesis_lab", 166, 708, 148, 140, "#0B0A1A"),
    ("buildings", "building_flying_dock", 318, 708, 158, 140, "#0B0A1A"),
    ("buildings", "building_supply_warehouse", 480, 708, 148, 140, "#0B0A1A"),
    ("buildings", "building_main_gate", 632, 708, 152, 140, "#0B0A1A"),
]

for args in bldg_crops:
    crop_asset(*args)

# 6. WATERFALL CLIFF
crop_asset("waterfalls", "waterfall_cliff_terraced", 1390, 16, 138, 320, "#0B0A1A")

# Save Manifest JSON
manifest_path = os.path.join(BASE_DIR, "sanctuary_assets_manifest.json")
with open(manifest_path, "w", encoding="utf-8") as f:
    json.dump(manifest, f, indent=2, ensure_ascii=False)

print(f"Manifest written to {manifest_path}")

# 7. GENERATE PREVIEW CONTACT SHEET
contact_cmd = [
    "montage",
    os.path.join(BASE_DIR, "tiles", "*.png"),
    os.path.join(BASE_DIR, "roads", "*.png"),
    os.path.join(BASE_DIR, "vegetation", "*.png"),
    os.path.join(BASE_DIR, "props", "*.png"),
    os.path.join(BASE_DIR, "buildings", "*.png"),
    "-geometry", "128x128+4+4",
    "-background", "#121A22",
    "-tile", "8x",
    os.path.join(BASE_DIR, "preview_contact_sheet.png")
]
subprocess.run(contact_cmd, check=True)
print("Preview contact sheet generated successfully!")
