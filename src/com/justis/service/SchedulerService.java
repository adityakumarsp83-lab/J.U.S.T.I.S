package com.justis.service;

import com.justis.analytics.BNSSEligibility;
import com.justis.analytics.CourtStatistics;
import com.justis.ds.CitationGraph;
import com.justis.ds.CitationGraph.CitationAlert;
import com.justis.ds.InvertedIndex;
import com.justis.ds.MaxHeap;
import com.justis.ds.Trie;
import com.justis.exception.CaseNotFoundException;
import com.justis.model.Case;
import com.justis.model.CitationEdge;
import com.justis.service.DataWrangler.StatuteRecord;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Central Facade orchestrating data structures, statistical analytics,
 * and multi-tiered cause-list generation.
 *
 * <p><b>OOP Pattern:</b> Facade Pattern — provides a unified, high-level interface
 * hiding the complexities of the MaxHeap, CitationGraph, Trie, InvertedIndex,
 * and statistical calculation sub-systems.
 *
 * <p><b>Judicial Triaging Model:</b>
 * Cause list generated according to the 60/30/10 docket allocation formula:
 * <ul>
 *   <li><b>Tier 1 (60%): Urgent Matters & Starving Cases</b> — Dispatched via Binary Max-Heap</li>
 *   <li><b>Tier 2 (30%): Regular Backlog Docket</b> — Dispatched via FIFO Queue ordered by age</li>
 *   <li><b>Tier 3 (10%): Fresh Admissions</b> — Recently filed matters for quick admission hearings</li>
 * </ul>
 */
public class SchedulerService implements Serializable {

    private static final long serialVersionUID = 1L;

    public static class ScheduledSlot {
        private final Case scheduledCase;
        private final String tier;
        private final double priorityScore;
        private final String justification;

        public ScheduledSlot(Case scheduledCase, String tier, double priorityScore, String justification) {
            this.scheduledCase = scheduledCase;
            this.tier = tier;
            this.priorityScore = priorityScore;
            this.justification = justification;
        }

        public Case getScheduledCase()       { return scheduledCase; }
        public String getTier()              { return tier; }
        public double getPriorityScore()     { return priorityScore; }
        public String getJustification()     { return justification; }
    }

    private final List<Case> allCases;
    private final List<CitationEdge> allEdges;
    private final List<StatuteRecord> allStatutes;

    private final Map<Integer, Case> caseMap;
    private final CitationGraph citationGraph;
    private final Trie statuteTrie;
    private final InvertedIndex invertedIndex;
    private final BNSSEligibility bnssEligibility;
    private final SchedulingAuditLog auditLog;

    public SchedulerService(List<Case> cases, List<CitationEdge> edges, List<StatuteRecord> statutes) {
        this(cases, edges, statutes, "data/audit_log.csv");
    }

    public SchedulerService(List<Case> cases, List<CitationEdge> edges,
                            List<StatuteRecord> statutes, String auditLogPath) {
        this.allCases = new ArrayList<>(cases != null ? cases : Collections.emptyList());
        this.allEdges = new ArrayList<>(edges != null ? edges : Collections.emptyList());
        this.allStatutes = new ArrayList<>(statutes != null ? statutes : Collections.emptyList());

        this.caseMap = new HashMap<>();
        for (Case c : this.allCases) {
            caseMap.put(c.getCaseId(), c);
        }

        // Initialize Citation DAG
        this.citationGraph = new CitationGraph();
        this.citationGraph.loadEdges(this.allEdges);

        // Initialize Statute Trie
        this.statuteTrie = new Trie();
        for (StatuteRecord s : this.allStatutes) {
            this.statuteTrie.insert(s.getStatuteCode());
        }

        // Initialize Inverted Index
        this.invertedIndex = new InvertedIndex();
        for (Case c : this.allCases) {
            this.invertedIndex.index(c.getCaseId(), c.getTitle());
            this.invertedIndex.index(c.getCaseId(), c.getStatuteCode());
        }

        this.bnssEligibility = new BNSSEligibility(this.allStatutes);
        this.auditLog = new SchedulingAuditLog(auditLogPath);
    }

