package com.cmb.codeperf.cli.report;

/**
 * HTML 报告资源：内嵌 CSS 样式和 JavaScript 交互脚本。
 */
final class SourceScanHtmlAssets {

    private SourceScanHtmlAssets() {
    }

    static String style() {
        StringBuilder css = new StringBuilder(24576);
        css.append(":root{--bg:#f8fafc;--surface:#fff;--surface-subtle:#f1f5f9;--ink:#0f172a;--muted:#64748b;")
                .append("--line:#e4e5e7;--line-strong:#cbd5e1;--primary:#2563eb;--primary-soft:#eff6ff;--primary-line:#bfdbfe;")
                .append("--danger:#dc2626;--danger-soft:#fef2f2;--danger-line:#fecaca;--warning:#b45309;--warning-soft:#fffbeb;")
                .append("--success:#15803d;--success-soft:#f0fdf4;--code:#f8fafc;--code-hit:#fff1f2;--shadow:0 1px 2px rgba(15,23,42,.05);")
                .append("--ring:0 0 0 3px rgba(37,99,235,.18);}")
                .append("*{box-sizing:border-box}html{scroll-behavior:smooth}body{margin:0;background:var(--bg);color:var(--ink);")
                .append("font-family:'Microsoft YaHei','PingFang SC','Segoe UI',sans-serif;font-size:14px;line-height:1.48;}")
                .append("@media(prefers-reduced-motion:reduce){html{scroll-behavior:auto}*,*:before,*:after{transition:none!important;animation:none!important}}")
                .append(".report-page{max-width:1180px;margin:0 auto;padding:14px 18px 28px;}")
                .append(".topbar{display:grid;grid-template-columns:minmax(280px,1fr) 156px minmax(420px,.95fr);gap:10px;align-items:stretch;background:var(--surface);")
                .append("border:1px solid var(--line);border-radius:6px;box-shadow:var(--shadow);padding:12px;margin-bottom:10px;}")
                .append(".topbar-title{font-size:18px;font-weight:800;line-height:1.25}.topbar-subtitle{margin-top:4px;color:var(--muted);font-size:12px;}")
                .append(".quality-gate{border-radius:4px;border:1px solid var(--line);padding:8px 10px;display:flex;flex-direction:column;justify-content:center;")
                .append("align-items:center;font-size:15px;font-weight:900;text-align:center}.quality-gate span{font-size:12px;color:var(--muted);font-weight:600}")
                .append(".quality-gate.failed{background:var(--danger-soft);color:var(--danger);border-color:var(--danger-line)}")
                .append(".quality-gate.passed{background:var(--success-soft);color:var(--success);border-color:#bbf7d0}")
                .append(".summary-metrics{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:6px}.metric{border:1px solid var(--line);border-radius:4px;")
                .append("padding:7px 9px;background:#fbfdff;min-width:0}.metric-label{font-size:12px;color:var(--muted);white-space:nowrap}.metric-value{display:block;")
                .append("margin-top:2px;font-size:18px;font-weight:800;color:var(--ink);overflow-wrap:anywhere;font-variant-numeric:tabular-nums}")
                .append(".filter-toolbar{position:sticky;top:0;z-index:20;display:grid;grid-template-columns:72px minmax(220px,1.5fr) repeat(5,minmax(118px,1fr));")
                .append("gap:8px;align-items:end;background:rgba(255,255,255,.97);border:1px solid var(--line);border-radius:6px;box-shadow:var(--shadow);")
                .append("padding:10px;margin-bottom:12px;backdrop-filter:blur(6px)}.toolbar-title{align-self:center;font-weight:900;color:#334155}")
                .append(".filter-group{min-width:0}.filter-label{display:block;font-size:12px;color:var(--muted);font-weight:700;margin-bottom:4px}")
                .append(".filter-toolbar input,.filter-toolbar select{width:100%;min-height:34px;border:1px solid var(--line-strong);border-radius:4px;background:#fff;color:var(--ink);")
                .append("padding:6px 8px;outline:none}.filter-toolbar input:focus-visible,.filter-toolbar select:focus-visible,.issue-row:focus-visible{border-color:var(--primary);box-shadow:var(--ring)}")
                .append(".issue-feed{background:var(--surface);border:1px solid var(--line);border-radius:6px;box-shadow:var(--shadow);overflow:hidden}.workspace-head{")
                .append("padding:12px 14px;border-bottom:1px solid var(--line);display:flex;justify-content:space-between;gap:10px;align-items:center;background:#fbfdff}")
                .append(".workspace-head h2{font-size:16px;margin:0}.workspace-head p{margin:3px 0 0;color:var(--muted);font-size:12px}.module-count{color:var(--muted);font-size:12px;font-weight:600}")
                .append(".module-block{border-top:1px solid var(--line)}.module-block:first-of-type{border-top:0}.module-head{padding:8px 14px;background:var(--surface-subtle);")
                .append("font-weight:900;display:flex;justify-content:space-between;gap:10px}.file-card{border-top:1px solid var(--line);scroll-margin-top:74px}.file-head{")
                .append("padding:12px 14px 8px;display:grid;grid-template-columns:minmax(0,1fr) auto;gap:8px;align-items:start}.file-name{font-weight:900;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}")
                .append(".file-path{font-size:12px;color:var(--muted);margin-top:2px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.file-count{font-size:12px;color:var(--muted);white-space:nowrap}")
                .append(".chips{display:flex;flex-wrap:wrap;gap:5px;padding:0 14px 8px}.chip{display:inline-flex;align-items:center;height:20px;border-radius:3px;background:#f1f5f9;")
                .append("border:1px solid #e2e8f0;color:#334155;padding:0 6px;font-size:11px;font-weight:800;max-width:100%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}")
                .append(".chip.red{background:var(--danger-soft);border-color:var(--danger-line);color:var(--danger)}.chip.orange{background:var(--warning-soft);border-color:#fde68a;color:var(--warning)}")
                .append(".chip.green{background:var(--success-soft);border-color:#bbf7d0;color:var(--success)}.chip.blue{background:var(--primary-soft);border-color:var(--primary-line);color:#1d4ed8}")
                .append(".issue-list{border-top:1px solid var(--line)}.issue-row{display:grid;grid-template-columns:48px minmax(0,1fr) auto;gap:9px;align-items:center;width:100%;")
                .append("min-height:50px;border:0;border-top:1px solid #eef2f7;background:#fff;text-align:left;padding:8px 14px;color:var(--ink);cursor:pointer;font:inherit;")
                .append("scroll-margin-top:80px;transition:background-color .16s ease,box-shadow .16s ease}.issue-row:first-child{border-top:0}.issue-row:hover,.issue-row.active{background:#f8fbff}.issue-row.active{box-shadow:inset 3px 0 0 var(--primary)}")
                .append(".issue-line{font-weight:900;color:var(--primary);font-variant-numeric:tabular-nums}.issue-title{font-weight:800;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}")
                .append(".issue-meta{margin-top:3px;display:flex;flex-wrap:wrap;gap:5px;min-width:0}.issue-evidence{display:block;margin-top:3px;font-family:Consolas,'Fira Code','Courier New',monospace;color:var(--muted);")
                .append("font-size:12px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.issue-method{font-size:12px;color:var(--muted);white-space:nowrap}")
                .append(".detail-card{display:none;margin:0 14px 12px 62px;padding:12px 14px;background:#fbfdff;border:1px solid var(--line);border-left:3px solid var(--primary);border-radius:4px}.detail-card.active{display:block}")
                .append(".detail-location{font-size:16px;font-weight:900;line-height:1.35;overflow-wrap:anywhere}.detail-section{margin-top:12px;border-top:1px solid var(--line);padding-top:10px}")
                .append(".detail-section h3{font-size:12px;margin:0 0 7px;color:var(--muted);text-transform:uppercase;letter-spacing:.045em}.detail-text{font-size:13px;line-height:1.58;color:#334155;overflow-wrap:anywhere}")
                .append(".meta-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:7px}.meta-item{font-size:12px;color:var(--muted);min-width:0}.meta-item b{display:block;margin-top:2px;color:var(--ink);font-size:13px;overflow-wrap:anywhere;font-weight:700}")
                .append(".evidence-box{font-family:Consolas,'Fira Code','Courier New',monospace;background:#fff;color:#0f172a;border:1px solid var(--line);border-left:3px solid var(--primary);border-radius:4px;padding:8px 9px;font-size:12px;line-height:1.42;overflow:auto;white-space:pre-wrap;overflow-wrap:anywhere}")
                .append(".source-block{margin:0;background:var(--code);border:1px solid var(--line);border-radius:4px;overflow:auto;font-family:Consolas,'Fira Code','Courier New',monospace;font-size:12px;line-height:1.3;padding:4px 0;}")
                .append(".source-line{display:block;white-space:pre;min-height:16px;padding:0 10px}.source-line.hit{background:var(--code-hit);box-shadow:inset 3px 0 0 var(--danger)}.line-no{display:inline-block;width:36px;margin-right:12px;text-align:right;color:#64748b;user-select:none;font-variant-numeric:tabular-nums}")
                .append(".call-chain{display:flex;flex-wrap:wrap;gap:5px;align-items:center}.chain-step{border:1px solid var(--primary-line);background:var(--primary-soft);color:#1d4ed8;border-radius:3px;padding:3px 6px;font-size:12px;font-weight:800;max-width:100%;overflow-wrap:anywhere}")
                .append(".suggestions{margin:0;padding-left:18px;color:#334155;font-size:13px;line-height:1.62}.empty-state{margin:14px;padding:14px;border:1px dashed var(--line-strong);border-radius:4px;color:var(--muted);background:#fbfdff}")
                .append(".floating-toc{position:fixed;right:18px;top:118px;z-index:30;width:210px;max-height:calc(100vh - 150px);overflow:auto;background:rgba(255,255,255,.97);")
                .append("border:1px solid var(--line);border-radius:6px;box-shadow:0 8px 24px rgba(15,23,42,.12);padding:10px}.toc-title{font-weight:900;margin-bottom:8px}.toc-empty{color:var(--muted);font-size:12px}")
                .append(".toc-module{margin-top:9px;padding-top:8px;border-top:1px solid var(--line);font-size:12px;font-weight:900;color:#334155}.toc-file,.toc-issue{display:block;text-decoration:none;border-radius:4px;color:#334155;line-height:1.35;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.toc-file{margin-top:5px;padding:5px 6px;font-size:12px;font-weight:800}.toc-issue{margin-left:8px;padding:3px 6px;font-size:12px;color:var(--muted)}.toc-file:hover,.toc-issue:hover{background:var(--primary-soft);color:#1d4ed8}")
                .append(".parse-errors{margin-top:12px;background:var(--surface);border:1px solid var(--line);border-radius:4px;box-shadow:var(--shadow);padding:12px 14px}.parse-errors h2{font-size:15px;margin:0 0 7px}.parse-item{font-family:Consolas,'Fira Code','Courier New',monospace;color:var(--danger);font-size:12px;margin-top:5px;overflow-wrap:anywhere}")
                .append(".hidden{display:none!important}@media(max-width:1500px){.floating-toc{right:10px;width:180px}.report-page{padding-right:200px}}")
                .append("@media(max-width:1180px){.report-page{max-width:980px;padding-right:18px}.floating-toc{position:sticky;top:8px;width:auto;max-height:220px;margin:0 0 12px auto}.filter-toolbar{grid-template-columns:1fr 1fr 1fr}.toolbar-title{grid-column:1/-1}.topbar{grid-template-columns:1fr}.summary-metrics{grid-template-columns:repeat(5,minmax(0,1fr))}}")
                .append("@media(max-width:760px){.report-page{padding:10px}.summary-metrics{grid-template-columns:repeat(2,minmax(0,1fr))}.filter-toolbar{grid-template-columns:1fr}.issue-row{grid-template-columns:44px minmax(0,1fr);min-height:54px}.issue-method{display:none}.detail-card{margin:0 10px 10px 10px}.meta-grid{grid-template-columns:1fr}}");
        return css.toString();
    }

