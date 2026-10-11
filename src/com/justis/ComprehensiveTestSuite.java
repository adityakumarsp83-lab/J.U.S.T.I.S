package com.justis;

import com.justis.analytics.BNSSEligibility;
import com.justis.analytics.CourtStatistics;
import com.justis.ds.CitationGraph;
import com.justis.ds.CitationGraph.CitationAlert;
import com.justis.ds.CitationGraph.Severity;
import com.justis.ds.InvertedIndex;
import com.justis.ds.MaxHeap;
import com.justis.ds.Trie;
import com.justis.model.Case;
import com.justis.model.CitationEdge;
import com.justis.model.CitationEdge.RelationType;
import com.justis.model.CivilCase;
import com.justis.model.CriminalCase;
import com.justis.model.WritCase;
import com.justis.service.CheckpointManager;
import com.justis.service.DataWrangler;
import com.justis.service.DataWrangler.StatuteRecord;
import com.justis.service.SchedulerService;
import com.justis.service.SchedulerService.ScheduledSlot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive Automated Test Suite for J.U.S.T.I.S.
 *
 * <p>Validates all OOP and FODS components across all 5 project phases:
 * <ul>
 *   <li>Phase 2: Domain Hierarchy, CSV Ingestion, Polymorphic Priority Scoring</li>
 *   <li>Phase 3: Custom Data Structures (MaxHeap, Citation DAG, Trie, Inverted Index)</li>
 *   <li>Phase 4: Statistical Engine (Quickselect), BNSS Sec 479 Compliance, Scheduler Facade</li>
 *   <li>Phase 5: Checkpointing, Persistence, Cause-List Generation</li>
 * </ul>
 */
public class ComprehensiveTestSuite {

    private static int testsPassed = 0;
    private static int testsTotal = 0;

    private static void assertTrue(String testName, boolean condition) {
        testsTotal++;
        if (condition) {
            testsPassed++;
            System.out.println("  ✅ PASS: " + testName);
        } else {
            System.err.println("  ❌ FAIL: " + testName);
        }
    }

    public static void main(String[] args) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║        J.U.S.T.I.S. — FULL COMPREHENSIVE TEST SUITE           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        testDomainAndIngestion();
        testMaxHeap();
        testCitationGraph();
        testTrie();
        testInvertedIndex();
        testAnalyticsAndBNSS();
        testSchedulerServiceAndCheckpoint();

        System.out.println("\n────────────────────────────────────────────────────────────────");
        System.out.printf("Test Results: %d / %d Passed (%.1f%%)%n",
                testsPassed, testsTotal, (testsPassed * 100.0) / testsTotal);
        System.out.println("────────────────────────────────────────────────────────────────\n");

