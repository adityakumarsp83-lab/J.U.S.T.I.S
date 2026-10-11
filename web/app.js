/**
 * ═══════════════════════════════════════════════════════════════════
 *  J.U.S.T.I.S. — Frontend Application Logic
 *  Judicial Unified Search, Triaging & Intelligent Scheduling
 * ═══════════════════════════════════════════════════════════════════
 */

// Global State
let allCases = [];
let allStatutes = [];
let citationData = { edges: [], overruledNodes: [] };
let activeTab = 'causelist';

document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initControls();
    initSearch();
    loadAllData();
});

// ─── Data Loading ──────────────────────────────────────────────────
async function loadAllData() {
    try {
        await Promise.all([
            fetchCases(),
            fetchCauseList(10),
            fetchCitations(),
            fetchStats(),
            fetchUndertrialData()
        ]);
        showToast("Connected to J.U.S.T.I.S. Engine");
    } catch (e) {
        console.error("Initialization error:", e);
        showToast("Error connecting to JVM backend: " + e.message, true);
    }
}

async function fetchCases() {
    const res = await fetch('/api/cases');
    allCases = await res.json();
    renderDocketCards(allCases);
    document.getElementById('total-docket-badge').textContent = `${allCases.length} Cases`;
}

async function fetchStats() {
    const res = await fetch('/api/stats');
    const stats = await res.json();

    document.getElementById('metric-median').textContent = stats.medianAge.toFixed(1);
    document.getElementById('metric-mean').textContent = stats.meanAge.toFixed(1);
    document.getElementById('metric-stddev').textContent = stats.stdDev.toFixed(1);
    document.getElementById('metric-starving').textContent = stats.starvingCount;
    document.getElementById('metric-total').textContent = stats.totalCases;

    renderAnalyticsCharts(stats);
}

// ─── Tab Navigation ───────────────────────────────────────────────
function initNavigation() {
    const tabs = document.querySelectorAll('.nav-item');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const tabId = tab.getAttribute('data-tab');
            switchTab(tabId);
        });
    });
}

function switchTab(tabId) {
    activeTab = tabId;
    document.querySelectorAll('.nav-item').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));

    const activeBtn = document.querySelector(`.nav-item[data-tab="${tabId}"]`);
    const activePane = document.getElementById(`tab-${tabId}`);
    if (activeBtn) activeBtn.classList.add('active');
    if (activePane) activePane.classList.add('active');

    // Update Header Titles
    const titles = {
        causelist: { title: "Daily Cause List Generator", sub: "Algorithmic triaging: 60% Urgent (Max-Heap), 30% Backlog (FIFO), 10% Fresh Admissions" },
        dag: { title: "Citation DAG & Precedent Health", sub: "Directed citation network with DFS overrule detection & circular dependency checks" },
        undertrial: { title: "BNSS Section 479 Compliance", sub: "Automated tracking for mandatory release of undertrial prisoners detained beyond statutory limits" },
        search: { title: "Unified Search & Autocomplete", sub: "Inverted Index (Separate Chaining Hash Table) and Trie (O(K) statutory code prefix tree)" },
        analytics: { title: "Court Pendency & Analytics Dashboard", sub: "Hoare's Quickselect O(N) median computation, age distributions, and backlog indicators" },
        docket: { title: "Master Docket Directory", sub: "Supreme Court inventory of all indexed matters with complete record sheets" }
    };

    if (titles[tabId]) {
        document.getElementById('page-title').textContent = titles[tabId].title;
        document.getElementById('page-subtitle').textContent = titles[tabId].sub;
    }

    if (tabId === 'dag') {
        renderDagCanvas();
    }
}

