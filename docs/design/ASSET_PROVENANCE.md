# Bundled visual asset provenance

Status: verified asset files generated/downloaded on 2026-10-21. Runtime rendering is verified separately by the application checks.

## Fonts

Fonts come from the official Google Fonts repository (`google/fonts`, OFL directories). Each source is a variable TrueType font. Static weight instances were generated with fontTools 4.63.0 (`instantiateVariableFont`, wght axis only) so Compose and Android API 26+ do not need runtime downloads or variable-font configuration. The family names remain intact; these files are part of the application, not sold separately.

Both families are licensed under SIL Open Font License 1.1. Complete upstream copyright and license text is retained in `fonts/plus_jakarta_sans_OFL.txt` and `fonts/sora_OFL.txt`.

| Resource | Weight | Bytes | SHA-256 |
| --- | --- | --- | --- |
| `plus_jakarta_sans_regular.ttf` | 400 | 96,572 | `80ae9939a3c1aad4a74dfc59d57085643be02bf1d167befe8f0ec4f9a23bffe3` |
| `plus_jakarta_sans_medium.ttf` | 500 | 96,584 | `4191e6dc4d2c6519d35ca84691a3235fe1272bcc1b17e5021c4a94ed50483d7d` |
| `plus_jakarta_sans_semibold.ttf` | 600 | 96,564 | `d9cbf8f516244a7899434bc2b87ceb0d87503d9bc928ba40e731a85453364031` |
| `plus_jakarta_sans_bold.ttf` | 700 | 96,500 | `a91dd73aef74b098466a3af55d02ed285ca84077d417937e7c3959eccb4cf497` |
| `sora_semibold.ttf` | 600 | 58,604 | `d96ed1c2dcf73ec901cfe07c1bbb07d85517c0b33836d2e6dc7168069137cd91` |
| `sora_bold.ttf` | 700 | 58,568 | `533096f9211195f7def86ced0232c2851d67dde5d10b792054bcd6d4a16519db` |

Upstream source URLs:

- https://github.com/google/fonts/tree/main/ofl/plusjakartasans
- https://github.com/google/fonts/tree/main/ofl/sora

The downloaded source URLs use the upstream main branch; the SHA-256 values above identify the exact bundled static output. Binary inspection verified each OS/2 weight, the absence of fvar, and glyphs for basic Indonesian financial labels and rupiah numbers.

## Lottie illustrations

`res/raw/mankeu_empty.json` and `res/raw/mankeu_success.json` are original vector compositions authored for Mankeu in this change and distributed under the repository GPL-3.0 license. No third-party animation was copied. They contain only shape layers (rounded rectangles, strokes, circles, paths), no fonts, raster images, expressions, embedded financial data or remote resources. Colors use Arina green, mint, terracotta and paper white.

- `mankeu_empty.json`: 128 × 128 ledger illustration, 30fps / 60 frames; a restrained 12-frame entry fade and a line reveal completed at frame 24. The fully visible final frame is held through the rest of the composition and is suitable for Reduced/Off motion.
- `mankeu_success.json`: 128 × 128 badge, 30fps / 60 frames; trim-path check reveal completes at frame 28 and remains visible for the rest of the composition. At progress 1 the full check remains visible.

Both JSON files were parsed and checked for empty asset arrays and valid layer timelines. The application must treat them as decoration, keep accessible status text, honor Reduced/Off motion, and fall back to Material icons if parsing fails.

Animation file checksums (SHA-256):

- `mankeu_empty.json` (5,102 bytes): `79e56b147cb3db4256b06b27742f76dcec7b519dba8cda1d2b642ce9cea8a2b8`
- `mankeu_success.json` (1,654 bytes): `8a06766bb1d01b53a00bb1599f458210570d87a1378c8a6d1aa7beb1376600bc`
