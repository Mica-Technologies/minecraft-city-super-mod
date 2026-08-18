#!/usr/bin/env python3
"""Validate a translated Minecraft .lang file against the English source.

Checks the things a reviewer who does not speak the target language cannot check by eye:
key parity and order, %s format specifiers, protected proper nouns surviving verbatim,
leading whitespace, encoding, and how much of the file came back untranslated.

Nothing here judges translation quality -- it catches the mechanical damage that makes a
language file break the game or silently lose entries, which is the part a non-speaker can
still be responsible for.

Note that Minecraft .lang files are read as raw UTF-8. Unlike Java .properties bundles they
must NOT use \\uXXXX escapes, so this checks for real UTF-8 bytes and rejects a BOM.

Usage:
    python validate_lang_translations.py <english.lang> <translated.lang>

Exits non-zero if any check fails, so it can gate a batch.
"""

import io
import re
import sys

# Never translated: manufacturers, agencies, the fictional city, and technical acronyms.
PROTECTED = [
    "EST", "Wheelock", "Simplex", "SpectrAlert", "Gentex", "TrueAlert", "Genesis",
    "Adaptahorn", "Integrity", "NOV", "GreenCobra", "DuraLife", "MacLean", "PUPI", "GE",
    "McCain", "Econolite", "Miovision", "Bose", "Verizon", "MBTA", "MassDOT", "LADOT",
    "Alto", "HOV", "FYA", "MPH", "FDC", "ARV", "ATV", "LED", "HVAC", "CSM", "TV",
    "APS", "ISA", "GCM", "MT", "BG", "TE",
]


def read_pairs(path):
    raw = io.open(path, "rb").read()
    text = raw.decode("utf-8")
    pairs, order = {}, []
    for line in text.split("\n"):
        if not line.strip():
            continue
        m = re.match(r"^([A-Za-z0-9_.]+)=(.*)$", line.rstrip("\r"))
        if not m:
            continue
        pairs[m.group(1)] = m.group(2)
        order.append(m.group(1))
    return pairs, order, raw, text


def duplicate_keys(order):
    """Keys appearing more than once. A dict silently keeps the last one, so check the list."""
    seen, dupes = set(), []
    for key in order:
        if key in seen and key not in dupes:
            dupes.append(key)
        seen.add(key)
    return dupes


def main():
    src_path, dst_path = sys.argv[1], sys.argv[2]
    src, src_order, src_raw, _ = read_pairs(src_path)
    dst, dst_order, dst_raw, dst_text = read_pairs(dst_path)

    problems, warnings = [], []

    if dst_raw.startswith(b"\xef\xbb\xbf"):
        problems.append("file starts with a UTF-8 BOM")
    # Match whatever the English file uses rather than imposing a convention: this repo's
    # en_us.lang is CRLF, and a translation that disagrees makes a noisy whole-file diff.
    src_crlf = b"\r\n" in src_raw
    dst_crlf = b"\r\n" in dst_raw
    if src_crlf != dst_crlf:
        problems.append("line endings differ from the English file (%s vs %s)"
                        % ("CRLF" if src_crlf else "LF", "CRLF" if dst_crlf else "LF"))

    for label, order in (("english", src_order), ("translated", dst_order)):
        dupes = duplicate_keys(order)
        if dupes:
            problems.append("%d duplicate key(s) in the %s file: %s"
                            % (len(dupes), label, dupes[:5]))

    missing = [k for k in src_order if k not in dst]
    extra = [k for k in dst_order if k not in src]
    if missing:
        problems.append("%d key(s) missing: %s" % (len(missing), missing[:8]))
    if extra:
        problems.append("%d unexpected key(s): %s" % (len(extra), extra[:8]))
    common_src = [k for k in src_order if k in dst]
    if common_src != [k for k in dst_order if k in src]:
        warnings.append("key order differs from the English file")

    identical = []
    for key in common_src:
        english, other = src[key], dst[key]

        if english.count("%s") != other.count("%s"):
            problems.append("%s: %%s count %d -> %d"
                            % (key, english.count("%s"), other.count("%s")))
        if len(english) - len(english.lstrip()) != len(other) - len(other.lstrip()):
            problems.append("%s: leading whitespace changed" % key)
        for term in PROTECTED:
            if re.search(r"\b%s\b" % re.escape(term), english) and term not in other:
                problems.append("%s: protected term %r lost -> %r" % (key, term, other))
        if english.strip() == other.strip() and re.search(r"[a-z]{4}", english):
            identical.append(key)

    print("=== %s" % dst_path)
    print("keys: %d source / %d translated" % (len(src), len(dst)))
    if identical:
        pct = 100.0 * len(identical) / max(1, len(common_src))
        warnings.append("%d value(s) (%.0f%%) identical to English, e.g. %s"
                        % (len(identical), pct, identical[:5]))

    for p in problems:
        print("  FAIL  %s" % p)
    for w in warnings:
        print("  warn  %s" % w)
    if not problems:
        print("  OK    key parity, format specifiers, protected terms, encoding all clean")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