    static String script() {
        StringBuilder js = new StringBuilder(8192);
        js.append("(function(){")
                .append("var rows=[].slice.call(document.querySelectorAll('[data-issue-row]'));")
                .append("var details=[].slice.call(document.querySelectorAll('[data-detail]'));")
                .append("var empty=document.getElementById('emptyResult');")
                .append("function value(id){var el=document.getElementById(id);return el?el.value:'';}")
                .append("function select(row,updateHash){if(!row){return;}rows.forEach(function(item){item.classList.toggle('active',item===row);});")
                .append("details.forEach(function(card){card.classList.toggle('active',card.id===row.dataset.target);});")
                .append("if(updateHash&&history.replaceState){history.replaceState(null,'','#'+row.id);}}")
                .append("function apply(){var q=value('filterText').toLowerCase();var module=value('filterModule');var io=value('filterIo');")
                .append("var severity=value('filterSeverity');var confidence=value('filterConfidence');var scope=value('filterScope');var visible=0;var first=null;")
                .append("rows.forEach(function(row){var ok=true;if(q&&row.dataset.search.toLowerCase().indexOf(q)<0){ok=false;}")
                .append("if(module&&row.dataset.module!==module){ok=false;}if(io&&row.dataset.io!==io){ok=false;}")
                .append("if(severity&&row.dataset.severity!==severity){ok=false;}if(confidence&&row.dataset.confidence!==confidence){ok=false;}")
                .append("if(scope&&row.dataset.scope!==scope){ok=false;}row.classList.toggle('hidden',!ok);")
                .append("var detail=document.getElementById(row.dataset.target);if(detail&&detail.classList.contains('active')&&!ok){detail.classList.remove('active');}")
                .append("if(ok){visible++;if(!first){first=row;}}});")
                .append("document.querySelectorAll('[data-file-card]').forEach(function(card){card.classList.toggle('hidden',card.querySelectorAll('[data-issue-row]:not(.hidden)').length===0);});")
                .append("document.querySelectorAll('[data-module-block]').forEach(function(block){block.classList.toggle('hidden',block.querySelectorAll('[data-issue-row]:not(.hidden)').length===0);});")
                .append("if(empty){empty.classList.toggle('hidden',visible!==0);}var active=document.querySelector('[data-issue-row].active:not(.hidden)');")
                .append("if(!active&&first){select(first,false);}if(!first){details.forEach(function(card){card.classList.remove('active');});}}")
                .append("rows.forEach(function(row){row.addEventListener('click',function(){select(row,true);});});")
                .append("['filterText','filterModule','filterIo','filterSeverity','filterConfidence','filterScope'].forEach(function(id){var el=document.getElementById(id);if(el){el.addEventListener('input',apply);el.addEventListener('change',apply);}});")
                .append("var hash=window.location.hash?window.location.hash.substring(1):'';var initial=hash?document.getElementById(hash):null;")
                .append("if(initial&&initial.hasAttribute('data-issue-row')){select(initial,false);initial.scrollIntoView({block:'center'});}else if(rows.length>0){select(rows[0],false);}apply();")
                .append("})();");
        return js.toString();
    }
}

