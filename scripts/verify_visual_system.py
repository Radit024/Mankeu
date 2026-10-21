#!/usr/bin/env python3
"""Audit the real Compose brand tokens and offline assets (stdlib only).

Run from any directory: python scripts/verify_visual_system.py
This does not replace device rendering, TalkBack or large-font checks.
"""
from __future__ import annotations

import json
import re
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
SOURCE = ROOT / "app/src/main/java/com/mankeu/presentation"
failures: list[str] = []
checks = 0
contrasts: list[tuple[str, float, float]] = []


def check(condition: bool, message: str) -> None:
    global checks
    checks += 1
    if not condition:
        failures.append(message)


def block(source: str, name: str) -> str:
    found = re.search(rf"\bval {name}\s*=\s*\w+\((.*?)\n\)", source, re.S)
    if not found:
        raise ValueError(f"Cannot find palette {name}")
    return found.group(1)


def colors(source: str) -> dict[str, str]:
    return dict(re.findall(r"(\w+)\s*=\s*Color\(0xFF([0-9A-Fa-f]{6})\)", source))


def luminance(color: str) -> float:
    values = [int(color[i : i + 2], 16) / 255 for i in (0, 2, 4)]
    linear = [v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4 for v in values]
    return sum(c * w for c, w in zip(linear, (0.2126, 0.7152, 0.0722)))


def contrast(label: str, foreground: str, background: str, threshold: float = 4.5) -> None:
    low, high = sorted((luminance(foreground), luminance(background)))
    ratio = (high + 0.05) / (low + 0.05)
    contrasts.append((label, ratio, threshold))
    check(ratio >= threshold, f"{label}: #{foreground} on #{background} = {ratio:.2f}:1, needs {threshold}:1")


def audit_palette() -> None:
    source = (SOURCE / "Theme.kt").read_text(encoding="utf-8")
    for mode, required in (
        ("Light", {"primary": "2D6A4F", "secondary": "E07A5F", "background": "FAFAF8", "surfaceContainerLow": "FFFFFF", "onSurface": "2C2A29", "onSurfaceVariant": "6B6866"}),
        ("Dark", {"primary": "52B788", "secondary": "D4836A", "background": "1E2620", "surfaceContainerLow": "262D28", "onSurface": "DDE8DF", "onSurfaceVariant": "8BA08E"}),
    ):
        palette = colors(block(source, f"Mankeu{mode}Colors"))
        semantic_source = block(source, f"Mankeu{mode}SemanticColors")
        semantic = colors(semantic_source)
        for role, expected in required.items():
            check(palette.get(role) == expected, f"{mode}.{role} must preserve reference #{expected}")
        for background, foreground in (
            ("primary", "onPrimary"), ("primaryContainer", "onPrimaryContainer"),
            ("secondary", "onSecondary"), ("secondaryContainer", "onSecondaryContainer"),
            ("tertiary", "onTertiary"), ("tertiaryContainer", "onTertiaryContainer"),
            ("error", "onError"), ("errorContainer", "onErrorContainer"),
            ("background", "onBackground"), ("inverseSurface", "inverseOnSurface"),
        ):
            contrast(f"{mode}.{foreground}/{background}", palette[foreground], palette[background])
        # Text drawn in cards, notices, form surfaces and the result snackbar. Include explicit
        # variant/bright/dim roles: checking just the five container tiers missed a dark-chip
        # regression even though the secondary foreground was correct on the normal card.
        for surface in (
            "surface", "surfaceVariant", "surfaceBright", "surfaceDim", "surfaceContainerLowest",
            "surfaceContainerLow", "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest",
        ):
            contrast(f"{mode}.onSurface/{surface}", palette["onSurface"], palette[surface])
            contrast(f"{mode}.onSurfaceVariant/{surface}", palette["onSurfaceVariant"], palette[surface])
            for foreground in ("success", "danger"):
                contrast(f"{mode}.{foreground}/{surface}", semantic[foreground], palette[surface])
            for foreground in ("primary", "error"):
                contrast(f"{mode}.{foreground}/{surface} text", palette[foreground], palette[surface])
        contrast(f"{mode}.outline/card control", palette["outline"], palette["surfaceContainerLow"], 3)
        for background, foreground in (
            ("success", "onSuccess"), ("danger", "onDanger"),
            ("successContainer", "onSuccessContainer"), ("dangerContainer", "onDangerContainer"),
            ("warningContainer", "onWarningContainer"),
        ):
            contrast(f"{mode}.{foreground}/{background}", semantic[foreground], semantic[background])
        for foreground, container in (("success", "successContainer"), ("danger", "dangerContainer")):
            contrast(f"{mode}.{foreground}/{container} icon", semantic[foreground], semantic[container], 3)
        contrast(f"{mode}.primary/primaryContainer icon", palette["primary"], palette["primaryContainer"], 3)
        contrast(f"{mode}.error/errorContainer icon", palette["error"], palette["errorContainer"], 3)
        chart = re.search(r"chart\s*=\s*listOf\((.*?)\n\s*\)", semantic_source, re.S)
        check(chart is not None, f"{mode} chart palette must exist")
        if chart:
            values = re.findall(r"Color\(0xFF([0-9A-Fa-f]{6})\)", chart.group(1))
            check(len(values) >= 6, f"{mode} requires six category chart colors")
            for i, value in enumerate(values):
                contrast(f"{mode}.chart[{i}]/card", value, palette["surfaceContainerLow"], 3)
        splash = RES / ("values" if mode == "Light" else "values-night") / "colors.xml"
        check(f">#{palette['background']}<" in splash.read_text(encoding="utf-8"), f"{mode} splash must equal background")