// ─── Controls & Cause List ─────────────────────────────────────────
function initControls() {
    const slider = document.getElementById('slot-slider');
    const slotDisplay = document.getElementById('slot-value');
    const barTier1 = document.getElementById('bar-tier1');
    const barTier2 = document.getElementById('bar-tier2');
    const barTier3 = document.getElementById('bar-tier3');

    slider.addEventListener('input', (e) => {
        const slots = parseInt(e.target.value);
        slotDisplay.textContent = `${slots} Slots`;

        const t1 = Math.round(slots * 0.6);
        const t2 = Math.round(slots * 0.3);
        const t3 = slots - t1 - t2;

        barTier1.style.width = `${(t1 / slots) * 100}%`;
        barTier2.style.width = `${(t2 / slots) * 100}%`;
        barTier3.style.width = `${(t3 / slots) * 100}%`;
    });

    document.getElementById('btn-generate-list').addEventListener('click', () => {
        const slots = parseInt(slider.value);
        fetchCauseList(slots);
    });

    document.getElementById('btn-refresh').addEventListener('click', () => {
        loadAllData();
    });

    document.getElementById('btn-export-docket').addEventListener('click', () => {
        window.print();
    });

    // Modal close
    document.getElementById('modal-close-btn').addEventListener('click', closeModal);
    document.getElementById('modal-dismiss-btn').addEventListener('click', closeModal);
    document.getElementById('case-modal').addEventListener('click', (e) => {
        if (e.target.id === 'case-modal') closeModal();
    });
}

async function fetchCauseList(slots) {
    const res = await fetch(`/api/causelist?slots=${slots}`);
    const data = await res.json();
    renderCauseListTable(data.items);
}

function renderCauseListTable(items) {
    const tbody = document.getElementById('causelist-tbody');
    tbody.innerHTML = '';

    if (!items || items.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align:center; padding:20px; color:var(--text-muted);">No cases scheduled.</td></tr>';
        return;
    }

    items.forEach((item, index) => {
        const c = item.case;
        const tr = document.createElement('tr');

        let ageCustody = `${c.caseAgeDays}d`;
        if (c.caseType === 'CRIMINAL') {
            ageCustody += ` / <span style="color:var(--rose-primary); font-weight:600;">${c.custodyDays}d cust</span>`;
        } else if (c.caseType === 'WRIT' && c.custodyDays > 0) {
            ageCustody += ` / <span style="color:var(--purple-primary); font-weight:600;">${c.custodyDays}d det</span>`;
        } else if (c.caseType === 'CIVIL') {
            ageCustody += ` / ₹${(c.disputeValueINR / 100000).toFixed(1)}L`;
        }

        let tierClass = 'tier-surplus';
        let tierLabel = 'Surplus Cascade';
        if (item.tier === 'TIER_1_URGENT_HEAP') {
            tierClass = 'tier-urgent';
            tierLabel = 'Tier 1 (Heap)';
        } else if (item.tier === 'TIER_2_BACKLOG_FIFO') {
            tierClass = 'tier-backlog';
            tierLabel = 'Tier 2 (FIFO)';
        } else if (item.tier === 'TIER_3_FRESH') {
            tierClass = 'tier-fresh';
            tierLabel = 'Tier 3 (Fresh)';
        }

        tr.innerHTML = `
            <td style="font-weight:700; color:var(--text-muted);">${index + 1}</td>
            <td style="font-family:var(--font-mono); font-weight:600; color:var(--cyan-primary);">#${c.caseId}</td>
            <td><span class="badge ${getTypeBadge(c.caseType)}">${c.caseType}</span></td>
            <td style="font-weight:600; cursor:pointer;" onclick="showCaseDetails(${c.caseId})">
                ${c.title}
                ${c.isStarving ? '<span class="starving-indicator" title="Pending >1000 days">⚡ STARVING</span>' : ''}
            </td>
            <td><code style="color:var(--text-secondary);">${c.statuteCode}</code></td>
            <td>${ageCustody}</td>
            <td><span class="score-badge">${item.priorityScore.toFixed(1)}</span></td>
            <td><span class="tier-tag ${tierClass}">${tierLabel}</span></td>
            <td style="font-size:12px; color:var(--text-secondary); max-width:240px;">${item.justification}</td>
        `;
        tbody.appendChild(tr);
    });
}