    /**
     * Generates a cause list of the requested capacity using the 60-30-10 multi-tier algorithm.
     *
     * @param totalSlots Maximum number of slots on the daily docket
     * @return List of scheduled slots with case and justification
     */
    public List<ScheduledSlot> generateCauseList(int totalSlots) {
        if (totalSlots <= 0 || allCases.isEmpty()) {
            return Collections.emptyList();
        }

        int targetTier1 = Math.max(1, (int) Math.round(totalSlots * 0.60));
        int targetTier2 = Math.max(1, (int) Math.round(totalSlots * 0.30));
        int targetTier3 = totalSlots - targetTier1 - targetTier2;
        if (targetTier3 < 0) targetTier3 = 0;

        List<Case> tier1Candidates = new ArrayList<>();
        List<Case> tier2Candidates = new ArrayList<>();
        List<Case> tier3Candidates = new ArrayList<>();

        for (Case c : allCases) {
            if (c.getCaseAgeDays() <= 60) {
                tier3Candidates.add(c);
            } else if (c.getUrgencyFlag() >= 4 || c.isStarving()) {
                tier1Candidates.add(c);
            } else {
                tier2Candidates.add(c);
            }
        }

        // Tier 1: MaxHeap
        MaxHeap tier1Heap = new MaxHeap();
        tier1Heap.buildHeap(tier1Candidates);

        // Tier 2: FIFO ordered by age (longest pending first)
        tier2Candidates.sort((c1, c2) -> Integer.compare(c2.getCaseAgeDays(), c1.getCaseAgeDays()));
        Queue<Case> tier2Queue = new ArrayDeque<>(tier2Candidates);

        // Tier 3: Fresh admissions FIFO
        tier3Candidates.sort(Comparator.comparingInt(Case::getCaseAgeDays));
        Queue<Case> tier3Queue = new ArrayDeque<>(tier3Candidates);

        List<ScheduledSlot> causeList = new ArrayList<>();
        Set<Integer> scheduledIds = new HashSet<>();

        // Fill Tier 1 (Urgent Heap)
        while (causeList.size() < targetTier1 && !tier1Heap.isEmpty()) {
            Case c = tier1Heap.extractMax();
            if (scheduledIds.add(c.getCaseId())) {
                String reason = c.isStarving()
                        ? "Urgent Docket: Starvation Circuit Breaker (>1000 days pending)"
                        : "Urgent Docket: High Urgency Flag (" + c.getUrgencyFlag() + "/5) and Priority Score " + String.format("%.1f", c.calculatePriorityScore());
                causeList.add(new ScheduledSlot(c, "TIER_1_URGENT_HEAP", c.calculatePriorityScore(), reason));
                auditLog.record(c, "TIER_1_URGENT_HEAP", c.calculatePriorityScore(), reason);
            }
        }

        // Fill Tier 2 (Backlog FIFO)
        int tier2Limit = causeList.size() + targetTier2;
        while (causeList.size() < tier2Limit && !tier2Queue.isEmpty()) {
            Case c = tier2Queue.poll();
            if (scheduledIds.add(c.getCaseId())) {
                String reason = "Regular Backlog Docket: FIFO allocation (Age: " + c.getCaseAgeDays() + " days)";
                causeList.add(new ScheduledSlot(c, "TIER_2_BACKLOG_FIFO", c.calculatePriorityScore(), reason));
                auditLog.record(c, "TIER_2_BACKLOG_FIFO", c.calculatePriorityScore(), reason);
            }
        }

        // Fill Tier 3 (Fresh Admissions)
        int tier3Limit = causeList.size() + targetTier3;
        while (causeList.size() < tier3Limit && !tier3Queue.isEmpty()) {
            Case c = tier3Queue.poll();
            if (scheduledIds.add(c.getCaseId())) {
                String reason = "Fresh Admission: Recent filing within 60 days (Age: " + c.getCaseAgeDays() + " days)";
                causeList.add(new ScheduledSlot(c, "TIER_3_FRESH", c.calculatePriorityScore(), reason));
                auditLog.record(c, "TIER_3_FRESH", c.calculatePriorityScore(), reason);
            }
        }

        // Cascading overflow: If slots remain open, fill from any remaining cases by heap priority
        if (causeList.size() < totalSlots) {
            MaxHeap remainingHeap = new MaxHeap();
            for (Case c : allCases) {
                if (!scheduledIds.contains(c.getCaseId())) {
                    remainingHeap.insert(c);
                }
            }
            while (causeList.size() < totalSlots && !remainingHeap.isEmpty()) {
                Case c = remainingHeap.extractMax();
                if (scheduledIds.add(c.getCaseId())) {
                    String reason = "Cascading Surplus Allocation (Score: " + String.format("%.1f", c.calculatePriorityScore()) + ")";
                    causeList.add(new ScheduledSlot(c, "TIER_SURPLUS_CASCADE", c.calculatePriorityScore(), reason));
                    auditLog.record(c, "TIER_SURPLUS_CASCADE", c.calculatePriorityScore(), reason);
                }
            }
        }

        return causeList;
    }

