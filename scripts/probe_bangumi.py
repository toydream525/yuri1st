#!/usr/bin/env python3
"""Probe Bangumi's experimental tag-only subject search without writing data."""

import argparse
import json
import os
import urllib.error
import urllib.request


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--category",
        choices=("anime", "manga", "light-novel", "game"),
        default="anime",
    )
    parser.add_argument("--limit", type=int, default=5)
    parser.add_argument("--nsfw", action="store_true")
    args = parser.parse_args()
    user_agent = os.environ.get("BANGUMI_USER_AGENT")
    if not user_agent:
        parser.error("set BANGUMI_USER_AGENT with a developer ID or project URL first")

    category_config = {
        "anime": (2, []),
        "manga": (1, ["漫画"]),
        "light-novel": (1, ["小说"]),
        "game": (4, []),
    }
    subject_type, meta_tags = category_config[args.category]

    def fetch_page(nsfw_only: bool) -> dict:
        payload = json.dumps(
            {
                "keyword": "",
                "sort": "rank",
                "filter": {
                    "type": [subject_type],
                    "meta_tags": meta_tags,
                    "tag": ["百合"],
                    "nsfw": nsfw_only,
                },
            },
            ensure_ascii=False,
        ).encode("utf-8")
        request = urllib.request.Request(
            f"https://api.bgm.tv/v0/search/subjects?limit={args.limit}&offset=0",
            data=payload,
            method="POST",
            headers={
                "Accept": "application/json",
                "Content-Type": "application/json",
                "User-Agent": user_agent,
            },
        )
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)

    accepted_by_id = {}
    try:
        for nsfw_only in ([False, True] if args.nsfw else [False]):
            result = fetch_page(nsfw_only)
            print(f"reported total (nsfw={str(nsfw_only).lower()}): {result.get('total', 0)}")
            for subject in result.get("data", []):
                if any(tag.get("name") == "百合" for tag in subject.get("tags", [])):
                    accepted_by_id[subject.get("id")] = subject
    except (urllib.error.URLError, urllib.error.HTTPError) as error:
        print(f"Probe failed: {error}")
        return 1

    print(f"page entries with exact 百合 tag after merge: {len(accepted_by_id)}")
    for subject in accepted_by_id.values():
        title = subject.get("name_cn") or subject.get("name") or "(untitled)"
        rating = subject.get("rating") or {}
        print(f"- {subject.get('id')} {title} score={rating.get('score', 0)} votes={rating.get('total', 0)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
