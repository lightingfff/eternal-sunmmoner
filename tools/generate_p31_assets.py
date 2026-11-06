import os
import math
import zlib
import struct
import hashlib

def write_png(filename, width, height, pixels, has_alpha=True):
    """
    Writes a PNG file using only built-in zlib and struct.
    pixels: list of tuples (r, g, b) or (r, g, b, a) row by row from top to bottom.
    """
    os.makedirs(os.path.dirname(os.path.abspath(filename)), exist_ok=True)
    
    color_type = 6 if has_alpha else 2  # 6 = RGBA, 2 = RGB
    bytes_per_pixel = 4 if has_alpha else 3
    
    # Raw scanlines with filter type 0 (None)
    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0)  # filter type
        row_start = y * width
        for x in range(width):
            px = pixels[row_start + x]
            if has_alpha:
                r, g, b, a = px
                raw_data.extend([int(r) & 0xFF, int(g) & 0xFF, int(b) & 0xFF, int(a) & 0xFF])
            else:
                r, g, b = px[:3]
                raw_data.extend([int(r) & 0xFF, int(g) & 0xFF, int(b) & 0xFF])
                
    compressed_data = zlib.compress(bytes(raw_data), level=9)
    
    def chunk(chunk_type, data):
        c = chunk_type + data
        crc = zlib.crc32(c) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + c + struct.pack(">I", crc)
    
    header = b"\x89PNG\r\n\x1a\n"
    ihdr_data = struct.pack(">IIBBBBB", width, height, 8, color_type, 0, 0, 0)
    ihdr = chunk(b"IHDR", ihdr_data)
    idat = chunk(b"IDAT", compressed_data)
    iend = chunk(b"IEND", b"")
    
    with open(filename, "wb") as f:
        f.write(header + ihdr + idat + iend)

# --- Procedural Pixel Art Synthesis Utilities ---

def pseudo_noise(x, y, seed=1337):
    n = (x * 374761393 + y * 668265263 + seed * 1013904223) & 0x7FFFFFFF
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0x7FFFFFFF) / float(0x7FFFFFFF)

def smooth_noise(x, y, w, h, scale, seed=1337):
    """Seamless noise using torus mapping."""
    u = (x % w) / float(w)
    v = (y % h) / float(h)
    angle_u = u * 2.0 * math.pi
    angle_v = v * 2.0 * math.pi
    nx = math.cos(angle_u) * scale
    ny = math.sin(angle_u) * scale
    nz = math.cos(angle_v) * scale
    nw = math.sin(angle_v) * scale
    
    val = (math.sin(nx * 1.5 + seed) + math.cos(ny * 1.5 + seed * 2) +
           math.sin(nz * 1.5 + seed * 3) + math.cos(nw * 1.5 + seed * 4)) / 4.0
    return (val + 1.0) / 2.0

def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, int(v)))

