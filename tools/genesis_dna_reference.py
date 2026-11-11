#!/usr/bin/env python3
"""Independent Python oracle for the frozen Genesis V1 binary contract.

Normally --check only. --write is a deliberate fixture-authoring operation, never CI.
Do not regenerate existing V1 expectations to accommodate a changed implementation.
"""
import argparse
import hashlib
from pathlib import Path
import struct
import zlib

MASK = (1 << 64) - 1
UNIT = 1_000_000
FIXTURE = Path(__file__).resolve().parents[1] / "app/src/test/resources/dna/genesis-v1.properties"


class SplitMix64:
    def __init__(self, seed):
        self.state = seed & MASK

    def next(self):
        self.state = (self.state + 0x9E3779B97F4A7C15) & MASK
        z = self.state
        z = ((z ^ (z >> 30)) * 0xBF58476D1CE4E5B9) & MASK
        z = ((z ^ (z >> 27)) * 0x94D049BB133111EB) & MASK
        return z ^ (z >> 31)

    def bounded(self, bound):
        while True:
            bits = self.next() >> 1
            value = bits % bound
            if bits - value + bound - 1 < 1 << 63:
                return value


def envelope(magic, payload):
    body = magic + struct.pack(">H", 1) + payload
    return body + struct.pack(">I", zlib.crc32(body))


def vector(identity, f):
    bass, mid, treble, centroid, flux, tempo, confidence, dynamic = f[:8]
    chroma = f[8:]
    payload = struct.pack(">20I", *f)
    digest = hashlib.sha256(b"RIKO:MDNA:GENESIS:SEED\0" + b"\0\1"
                            + bytes.fromhex(identity) + payload).digest()
    seed = int.from_bytes(digest[:8], "big")
    rng = SplitMix64(seed)
    geometry = rng.next()
    weights = (250000 + bass // 2 + confidence // 4,
               250000 + mid // 2 + (UNIT - flux) // 4,
               250000 + treble // 2 + flux // 4)
    choice = rng.bounded(sum(weights))
    archetype = 1 if choice < weights[0] else 2 if choice < sum(weights[:2]) else 3
    particles = 4096 + bass * 16384 // UNIT + treble * 8192 // UNIT + rng.bounded(4097)
    layers = 2 + (mid + treble) * 2 // (2 * UNIT) + rng.bounded(2)
    dominant = max(range(12), key=lambda i: chroma[i])
    symmetry = 2 + dominant % 6 + chroma[dominant] * 4 // UNIT
    expansion = 50000 + bass * 300000 // UNIT + rng.bounded(50001)
    brightness = 250000 + centroid * 550000 // 11025000 + rng.bounded(100001)
    sharpness = 100000 + centroid * 650000 // 11025000 + rng.bounded(150001)
    burst = 50000 + flux * 850000 // UNIT + rng.bounded(100001)
    rhythm = tempo if confidence >= 350000 else 0
    deformation = 30000 + dynamic * 350000 // 120000 + rng.bounded(50001)
    flow = 100000 + mid * 500000 // UNIT + rhythm * 800000 // 300000 + rng.bounded(100001)
    detail = 100000 + treble * 750000 // UNIT + rng.bounded(150001)
    rotation = rng.bounded(360000)
    params = (particles, layers, symmetry, expansion, brightness, sharpness, burst,
              rhythm, deformation, flow, detail, rotation, *chroma)
    genome = envelope(b"MDGN", struct.pack(">HQQB24I", 1, seed, geometry, archetype, *params))
    return {"identity": identity, "features": envelope(b"MDFE", payload).hex(),
            "seed": f"{seed:016x}", "genome": genome.hex()}


def expected():
    rng = SplitMix64(0)
    assert [rng.next() for _ in range(3)] == [0xe220a8397b1dcdaf, 0x6e789e6aa1b965f4, 0x06c45d188009454f]
    cases = {
        "silence": ("00" * 32, [0] * 20),
        "a_minor_pulse": ("01" * 32, [700000, 420000, 180000, 2100000, 310000,
            124500, 870000, 18750, 250000, 0, 0, 0, 250000, 0, 0, 0, 0, 500000, 0, 0]),
        "bright_noise": ("ff" * 32, [120000, 380000, 920000, 8500000, 800000,
            0, 0, 44000] + [83334] * 4 + [83333] * 8),
    }
    lines = ["# Frozen Genesis V1 vectors; independently calculated in Python.",
             "# Changing V1 mapping requires a NEW algorithm version, not new V1 expectations."]
    for name, (identity, features) in cases.items():
        lines.append("")
        lines.extend(f"{name}.{key}={value}" for key, value in vector(identity, features).items())
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--check", action="store_true")
    group.add_argument("--write", action="store_true")
    args = parser.parse_args()
    result = expected()
    if args.write:
        if FIXTURE.exists():
            raise SystemExit("Refusing to overwrite an existing frozen fixture")
        FIXTURE.parent.mkdir(parents=True, exist_ok=True)
        FIXTURE.write_text(result, encoding="ascii")
    else:
        assert FIXTURE.read_text(encoding="ascii") == result, "V1 fixture mismatch"
        print("PASS: independent Python oracle matches all 3 frozen Genesis V1 vectors")
