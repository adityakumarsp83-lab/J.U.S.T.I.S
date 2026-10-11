package com.justis;

import com.justis.model.*;
import com.justis.service.DataWrangler;
import com.justis.service.DataWrangler.StatuteRecord;

import java.io.IOException;
import java.util.List;

/**
 * Phase 2 Smoke Test — validates the complete ingestion pipeline.
 *
 * <p>Verifies:
 * <ol>
 *   <li>All 10 cases load successfully from CSV</li>
 *   <li>Correct subclass instantiation (CriminalCase, CivilCase, WritCase)</li>
 *   <li>Priority scores are computed correctly</li>
 *   <li>Starvation flags are set correctly</li>
 *   <li>Statutes and citations load correctly</li>
 * </ol>
 */
public class Phase2SmokeTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  J.U.S.T.I.S. — Phase 2 Smoke Test");
        System.out.println("═══════════════════════════════════════════════════\n");

        try {
            // ─── Test 1: Load Cases ───────────────────────────────
            List<Case> cases = DataWrangler.loadCases("data/cases_master.csv");
            assertTest("Cases loaded count", cases.size() == 10,
                       "Expected 10, got " + cases.size());

            // ─── Test 2: Correct Subclass Types ───────────────────
            Case case101 = findCase(cases, 101);
            Case case103 = findCase(cases, 103);
            Case case108 = findCase(cases, 108);

            assertTest("Case 101 is CriminalCase", case101 instanceof CriminalCase,
                       "Got " + case101.getClass().getSimpleName());
            assertTest("Case 103 is CivilCase", case103 instanceof CivilCase,
                       "Got " + case103.getClass().getSimpleName());
            assertTest("Case 108 is WritCase", case108 instanceof WritCase,
                       "Got " + case108.getClass().getSimpleName());

            // ─── Test 3: Priority Score Computation ───────────────
            // Case 101 (Criminal): P = (3.0 × 450) + (2.0 × 1250) + (10.0 × 5) = 1350 + 2500 + 50 = 3900
            double score101 = case101.calculatePriorityScore();
            assertTest("Case 101 priority score = 3900.0",
                       Math.abs(score101 - 3900.0) < 0.01,
                       "Got " + score101);

            // Case 103 (Civil): P = (1.5 × 2100) + (12500000 / 100000) = 3150 + 125 = 3275
            double score103 = case103.calculatePriorityScore();
            assertTest("Case 103 priority score = 3275.0",
                       Math.abs(score103 - 3275.0) < 0.01,
                       "Got " + score103);

            // Case 108 (Writ): P = (5.0 × 15) + (15.0 × 5) + 500 = 75 + 75 + 500 = 650
            double score108 = case108.calculatePriorityScore();
            assertTest("Case 108 priority score = 650.0",
                       Math.abs(score108 - 650.0) < 0.01,
                       "Got " + score108);

            // ─── Test 4: Starvation Flags ─────────────────────────
            assertTest("Case 101 (1250 days) is starving", case101.isStarving(),
                       "isStarving=" + case101.isStarving());
            assertTest("Case 104 (120 days) is NOT starving", !findCase(cases, 104).isStarving(),
                       "isStarving=" + findCase(cases, 104).isStarving());

            // ─── Test 5: Case Status Default ──────────────────────
            assertTest("Case 101 status is PENDING",
                       case101.getStatus() == CaseStatus.PENDING,
                       "Got " + case101.getStatus());

            // ─── Test 6: Cited Case IDs Parsing ───────────────────
            Case case109 = findCase(cases, 109);
            assertTest("Case 109 has 2 cited cases",
                       case109.getCitedCaseIds().length == 2,
                       "Got " + case109.getCitedCaseIds().length);
            assertTest("Case 101 has 0 cited cases (NONE)",
                       case101.getCitedCaseIds().length == 0,
                       "Got " + case101.getCitedCaseIds().length);

            // ─── Test 7: Load Statutes ────────────────────────────
            System.out.println();
            List<StatuteRecord> statutes = DataWrangler.loadStatutes("data/statutes_catalog.csv");
            assertTest("Statutes loaded count", statutes.size() == 8,
                       "Expected 8, got " + statutes.size());

            // ─── Test 8: Load Citations ───────────────────────────
            List<CitationEdge> edges = DataWrangler.loadCitations("data/citations_dag.csv");
            assertTest("Citations loaded count", edges.size() == 9,
                       "Expected 9, got " + edges.size());

            // ─── Test 9: CompareTo Ordering ───────────────────────
            // Case 109 (Criminal, 1450 days, 600 custody, urgency 5)
            // P = (3.0*600) + (2.0*1450) + (10.0*5) = 1800 + 2900 + 50 = 4750
            // Case 101 P = 3900, so 109 should rank higher
            Case case109b = findCase(cases, 109);
            assertTest("Case 109 ranks higher than Case 101",
                       case109b.compareTo(case101) < 0,
                       "compareTo returned: " + case109b.compareTo(case101));

            // ─── Test 10: Print All Cases ─────────────────────────
            System.out.println("\n─── All Loaded Cases ───────────────────────────");
            for (Case c : cases) {
                System.out.printf("  %-6s #%d | Score: %8.1f | %s%n",
                    c.getCaseType(), c.getCaseId(),
                    c.calculatePriorityScore(),
                    c.getTitle());
            }

        } catch (IOException e) {
            System.out.println("\n[FATAL] I/O Error: " + e.getMessage());
            e.printStackTrace();
        }

        // ─── Final Report ─────────────────────────────────────────
        System.out.println("\n═══════════════════════════════════════════════════");
        System.out.printf("  RESULTS: %d PASSED  |  %d FAILED  |  %d TOTAL%n",
                          passed, failed, passed + failed);
        System.out.println("═══════════════════════════════════════════════════");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void assertTest(String testName, boolean condition, String detail) {
        if (condition) {
            passed++;
            System.out.println("  ✅ PASS: " + testName);
        } else {
            failed++;
            System.out.println("  ❌ FAIL: " + testName + " — " + detail);
        }
    }

    private static Case findCase(List<Case> cases, int caseId) {
        for (Case c : cases) {
            if (c.getCaseId() == caseId) return c;
        }
        throw new RuntimeException("Case not found: " + caseId);
    }
}