# --- Texture 1: Flagstone Ancient 64x64 ---
def gen_flagstone_ancient(w=64, h=64):
    pixels = []
    # Grid of irregular flagstone blocks: ~4x4 stones with carved edges and mossy mortar
    block_w = 16
    block_h = 16
    for y in range(h):
        for x in range(w):
            bx = (x // block_w)
            by = (y // block_h)
            lx = x % block_w
            ly = y % block_h
            
            # Distance to border of stone (mortar)
            edge_dist = min(lx, block_w - 1 - lx, ly, block_h - 1 - ly)
            
            # Subtle stone variation per block
            block_seed = (bx * 7 + by * 13)
            base_luma = 120 + int(pseudo_noise(bx, by, 42) * 35) # 120..155
            
            # Noise inside the stone
            n1 = smooth_noise(x, y, w, h, 2.5, seed=101)
            grain = int((n1 - 0.5) * 24)
            
            # Mortar line
            if edge_dist == 0:
                # Deep dark mortar with moss hint
                r = clamp(26 + grain // 2)
                g = clamp(28 + grain // 2 + 6)
                b = clamp(36 + grain // 2)
            elif edge_dist == 1:
                # Beveled stone edge (shadow on bottom-right, highlight on top-left)
                is_top_left = (lx <= 1 or ly <= 1)
                mod = 18 if is_top_left else -24
                r = clamp(base_luma + mod - 6 + grain)
                g = clamp(base_luma + mod - 4 + grain)
                b = clamp(base_luma + mod + 2 + grain)
            else:
                # Stone surface: aged limestone with subtle mossy fissures
                moss = 8 if (n1 > 0.65) else 0
                r = clamp(base_luma - 10 + grain)
                g = clamp(base_luma - 8 + moss + grain)
                b = clamp(base_luma - 2 + grain)
            pixels.append((r, g, b, 255))
    return pixels

# --- Texture 2: Weathered Cobble 64x64 ---
def gen_weathered_cobble(w=64, h=64):
    pixels = []
    # Smaller irregular rounded cobbles ~8x8
    cell_size = 8
    for y in range(h):
        for x in range(w):
            cx = x // cell_size
            cy = y // cell_size
            lx = (x % cell_size) - cell_size / 2.0 + 0.5
            ly = (y % cell_size) - cell_size / 2.0 + 0.5
            dist = math.sqrt(lx * lx + ly * ly)
            
            cobble_seed = (cx * 11 + cy * 19)
            base_luma = 95 + int(pseudo_noise(cx, cy, 77) * 35)
            
            n = smooth_noise(x, y, w, h, 3.0, seed=202)
            grain = int((n - 0.5) * 18)
            
            if dist > 3.4:
                # Deep mortar
                r = clamp(20 + grain // 2)
                g = clamp(22 + grain // 2)
                b = clamp(28 + grain // 2)
            else:
                # Rounded dome shading (light from NW)
                light = (-lx - ly) * 4.5
                r = clamp(base_luma + light - 8 + grain)
                g = clamp(base_luma + light - 6 + grain)
                b = clamp(base_luma + light + grain)
            pixels.append((r, g, b, 255))
    return pixels

# --- Texture 3: Corrupted Grass 64x64 ---
def gen_corrupted_grass(w=64, h=64):
    pixels = []
    for y in range(h):
        for x in range(w):
            n1 = smooth_noise(x, y, w, h, 2.0, seed=301)
            n2 = smooth_noise(x, y, w, h, 4.5, seed=302)
            grain = int((pseudo_noise(x, y, 99) - 0.5) * 14)
            
            # Palette: grim dark moss-green mixed with ash-grey
            base_g = 52 + int(n1 * 34) + int(n2 * 18) + grain
            base_r = int(base_g * 0.72)
            base_b = int(base_g * 0.65)
            
            # Specks of dried clover / blight spots
            if pseudo_noise(x, y, 444) > 0.96:
                base_r = clamp(base_r + 20)
                base_g = clamp(base_g - 10)
                base_b = clamp(base_b + 15)
                
            pixels.append((clamp(base_r), clamp(base_g), clamp(base_b), 255))
    return pixels

# --- Texture 4: Packed Soil 64x64 ---
def gen_packed_soil(w=64, h=64):
    pixels = []
    for y in range(h):
        for x in range(w):
            n1 = smooth_noise(x, y, w, h, 2.8, seed=401)
            n2 = smooth_noise(x, y, w, h, 5.0, seed=402)
            grain = int((pseudo_noise(x, y, 555) - 0.5) * 22)
            
            # Palette: warm compacted earth with grit
            base_val = 88 + int(n1 * 32) + int(n2 * 14) + grain
            r = clamp(base_val + 12)
            g = clamp(base_val)
            b = clamp(base_val - 14)
            
            # Small embedded pebbles
            if pseudo_noise(x, y, 888) > 0.97:
                pebble_luma = 145
                r = clamp(pebble_luma - 5)
                g = clamp(pebble_luma - 5)
                b = clamp(pebble_luma)
                
            pixels.append((r, g, b, 255))
    return pixels

# --- Texture 5: Abyss Verge 64x64 ---
def gen_abyss_verge(w=64, h=64):
    pixels = []
    # Crumbling stone rim fading towards bottom into void (alpha)
    for y in range(h):
        for x in range(w):
            n1 = smooth_noise(x, y, w, h, 3.2, seed=501)
            grain = int((pseudo_noise(x, y, 666) - 0.5) * 16)
            
            # Rock ledge gradient from top to bottom
            cliff_depth = y / float(h)
            
            if cliff_depth > 0.85 and pseudo_noise(x, y, 777) > (1.0 - (cliff_depth - 0.85) * 4):
                # Fragmented void transparent gap
                pixels.append((0, 0, 0, 0))
            else:
                base_luma = int((1.0 - cliff_depth * 0.6) * 105) + grain
                r = clamp(base_luma - 8)
                g = clamp(base_luma - 6)
                b = clamp(base_luma + 4)
                alpha = clamp(255 - int(cliff_depth * cliff_depth * 140))
                pixels.append((r, g, b, alpha))
    return pixels

# --- Animated Spritesheet 1: Lantern Flame Sheet (64x24 px, 4 frames of 16x24) ---
def gen_lantern_flame_sheet():
    w = 64
    h = 24
    frame_w = 16
    pixels = []
    
    for y in range(h):
        for x in range(w):
            f = x // frame_w
            lx = x % frame_w
            # Center of flame at (8, 16)
            cx = 8.0 + math.sin(f * 1.57 + y * 0.3) * 1.2
            cy = 16.0
            
            dx = (lx - cx)
            dy = (y - cy)
            
            # Vertical tear shape
            dist_x = abs(dx) / 3.0
            dist_y = (cy - y) / 10.0  # upwards taper
            
            in_flame = False
            r, g, b, a = 0, 0, 0, 0
            
            if 0.0 <= dist_y <= 1.5:
                rad = (1.5 - dist_y) * 1.1
                if dist_x < rad:
                    in_flame = True
                    # Intensity: core vs rim
                    core_factor = 1.0 - (dist_x / max(0.1, rad))
                    if dist_y < 0.6 and core_factor > 0.5:
                        # Bright yellow-white core
                        r, g, b, a = 254, 240, 138, 255
                    elif core_factor > 0.2:
                        # Warm amber flame
                        r, g, b, a = 245, 158, 11, 230
                    else:
                        # Outer red/orange flicker rim
                        r, g, b, a = 180, 83, 9, 160
            pixels.append((r, g, b, a))
    return pixels

# --- Animated Spritesheet 2: Tower Gate Rift Sheet (1024x160 px, 8 frames of 128x160) ---
def gen_tower_rift_sheet():
    w = 1024
    h = 160
    frame_w = 128
    pixels = []
    
    for y in range(h):
        for x in range(w):
            f = x // frame_w
            lx = x % frame_w
            cx = 64.0
            cy = 80.0
            
            dx = (lx - cx)
            dy = (y - cy) * (1.0 / 0.72) # oblique vertical ratio
            dist = math.sqrt(dx * dx + dy * dy)
            
            # Swirling astral vortex angle
            angle = math.atan2(dy, dx) + f * (math.pi / 4.0)
            swirl = math.sin(angle * 3.0 + dist * 0.15)
            
            r, g, b, a = 0, 0, 0, 0
            
            if dist < 54.0:
                if dist < 22.0:
                    # Void core: pitch dark astral abyss with subtle cyan fringe
                    r, g, b, a = 10, 14, 26, 255
                elif dist < 38.0:
                    # Swirling arcane cyan energy band
                    val = 0.5 + 0.5 * swirl
                    r = clamp(30 + val * 26)
                    g = clamp(140 + val * 55)
                    b = clamp(220 + val * 35)
                    a = clamp(200 + val * 55)
                else:
                    # Outer disintegrating mana ring
                    fade = (54.0 - dist) / 16.0
                    val = 0.5 + 0.5 * swirl
                    r = clamp(56 * fade)
                    g = clamp(189 * fade)
                    b = clamp(248 * fade)
                    a = clamp(220 * fade * (0.6 + 0.4 * val))
            pixels.append((r, g, b, a))
    return pixels

# --- Animated Spritesheet 3: Forge Chimney Smoke Sheet (144x48 px, 6 frames of 24x48) ---
def gen_forge_smoke_sheet():
    w = 144
    h = 48
    frame_w = 24
    pixels = []
    
    for y in range(h):
        for x in range(w):
            f = x // frame_w
            lx = x % frame_w
            
            # Smoke rises upwards from bottom y=48 to top y=0, swaying right with wind
            height_pct = (48.0 - y) / 48.0  # 0 at bottom, 1 at top
            wind_sway = height_pct * 7.0 + math.sin(f * 1.05 + height_pct * 3.14) * 2.5
            cx = 10.0 + wind_sway
            
            dx = abs(lx - cx)
            radius = 3.0 + height_pct * 6.5
            
            r, g, b, a = 0, 0, 0, 0
            if dx < radius and height_pct > 0.05:
                edge_factor = 1.0 - (dx / radius)
                dissolve = 1.0 - height_pct * 0.85
                luma = 75 + int(height_pct * 40)
                r, g, b = luma, luma + 2, luma + 6
                a = clamp(int(edge_factor * dissolve * 190))
            pixels.append((r, g, b, a))
    return pixels

# --- Generate All Assets and Record File Metadata ---
def main():
    assets_dir = "app/src/main/assets/sanctuary_p31"
    docs_dir = "docs/art/sanctuary_revamp"
    
    file_specs = [
        ("terrain/flagstone_ancient_64.png", 64, 64, gen_flagstone_ancient(), True),
        ("terrain/weathered_cobble_64.png", 64, 64, gen_weathered_cobble(), True),
        ("terrain/corrupted_grass_64.png", 64, 64, gen_corrupted_grass(), True),
        ("terrain/packed_soil_64.png", 64, 64, gen_packed_soil(), True),
        ("terrain/abyss_verge_64.png", 64, 64, gen_abyss_verge(), True),
        ("props/lantern_flame_sheet.png", 64, 24, gen_lantern_flame_sheet(), True),
        ("vfx/tower_rift_sheet.png", 1024, 160, gen_tower_rift_sheet(), True),
        ("vfx/forge_smoke_sheet.png", 144, 48, gen_forge_smoke_sheet(), True),
    ]
    
    generated_report = []
    
    for rel_path, w, h, px, has_alpha in file_specs:
        full_path = os.path.join(assets_dir, rel_path)
        write_png(full_path, w, h, px, has_alpha)
        
        # Calculate SHA-256 and byte size
        with open(full_path, "rb") as f:
            data = f.read()
            sha256 = hashlib.sha256(data).hexdigest()
            size_bytes = len(data)
            
        generated_report.append({
            "path": full_path,
            "width": w,
            "height": h,
            "size_bytes": size_bytes,
            "sha256": sha256
        })
        print(f"Generated {full_path} ({w}x{h}, {size_bytes} bytes, sha256={sha256[:12]}...)")
        
    # --- Generate Contact Sheet Preview ---
    # Compose all generated assets onto a single preview sheet: 512x256 px
    cw, ch = 512, 280
    contact_pixels = [(18, 20, 29, 255)] * (cw * ch) # Dark slate background
    
    def blit(src_px, sw, sh, dst_x, dst_y):
        for sy in range(sh):
            for sx in range(sw):
                dx = dst_x + sx
                dy = dst_y + sy
                if 0 <= dx < cw and 0 <= dy < ch:
                    spx = src_px[sy * sw + sx]
                    # Alpha blend onto contact sheet
                    sr, sg, sb, sa = spx
                    alpha = sa / 255.0
                    dr, dg, db, _ = contact_pixels[dy * cw + dx]
                    nr = clamp(sr * alpha + dr * (1.0 - alpha))
                    ng = clamp(sg * alpha + dg * (1.0 - alpha))
                    nb = clamp(sb * alpha + db * (1.0 - alpha))
                    contact_pixels[dy * cw + dx] = (nr, ng, nb, 255)
                    
    # Place 5 terrain tiles in top row with spacing
    blit(file_specs[0][3], 64, 64, 20, 20)
    blit(file_specs[1][3], 64, 64, 100, 20)
    blit(file_specs[2][3], 64, 64, 180, 20)
    blit(file_specs[3][3], 64, 64, 260, 20)
    blit(file_specs[4][3], 64, 64, 340, 20)
    
    # Place lantern flame and forge smoke on second row
    blit(file_specs[5][3], 64, 24, 20, 100)
    blit(file_specs[7][3], 144, 48, 110, 95)
    
    # Place 4 frames of tower rift on third row (scale down or window)
    rift_px = file_specs[6][3]
    # Blit first 3 frames of rift (3 * 128 = 384 px)
    for ry in range(110): # crop bottom
        for rx in range(384):
            spx = rift_px[ry * 1024 + rx]
            dx = 20 + rx
            dy = 155 + ry
            if 0 <= dx < cw and 0 <= dy < ch:
                sr, sg, sb, sa = spx
                alpha = sa / 255.0
                dr, dg, db, _ = contact_pixels[dy * cw + dx]
                contact_pixels[dy * cw + dx] = (
                    clamp(sr * alpha + dr * (1.0 - alpha)),
                    clamp(sg * alpha + dg * (1.0 - alpha)),
                    clamp(sb * alpha + db * (1.0 - alpha)),
                    255
                )

    contact_path = os.path.join(docs_dir, "p31_pilot_contact_sheet.png")
    write_png(contact_path, cw, ch, contact_pixels, True)
    print(f"Generated Contact Sheet at {contact_path}")

if __name__ == "__main__":
    main()