def audit_fonts() -> None:
    source = (SOURCE / "Typography.kt").read_text(encoding="utf-8")
    expected = {
        "plus_jakarta_sans_regular": 400, "plus_jakarta_sans_medium": 500,
        "plus_jakarta_sans_semibold": 600, "plus_jakarta_sans_bold": 700,
        "sora_semibold": 600, "sora_bold": 700,
    }
    for name, weight in expected.items():
        file = RES / "font" / f"{name}.ttf"
        check(file.exists(), f"Missing local font {name}")
        if not file.exists():
            continue
        data = file.read_bytes()
        check(data[:4] in (b"\0\1\0\0", b"OTTO"), f"{name} is not an OpenType font")
        count = struct.unpack_from(">H", data, 4)[0]
        tables = {data[12 + i * 16 : 16 + i * 16].decode("ascii"): struct.unpack_from(">II", data, 20 + i * 16) for i in range(count)}
        check("fvar" not in tables, f"{name} must be instantiated/static for API 26")
        check("OS/2" in tables and "cmap" in tables, f"{name} needs weight metadata and glyph map")
        if "OS/2" in tables:
            actual = struct.unpack_from(">H", data, tables["OS/2"][0] + 4)[0]
            check(actual == weight, f"{name} OS/2 weight is {actual}, expected {weight}")
        check(f"R.font.{name}" in source, f"{name} is bundled but not wired into typography")
    check("fontFamily = Sora" in source and "fontFamily = PlusJakartaSans" in source, "Typography must assign both font families")
    for name in ("PlusJakartaSans_OFL.txt", "Sora_OFL.txt"):
        # The asset task records exact filenames; accept separator/case differences only.
        licenses = list((ROOT / "docs/design/fonts").glob("*.txt"))
        family = name.split("_")[0].lower()
        check(any(family in re.sub(r"[^a-z]", "", p.stem.lower()) and "SIL OPEN FONT LICENSE" in p.read_text(encoding="utf-8") for p in licenses), f"Missing full {family} SIL OFL license")


