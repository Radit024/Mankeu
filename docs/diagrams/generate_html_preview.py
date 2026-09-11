#!/usr/bin/env python3
"""
Mankeu Diagram Generator & HTML Preview Builder
Reads all markdown files in docs/diagrams/ and generates a single interactive
offline-capable HTML preview file (preview.html) using Mermaid.js.
"""

import os
import re
import glob

DIAGRAMS_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_HTML = os.path.join(DIAGRAMS_DIR, "preview.html")

HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mankeu Architecture & Domain Diagrams</title>
    <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
    <meta http-equiv="Pragma" content="no-cache">
    <meta http-equiv="Expires" content="0">
    <script src="https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"></script>
    <style>
        :root {
            --bg: #f8fafc;
            --surface: #ffffff;
            --text: #0f172a;
            --muted: #64748b;
            --primary: #2563eb;
            --border: #e2e8f0;
        }
        @media (prefers-color-scheme: dark) {
            :root {
                --bg: #0f172a;
                --surface: #1e293b;
                --text: #f8fafc;
                --muted: #94a3b8;
                --primary: #3b82f6;
                --border: #334155;
            }
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: var(--bg);
            color: var(--text);
            margin: 0;
            padding: 24px;
            line-height: 1.6;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
        }
        header {
            margin-bottom: 36px;
            border-bottom: 1px solid var(--border);
            padding-bottom: 16px;
        }
        h1 {
            font-size: 2rem;
            margin-bottom: 8px;
            color: var(--primary);
        }
        p.subtitle {
            color: var(--muted);
            margin: 0;
            font-size: 1.1rem;
        }
        nav {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin: 20px 0;
        }
        nav a {
            background: var(--surface);
            color: var(--text);
            border: 1px solid var(--border);
            padding: 8px 16px;
            border-radius: 8px;
            text-decoration: none;
            font-size: 0.9rem;
            font-weight: 500;
            transition: all 0.2s ease;
        }
        nav a:hover {
            border-color: var(--primary);
            color: var(--primary);
        }
        .card {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 24px;
            margin-bottom: 32px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }
        .card h2 {
            margin-top: 0;
            border-bottom: 1px solid var(--border);
            padding-bottom: 10px;
            font-size: 1.4rem;
        }
        .mermaid {
            display: flex;
            justify-content: center;
            background: var(--surface);
            padding: 20px 0;
            overflow-x: auto;
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>📊 Dokumentasi Diagram Sistem Mankeu</h1>
            <p class="subtitle">Katalog visual arsitektur, domain model, proses bisnis, dan siklus hidup sistem keuangan UMKM.</p>
            <nav>
                __NAV_LINKS__
            </nav>
        </header>

        <main>
            __CONTENT_SECTIONS__
        </main>
    </div>

    <script>
        mermaid.initialize({
            startOnLoad: true,
            theme: window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'default',
            securityLevel: 'loose',
            flowchart: { useMaxWidth: true, htmlLabels: true }
        });
    </script>
</body>
</html>
"""

def extract_diagrams():
    files = sorted(glob.glob(os.path.join(DIAGRAMS_DIR, "*.md")))
    nav_links = []
    content_sections = []

    for file_path in files:
        filename = os.path.basename(file_path)
        if filename.lower() == "readme.md":
            continue

        with open(file_path, "r", encoding="utf-8") as f:
            text = f.read()

        title_match = re.search(r"^#\s+(.+)$", text, re.MULTILINE)
        title = title_match.group(1) if title_match else filename

        section_id = re.sub(r"[^a-zA-Z0-9_-]", "", filename.replace(".md", ""))
        nav_links.append(f'<a href="#{section_id}">{title.split("-")[0].strip()}</a>')

        # Find mermaid blocks
        mermaid_blocks = re.findall(r"```mermaid\s*\n(.*?)\n```", text, re.DOTALL)
        
        section_html = f'<section id="{section_id}" class="card">\n'
        section_html += f'<h2>📄 {title}</h2>\n'
        
        # Include brief introductory text before mermaid if available
        intro_text = text.split("```mermaid")[0].strip()
        # Remove primary heading
        intro_clean = re.sub(r"^#\s+.+$", "", intro_text, flags=re.MULTILINE).strip()
        if intro_clean:
            # simple p wrapping
            section_html += f'<p style="color: var(--muted);">{intro_clean}</p>\n'

        for idx, block in enumerate(mermaid_blocks):
            section_html += f'<div class="mermaid">\n{block}\n</div>\n'

        section_html += '</section>\n'
        content_sections.append(section_html)

    full_html = HTML_TEMPLATE.replace("__NAV_LINKS__", "\n".join(nav_links))
    full_html = full_html.replace("__CONTENT_SECTIONS__", "\n".join(content_sections))

    with open(OUTPUT_HTML, "w", encoding="utf-8") as f:
        f.write(full_html)

    print(f"[OK] Pratinjau interaktif berhasil dibuat di: {OUTPUT_HTML}")

if __name__ == "__main__":
    extract_diagrams()