function getTypeBadge(type) {
    if (type === 'CRIMINAL') return 'badge-crimson';
    if (type === 'CIVIL') return 'badge-chakra';
    return 'badge-brass';
}

// ─── Citation DAG Graph (Canvas) ──────────────────────────────────
let nodePositions = {};

async function fetchCitations() {
    const res = await fetch('/api/citations');
    citationData = await res.json();
    document.getElementById('overruled-count-badge').textContent = `${citationData.overruledNodes.length} Overruled`;
}

function renderDagCanvas() {
    const canvas = document.getElementById('dag-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;

    ctx.clearRect(0, 0, width, height);

    // Compute circular node positions
    const cases = allCases.length > 0 ? allCases : [{caseId: 101}, {caseId: 102}, {caseId: 103}];
    const total = cases.length;
    const centerX = width / 2;
    const centerY = height / 2;
    const radius = Math.min(width, height) * 0.38;

    nodePositions = {};
    cases.forEach((c, idx) => {
        const angle = (idx / total) * 2 * Math.PI - Math.PI / 2;
        const x = centerX + radius * Math.cos(angle);
        const y = centerY + radius * Math.sin(angle);
        nodePositions[c.caseId] = { x, y, case: c };
    });

    // Draw Edges
    citationData.edges.forEach(edge => {
        const from = nodePositions[edge.source];
        const to = nodePositions[edge.target];
        if (from && to) {
            drawArrow(ctx, from.x, from.y, to.x, to.y, edge.relation);
        }
    });

    // Draw Nodes
    cases.forEach(c => {
        const pos = nodePositions[c.caseId];
        if (!pos) return;

        const isOverruled = citationData.overruledNodes.includes(c.caseId);

        ctx.save();
        ctx.beginPath();
        ctx.arc(pos.x, pos.y, 22, 0, 2 * Math.PI);

        if (isOverruled) {
            ctx.fillStyle = '#5c1d14';
            ctx.strokeStyle = '#b84a39';
            ctx.shadowColor = '#b84a39';
            ctx.shadowBlur = 16;
        } else {
            ctx.fillStyle = '#10222f';
            ctx.strokeStyle = '#d4af37';
            ctx.shadowColor = '#d4af37';
            ctx.shadowBlur = 10;
        }

        ctx.lineWidth = 2.5;
        ctx.fill();
        ctx.stroke();

        // Node Text
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 12px "JetBrains Mono"';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.shadowBlur = 0;
        ctx.fillText(`#${c.caseId}`, pos.x, pos.y);
        ctx.restore();
    });

    // Canvas click interaction
    canvas.onclick = (e) => {
        const rect = canvas.getBoundingClientRect();
        const clickX = e.clientX - rect.left;
        const clickY = e.clientY - rect.top;

        for (const [id, pos] of Object.entries(nodePositions)) {
            const dist = Math.hypot(clickX - pos.x, clickY - pos.y);
            if (dist <= 24) {
                inspectCaseCitations(parseInt(id));
                return;
            }
        }
    };
}

function drawArrow(ctx, fromX, fromY, toX, toY, relation) {
    const headLen = 10;
    const angle = Math.atan2(toY - fromY, toX - fromX);

    // Stop short of node radius
    const targetX = toX - 24 * Math.cos(angle);
    const targetY = toY - 24 * Math.sin(angle);
    const startX = fromX + 24 * Math.cos(angle);
    const startY = fromY + 24 * Math.sin(angle);

    ctx.save();
    ctx.beginPath();
    ctx.moveTo(startX, startY);
    ctx.lineTo(targetX, targetY);

    if (relation === 'OVERRULES') {
        ctx.strokeStyle = '#b84a39';
        ctx.lineWidth = 2.5;
        ctx.setLineDash([6, 4]);
    } else if (relation === 'RELIES_ON') {
        ctx.strokeStyle = '#d4af37';
        ctx.lineWidth = 2;
        ctx.setLineDash([]);
    } else {
        ctx.strokeStyle = '#60a5fa';
        ctx.lineWidth = 1.5;
        ctx.setLineDash([]);
    }
    ctx.stroke();

    // Arrowhead
    ctx.beginPath();
    ctx.moveTo(targetX, targetY);
    ctx.lineTo(targetX - headLen * Math.cos(angle - Math.PI / 6), targetY - headLen * Math.sin(angle - Math.PI / 6));
    ctx.lineTo(targetX - headLen * Math.cos(angle + Math.PI / 6), targetY - headLen * Math.sin(angle + Math.PI / 6));
    ctx.closePath();
    ctx.fillStyle = ctx.strokeStyle;
    ctx.fill();
    ctx.restore();
}

async function inspectCaseCitations(caseId) {
    const placeholder = document.getElementById('inspector-placeholder');
    const content = document.getElementById('inspector-content');

    const res = await fetch(`/api/citation-health?id=${caseId}`);
    const data = await res.json();
    const c = allCases.find(item => item.caseId === caseId);

    placeholder.style.display = 'none';
    content.style.display = 'block';

    let alertsHtml = '';
    if (data.alerts.length === 0) {
        alertsHtml = `
            <div style="background:rgba(16,185,129,0.1); border-left:3px solid var(--emerald-primary); padding:10px; border-radius:4px; margin-top:12px;">
                <span style="color:var(--emerald-primary); font-weight:700; font-size:12px;">✅ PRECEDENT CLEAN</span>
                <p style="font-size:12px; color:var(--text-secondary); margin-top:4px;">No relied or cited precedents in this lineage have been overruled.</p>
            </div>
        `;
    } else {
        alertsHtml = data.alerts.map(a => `
            <div class="alert-item">
                <span class="alert-severity">⚠️ ${a.severity} WARNING</span>
                <div class="alert-chain">${a.chain}</div>
            </div>
        `).join('');
    }

    content.innerHTML = `
        <div style="margin-bottom:14px;">
            <span class="badge ${getTypeBadge(c ? c.caseType : 'CRIMINAL')}">${c ? c.caseType : ''}</span>
            <h4 style="font-family:var(--font-heading); font-size:16px; margin-top:6px;">${c ? c.title : `Case #${caseId}`}</h4>
            <span style="font-size:12px; color:var(--text-muted); font-family:var(--font-mono);">Statute: ${c ? c.statuteCode : ''}</span>
        </div>
        <div style="font-size:12.5px; line-height:1.6; border-top:1px solid var(--border-subtle); padding-top:10px;">
            <div><strong>Citations:</strong> ${c && c.citedCaseIds.length > 0 ? c.citedCaseIds.join(', ') : 'None'}</div>
            <div><strong>In-Degree (Citing Authority):</strong> ${citationData.edges.filter(e => e.target === caseId).length} cases</div>
            <div><strong>Status:</strong> ${citationData.overruledNodes.includes(caseId) ? '<span style="color:var(--rose-primary); font-weight:700;">OVERRULED</span>' : 'VALID'}</div>
        </div>
        <h5 style="margin-top:16px; font-size:13px; font-weight:600;">DFS Lineage Health Check:</h5>
        ${alertsHtml}
    `;
}

// ─── BNSS Section 479 Compliance ──────────────────────────────────
let undertrialRecords = [];

async function fetchUndertrialData() {
    const res = await fetch('/api/undertrial');
    undertrialRecords = await res.json();

    const eligible = undertrialRecords.filter(r => r.status === 'ELIGIBLE_FOR_RELEASE');
    const inCustody = undertrialRecords.filter(r => r.custodyDays > 0);

    document.getElementById('kpi-eligible-count').textContent = eligible.length;
    document.getElementById('kpi-detained-count').textContent = inCustody.length;
    document.getElementById('eligible-badge').textContent = `${eligible.length} Eligible`;

    renderUndertrialTable(undertrialRecords);

    // Pill filters
    document.querySelectorAll('.filter-pills .pill').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.filter-pills .pill').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            const filter = btn.getAttribute('data-filter');

            if (filter === 'eligible') {
                renderUndertrialTable(eligible);
            } else if (filter === 'custody') {
                renderUndertrialTable(inCustody);
            } else {
                renderUndertrialTable(undertrialRecords);
            }
        });
    });
}

function renderUndertrialTable(records) {
    const tbody = document.getElementById('undertrial-tbody');
    tbody.innerHTML = '';

    records.forEach(r => {
        const tr = document.createElement('tr');
        const pct = r.thresholdDays > 0 ? Math.min(100, Math.round((r.custodyDays / r.thresholdDays) * 100)) : 0;
        const isEligible = r.status === 'ELIGIBLE_FOR_RELEASE';

        tr.innerHTML = `
            <td style="font-family:var(--font-mono); font-weight:700; color:var(--cyan-primary);">#${r.caseId}</td>
            <td style="font-weight:600;">${r.title}</td>
            <td><code>${r.statuteCode}</code></td>
            <td>${r.maxSentenceYears > 0 ? `${r.maxSentenceYears} Years` : 'Non-custodial'}</td>
            <td>
                <div class="detention-progress-bar" title="${pct}% of half-sentence limit">
                    <div class="progress-fill ${isEligible ? 'fill-over' : ''}" style="width: ${pct}%;"></div>
                </div>
            </td>
            <td style="font-weight:600; color:${isEligible ? 'var(--rose-primary)' : 'var(--text-primary)'};">${r.custodyDays} Days</td>
            <td>${r.thresholdDays > 0 ? `${r.thresholdDays} Days` : 'N/A'}</td>
            <td>
                ${isEligible ? '<span class="badge badge-success">🟢 Eligible for Release</span>'
                             : r.status === 'NOT_YET_ELIGIBLE' ? `<span class="badge badge-warning">🟡 ${r.daysRemaining}d to limit</span>`
                             : '<span class="badge badge-neutral">ℹ️ Non-custodial</span>'}
            </td>
            <td>
                ${isEligible ? `<button class="btn btn-secondary" style="padding:4px 8px; font-size:11px;" onclick="issueNotice(${r.caseId})">Issue Notice</button>` : '-'}
            </td>
        `;
        tbody.appendChild(tr);
    });
}

window.issueNotice = function(caseId) {
    showToast(`Notice drafted for Case #${caseId} under BNSS § 479 personal bond.`);
};

// ─── Unified Search & Trie Autocomplete ────────────────────────────
function initSearch() {
    const keyInput = document.getElementById('keyword-input');
    const statInput = document.getElementById('statute-input');
    const dropdown = document.getElementById('statute-dropdown');

    let debounceTimer;
    keyInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            executeSearch(e.target.value);
        }, 200);
    });

    statInput.addEventListener('input', async (e) => {
        const prefix = e.target.value.trim().toUpperCase();
        if (prefix.length === 0) {
            dropdown.style.display = 'none';
            return;
        }

        const res = await fetch(`/api/statute-autocomplete?prefix=${prefix}`);
        const matches = await res.json();

        dropdown.innerHTML = '';
        if (matches.length > 0) {
            matches.forEach(m => {
                const item = document.createElement('div');
                item.className = 'autocomplete-item';
                item.textContent = m;
                item.addEventListener('click', () => {
                    statInput.value = m;
                    dropdown.style.display = 'none';
                    executeSearch(m);
                });
                dropdown.appendChild(item);
            });
            dropdown.style.display = 'block';
        } else {
            dropdown.style.display = 'none';
        }
    });

    document.addEventListener('click', (e) => {
        if (!e.target.closest('.autocomplete-container')) {
            dropdown.style.display = 'none';
        }
    });

    // Master docket filter
    const docketFilter = document.getElementById('docket-filter-input');
    docketFilter.addEventListener('input', (e) => {
        const q = e.target.value.toLowerCase();
        const filtered = allCases.filter(c =>
            c.title.toLowerCase().includes(q) ||
            c.caseId.toString().includes(q) ||
            c.statuteCode.toLowerCase().includes(q)
        );
        renderDocketCards(filtered);
    });
}

