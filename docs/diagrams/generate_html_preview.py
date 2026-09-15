#!/usr/bin/env python3
"""
Builds docs/diagrams/preview.html from the numbered Markdown files in this folder.

Diagrams are embedded as static <pre class="mermaid"> blocks. Prose is embedded as raw Markdown
and rendered in the browser with marked. Before rendering, every diagram is run through
mermaid.parse(), and the result is shown at the top of the page (and exposed as
window.diagramReport) so a syntax error is visible instead of a silently blank block.
"""

import glob
import html
import os
import re

DIAGRAMS_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_HTML = os.path.join(DIAGRAMS_DIR, "preview.html")
MERMAID_BLOCK = re.compile(r"```mermaid\s*\n(.*?)\n```", re.DOTALL)

TEMPLATE = """<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Diagram Sistem Mankeu</title>
<script src="https://cdn.jsdelivr.net/npm/mermaid@11.4.1/dist/mermaid.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/marked@15.0.12/marked.min.js"></script>
<style>
  :root { --bg:#f8fafc; --surface:#ffffff; --text:#0f172a; --muted:#475569; --primary:#1d4ed8; --border:#e2e8f0; --ok:#15803d; --bad:#b91c1c; }
  @media (prefers-color-scheme: dark) {
    :root { --bg:#0f172a; --surface:#1e293b; --text:#f1f5f9; --muted:#94a3b8; --primary:#60a5fa; --border:#334155; --ok:#4ade80; --bad:#f87171; }
  }
  body { font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; background:var(--bg); color:var(--text); margin:0; padding:24px; line-height:1.6; }
  .container { max-width:1200px; margin:0 auto; }
  header { border-bottom:1px solid var(--border); padding-bottom:16px; margin-bottom:24px; }
  h1 { color:var(--primary); margin:0 0 4px; font-size:1.8rem; }
  nav { display:flex; flex-wrap:wrap; gap:8px; margin-top:16px; }
  nav a { background:var(--surface); color:var(--text); border:1px solid var(--border); padding:6px 12px; border-radius:8px; text-decoration:none; font-size:.9rem; }
  nav a:hover { border-color:var(--primary); color:var(--primary); }
  #report { margin-top:12px; font-weight:600; }
  #report.ok { color:var(--ok); } #report.bad { color:var(--bad); }
  #report pre { white-space:pre-wrap; font-weight:400; }
  section.card { background:var(--surface); border:1px solid var(--border); border-radius:12px; padding:8px 24px 24px; margin-bottom:28px; }
  .prose table { border-collapse:collapse; width:100%; margin:12px 0; display:block; overflow-x:auto; }
  .prose th, .prose td { border:1px solid var(--border); padding:6px 10px; text-align:left; vertical-align:top; }
  .prose code { background:var(--bg); padding:1px 4px; border-radius:4px; }
  .prose blockquote { border-left:4px solid var(--primary); margin:12px 0; padding:4px 12px; color:var(--muted); }
  pre.mermaid { background:transparent; overflow-x:auto; text-align:center; margin:16px 0; }
</style>
</head>
<body>
<div class="container">
  <header>
    <h1>Diagram Sistem Mankeu</h1>
    <div>Arsitektur, basis data, alur pengguna, proses bisnis, dan siklus hidup. Dibangun dari <code>docs/diagrams/*.md</code>.</div>
    <nav>__NAV__</nav>
    <div id="report">Memvalidasi diagram...</div>
  </header>
  <main>
__SECTIONS__
  </main>
</div>
<script>
  document.querySelectorAll('script[type="text/markdown"]').forEach((source) => {
    const target = document.createElement('div');
    target.className = 'prose';
    target.innerHTML = marked.parse(source.textContent);
    source.replaceWith(target);
  });

  const dark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  mermaid.initialize({ startOnLoad: false, theme: dark ? 'dark' : 'default', securityLevel: 'strict' });

  (async () => {
    const blocks = [...document.querySelectorAll('pre.mermaid')];
    const errors = [];
    for (const block of blocks) {
      try {
        await mermaid.parse(block.textContent);
      } catch (error) {
        errors.push(block.dataset.source + ': ' + String(error.message || error).split('\\n').slice(0, 3).join(' '));
      }
    }
    await mermaid.run({ nodes: blocks, suppressErrors: true });
    const rendered = blocks.filter((block) => block.querySelector('svg') && !block.querySelector('.error-icon')).length;
    window.diagramReport = { total: blocks.length, rendered, errors };
    const report = document.getElementById('report');
    report.className = errors.length ? 'bad' : 'ok';
    report.textContent = errors.length
      ? errors.length + ' dari ' + blocks.length + ' diagram gagal diparse.'
      : 'Semua ' + blocks.length + ' diagram lolos parser Mermaid dan dirender (' + rendered + ').';
    if (errors.length) {
      const detail = document.createElement('pre');
      detail.textContent = errors.join('\\n');
      report.appendChild(detail);
    }
  })();
</script>
</body>
</html>
"""


def markdown_script(text):
    # Raw Markdown inside a script element: only a closing tag could end it early.
    return '<script type="text/markdown">' + text.replace("</", "<\\/") + "</script>"


def build_section(path):
    name = os.path.basename(path)
    with open(path, "r", encoding="utf-8") as handle:
        text = handle.read()
    title_match = re.search(r"^#\s+(.+)$", text, re.MULTILINE)
    title = title_match.group(1).strip() if title_match else name
    body = re.sub(r"^#\s+.+$", "", text, count=1, flags=re.MULTILINE)
    section_id = re.sub(r"[^a-zA-Z0-9_-]", "", name[:-3])

    parts = []
    cursor = 0
    for index, match in enumerate(MERMAID_BLOCK.finditer(body), start=1):
        prose = body[cursor:match.start()].strip()
        if prose:
            parts.append(markdown_script(prose))
        parts.append(
            f'<pre class="mermaid" data-source="{html.escape(name)} #{index}">'
            f"{html.escape(match.group(1))}</pre>"
        )
        cursor = match.end()
    tail = body[cursor:].strip()
    if tail:
        parts.append(markdown_script(tail))

    nav = f'<a href="#{section_id}">{html.escape(title.split(" - ")[0])}</a>'
    section = (
        f'<section id="{section_id}" class="card">\n<h2>{html.escape(title)}</h2>\n'
        + "\n".join(parts)
        + "\n</section>"
    )
    return nav, section


def main():
    files = sorted(glob.glob(os.path.join(DIAGRAMS_DIR, "[0-9][0-9]_*.md")))
    built = [build_section(path) for path in files]
    output = TEMPLATE.replace("__NAV__", "\n    ".join(nav for nav, _ in built))
    output = output.replace("__SECTIONS__", "\n".join(section for _, section in built))
    with open(OUTPUT_HTML, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(output)
    count = sum(len(MERMAID_BLOCK.findall(open(path, encoding="utf-8").read())) for path in files)
    print(f"[OK] {OUTPUT_HTML} ({len(files)} dokumen, {count} diagram)")


if __name__ == "__main__":
    main()
