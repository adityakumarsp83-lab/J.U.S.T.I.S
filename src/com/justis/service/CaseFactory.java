package com.justis.service;

import com.justis.exception.InvalidDatasetException;
import com.justis.model.*;

/**
 * Factory class responsible for instantiating the correct {@link Case} subclass
 * based on parsed CSV column data.
 *
 * <p>Implements the <b>Factory Design Pattern</b> — the caller passes raw CSV data
 * and receives a fully constructed, type-safe domain object without needing to know
 * which concrete subclass was instantiated.
 *
 * <p><b>OOP Concepts:</b> Factory Pattern, static factory method, switch dispatching,
 * input validation with custom exceptions, encapsulation of construction logic.
 *
 * <p><b>Expected CSV Column Order (0-indexed):</b>
 * <pre>
 *   0: case_id           (int)
 *   1: case_type         (String → CaseType enum)
 *   2: title             (String)
 *   3: filing_year       (int)
 *   4: case_age_days     (int, ≥ 0)
 *   5: custody_days      (int, ≥ 0)
 *   6: urgency_flag      (int, 1–5)
 *   7: statute_code      (String)
 *   8: dispute_value_inr (double, ≥ 0)
 *   9: cited_case_ids    (String, pipe-delimited or "NONE")
 * </pre>
 *
 * @author J.U.S.T.I.S. Team
 */
public class CaseFactory {

    /** Expected number of columns in each CSV data row. */
    private static final int EXPECTED_COLUMNS = 10;

    /**
     * Creates the appropriate {@link Case} subclass from a parsed CSV row.
     *
     * @param columns    the string array of CSV column values
     * @param lineNumber the 1-indexed line number in the CSV file (for error reporting)
     * @param rawRow     the original raw CSV line (for error context)
     * @return a fully constructed {@link CriminalCase}, {@link CivilCase}, or {@link WritCase}
     * @throws InvalidDatasetException if validation fails on any field
     */
    public static Case createCase(String[] columns, int lineNumber, String rawRow)
            throws InvalidDatasetException {

        // ─── Column Count Validation ──────────────────────────────
        if (columns == null || columns.length < EXPECTED_COLUMNS) {
            throw new InvalidDatasetException(
                "Expected " + EXPECTED_COLUMNS + " columns, found " +
                (columns == null ? 0 : columns.length) + ".",
                lineNumber, rawRow
            );
        }

        try {
            // ─── Parse & Validate Individual Fields ───────────────
            int caseId = parsePositiveInt(columns[0].trim(), "case_id", lineNumber, rawRow);

            CaseType caseType = CaseType.fromString(columns[1]);

            String title = columns[2].trim();
            if (title.isEmpty()) {
                throw new InvalidDatasetException("Title is empty.", lineNumber, rawRow);
            }

            int filingYear = parseInt(columns[3].trim(), "filing_year", lineNumber, rawRow);
            if (filingYear < 1947 || filingYear > 2030) {
                throw new InvalidDatasetException(
                    "Filing year out of valid range (1947–2030): " + filingYear,
                    lineNumber, rawRow
                );
            }

            int caseAgeDays = parseNonNegativeInt(columns[4].trim(), "case_age_days", lineNumber, rawRow);

            int custodyDays = parseNonNegativeInt(columns[5].trim(), "custody_days", lineNumber, rawRow);

            int urgencyFlag = parseInt(columns[6].trim(), "urgency_flag", lineNumber, rawRow);
            if (urgencyFlag < 1 || urgencyFlag > 5) {
                throw new InvalidDatasetException(
                    "Urgency flag must be between 1 and 5, found: " + urgencyFlag,
                    lineNumber, rawRow
                );
            }

            String statuteCode = columns[7].trim();
            if (statuteCode.isEmpty()) {
                throw new InvalidDatasetException("Statute code is empty.", lineNumber, rawRow);
            }

            double disputeValueINR = parseNonNegativeDouble(columns[8].trim(), "dispute_value_inr",
                                                              lineNumber, rawRow);

            String[] citedCaseIds = parseCitedCaseIds(columns[9].trim());

            // ─── Factory Dispatch ─────────────────────────────────
            Case newCase;
            switch (caseType) {
                case CRIMINAL:
                    newCase = new CriminalCase(caseId, title, filingYear, caseAgeDays,
                                                custodyDays, urgencyFlag, statuteCode, citedCaseIds);
                    break;
                case CIVIL:
                    newCase = new CivilCase(caseId, title, filingYear, caseAgeDays,
                                             urgencyFlag, statuteCode, disputeValueINR, citedCaseIds);
                    break;
                case WRIT:
                    newCase = new WritCase(caseId, title, filingYear, caseAgeDays,
                                            custodyDays, urgencyFlag, statuteCode, citedCaseIds);
                    break;
                default:
                    throw new InvalidDatasetException(
                        "Unhandled case type: " + caseType, lineNumber, rawRow
                    );
            }

            return newCase;

        } catch (InvalidDatasetException e) {
            throw e;  // re-throw our own exceptions as-is
        } catch (Exception e) {
            throw new InvalidDatasetException(
                "Unexpected error parsing row: " + e.getMessage(), lineNumber, rawRow, e
            );
        }
    }

    // ─── Private Helper Methods ───────────────────────────────────

    /**
     * Parses a string as a positive integer (> 0).
     */
    private static int parsePositiveInt(String value, String fieldName,
                                         int lineNumber, String rawRow)
            throws InvalidDatasetException {
        int result = parseInt(value, fieldName, lineNumber, rawRow);
        if (result <= 0) {
            throw new InvalidDatasetException(
                fieldName + " must be positive, found: " + result, lineNumber, rawRow
            );
        }
        return result;
    }

    /**
     * Parses a string as a non-negative integer (≥ 0).
     */
    private static int parseNonNegativeInt(String value, String fieldName,
                                            int lineNumber, String rawRow)
            throws InvalidDatasetException {
        int result = parseInt(value, fieldName, lineNumber, rawRow);
        if (result < 0) {
            throw new InvalidDatasetException(
                fieldName + " must be non-negative, found: " + result, lineNumber, rawRow
            );
        }
        return result;
    }

    /**
     * Parses a string as an integer.
     */
    private static int parseInt(String value, String fieldName,
                                 int lineNumber, String rawRow)
            throws InvalidDatasetException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidDatasetException(
                "Invalid integer for " + fieldName + ": '" + value + "'", lineNumber, rawRow
            );
        }
    }

    /**
     * Parses a string as a non-negative double (≥ 0.0).
     */
    private static double parseNonNegativeDouble(String value, String fieldName,
                                                   int lineNumber, String rawRow)
            throws InvalidDatasetException {
        try {
            double result = Double.parseDouble(value);
            if (result < 0) {
                throw new InvalidDatasetException(
                    fieldName + " must be non-negative, found: " + result, lineNumber, rawRow
                );
            }
            return result;
        } catch (NumberFormatException e) {
            throw new InvalidDatasetException(
                "Invalid decimal for " + fieldName + ": '" + value + "'", lineNumber, rawRow
            );
        }
    }

    /**
     * Parses the cited_case_ids field, splitting on '|' delimiter.
     * Returns an empty array if the value is "NONE" or empty.
     */
    private static String[] parseCitedCaseIds(String value) {
        if (value == null || value.isEmpty() || value.equalsIgnoreCase("NONE")) {
            return new String[0];
        }
        return value.split("\\|");
    }
}
