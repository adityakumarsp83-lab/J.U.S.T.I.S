package com.justis;

import com.justis.analytics.BNSSEligibility;
import com.justis.analytics.CourtStatistics;
import com.justis.ds.CitationGraph.CitationAlert;
import com.justis.exception.CaseNotFoundException;
import com.justis.model.Case;
import com.justis.model.CitationEdge;
import com.justis.model.CivilCase;
import com.justis.model.CriminalCase;
import com.justis.model.WritCase;
import com.justis.service.CheckpointManager;
import com.justis.service.DataWrangler;
import com.justis.service.DataWrangler.StatuteRecord;
import com.justis.service.SchedulerService;
import com.justis.service.SchedulerService.ScheduledSlot;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * J.U.S.T.I.S. Interactive Terminal Application.
 *
 * <p><b>Judicial Unified Search, Triaging & Intelligent Scheduling</b>
 * <br>A pure Java (JDK 17+) algorithmic legal-technology platform.
 */
public class Main {

    // ANSI Colors
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BLUE   = "\u001B[34m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED    = "\u001B[31m";
    private static final String PURPLE = "\u001B[35m";

    private static SchedulerService scheduler;

    public static void main(String[] args) {
        printBanner();

        // Check if demo or batch mode is requested via args
        if (args.length > 0 && args[0].equalsIgnoreCase("--demo")) {
            runAutomatedDemo();
            return;
        }

        // Initialize system from CSV datasets
        boolean initialized = initSystem();
        if (!initialized) {
            System.out.println(RED + "Failed to initialize J.U.S.T.I.S. datasets. Exiting." + RESET);
            return;
        }

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMainMenu();
            System.out.print(BOLD + "Select an option [1-9]: " + RESET);
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1" -> handleGenerateCauseList(scanner);
                case "2" -> handleSearchKeywords(scanner);
                case "3" -> handleStatuteAutocomplete(scanner);
                case "4" -> handleCheckPrecedents(scanner);
                case "5" -> handleBNSSEligibilityReport();
                case "6" -> handleStatisticsDashboard();
                case "7" -> handleViewCaseDetails(scanner);
                case "8" -> handleSaveLoadCheckpoint(scanner);
                case "9" -> {
                    System.out.println("\n" + CYAN + "Thank you for using J.U.S.T.I.S. Justice Delayed is Justice Denied." + RESET);
                    running = false;
                }
                default -> System.out.println(RED + "⚠️  Invalid option. Please choose between 1 and 9." + RESET);
            }

            if (running) {
                System.out.print("\nPress Enter to return to main menu...");
                scanner.nextLine();
            }
        }
    }

    private static void printBanner() {
        System.out.println(CYAN + BOLD);
        System.out.println("╔════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                                ║");
        System.out.println("║      ⚖️   J . U . S . T . I . S .                                               ║");
        System.out.println("║      Judicial Unified Search, Triaging & Intelligent Scheduling               ║");
        System.out.println("║                                                                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println(PURPLE + "   Algorithmic Case Triaging • Precedent DAG Health • BNSS Sec 479 Compliance\n" + RESET);
    }

    private static boolean initSystem() {
        System.out.println(BOLD + "[System Initialization]" + RESET);
        try {
            System.out.print("  • Loading cases from data/cases_master.csv ... ");
            List<Case> cases = DataWrangler.loadCases("data/cases_master.csv");

            System.out.print("  • Loading statutes from data/statutes_catalog.csv ... ");
            List<StatuteRecord> statutes = DataWrangler.loadStatutes("data/statutes_catalog.csv");

            System.out.print("  • Loading citations DAG from data/citations_dag.csv ... ");
            List<CitationEdge> citations = DataWrangler.loadCitations("data/citations_dag.csv");

            scheduler = new SchedulerService(cases, citations, statutes);
            System.out.println(GREEN + "  ✅ All data structures initialized successfully!\n" + RESET);
            return true;
        } catch (IOException e) {
            System.out.println(RED + "  ❌ Error loading dataset: " + e.getMessage() + RESET);
            return false;
        }
    }

    private static void printMainMenu() {
        System.out.println("\n" + BOLD + "╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                     J.U.S.T.I.S. MAIN MENU                   ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  1. 📋 Generate Today's Cause List (60/30/10 Triaged Docket) ║");
        System.out.println("║  2. 🔍 Search Cases by Keyword (Inverted Index)              ║");
        System.out.println("║  3. 📜 Statute Autocomplete Search (Trie O(K))               ║");
        System.out.println("║  4. ⚖️  Check Precedent Validity (Citation DAG Overrule DFS)  ║");
        System.out.println("║  5. 🔓 BNSS Section 479 Undertrial Eligibility Report        ║");
        System.out.println("║  6. 📊 Court Pendency & Workload Dashboard (Quickselect)     ║");
        System.out.println("║  7. 📄 View Complete Case Details by ID                      ║");
        System.out.println("║  8. 💾 Save / Load Checkpoint (Binary Serialization)         ║");
        System.out.println("║  9. 🚪 Exit J.U.S.T.I.S.                                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝" + RESET);
    }

    private static void handleGenerateCauseList(Scanner scanner) {
        System.out.println("\n" + CYAN + BOLD + "─── [1] GENERATE DAILY CAUSE LIST ───" + RESET);
        System.out.print("Enter total docket capacity slots (e.g., 5, 8, 10): ");
        String slotStr = scanner.nextLine().trim();
        int slots;
        try {
            slots = Integer.parseInt(slotStr);
            if (slots <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println(YELLOW + "Invalid slot count. Defaulting to 10 slots." + RESET);
            slots = 10;
        }

        List<ScheduledSlot> causeList = scheduler.generateCauseList(slots);

        System.out.println("\n" + GREEN + BOLD + "╔═══════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.printf("║                                    DAILY CAUSE LIST (%2d SLOTS SCHEDULED)                             ║%n", causeList.size());
        System.out.println("╠════╦══════╦════════════╦═══════════════════════════════╦═══════╦═══════════════════╦══════════════════╣");
        System.out.println("║ SL ║ ID   ║ TYPE       ║ TITLE                         ║ SCORE ║ TIER              ║ AGE / CUSTODY    ║");
        System.out.println("╠════╬══════╬════════════╬═══════════════════════════════╬═══════╬═══════════════════╬══════════════════╣");

        int sl = 1;
        for (ScheduledSlot slot : causeList) {
            Case c = slot.getScheduledCase();
            String ageCustody;
            if (c instanceof CriminalCase cc) {
                ageCustody = String.format("%dd / %dd cust", cc.getCaseAgeDays(), cc.getCustodyDays());
            } else if (c instanceof WritCase wc) {
                ageCustody = String.format("%dd / %dd det", wc.getCaseAgeDays(), wc.getCustodyDays());
            } else {
                ageCustody = String.format("%dd / ₹%.0f", c.getCaseAgeDays(), ((CivilCase) c).getDisputeValueINR());
            }

            String titleShort = c.getTitle();
            if (titleShort.length() > 29) {
                titleShort = titleShort.substring(0, 26) + "...";
            }

            String tierDisplay = switch (slot.getTier()) {
                case "TIER_1_URGENT_HEAP" -> "URGENT (HEAP)";
                case "TIER_2_BACKLOG_FIFO" -> "BACKLOG (FIFO)";
                case "TIER_3_FRESH" -> "FRESH ADMISSION";
                default -> "SURPLUS CASCADE";
            };

            System.out.printf("║ %2d ║ #%-4d ║ %-10s ║ %-29s ║ %5.1f ║ %-17s ║ %-16s ║%n",
                    sl++, c.getCaseId(), c.getCaseType(), titleShort, slot.getPriorityScore(), tierDisplay, ageCustody);
        }
        System.out.println("╚════╩══════╩════════════╩═══════════════════════════════╩═══════╩═══════════════════╩══════════════════╝" + RESET);
        System.out.println(GREEN + "  Audit log successfully written to data/audit_log.csv" + RESET);
    }

    private static void handleSearchKeywords(Scanner scanner) {
        System.out.println("\n" + CYAN + BOLD + "─── [2] INVERTED INDEX KEYWORD SEARCH ───" + RESET);
        System.out.print("Enter search term(s) (e.g., 'State', 'Bail', 'Rahman'): ");
        String query = scanner.nextLine().trim();

        if (query.isEmpty()) {
            System.out.println(YELLOW + "Query cannot be empty." + RESET);
            return;
        }

        String[] tokens = query.split("\\s+");
        List<Case> results;
        if (tokens.length == 1) {
            results = scheduler.searchCasesByKeyword(tokens[0]);
        } else {
            results = scheduler.searchCasesByKeywords(tokens[0], tokens[1]);
        }

        if (results.isEmpty()) {
            System.out.println(YELLOW + "No matching cases found for: '" + query + "'" + RESET);
        } else {
            System.out.println(GREEN + "\nFound " + results.size() + " matching case(s):" + RESET);
            for (Case c : results) {
                System.out.printf("  • [Case #%d] %s (%s, Age: %d d, Score: %.1f)%n",
                        c.getCaseId(), c.getTitle(), c.getCaseType(), c.getCaseAgeDays(), c.calculatePriorityScore());
            }
        }
    }

    private static void handleStatuteAutocomplete(Scanner scanner) {
        System.out.println("\n" + CYAN + BOLD + "─── [3] STATUTORY CODE AUTOCOMPLETE (TRIE) ───" + RESET);
        System.out.print("Enter statutory prefix (e.g., 'BNS', 'BNSS', 'CONST', 'BNS_1'): ");
        String prefix = scanner.nextLine().trim();

        List<String> matches = scheduler.autocompleteStatute(prefix);
        if (matches.isEmpty()) {
            System.out.println(YELLOW + "No statutory codes matching prefix: '" + prefix + "'" + RESET);
        } else {
            System.out.println(GREEN + "\nFound " + matches.size() + " statutory provision(s):" + RESET);
            for (String code : matches) {
                System.out.println("  • " + BOLD + code + RESET);
            }
        }
    }

    private static void handleCheckPrecedents(Scanner scanner) {
        System.out.println("\n" + CYAN + BOLD + "─── [4] PRECEDENT VALIDITY & CITATION HEALTH ───" + RESET);
        System.out.print("Enter Case ID to inspect citation health (e.g., 102, 104, 108): ");
        String idStr = scanner.nextLine().trim();

        try {
            int caseId = Integer.parseInt(idStr);
            Case c = scheduler.getCaseById(caseId);
            System.out.println("\nInspecting citations for: " + BOLD + c.getTitle() + " (Case #" + caseId + ")" + RESET);

            List<CitationAlert> alerts = scheduler.checkPrecedentHealth(caseId);
            if (alerts.isEmpty()) {
                System.out.println(GREEN + "  ✅ CLEAN CITATION RECORD: No cited precedents have been overruled." + RESET);
            } else {
                System.out.println(RED + BOLD + "  ⚠️  CITATION HEALTH ALERTS DETECTED (" + alerts.size() + "):" + RESET);
                for (CitationAlert alert : alerts) {
                    String badge = switch (alert.getSeverity()) {
                        case CRITICAL -> RED + "[CRITICAL]" + RESET;
                        case WARNING  -> YELLOW + "[WARNING]" + RESET;
                        case INFO     -> CYAN + "[INFO]" + RESET;
                    };
                    System.out.println("  " + badge + " " + alert.getChain());
                }
            }
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid Case ID format." + RESET);
        } catch (CaseNotFoundException e) {
            System.out.println(RED + e.getMessage() + RESET);
        }
    }

    private static void handleBNSSEligibilityReport() {
        System.out.println("\n" + CYAN + BOLD + "─── [5] BNSS SECTION 479 UNDERTRIAL ELIGIBILITY REPORT ───" + RESET);
        List<BNSSEligibility.EligibilityResult> results = scheduler.getUndertrialEligibilityReport();

        int eligibleCount = 0;
        for (BNSSEligibility.EligibilityResult r : results) {
            if (r.getStatus() == BNSSEligibility.Status.ELIGIBLE_FOR_RELEASE) {
                eligibleCount++;
                System.out.println(GREEN + BOLD + "  🟢 " + r + RESET);
            } else if (r.getStatus() == BNSSEligibility.Status.NOT_YET_ELIGIBLE) {
                System.out.println(YELLOW + "  🟡 " + r + RESET);
            } else {
                System.out.println("  ℹ️  " + r);
            }
        }

        System.out.println("\n" + BOLD + "Summary: " + eligibleCount + " undertrial(s) eligible for immediate statutory release." + RESET);
    }

    private static void handleStatisticsDashboard() {
        System.out.println("\n" + CYAN + BOLD + "─── [6] COURT PENDENCY & WORKLOAD DASHBOARD ───" + RESET);
        System.out.println(scheduler.getCourtStatisticsSummary());

        System.out.println(BOLD + "Top 3 Oldest Pending Cases:" + RESET);
        List<Case> oldest = CourtStatistics.topKPendency(scheduler.getAllCases(), 3);
        for (int i = 0; i < oldest.size(); i++) {
            Case c = oldest.get(i);
            System.out.printf("  %d. [Case #%d] %s — Age: %d days (%s)%n",
                    i + 1, c.getCaseId(), c.getTitle(), c.getCaseAgeDays(), c.getCaseType());
        }
    }

    private static void handleViewCaseDetails(Scanner scanner) {
        System.out.println("\n" + CYAN + BOLD + "─── [7] VIEW CASE DETAILS ───" + RESET);
        System.out.print("Enter Case ID (e.g., 101 to 110): ");
        String idStr = scanner.nextLine().trim();

        try {
            int caseId = Integer.parseInt(idStr);
            Case c = scheduler.getCaseById(caseId);

            System.out.println("\n" + BOLD + "╔════════════════════════════════════════════════════════════════════════╗");
            System.out.printf("║  CASE RECORD: #%-55d ║%n", c.getCaseId());
            System.out.println("╠════════════════════════════════════════════════════════════════════════╣");
            System.out.printf("║  Title:            %-51s ║%n", c.getTitle());
            System.out.printf("║  Type:             %-51s ║%n", c.getCaseType());
            System.out.printf("║  Filing Year:      %-51d ║%n", c.getFilingYear());
            System.out.printf("║  Age in Days:      %-51d ║%n", c.getCaseAgeDays());
            System.out.printf("║  Urgency Flag:     %-51d ║%n", c.getUrgencyFlag());
            System.out.printf("║  Statute Code:     %-51s ║%n", c.getStatuteCode());
            System.out.printf("║  Priority Score:   %-51.2f ║%n", c.calculatePriorityScore());
            System.out.printf("║  Status:           %-51s ║%n", c.getStatus());
            System.out.printf("║  Starving Flag:    %-51s ║%n", c.isStarving() ? "YES (Age > 1000 days)" : "NO");

            if (c instanceof CriminalCase cc) {
                System.out.printf("║  Custody Duration: %-51s ║%n", cc.getCustodyDays() + " days");
            } else if (c instanceof CivilCase civ) {
                System.out.printf("║  Dispute Value:    ₹%-50.2f ║%n", civ.getDisputeValueINR());
            } else if (c instanceof WritCase wc) {
                System.out.printf("║  Detention Period: %-51s ║%n", wc.getCustodyDays() + " days");
            }

            System.out.printf("║  Citations:        %-51s ║%n", Arrays.toString(c.getCitedCaseIds()));
            System.out.println("╚════════════════════════════════════════════════════════════════════════╝" + RESET);
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid Case ID format." + RESET);
        } catch (CaseNotFoundException e) {
            System.out.println(RED + e.getMessage() + RESET);
        }
    }

    private static void handleSaveLoadCheckpoint(Scanner scanner) {
        System.out.println("\n" + CYAN + BOLD + "─── [8] CHECKPOINT PERSISTENCE ───" + RESET);
        System.out.println("  1. Save current state to data/checkpoint.dat");
        System.out.println("  2. Load state from data/checkpoint.dat");
        System.out.print("Select choice [1 or 2]: ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            try {
                CheckpointManager.save(scheduler, "data/checkpoint.dat");
                System.out.println(GREEN + "✅ System state saved successfully to data/checkpoint.dat" + RESET);
            } catch (IOException e) {
                System.out.println(RED + "Failed to save checkpoint: " + e.getMessage() + RESET);
            }
        } else if (choice.equals("2")) {
            try {
                scheduler = CheckpointManager.load("data/checkpoint.dat");
                System.out.println(GREEN + "✅ System state loaded successfully from data/checkpoint.dat" + RESET);
            } catch (Exception e) {
                System.out.println(RED + "Failed to load checkpoint: " + e.getMessage() + RESET);
            }
        } else {
            System.out.println(YELLOW + "Invalid choice." + RESET);
        }
    }

    private static void runAutomatedDemo() {
        System.out.println(BOLD + "\n[RUNNING J.U.S.T.I.S. AUTOMATED VERIFICATION DEMO]" + RESET);
        initSystem();

        System.out.println("\n1. Triaged Cause List Generation (10 slots):");
        List<ScheduledSlot> causeList = scheduler.generateCauseList(10);
        for (int i = 0; i < causeList.size(); i++) {
            ScheduledSlot s = causeList.get(i);
            System.out.printf("   #%d: [Case #%d] %s (%s, Score: %.1f)%n",
                    i + 1, s.getScheduledCase().getCaseId(), s.getScheduledCase().getTitle(), s.getTier(), s.getPriorityScore());
        }

        System.out.println("\n2. Keyword Search ('State'):");
        for (Case c : scheduler.searchCasesByKeyword("state")) {
            System.out.printf("   • Case #%d: %s%n", c.getCaseId(), c.getTitle());
        }

        System.out.println("\n3. Statute Autocomplete ('BNS'):");
        for (String st : scheduler.autocompleteStatute("BNS")) {
            System.out.printf("   • %s%n", st);
        }

        System.out.println("\n4. Citation Health for Case #102:");
        for (CitationAlert alert : scheduler.checkPrecedentHealth(102)) {
            System.out.printf("   • %s%n", alert);
        }

        System.out.println("\n5. Court Statistics Dashboard:");
        System.out.println(scheduler.getCourtStatisticsSummary());

        System.out.println(GREEN + BOLD + "✅ Automated Verification Demo Complete!\n" + RESET);
    }
}
