#!/usr/bin/env python3
"""Generate GeckoLib resources from the supplied, private ModelEngine blueprints.

Generated output needs visual validation in game before release. The MythicMobs
Ancient City randomspawns (including its Warden replacement) are NOT imported.
"""

import argparse
import base64
from collections import Counter
import json
from pathlib import Path
import re
from zipfile import ZipFile

MODELS = {
    "HPC_SCULK_GOLIATH": "sculk_goliath",
    "HPC_SCULK_GOLIATH_DESTROYED": "sculk_goliath_destroyed",
    "HPC_SCULK_GOLIATH_DIG": "sculk_goliath_dig",
    "HPC_GENERIC_GROUNDBREAK_FX": "sculk_goliath_groundbreak",
}


def clean_name(value):
    return re.sub(r"[^a-zA-Z0-9_]", "_", value.strip()) or "bone"


def build_geometry(model, identifier):
    elements = {e["uuid"]: e for e in model["elements"]}
    groups = {g["uuid"]: g for g in model.get("groups", [])}
    bones, names, seen = [], {}, Counter()

    def visit(node, parent=None):
        src = groups.get(node["uuid"], node)
        base = clean_name(src.get("name", "bone"))
        seen[base] += 1
        name = base if seen[base] == 1 else f"{base}_{seen[base]}"
        names[node["uuid"]] = name
        p = src.get("origin", [0, 0, 0])
        bone = {"name": name, "pivot": [-p[0], p[1], p[2]]}
        if parent:
            bone["parent"] = parent
        r = src.get("rotation", [0, 0, 0])
        if any(r):
            bone["rotation"] = [-r[0], -r[1], r[2]]
        cubes = []
        for child in node.get("children", []):
            if not isinstance(child, str):
                continue
            cube_src = elements.get(child)
            if not cube_src or not cube_src.get("export", True) or not cube_src.get("visibility", True):
                continue
            if cube_src.get("type") != "cube":
                raise ValueError(f"Unsupported geometry: {cube_src.get('type')}")
            a, b = cube_src["from"], cube_src["to"]
            cube = {"origin": [-b[0], a[1], a[2]], "size": [b[i] - a[i] for i in range(3)]}
            if cube_src.get("inflate"):
                cube["inflate"] = cube_src["inflate"]
            cr = cube_src.get("rotation", [0, 0, 0])
            if any(cr):
                cp = cube_src.get("origin", [0, 0, 0])
                cube["pivot"] = [-cp[0], cp[1], cp[2]]
                cube["rotation"] = [-cr[0], -cr[1], cr[2]]
            faces = {}
            for face, info in cube_src.get("faces", {}).items():
                if not info or info.get("enabled") is False or "uv" not in info:
                    continue
                u, v, u2, v2 = info["uv"]
                faces[face] = {"uv": [u, v], "uv_size": [u2 - u, v2 - v]}
                if info.get("rotation"):
                    faces[face]["uv_rotation"] = info["rotation"]
            if faces:
                cube["uv"] = faces
            cubes.append(cube)
        if cubes:
            bone["cubes"] = cubes
        bones.append(bone)
        for child in node.get("children", []):
            if isinstance(child, dict):
                visit(child, name)

    for root in model["outliner"]:
        if isinstance(root, dict):
            visit(root)
    if not bones:
        raise ValueError(f"No bones in {identifier}")
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.tnkcore.{identifier}",
                "texture_width": model["resolution"]["width"],
                "texture_height": model["resolution"]["height"],
                "visible_bounds_width": 18,
                "visible_bounds_height": 18,
                "visible_bounds_offset": [0, 6, 0],
            },
            "bones": bones,
        }],
    }, names


def build_animations(model, names, identifier):
    result = {}
    for animation in model["animations"]:
        bones = {}
        for bone_uuid, controller in animation.get("animators", {}).items():
            if bone_uuid not in names:
                continue
            channels = {}
            for key in controller.get("keyframes", []):
                channel = key.get("channel")
                if channel not in ("position", "rotation", "scale") or not key.get("data_points"):
                    continue
                values = [float(key["data_points"][0].get(v, 0)) for v in "xyz"]
                if channel in ("position", "rotation"):
                    values[0] = -values[0]
                if channel == "rotation":
                    values[1] = -values[1]
                t = f"{float(key['time']):.6f}".rstrip("0").rstrip(".") or "0"
                mode = key.get("interpolation", "linear")
                if mode not in ("linear", "step", "catmullrom"):
                    raise ValueError(f"Unsupported interpolation: {mode}")
                channels.setdefault(channel, {})[t] = (
                    values if mode == "linear" else {"post": values, "lerp_mode": mode}
                )
            if channels:
                bones[names[bone_uuid]] = channels
        animation_name = clean_name(animation["name"]).lower()
        result[f"animation.tnkcore.{identifier}.{animation_name}"] = {
            "loop": animation.get("loop") == "loop",
            "animation_length": animation["length"],
            "bones": bones,
        }
    return {"format_version": "1.8.0", "animations": result}


def save_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("input_zip", type=Path)
    parser.add_argument("output_resources", type=Path)
    args = parser.parse_args()
    root = args.output_resources / "assets" / "tnkcore"
    manifest = {}
    with ZipFile(args.input_zip) as archive:
        for source, target in MODELS.items():
            model = json.loads(archive.read(f"ModelEngine/blueprints/{source}.bbmodel"))
            geometry, names = build_geometry(model, target)
            anim = build_animations(model, names, target)
            save_json(root / "geo" / f"{target}.geo.json", geometry)
            save_json(root / "animations" / f"{target}.animation.json", anim)
            textures = model["textures"]
            if len(textures) != 1 or not textures[0]["source"].startswith("data:image/png;base64,"):
                raise ValueError(f"{source} lacks exactly one embedded PNG texture")
            png = base64.b64decode(textures[0]["source"].split(",", 1)[1], validate=True)
            filename = root / "textures" / "entity" / f"{target}.png"
            filename.parent.mkdir(parents=True, exist_ok=True)
            filename.write_bytes(png)
            manifest[target] = {"bones": len(geometry["minecraft:geometry"][0]["bones"]),
                                "animations": list(anim["animations"])}
    save_json(args.output_resources / "sculk_goliath_manifest.json", manifest)
    print(json.dumps({key: (v["bones"], len(v["animations"])) for key, v in manifest.items()}))


if __name__ == "__main__":
    main()