        if (testsPassed != testsTotal) {
            System.exit(1);
        }
    }

    private static void testDomainAndIngestion() {
        System.out.println("[Group 1] Domain Models & Ingestion Pipeline");
        try {
            List<Case> cases = DataWrangler.loadCases("data/cases_master.csv");
            assertTrue("Ingestion: Load 10 cases from CSV", cases.size() == 10);

            List<StatuteRecord> statutes = DataWrangler.loadStatutes("data/statutes_catalog.csv");
            assertTrue("Ingestion: Load 8 statutes from catalog", statutes.size() == 8);

            List<CitationEdge> edges = DataWrangler.loadCitations("data/citations_dag.csv");
            assertTrue("Ingestion: Load 9 citation edges", edges.size() == 9);

            // Polymorphism & scoring checks
            Case c108 = null;
            for (Case c : cases) {
                if (c.getCaseId() == 108) c108 = c;
            }
            assertTrue("Polymorphism: Case 108 is WritCase", c108 instanceof WritCase);
            assertTrue("Priority Scoring: Writ ceiling bonus elevates Case 108",
                    c108 != null && c108.calculatePriorityScore() > 600.0);

        } catch (Exception e) {
            assertTrue("Domain/Ingestion exception: " + e.getMessage(), false);
        }
    }

    private static void testMaxHeap() {
        System.out.println("\n[Group 2] Custom MaxHeap (FODS)");
        MaxHeap heap = new MaxHeap();
        assertTrue("MaxHeap: Initially empty", heap.isEmpty() && heap.size() == 0);

        CriminalCase c1 = new CriminalCase(1, "Case 1", 2024, 100, 50, 2, "BNS_103", new String[0]); // P = 3*50 + 2*100 + 10*2 = 370
        CriminalCase c2 = new CriminalCase(2, "Case 2", 2024, 200, 100, 4, "BNS_103", new String[0]); // P = 3*100 + 2*200 + 10*4 = 740
        CivilCase c3 = new CivilCase(3, "Case 3", 2023, 500, 1, "CPC_09", 100000.0, new String[0]); // P = 1.5*500 + 1 = 751

        heap.insert(c1);
        heap.insert(c2);
        heap.insert(c3);

        assertTrue("MaxHeap: Size is 3 after inserts", heap.size() == 3);
        assertTrue("MaxHeap: peekMax returns highest priority (Case 3: 751)", heap.peekMax().getCaseId() == 3);

        Case extracted1 = heap.extractMax();
        assertTrue("MaxHeap: First extractMax is Case 3", extracted1.getCaseId() == 3);
        Case extracted2 = heap.extractMax();
        assertTrue("MaxHeap: Second extractMax is Case 2", extracted2.getCaseId() == 2);
        Case extracted3 = heap.extractMax();
        assertTrue("MaxHeap: Third extractMax is Case 1", extracted3.getCaseId() == 1);
        assertTrue("MaxHeap: Empty after extracting all", heap.isEmpty());

        // Floyd's buildHeap
        List<Case> list = List.of(c1, c2, c3);
        MaxHeap heap2 = new MaxHeap();
        heap2.buildHeap(list);
        assertTrue("MaxHeap: buildHeap produces correct max root", heap2.extractMax().getCaseId() == 3);
    }

    private static void testCitationGraph() {
        System.out.println("\n[Group 3] Custom Citation Graph (DAG & Overrule DFS)");
        CitationGraph graph = new CitationGraph();

        // 109 overrules 101, 102 cites 101, 104 cites 102
        graph.addEdge(new CitationEdge(109, 101, RelationType.OVERRULES));
        graph.addEdge(new CitationEdge(102, 101, RelationType.CITES));
        graph.addEdge(new CitationEdge(104, 102, RelationType.RELIES_ON));

        assertTrue("CitationGraph: Cycle detection reports clean DAG", !graph.hasCycle());
        assertTrue("CitationGraph: Case 101 recognized as overruled", graph.isOverruled(101));
        assertTrue("CitationGraph: Case 101 in-degree is 2", graph.getInDegree(101) == 2);

        List<CitationAlert> alerts102 = graph.checkOverruled(102);
        assertTrue("CitationGraph: Case 102 generates overrule alert", !alerts102.isEmpty());
        assertTrue("CitationGraph: Alert identifies Case 101 as problematic",
                alerts102.get(0).getProblematicCaseId() == 101);

        List<CitationAlert> alerts104 = graph.checkOverruled(104);
        assertTrue("CitationGraph: Transitive overrule alert reaches Case 104", !alerts104.isEmpty());

        // Cycle injection test
        CitationGraph cyclicGraph = new CitationGraph();
        cyclicGraph.addEdge(new CitationEdge(1, 2, RelationType.CITES));
        cyclicGraph.addEdge(new CitationEdge(2, 3, RelationType.CITES));
        cyclicGraph.addEdge(new CitationEdge(3, 1, RelationType.CITES));
        assertTrue("CitationGraph: Correctly detects cycle in cyclic graph", cyclicGraph.hasCycle());
    }

    private static void testTrie() {
        System.out.println("\n[Group 4] Custom Trie (Prefix Search)");
        Trie trie = new Trie();
        trie.insert("BNS_103");
        trie.insert("BNS_111");
        trie.insert("BNS_303");
        trie.insert("BNSS_479");
        trie.insert("CONST_226");

        assertTrue("Trie: Search exact match 'BNS_103'", trie.search("BNS_103"));
        assertTrue("Trie: Search non-existent 'BNS_999'", !trie.search("BNS_999"));

        List<String> bnsMatches = trie.autocomplete("BNS");
        assertTrue("Trie: Autocomplete 'BNS' finds 4 codes (including BNSS)", bnsMatches.size() == 4);

        List<String> bnssMatches = trie.autocomplete("BNSS");
        assertTrue("Trie: Autocomplete 'BNSS' finds BNSS_479",
                bnssMatches.size() == 1 && bnssMatches.contains("BNSS_479"));

        List<String> emptyMatches = trie.autocomplete("XYZ");
        assertTrue("Trie: Autocomplete unmatched prefix returns empty", emptyMatches.isEmpty());
    }

    private static void testInvertedIndex() {
        System.out.println("\n[Group 5] Custom Inverted Index (Separate Chaining)");
        InvertedIndex index = new InvertedIndex(16);

        index.index(101, "State of Maharashtra v. Rahul Sharma");
        index.index(102, "Anand Kumar v. Union of India");
        index.index(104, "State of Gujarat v. Vikas Patel");

        List<Integer> stateSearch = index.search("state");
        assertTrue("InvertedIndex: Search 'state' returns 101 and 104",
                stateSearch.size() == 2 && stateSearch.contains(101) && stateSearch.contains(104));

        List<Integer> rahulSearch = index.search("rahul");
        assertTrue("InvertedIndex: Search 'rahul' returns 101",
                rahulSearch.size() == 1 && rahulSearch.contains(101));

        List<Integer> andSearch = index.searchAnd("state", "vikas");
        assertTrue("InvertedIndex: AND search ('state' AND 'vikas') returns 104",
                andSearch.size() == 1 && andSearch.contains(104));

        List<Integer> noMatch = index.searchAnd("anand", "patel");
        assertTrue("InvertedIndex: AND search with non-overlapping terms returns empty", noMatch.isEmpty());
    }

    private static void testAnalyticsAndBNSS() {
        System.out.println("\n[Group 6] Court Analytics & BNSS Compliance");
        try {
            List<Case> cases = DataWrangler.loadCases("data/cases_master.csv");
            List<StatuteRecord> statutes = DataWrangler.loadStatutes("data/statutes_catalog.csv");

            // Quickselect median
            double median = CourtStatistics.medianCaseAge(cases);
            double mean = CourtStatistics.meanCaseAge(cases);
            assertTrue("CourtStatistics: Median case age computed (> 0)", median > 0);
            assertTrue("CourtStatistics: Mean case age computed (> 0)", mean > 0);

            // BNSS Eligibility
            BNSSEligibility bnss = new BNSSEligibility(statutes);
            List<BNSSEligibility.EligibilityResult> results = bnss.evaluateDocket(cases);
            assertTrue("BNSSEligibility: Evaluated all 10 cases", results.size() == 10);

            // Case 106 has statute BNSS_479 (maxSentence = 0 -> non custodial provision)
            boolean foundNonCustodial = false;
            for (BNSSEligibility.EligibilityResult r : results) {
                if (r.getCaseId() == 106 && r.getStatus() == BNSSEligibility.Status.NON_CUSTODIAL_PROVISION) {
                    foundNonCustodial = true;
                }
            }
            assertTrue("BNSSEligibility: Identified non-custodial statute provision for Case 106", foundNonCustodial);

        } catch (Exception e) {
            assertTrue("Analytics/BNSS exception: " + e.getMessage(), false);
        }
    }

    private static void testSchedulerServiceAndCheckpoint() {
        System.out.println("\n[Group 7] Scheduler Facade & Checkpointing");
        try {
            List<Case> cases = DataWrangler.loadCases("data/cases_master.csv");
            List<StatuteRecord> statutes = DataWrangler.loadStatutes("data/statutes_catalog.csv");
            List<CitationEdge> edges = DataWrangler.loadCitations("data/citations_dag.csv");

            SchedulerService svc = new SchedulerService(cases, edges, statutes, "data/test_audit_log.csv");
            List<ScheduledSlot> causeList = svc.generateCauseList(10);
            assertTrue("SchedulerService: Generated cause list of 10 slots", causeList.size() == 10);

            // Audit log created
            File logFile = new File("data/test_audit_log.csv");
            assertTrue("SchedulerService: Audit log file generated", logFile.exists() && logFile.length() > 0);
            logFile.delete(); // cleanup test file

            // Checkpoint save and load
            String cpPath = "data/test_checkpoint.dat";
            CheckpointManager.save(svc, cpPath);
            File cpFile = new File(cpPath);
            assertTrue("CheckpointManager: Saved binary checkpoint file", cpFile.exists() && cpFile.length() > 0);

            SchedulerService restored = CheckpointManager.load(cpPath);
            assertTrue("CheckpointManager: Restored all 10 cases", restored.getAllCases().size() == 10);
            assertTrue("CheckpointManager: Restored all 9 citation edges", restored.getAllEdges().size() == 9);
            cpFile.delete(); // cleanup test file

        } catch (Exception e) {
            assertTrue("Scheduler/Checkpoint exception: " + e.getMessage(), false);
        }
    }
}
