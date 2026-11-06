#!/usr/bin/env python3
"""Extract Android strings.xml files into platform-neutral JSON.

Reads ../../../android/app/src/main/res/values{,-xx}/strings.xml and writes
../data/strings/{lang}.json, one file per shipped locale. Preserves Android's
positional format tokens (%1$s, %2$d) verbatim so JS/Swift/other consumers
can decide how to render them. Strips the [REVIEW] sentinel prefix and emits
a sibling `_review.json` listing which keys were machine-translated.
"""
from __future__ import annotations

import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

HERE = Path(__file__).resolve().parent
PKG_ROOT = HERE.parent
REPO_ROOT = PKG_ROOT.parent.parent
RES_ROOT = REPO_ROOT / "android" / "app" / "src" / "main" / "res"
OUT_DIR = PKG_ROOT / "data" / "strings"

SHIPPED = ["en", "es", "fr", "pt", "hi", "ar", "de", "id", "te"]
REVIEW_PREFIX = "[REVIEW]"

# Android escapes: \' \" \n \\ — unescape to natural characters.
UNESCAPE = {r"\'": "'", r'\"': '"', r"\n": "\n", r"\\": "\\"}
UNESCAPE_RE = re.compile("|".join(re.escape(k) for k in UNESCAPE))


def unescape_android(text: str) -> str:
    return UNESCAPE_RE.sub(lambda m: UNESCAPE[m.group(0)], text)


def extract(lang: str) -> tuple[dict[str, str], list[str]]:
    folder = "values" if lang == "en" else f"values-{lang}"
    path = RES_ROOT / folder / "strings.xml"
    if not path.exists():
        print(f"warn: {path} missing, skipping {lang}", file=sys.stderr)
        return {}, []

    tree = ET.parse(path)
    root = tree.getroot()
    strings: dict[str, str] = {}
    review: list[str] = []

    for node in root.findall("string"):
        name = node.get("name")
        if not name:
            continue
        # ElementTree concatenates text + tail of children; for plain <string>
        # nodes this equals node.text, which is what we want.
        raw = (node.text or "").strip()
        if raw.startswith(REVIEW_PREFIX):
            raw = raw[len(REVIEW_PREFIX) :].lstrip()
            review.append(name)
        strings[name] = unescape_android(raw)

    return strings, review


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    totals: dict[str, int] = {}
    reviews: dict[str, list[str]] = {}

    for lang in SHIPPED:
        strings, review = extract(lang)
        if not strings:
            continue
        out = OUT_DIR / f"{lang}.json"
        out.write_text(
            json.dumps(strings, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        totals[lang] = len(strings)
        if review:
            reviews[lang] = review

    # One combined index of review-pending keys per language.
    (OUT_DIR / "_review.json").write_text(
        json.dumps(reviews, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )

    for lang, n in totals.items():
        pending = len(reviews.get(lang, []))
        print(f"  {lang}: {n} strings  ({pending} pending review)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
