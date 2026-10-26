#!/usr/bin/env python3
"""Convert the private MythicMobs Sculk Goliath source to native mod data.

All 115 named definitions, including cooldown-only guard definitions, are
preserved in source order. Runtime parses mechanics, targets, conditions and
delays. No MythicMobs plugin or YAML library is needed in-game.
"""

import argparse
import json
import re
import zipfile
from pathlib import Path

import yaml


def triggers(source: dict, key: str) -> list[dict]:
    events = []
    for line in source[key].get("Skills", []):
        text = str(line).strip()
        if "~on" not in text:
            continue
        event_match = re.search(r"~(on[A-Za-z]+)(?::(\d+))?", text)
        if not event_match:
            continue
        kind = "random" if text.lower().startswith("randomskill") else "skill"
        group = re.search(r"\{(?:skills?|skill)\s*=\s*([^};]+)", text, re.I)
        if not group:
            continue
        names = [item.strip() for item in group.group(1).split(",")]
        health = re.search(r"<\s*(\d+)\s*%", text)
        events.append({
            "event": event_match.group(1).lower(),
            "period": int(event_match.group(2) or 0),
            "type": kind,
            "skills": names,
            "health_below": int(health.group(1)) if health else 101,
            "requires_mace": "holding{types=MACE}" in text,
        })
    return events


def main() -> None:
    arg = argparse.ArgumentParser()
    arg.add_argument("source_zip", type=Path)
    arg.add_argument("output", type=Path)
    args = arg.parse_args()
    with zipfile.ZipFile(args.source_zip) as archive:
        skills = yaml.safe_load(archive.read("MythicMobs/skills/HPC_SCULK_GOLIATH.yml"))
        mobs = yaml.safe_load(archive.read("MythicMobs/mobs/HPC_SCULK_GOLIATH.yml"))
    assert len(skills) == 115, f"expected all 115 original definitions; got {len(skills)}"
    definitions = {}
    for name, definition in skills.items():
        entry = definition or {}
        definitions[name] = {
            "cooldown": float(entry.get("Cooldown", 0) or 0),
            "conditions": entry.get("Conditions") or [],
            "steps": entry.get("Skills") or [],
        }
    data = {
        "format": 1,
        "source": "HypactC Sculk Goliath MythicMobs (private TNK3 conversion)",
        "definition_count": len(definitions),
        "mechanic_count": sum(len(value["steps"]) for value in definitions.values()),
        "definitions": definitions,
        "triggers": {
            "phase1": triggers(mobs, "HPC_SCULK_GOLIATH"),
            "phase2": triggers(mobs, "HPC_SCULK_GOLIATH_PHASE2"),
        },
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Converted {data['definition_count']} definitions and {data['mechanic_count']} mechanics")
    print(f"Phase triggers: {len(data['triggers']['phase1'])}, {len(data['triggers']['phase2'])}")


if __name__ == "__main__":
    main()
