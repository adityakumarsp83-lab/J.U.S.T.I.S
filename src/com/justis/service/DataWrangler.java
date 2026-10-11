package com.justis.service;

import com.justis.exception.InvalidDatasetException;
import com.justis.model.Case;
import com.justis.model.CitationEdge;
import com.justis.model.CitationEdge.RelationType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data ingestion pipeline for loading, cleaning, and parsing CSV datasets
 * into type-safe Java domain objects.
 *
 * <p>Handles three CSV files:
 * <ul>
 *   <li>{@code cases_master.csv} → {@link Case} objects via {@link CaseFactory}</li>
 *   <li>{@code statutes_catalog.csv} → {@link StatuteRecord} objects</li>
 *   <li>{@code citations_dag.csv} → {@link CitationEdge} objects</li>
 * </ul>
 *
 * <p><b>OOP Concepts:</b> BufferedReader I/O, delegation to Factory Pattern,
 * custom checked exceptions, separation of concerns (parsing vs. construction).
 *
 * <p><b>FODS Concepts:</b> CSV data wrangling, string normalization, missing value
 * imputation, row-level validation with error aggregation.
 *
 * @author J.U.S.T.I.S. Team
 */
public class DataWrangler {

    /**
     * Immutable record representing a row from the statutes catalog CSV.
     */
    public static class StatuteRecord {
        private final String statuteCode;
        private final String actName;
        private final String description;
        private final int maxSentenceYears;

        public StatuteRecord(String statuteCode, String actName,
                              String description, int maxSentenceYears) {
            this.statuteCode = statuteCode;
            this.actName = actName;
            this.description = description;
            this.maxSentenceYears = maxSentenceYears;
        }

        public String getStatuteCode()    { return statuteCode; }
        public String getActName()        { return actName; }
        public String getDescription()    { return description; }
        public int getMaxSentenceYears()  { return maxSentenceYears; }

        @Override
        public String toString() {
            return statuteCode + " | " + actName + " | " + description
                 + " | Max: " + maxSentenceYears + " yrs";
        }
    }

    // ─── Cases Ingestion ──────────────────────────────────────────

    /**
     * Loads and parses the primary cases master CSV file.
     *
     * <p>Each valid row is passed to {@link CaseFactory#createCase(String[], int, String)}
     * for construction. Invalid rows are logged and skipped — they do not cause the
     * entire ingestion to fail.
     *
     * @param filePath absolute or relative path to {@code cases_master.csv}
     * @return a list of successfully parsed {@link Case} objects
     * @throws IOException if the file cannot be read
     */
    public static List<Case> loadCases(String filePath) throws IOException {
        List<Case> cases = new ArrayList<>();
        int totalRows = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // Skip header line
            String header = reader.readLine();
            if (header == null) {
                System.out.println("[DataWrangler] WARNING: Empty file — " + filePath);
                return cases;
            }

            String line;
            int lineNumber = 1; // header is line 1
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                totalRows++;

                // Skip blank lines
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                // Split CSV row (simple comma split — titles don't contain commas in our dataset)
                String[] columns = trimmed.split(",", -1);

                try {
                    Case newCase = CaseFactory.createCase(columns, lineNumber, trimmed);
                    cases.add(newCase);
                } catch (InvalidDatasetException e) {
                    errorCount++;
                    errors.add(e.toString());
                    System.out.println("[DataWrangler] SKIPPED: " + e);
                }
            }
        }

        // Print ingestion report
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║         Cases Ingestion Report                  ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║  File:     %-37s ║%n", filePath);
        System.out.printf("║  Total Rows Read:    %-27d ║%n", totalRows);
        System.out.printf("║  Successfully Loaded: %-26d ║%n", cases.size());
        System.out.printf("║  Errors / Skipped:    %-26d ║%n", errorCount);
        System.out.println("╚══════════════════════════════════════════════════╝");

        if (!errors.isEmpty()) {
            System.out.println("\n[DataWrangler] Error Details:");
            for (String err : errors) {
                System.out.println("  • " + err);
            }
        }

        return cases;
    }

    // ─── Statutes Catalog Ingestion ───────────────────────────────

    /**
     * Loads and parses the statutes catalog CSV file.
     *
     * <p><b>Expected columns:</b> statute_code, act_name, description, max_sentence_years
     *
     * @param filePath path to {@code statutes_catalog.csv}
     * @return a list of {@link StatuteRecord} objects
     * @throws IOException if the file cannot be read
     */
    public static List<StatuteRecord> loadStatutes(String filePath) throws IOException {
        List<StatuteRecord> statutes = new ArrayList<>();
        int errorCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // Skip header
            String header = reader.readLine();
            if (header == null) {
                System.out.println("[DataWrangler] WARNING: Empty statutes file — " + filePath);
                return statutes;
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                String[] cols = trimmed.split(",", -1);
                if (cols.length < 4) {
                    errorCount++;
                    System.out.println("[DataWrangler] SKIPPED statute line " + lineNumber
                                     + ": insufficient columns");
                    continue;
                }

                try {
                    String code = cols[0].trim();
                    String actName = cols[1].trim();
                    String description = cols[2].trim();
                    int maxYears = Integer.parseInt(cols[3].trim());

                    statutes.add(new StatuteRecord(code, actName, description, maxYears));
                } catch (NumberFormatException e) {
                    errorCount++;
                    System.out.println("[DataWrangler] SKIPPED statute line " + lineNumber
                                     + ": invalid max_sentence_years");
                }
            }
        }

        System.out.printf("[DataWrangler] Statutes loaded: %d (errors: %d) from %s%n",
                          statutes.size(), errorCount, filePath);
        return statutes;
    }

    // ─── Citations DAG Ingestion ──────────────────────────────────

    /**
     * Loads and parses the citations DAG CSV file.
     *
     * <p><b>Expected columns:</b> source_case_id, target_case_id, relation_type
     *
     * @param filePath path to {@code citations_dag.csv}
     * @return a list of {@link CitationEdge} objects
     * @throws IOException if the file cannot be read
     */
    public static List<CitationEdge> loadCitations(String filePath) throws IOException {
        List<CitationEdge> edges = new ArrayList<>();
        int errorCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // Skip header
            String header = reader.readLine();
            if (header == null) {
                System.out.println("[DataWrangler] WARNING: Empty citations file — " + filePath);
                return edges;
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                String[] cols = trimmed.split(",", -1);
                if (cols.length < 3) {
                    errorCount++;
                    System.out.println("[DataWrangler] SKIPPED citation line " + lineNumber
                                     + ": insufficient columns");
                    continue;
                }

                try {
                    int sourceId = Integer.parseInt(cols[0].trim());
                    int targetId = Integer.parseInt(cols[1].trim());
                    RelationType relType = RelationType.fromString(cols[2].trim());

                    edges.add(new CitationEdge(sourceId, targetId, relType));
                } catch (NumberFormatException e) {
                    errorCount++;
                    System.out.println("[DataWrangler] SKIPPED citation line " + lineNumber
                                     + ": invalid case ID");
                } catch (IllegalArgumentException e) {
                    errorCount++;
                    System.out.println("[DataWrangler] SKIPPED citation line " + lineNumber
                                     + ": " + e.getMessage());
                }
            }
        }

        System.out.printf("[DataWrangler] Citations loaded: %d (errors: %d) from %s%n",
                          edges.size(), errorCount, filePath);
        return edges;
    }
}
