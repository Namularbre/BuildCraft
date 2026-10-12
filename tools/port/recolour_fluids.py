"""Generates the textures of BuildCraft's oil fluids.

1.12 recoloured three grey textures (one per heat level) at runtime, mixing each fluid's dark and light colours by the
grey value of every pixel. This does the same ahead of time, so the textures are normal resources. Only needs the
standard library (PNG decoding and encoding with zlib).

Usage: python -I tools/port/recolour_fluids.py <legacy fluid texture dir> <output dir>
"""

import os
import shutil
import struct
import sys
import zlib

# name: (light colour, dark colour), from 1.12's BCEnergyFluids
FLUIDS = {
    "oil": (0x50_50_50, 0x05_05_05),
    "oil_residue": (0x10_0F_10, 0x42_10_42),
    "oil_heavy": (0xA0_8F_1F, 0x42_35_20),
    "oil_dense": (0x87_6E_77, 0x42_24_24),
    "oil_distilled": (0xE4_AF_78, 0xB4_7F_00),
    "fuel_dense": (0xFF_AF_3F, 0xE0_7F_00),
    "fuel_mixed_heavy": (0xF2_A7_00, 0xC4_87_00),
    "fuel_light": (0xFF_FF_30, 0xE4_CF_00),
    "fuel_mixed_light": (0xF6_D7_00, 0xC4_B7_00),
    "fuel_gaseous": (0xFA_F6_30, 0xE0_D9_00),
}


def read_png(path):
    """Reads an 8-bit, non-interlaced RGBA PNG. Returns (width, height, rows of bytes)."""
    with open(path, "rb") as f:
        data = f.read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    pos = 8
    idat = b""
    width = height = None
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IHDR":
            width, height, depth, colour, _, _, interlace = struct.unpack(">IIBBBBB", chunk)
            assert depth == 8 and colour == 6 and interlace == 0, (path, depth, colour, interlace)
        elif kind == b"IDAT":
            idat += chunk
    raw = zlib.decompress(idat)
    stride = width * 4
    rows = []
    previous = bytearray(stride)
    for y in range(height):
        start = y * (stride + 1)
        filter_type = raw[start]
        row = bytearray(raw[start + 1:start + 1 + stride])
        for i in range(stride):
            left = row[i - 4] if i >= 4 else 0
            up = previous[i]
            up_left = previous[i - 4] if i >= 4 else 0
            if filter_type == 1:
                row[i] = (row[i] + left) & 0xFF
            elif filter_type == 2:
                row[i] = (row[i] + up) & 0xFF
            elif filter_type == 3:
                row[i] = (row[i] + (left + up) // 2) & 0xFF
            elif filter_type == 4:
                p = left + up - up_left
                pa, pb, pc = abs(p - left), abs(p - up), abs(p - up_left)
                predictor = left if pa <= pb and pa <= pc else up if pb <= pc else up_left
                row[i] = (row[i] + predictor) & 0xFF
        rows.append(row)
        previous = row
    return width, height, rows


def write_png(path, width, height, rows):
    def chunk(kind, payload):
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload))

    raw = b"".join(b"\x00" + bytes(row) for row in rows)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)))
        f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        f.write(chunk(b"IEND", b""))


def recolour(rows, light, dark):
    """Exactly like 1.12's AtlasSpriteFluid: every channel goes from the dark colour (at 0) to the light one (at 255),
    and the result is opaque."""
    channels = [(light >> 16) & 0xFF, (light >> 8) & 0xFF, light & 0xFF]
    dark_channels = [(dark >> 16) & 0xFF, (dark >> 8) & 0xFF, dark & 0xFF]
    result = []
    for row in rows:
        out = bytearray(len(row))
        for i in range(0, len(row), 4):
            for c in range(3):
                value = row[i + c]
                out[i + c] = (dark_channels[c] * (256 - value) + channels[c] * value) // 256
            out[i + 3] = 0xFF
        result.append(out)
    return result


def main(source, output):
    os.makedirs(output, exist_ok=True)
    for heat in range(3):
        for kind in ("still", "flow"):
            base = os.path.join(source, f"heat_{heat}_{kind}.png")
            width, height, rows = read_png(base)
            for name, (light, dark) in FLUIDS.items():
                target = os.path.join(output, f"{name}_heat_{heat}_{kind}.png")
                write_png(target, width, height, recolour(rows, light, dark))
                shutil.copyfile(base + ".mcmeta", target + ".mcmeta")
    print(f"Wrote {len(FLUIDS) * 6} textures to {output}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