def walk(value):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from walk(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk(child)


def final_value(property_value):
    if not isinstance(property_value, dict):
        return None
    key = property_value.get("k")
    if property_value.get("a") == 1 and isinstance(key, list):
        return key[-1].get("s") if key and isinstance(key[-1], dict) else None
    return key


def audit_lottie() -> None:
    for name in ("mankeu_empty", "mankeu_success"):
        file = RES / "raw" / f"{name}.json"
        check(file.exists(), f"Missing local Lottie {name}")
        if not file.exists():
            continue
        data = json.loads(file.read_text(encoding="utf-8"))
        check(data.get("w", 0) > 0 and data.get("h", 0) > 0, f"{name} needs a canvas")
        check(data.get("op", 0) > data.get("ip", 0), f"{name} needs a duration")
        check(bool(data.get("layers")), f"{name} needs drawable layers")
        check(not data.get("assets"), f"{name} must be standalone without external media")
        check("http" not in file.read_text(encoding="utf-8"), f"{name} must not request remote media")
        animated = False
        for obj in walk(data):
            if obj.get("a") == 1:
                animated = True
            if obj.get("ty") == "tm":
                final = final_value(obj.get("e"))
                check(final in (100, [100]), f"{name} trim path must be complete at static final frame")
            if "ks" in obj and isinstance(obj["ks"], dict) and "o" in obj["ks"]:
                check(final_value(obj["ks"]["o"]) in (100, [100]), f"{name} layer must remain visible at static final frame")
        check(animated, f"{name} must contain a real one-shot animation")


def audit_splash_logo() -> None:
    """The splash must show the Arina mark from public/logo arina.svg, safely inside the mask."""
    source_svg = ROOT / "public" / "logo arina.svg"
    drawable = RES / "drawable" / "ic_arina_logo.xml"
    inset = RES / "drawable" / "ic_arina_logo_inset.xml"
    check(source_svg.exists(), "Missing public/logo arina.svg source")
    check(drawable.exists(), "Missing drawable/ic_arina_logo.xml")
    check(inset.exists(), "Missing drawable/ic_arina_logo_inset.xml")
    if not (source_svg.exists() and drawable.exists()):
        return
    markup = drawable.read_text(encoding="utf-8")
    path_data = re.search(r'android:pathData="([^"]+)"', markup)
    check(path_data is not None, "ic_arina_logo.xml needs a path")
    check('android:fillColor="@color/arina_brand_green"' in markup, "ic_arina_logo.xml must use the brand colour resource")
    check('android:fillType="evenOdd"' in markup, "ic_arina_logo.xml must keep the source fill rule")
    check('android:viewportWidth="8669.86"' in markup, "ic_arina_logo.xml must keep the source viewBox")
    if path_data:
        svg_paths = re.findall(r"<path[^>]*\bd=\"([^\"]+)\"", source_svg.read_text(encoding="utf-8"))
        check(bool(svg_paths), "logo arina.svg has no path")
        if svg_paths:
            normalise = lambda value: re.sub(r"[\s,]+", "", value)
            check(
                normalise(path_data.group(1)) == normalise(max(svg_paths, key=len)),
                "ic_arina_logo.xml no longer matches public/logo arina.svg",
            )
    wrapper = inset.read_text(encoding="utf-8")
    check('android:drawable="@drawable/ic_arina_logo"' in wrapper, "the inset must wrap ic_arina_logo")
    insets = [int(value) for value in re.findall(r'android:inset(?:Left|Top|Right|Bottom)="(\d+)%"', wrapper)]
    check(len(insets) == 4 and min(insets) >= 10, "the splash mark needs a safe-zone inset on every side")
    for mode, folder in (("Light", "values"), ("Dark", "values-night")):
        themes = (RES / folder / "themes.xml").read_text(encoding="utf-8")
        check(
            "<item name=\"windowSplashScreenAnimatedIcon\">@drawable/ic_arina_logo_inset</item>" in themes,
            f"{mode} splash must show the Arina mark",
        )
        check(
            "<item name=\"windowSplashScreenBackground\">@color/splash_screen_background</item>" in themes,
            f"{mode} splash must use splash_screen_background",
        )
        palette = (RES / folder / "colors.xml").read_text(encoding="utf-8")
        logo = re.search(r'<color name="arina_brand_green">#([0-9A-Fa-f]{6})</color>', palette)
        background = re.search(r'<color name="splash_screen_background">#([0-9A-Fa-f]{6})</color>', palette)
        check(logo is not None, f"{mode} needs an arina_brand_green colour")
        check(background is not None, f"{mode} needs a splash_screen_background colour")
        check(background and background.group(1).upper() == "FAFAF8", f"{mode} splash must remain light (#FAFAF8)")
        if logo and background:
            # WCAG 1.4.11 non-text contrast for a graphical object: 3:1.
            contrast(f"{mode}.splash logo/background", logo.group(1).upper(), background.group(1).upper(), 3)


def main() -> int:
    for audit in (audit_palette, audit_fonts, audit_lottie, audit_splash_logo):
        try:
            audit()
        except (ValueError, KeyError, OSError, struct.error) as error:
            failures.append(f"{audit.__name__}: {error}")
    if failures:
        print(f"FAIL: {len(failures)} visual-system issue(s), {checks} checks")
        for failure in failures:
            print(f"  - {failure}")
        return 1
    worst = min(contrasts, key=lambda item: item[1] / item[2])
    print(f"PASS: {checks} checks; {len(contrasts)} actual-token contrast pairs; 6 fonts; 2 offline Lottie assets")
    print(f"Tightest threshold margin: {worst[0]} = {worst[1]:.2f}:1 (minimum {worst[2]}:1)")
    print("Device rendering, TalkBack and font scaling still require instrumented/manual verification.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