    /**
     * Searches cases matching a single keyword using the Inverted Index.
     */
    public List<Case> searchCasesByKeyword(String keyword) {
        List<Integer> ids = invertedIndex.search(keyword);
        List<Case> results = new ArrayList<>();
        for (int id : ids) {
            Case c = caseMap.get(id);
            if (c != null) results.add(c);
        }
        return results;
    }

    /**
     * Searches cases matching two keywords concurrently (AND search) using Inverted Index.
     */
    public List<Case> searchCasesByKeywords(String k1, String k2) {
        List<Integer> ids = invertedIndex.searchAnd(k1, k2);
        List<Case> results = new ArrayList<>();
        for (int id : ids) {
            Case c = caseMap.get(id);
            if (c != null) results.add(c);
        }
        return results;
    }

    /**
     * Autocompletes a statutory code prefix using the Trie.
     */
    public List<String> autocompleteStatute(String prefix) {
        return statuteTrie.autocomplete(prefix);
    }

    /**
     * Inspects precedent validity and detects overruled chains using the Citation Graph.
     */
    public List<CitationAlert> checkPrecedentHealth(int caseId) {
        return citationGraph.checkOverruled(caseId);
    }

    /**
     * Evaluates Section 479 BNSS undertrial release eligibility for all cases.
     */
    public List<BNSSEligibility.EligibilityResult> getUndertrialEligibilityReport() {
        return bnssEligibility.evaluateDocket(allCases);
    }

    /**
     * Generates court pendency and statistical analytics dashboard.
     */
    public String getCourtStatisticsSummary() {
        return CourtStatistics.generateDashboardReport(allCases);
    }

    /**
     * Looks up a case by ID.
     */
    public Case getCaseById(int caseId) {
        Case c = caseMap.get(caseId);
        if (c == null) {
            throw new CaseNotFoundException(caseId);
        }
        return c;
    }

    public List<Case> getAllCases()            { return Collections.unmodifiableList(allCases); }
    public List<CitationEdge> getAllEdges()    { return Collections.unmodifiableList(allEdges); }
    public List<StatuteRecord> getAllStatutes(){ return Collections.unmodifiableList(allStatutes); }
    public CitationGraph getCitationGraph()    { return citationGraph; }
    public Trie getStatuteTrie()                { return statuteTrie; }
    public InvertedIndex getInvertedIndex()    { return invertedIndex; }
    public SchedulingAuditLog getAuditLog()    { return auditLog; }
}
