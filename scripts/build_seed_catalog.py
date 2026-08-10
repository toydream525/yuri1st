#!/usr/bin/env python3
"""Build app/src/main/assets/seed_catalog.json from the Bangumi v0 search API.

The seed contains the top public (non-NSFW) entries carrying the exact
"百合" or "轻百合" tags, grouped by category, so first launch has content
without waiting for a network sync.
"""

import json
import sys
import time
import urllib.request
from pathlib import Path

UA = "yuri1st-seed/0.1 (local seed builder)"
BASE_URL = "https://api.bgm.tv/v0/search/subjects"
PAGE_SIZE = 20
PAGES_PER_QUERY = 2
MAX_RETRIES = 3
QUERIES = [
    (2, [], "百合"),
    (2, [], "轻百合"),
    (1, ["漫画"], "百合"),
    (1, ["漫画"], "轻百合"),
    (1, ["小说"], "百合"),
    (1, ["小说"], "轻百合"),
    (4, [], "百合"),
    (4, [], "轻百合"),
]


def fetch_page(type_id, meta_tags, tag, offset):
    body = {
        "keyword": "",
        "sort": "rank",
        "filter": {
            "type": [type_id],
            "tag": [tag],
            "meta_tags": meta_tags,
            "nsfw": False,
        },
    }
    url = f"{BASE_URL}?limit={PAGE_SIZE}&offset={offset}"
    request = urllib.request.Request(
        url,
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": UA,
        },
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def fetch_page_with_retry(type_id, meta_tags, tag, offset):
    last_error = None
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            return fetch_page(type_id, meta_tags, tag, offset)
        except Exception as exc:  # noqa: BLE001
            last_error = exc
            print(f"retry {attempt}/{MAX_RETRIES} type={type_id} tag={tag} offset={offset}", file=sys.stderr)
            time.sleep(1.0 * attempt)
    raise last_error


def main():
    subjects = {}
    errors = 0
    for type_id, meta_tags, tag in QUERIES:
        for offset in range(0, PAGE_SIZE * PAGES_PER_QUERY, PAGE_SIZE):
            try:
                page = fetch_page_with_retry(type_id, meta_tags, tag, offset)
                for item in page.get("data", []):
                    subjects[item["id"]] = item
                print(f"type={type_id} meta={meta_tags or '-'} tag={tag} offset={offset}: +{len(page.get('data', []))}", file=sys.stderr)
            except Exception as exc:  # noqa: BLE001 - build script should tolerate remote hiccups
                errors += 1
                print(f"warning: type={type_id} tag={tag} offset={offset}: {exc}", file=sys.stderr)
            time.sleep(0.3)

    entries = sorted(subjects.values(), key=lambda item: -(item.get("rating") or {}).get("score", 0) or 0)
    output = {"generatedAt": time.strftime("%Y-%m-%d"), "subjects": entries}
    target = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "seed_catalog.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(output, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    size_kb = target.stat().st_size / 1024
    print(f"wrote {target} with {len(entries)} subjects ({size_kb:.0f} KB), errors={errors}", file=sys.stderr)
    if not entries:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
