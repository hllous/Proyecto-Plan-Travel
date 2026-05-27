"""
Quality Metrics Prototype — ProyectoPlanTravel
Metrics: (1) Test Coverage via JaCoCo XML, (2) Defect Density via git log
Output:  metrics/report.html
"""

import subprocess
import xml.etree.ElementTree as ET
import re
from pathlib import Path
from datetime import datetime

# ── Config ────────────────────────────────────────────────────────────────────

REPO_ROOT   = Path(__file__).parent.parent
JACOCO_XML  = REPO_ROOT / "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
SRC_MAIN    = REPO_ROOT / "app/src/main"
DEFECT_RE   = re.compile(r"\b(fix|bug|corregir|error|hotfix|patch|crash|defect)\b", re.IGNORECASE)
SKIP_PACKAGES = {"hilt_aggregated_deps", "dagger"}

# ── Metric 1: Coverage ────────────────────────────────────────────────────────

def parse_coverage():
    tree = ET.parse(JACOCO_XML)
    root = tree.getroot()

    # Overall totals from root-level <counter> elements
    overall = {}
    for c in root.findall("counter"):
        overall[c.get("type")] = {
            "covered": int(c.get("covered")),
            "missed":  int(c.get("missed")),
        }

    def pct(d):
        total = d["covered"] + d["missed"]
        return round(d["covered"] / total * 100, 1) if total else 0.0

    # Per-package breakdown (skip generated Hilt/Dagger packages)
    packages = []
    for pkg in root.findall("package"):
        name = pkg.get("name", "")
        if any(skip in name for skip in SKIP_PACKAGES):
            continue
        short = name.replace("com/hllous/plantravel/", "").replace("/", ".")
        if not short:
            short = "root"
        counters = {c.get("type"): {"covered": int(c.get("covered")), "missed": int(c.get("missed"))}
                    for c in pkg.findall("counter")}
        if "LINE" not in counters:
            continue
        packages.append({
            "name":  short,
            "covered": counters["LINE"]["covered"],
            "missed":  counters["LINE"]["missed"],
            "pct":     pct(counters["LINE"]),
        })

    packages.sort(key=lambda x: x["pct"], reverse=True)

    return {
        "line_pct":      pct(overall.get("LINE", {"covered": 0, "missed": 1})),
        "method_pct":    pct(overall.get("METHOD", {"covered": 0, "missed": 1})),
        "branch_pct":    pct(overall.get("BRANCH", {"covered": 0, "missed": 1})),
        "lines_covered": overall.get("LINE", {}).get("covered", 0),
        "lines_total":   overall.get("LINE", {}).get("covered", 0) + overall.get("LINE", {}).get("missed", 0),
        "packages":      packages,
    }

# ── Metric 2: Defect Density ──────────────────────────────────────────────────

def count_production_loc():
    total = 0
    for f in SRC_MAIN.rglob("*.kt"):
        total += sum(1 for line in f.read_text(encoding="utf-8", errors="ignore").splitlines()
                     if line.strip())
    return total

def parse_defects():
    result = subprocess.run(
        ["git", "log", "--oneline", "--no-merges"],
        capture_output=True, text=True, cwd=REPO_ROOT
    )
    commits = result.stdout.strip().splitlines()
    defect_commits = [c for c in commits if DEFECT_RE.search(c)]
    return {
        "total_commits":   len(commits),
        "defect_commits":  len(defect_commits),
        "defect_messages": defect_commits,
    }

# ── HTML Report ───────────────────────────────────────────────────────────────

