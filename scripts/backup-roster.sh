#!/usr/bin/env bash
# Manually back up the live cloud roster into data/roster.json and commit locally.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLOUD_URL="${CLOUD_URL:-https://jsonblob.com/api/jsonBlob/019fe58b-6c60-7a87-99b0-a05dfe8465d0}"

mkdir -p "$ROOT/data"
curl -fsSL "$CLOUD_URL" -o /tmp/cloud-roster.json
python3 - <<PY
import json
from pathlib import Path
cloud = json.loads(Path('/tmp/cloud-roster.json').read_text())
path = Path("$ROOT/data/roster.json")
path.write_text(json.dumps(cloud, indent=2) + "\n")
print(f"Wrote {path} with {len(cloud.get('players', []))} players")
for p in cloud.get('players', []):
    print(f" - {p.get('name')}")
PY

echo "Review data/roster.json, then commit/push when ready."