async function executeSearch(query) {
    const res = await fetch(`/api/search?q=${encodeURIComponent(query)}`);
    const results = await res.json();
    renderSearchResults(results, query);
}

function renderSearchResults(results, query) {
    const tbody = document.getElementById('search-tbody');
    const countBadge = document.getElementById('search-match-count');
    countBadge.textContent = `${results.length} Results`;
    tbody.innerHTML = '';

    if (results.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:20px; color:var(--text-muted);">No records found matching "${query}".</td></tr>`;
        return;
    }

    results.forEach(c => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td style="font-family:var(--font-mono); font-weight:700; color:var(--cyan-primary);">#${c.caseId}</td>
            <td><span class="badge ${getTypeBadge(c.caseType)}">${c.caseType}</span></td>
            <td style="font-weight:600; cursor:pointer;" onclick="showCaseDetails(${c.caseId})">${c.title}</td>
            <td><code>${c.statuteCode}</code></td>
            <td>${c.filingYear}</td>
            <td>${c.caseAgeDays} days</td>
            <td><span class="score-badge">${c.priorityScore.toFixed(1)}</span></td>
            <td><span class="badge badge-accent">${c.status}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

// ─── Analytics Charts ──────────────────────────────────────────────
function renderAnalyticsCharts(stats) {
    // Type Breakdown
    const typeContainer = document.getElementById('type-distribution-container');
    typeContainer.innerHTML = '';

    const types = [
        { label: 'Criminal Trials & Bail', count: stats.criminalCount, color: 'var(--sandstone-red)' },
        { label: 'Civil & Commercial Disputes', count: stats.civilCount, color: 'var(--chakra-blue)' },
        { label: 'Constitutional Writs (Art. 226/32)', count: stats.writCount, color: 'var(--gold-primary)' }
    ];

    types.forEach(t => {
        const pct = Math.round((t.count / stats.totalCases) * 100);
        const row = document.createElement('div');
        row.className = 'type-bar-row';
        row.innerHTML = `
            <div class="type-bar-meta">
                <span>${t.label}</span>
                <span style="font-weight:700;">${t.count} cases (${pct}%)</span>
            </div>
            <div class="type-progress">
                <div class="type-progress-fill" style="width:${pct}%; background:${t.color};"></div>
            </div>
        `;
        typeContainer.appendChild(row);
    });

    // Urgency Spectrum
    const urgencyContainer = document.getElementById('urgency-container');
    urgencyContainer.innerHTML = '';
    const urgencyCounts = [
        { lvl: 5, label: 'Level 5 (Emergency Bail & Habeas Corpus)', count: 4, color: '#b84a39' },
        { lvl: 4, label: 'Level 4 (Urgent Custodial & Public Interest)', count: 2, color: '#f97316' },
        { lvl: 3, label: 'Level 3 (Standard Contested Litigation)', count: 0, color: '#d4af37' },
        { lvl: 2, label: 'Level 2 (Commercial Regular Docket)', count: 2, color: '#2563eb' },
        { lvl: 1, label: 'Level 1 (Routine Chamber Matters)', count: 2, color: '#64748b' }
    ];

    urgencyCounts.forEach(u => {
        const pct = Math.round((u.count / stats.totalCases) * 100);
        const row = document.createElement('div');
        row.className = 'type-bar-row';
        row.innerHTML = `
            <div class="type-bar-meta">
                <span>${u.label}</span>
                <span style="font-weight:700;">${u.count} cases</span>
            </div>
            <div class="type-progress">
                <div class="type-progress-fill" style="width:${pct}%; background:${u.color};"></div>
            </div>
        `;
        urgencyContainer.appendChild(row);
    });
}

// ─── Master Docket Cards ───────────────────────────────────────────
function renderDocketCards(cases) {
    const container = document.getElementById('docket-cards-container');
    container.innerHTML = '';

    cases.forEach(c => {
        const card = document.createElement('div');
        card.className = 'case-card';
        card.onclick = () => showCaseDetails(c.caseId);

        let metricExtra = '';
        if (c.caseType === 'CRIMINAL') {
            metricExtra = `<span>Detained: <strong>${c.custodyDays}d</strong></span>`;
        } else if (c.caseType === 'CIVIL') {
            metricExtra = `<span>Dispute: <strong>₹${(c.disputeValueINR / 100000).toFixed(1)}L</strong></span>`;
        } else {
            metricExtra = `<span>Writ: <strong>Art. 226</strong></span>`;
        }

        card.innerHTML = `
            <div>
                <div class="case-card-header">
                    <span class="case-card-id">#${c.caseId} • ${c.filingYear}</span>
                    <span class="badge ${getTypeBadge(c.caseType)}">${c.caseType}</span>
                </div>
                <h4 class="case-card-title">${c.title}</h4>
                <div style="font-size:12px; color:var(--text-secondary); margin-bottom:12px;">
                    Statute: <code>${c.statuteCode}</code>
                </div>
            </div>
            <div class="case-card-footer">
                ${metricExtra}
                <span class="score-badge">${c.priorityScore.toFixed(1)}</span>
            </div>
        `;
        container.appendChild(card);
    });
}

// ─── Modal Case Sheet ──────────────────────────────────────────────
window.showCaseDetails = function(caseId) {
    const c = allCases.find(item => item.caseId === caseId);
    if (!c) return;

    document.getElementById('modal-title').textContent = `Docket Sheet: Case #${c.caseId}`;
    const body = document.getElementById('modal-body');

    let specificField = '';
    if (c.caseType === 'CRIMINAL') {
        specificField = `<div><strong>Incarceration Days:</strong> ${c.custodyDays} days</div>`;
    } else if (c.caseType === 'CIVIL') {
        specificField = `<div><strong>Dispute Claim:</strong> ₹${c.disputeValueINR.toLocaleString()}</div>`;
    } else if (c.caseType === 'WRIT') {
        specificField = `<div><strong>Detention Period:</strong> ${c.custodyDays} days</div>`;
    }

    body.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:14px;">
            <span class="badge ${getTypeBadge(c.caseType)}">${c.caseType}</span>
            <span class="score-badge" style="font-size:14px;">Priority Score: ${c.priorityScore.toFixed(2)}</span>
        </div>
        <h4 style="font-family:var(--font-heading); font-size:18px; margin-bottom:10px;">${c.title}</h4>
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; font-size:13px; line-height:1.7; background:rgba(0,0,0,0.2); padding:14px; border-radius:8px;">
            <div><strong>Filing Year:</strong> ${c.filingYear}</div>
            <div><strong>Total Pendency:</strong> ${c.caseAgeDays} days</div>
            <div><strong>Urgency Level:</strong> ${c.urgencyFlag} / 5</div>
            <div><strong>Statutory Code:</strong> <code>${c.statuteCode}</code></div>
            <div><strong>Status:</strong> ${c.status}</div>
            <div><strong>Circuit Breaker:</strong> ${c.isStarving ? '<span style="color:var(--rose-primary); font-weight:700;">STARVING (>1000d)</span>' : 'Normal'}</div>
            ${specificField}
            <div><strong>Citations:</strong> ${c.citedCaseIds.length > 0 ? c.citedCaseIds.join(', ') : 'None'}</div>
        </div>
    `;

    document.getElementById('case-modal').classList.add('open');
};

function closeModal() {
    document.getElementById('case-modal').classList.remove('open');
}

function showToast(msg, isError = false) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.style.borderColor = isError ? 'var(--sandstone-red)' : 'var(--gold-primary)';
    toast.classList.add('show');
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}