def build_report(cov, defects, loc):
    density = round(defects["defect_commits"] / (loc / 1000), 2) if loc > 0 else 0

    pkg_labels = [p["name"] for p in cov["packages"]]
    pkg_covered = [p["covered"] for p in cov["packages"]]
    pkg_missed  = [p["missed"]  for p in cov["packages"]]
    pkg_pcts    = [p["pct"]     for p in cov["packages"]]

    defect_rows = "".join(
        f'<tr><td class="mono">{msg[:80]}</td></tr>'
        for msg in defects["defect_messages"]
    )

    pkg_rows = "".join(
        f"""<tr>
              <td>{p['name']}</td>
              <td>{p['covered']}</td>
              <td>{p['missed']}</td>
              <td>
                <div class="bar-wrap">
                  <div class="bar-fill {'bar-good' if p['pct']>=50 else 'bar-mid' if p['pct']>=20 else 'bar-bad'}"
                       style="width:{p['pct']}%"></div>
                </div>
                {p['pct']}%
              </td>
            </tr>"""
        for p in cov["packages"]
    )

    html = f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Métricas de Calidad — ProyectoPlanTravel</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<style>
  :root {{
    --bg: #0f1117; --surface: #1a1d27; --surface2: #22263a;
    --accent: #6c63ff; --green: #4caf50; --yellow: #ffc107; --red: #f44336;
    --text: #e8eaf6; --muted: #9e9eb8; --border: #2e3255;
    --radius: 12px; --font: 'Segoe UI', system-ui, sans-serif;
  }}
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{ background: var(--bg); color: var(--text); font-family: var(--font); padding: 32px 24px; }}
  h1 {{ font-size: 1.8rem; font-weight: 700; margin-bottom: 4px; }}
  .subtitle {{ color: var(--muted); font-size: 0.9rem; margin-bottom: 36px; }}
  .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 36px; }}
  .card {{ background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); padding: 24px; }}
  .card-label {{ font-size: 0.75rem; text-transform: uppercase; letter-spacing: .08em; color: var(--muted); margin-bottom: 8px; }}
  .card-value {{ font-size: 2.4rem; font-weight: 800; }}
  .card-sub {{ font-size: 0.8rem; color: var(--muted); margin-top: 4px; }}
  .green {{ color: var(--green); }} .yellow {{ color: var(--yellow); }} .red {{ color: var(--red); }}
  .accent {{ color: var(--accent); }}
  section {{ background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius);
             padding: 24px; margin-bottom: 24px; }}
  section h2 {{ font-size: 1.1rem; font-weight: 600; margin-bottom: 20px; border-bottom: 1px solid var(--border); padding-bottom: 12px; }}
  .chart-wrap {{ position: relative; height: 320px; }}
  table {{ width: 100%; border-collapse: collapse; font-size: 0.85rem; }}
  th {{ text-align: left; padding: 8px 12px; color: var(--muted); font-weight: 600;
        font-size: 0.75rem; text-transform: uppercase; border-bottom: 1px solid var(--border); }}
  td {{ padding: 9px 12px; border-bottom: 1px solid var(--border); vertical-align: middle; }}
  tr:last-child td {{ border-bottom: none; }}
  tr:hover td {{ background: var(--surface2); }}
  .mono {{ font-family: monospace; font-size: 0.82rem; }}
  .bar-wrap {{ display: inline-block; width: 100px; height: 8px; background: var(--surface2);
               border-radius: 4px; vertical-align: middle; margin-right: 8px; }}
  .bar-fill {{ height: 100%; border-radius: 4px; }}
  .bar-good {{ background: var(--green); }} .bar-mid {{ background: var(--yellow); }} .bar-bad {{ background: var(--red); }}
  .tag {{ display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 0.75rem;
          font-weight: 600; background: var(--surface2); color: var(--accent); }}
  footer {{ text-align: center; color: var(--muted); font-size: 0.8rem; margin-top: 32px; }}
</style>
</head>
<body>

<h1>Métricas de Calidad de Software</h1>
<p class="subtitle">Proyecto: ProyectoPlanTravel &nbsp;·&nbsp; Generado: {datetime.now().strftime('%Y-%m-%d %H:%M')} &nbsp;·&nbsp; Branch: university/quality-metrics-exercise</p>

<div class="grid">
  <div class="card">
    <div class="card-label">Cobertura de Líneas</div>
    <div class="card-value {'green' if cov['line_pct']>=50 else 'yellow' if cov['line_pct']>=20 else 'red'}">{cov['line_pct']}%</div>
    <div class="card-sub">{cov['lines_covered']:,} / {cov['lines_total']:,} líneas cubiertas</div>
  </div>
  <div class="card">
    <div class="card-label">Cobertura de Métodos</div>
    <div class="card-value {'green' if cov['method_pct']>=50 else 'yellow' if cov['method_pct']>=20 else 'red'}">{cov['method_pct']}%</div>
    <div class="card-sub">JaCoCo unit tests (JVM)</div>
  </div>
  <div class="card">
    <div class="card-label">Cobertura de Ramas</div>
    <div class="card-value {'green' if cov['branch_pct']>=50 else 'yellow' if cov['branch_pct']>=20 else 'red'}">{cov['branch_pct']}%</div>
    <div class="card-sub">Branches cubiertas</div>
  </div>
  <div class="card">
    <div class="card-label">Densidad de Defectos</div>
    <div class="card-value accent">{density}</div>
    <div class="card-sub">defectos por cada 1,000 LOC</div>
  </div>
  <div class="card">
    <div class="card-label">LOC Producción</div>
    <div class="card-value">{loc:,}</div>
    <div class="card-sub">Líneas no-vacías en src/main/</div>
  </div>
  <div class="card">
    <div class="card-label">Commits Defecto</div>
    <div class="card-value">{defects['defect_commits']}</div>
    <div class="card-sub">de {defects['total_commits']} commits totales</div>
  </div>
