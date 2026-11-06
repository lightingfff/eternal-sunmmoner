import os
import struct
import zlib
import hashlib
import json

def verify_png(path):
    with open(path, "rb") as f:
        data = f.read()
    
    # Check signature
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        return False, "Invalid PNG signature", None
    
    # Read IHDR chunk
    # Chunk format: 4 bytes length, 4 bytes type, length bytes data, 4 bytes crc
    length = struct.unpack(">I", data[8:12])[0]
    chunk_type = data[12:16]
    if chunk_type != b"IHDR":
        return False, "First chunk is not IHDR", None
    
    ihdr_data = data[16:16+length]
    w, h, bit_depth, color_type, comp, filt, interlace = struct.unpack(">IIBBBBB", ihdr_data)
    
    sha256 = hashlib.sha256(data).hexdigest()
    return True, "Valid PNG", {
        "width": w,
        "height": h,
        "bit_depth": bit_depth,
        "color_type": color_type,
        "size_bytes": len(data),
        "sha256": sha256
    }

def main():
    assets = [
        ("terrain/flagstone_ancient_64.png", 64, 64, 6),
        ("terrain/weathered_cobble_64.png", 64, 64, 6),
        ("terrain/corrupted_grass_64.png", 64, 64, 6),
        ("terrain/packed_soil_64.png", 64, 64, 6),
        ("terrain/abyss_verge_64.png", 64, 64, 6),
        ("props/lantern_flame_sheet.png", 64, 24, 6),
        ("vfx/tower_rift_sheet.png", 1024, 160, 6),
        ("vfx/forge_smoke_sheet.png", 144, 48, 6),
    ]
    
    base_dir = "app/src/main/assets/sanctuary_p31"
    results = {}
    all_valid = True
    
    for rel_path, exp_w, exp_h, exp_ct in assets:
        full_path = os.path.join(base_dir, rel_path)
        valid, msg, meta = verify_png(full_path)
        if not valid or meta["width"] != exp_w or meta["height"] != exp_h:
            print(f"FAILED: {rel_path} - {msg} (Meta: {meta})")
            all_valid = False
        else:
            print(f"PASSED: {rel_path} -> {meta['width']}x{meta['height']}, {meta['size_bytes']} bytes, SHA: {meta['sha256'][:16]}...")
            results[rel_path] = meta
            
    contact_sheet = "docs/art/sanctuary_revamp/p31_pilot_contact_sheet.png"
    valid, msg, meta = verify_png(contact_sheet)
    if valid:
        print(f"PASSED: Contact sheet -> {meta['width']}x{meta['height']}, {meta['size_bytes']} bytes")
    else:
        print(f"FAILED: Contact sheet - {msg}")
        all_valid = False
        
    print(f"\nALL ASSETS VERIFIED: {all_valid}")
    
    # Save verification report
    with open("docs/art/sanctuary_revamp/p31_verification_report.json", "w") as f:
        json.dump(results, f, indent=2)

if __name__ == "__main__":
    main()