</div>

<!-- Coverage chart -->
<section>
  <h2>Métrica 1 — Cobertura de Código (JaCoCo)</h2>
  <div class="chart-wrap">
    <canvas id="coverageChart"></canvas>
  </div>
</section>

<!-- Coverage table -->
<section>
  <h2>Cobertura por Paquete</h2>
  <table>
    <thead><tr><th>Paquete</th><th>Líneas cubiertas</th><th>Líneas no cubiertas</th><th>Cobertura</th></tr></thead>
    <tbody>{pkg_rows}</tbody>
  </table>
</section>

<!-- Defect density chart -->
<section>
  <h2>Métrica 2 — Densidad de Defectos</h2>
  <div class="chart-wrap" style="height:200px">
    <canvas id="defectChart"></canvas>
  </div>
  <p style="margin-top:16px; color:var(--muted); font-size:0.82rem;">
    Fórmula: defectos / (LOC / 1,000) = {defects['defect_commits']} / ({loc:,} / 1,000) = <strong style="color:var(--text)">{density}</strong><br>
    Criterio de defecto: commit que contiene las palabras clave <span class="tag">fix</span> <span class="tag">bug</span>
    <span class="tag">corregir</span> <span class="tag">error</span> <span class="tag">hotfix</span> <span class="tag">crash</span>
  </p>
</section>

<!-- Defect commits table -->
<section>
  <h2>Commits Clasificados como Defectos ({defects['defect_commits']})</h2>
  <table>
    <thead><tr><th>Mensaje de commit</th></tr></thead>
    <tbody>{defect_rows}</tbody>
  </table>
</section>

<footer>Prototipo académico &nbsp;·&nbsp; Métricas calculadas sobre código fuente Kotlin + historial Git</footer>

<script>
const labels  = {pkg_labels};
const covered = {pkg_covered};
const missed  = {pkg_missed};
const pcts    = {pkg_pcts};

new Chart(document.getElementById('coverageChart'), {{
  type: 'bar',
  data: {{
    labels,
    datasets: [
      {{ label: 'Líneas cubiertas', data: covered, backgroundColor: '#4caf50cc', borderRadius: 4 }},
      {{ label: 'Líneas no cubiertas', data: missed,  backgroundColor: '#f4433666', borderRadius: 4 }}
    ]
  }},
  options: {{
    responsive: true, maintainAspectRatio: false,
    plugins: {{ legend: {{ labels: {{ color: '#e8eaf6' }} }} }},
    scales: {{
      x: {{ stacked: true, ticks: {{ color: '#9e9eb8', maxRotation: 45 }}, grid: {{ color: '#2e3255' }} }},
      y: {{ stacked: true, ticks: {{ color: '#9e9eb8' }}, grid: {{ color: '#2e3255' }} }}
    }}
  }}
}});

new Chart(document.getElementById('defectChart'), {{
  type: 'doughnut',
  data: {{
    labels: ['Commits defecto', 'Commits limpios'],
    datasets: [{{
      data: [{defects['defect_commits']}, {defects['total_commits'] - defects['defect_commits']}],
      backgroundColor: ['#f44336bb', '#4caf5066'],
      borderColor: ['#f44336', '#4caf50'],
      borderWidth: 2
    }}]
  }},
  options: {{
    responsive: true, maintainAspectRatio: false,
    plugins: {{
      legend: {{ position: 'right', labels: {{ color: '#e8eaf6' }} }},
      tooltip: {{ callbacks: {{ label: ctx => ` ${{ctx.label}}: ${{ctx.raw}} (${{(ctx.raw/{defects['total_commits']}*100).toFixed(1)}}%)` }} }}
    }}
  }}
}});
</script>
</body>
</html>"""
    return html

# ── Main ──────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print("Parsing JaCoCo XML...")
    cov = parse_coverage()
    print(f"  Coverage: {cov['line_pct']}% line | {cov['method_pct']}% method | {cov['branch_pct']}% branch")

    print("Counting production LOC...")
    loc = count_production_loc()
    print(f"  LOC (src/main/*.kt): {loc:,}")

    print("Scanning git log for defects...")
    defects = parse_defects()
    print(f"  Defect commits: {defects['defect_commits']} / {defects['total_commits']}")

    density = round(defects["defect_commits"] / (loc / 1000), 2)
    print(f"  Defect density: {density} defects/KLOC")

    print("Generating report...")
    out = Path(__file__).parent / "report.html"
    out.write_text(build_report(cov, defects, loc), encoding="utf-8")
    print(f"  Report saved: {out}")
