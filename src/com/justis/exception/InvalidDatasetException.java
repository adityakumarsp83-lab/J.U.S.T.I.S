package com.justis.exception;

/**
 * Checked exception thrown when the data ingestion pipeline encounters
 * malformed, missing, or invalid data in a CSV dataset row.
 *
 * <p>Typical causes include:
 * <ul>
 *   <li>Incorrect column count in a CSV row</li>
 *   <li>Non-numeric values in numeric fields (case_age_days, custody_days, etc.)</li>
 *   <li>Negative values where only non-negative values are valid</li>
 *   <li>Unknown case types or statute codes</li>
 *   <li>Missing required fields (null or empty case_id, title, etc.)</li>
 * </ul>
 *
 * <p><b>OOP Concepts:</b> Custom checked exception extending {@code Exception},
 * additional context fields (line number, raw row data), overridden {@code toString()}.
 *
 * @author J.U.S.T.I.S. Team
 */
public class InvalidDatasetException extends Exception {

    private final int lineNumber;
    private final String rawRow;

    /**
     * Constructs an {@code InvalidDatasetException} with a descriptive message.
     *
     * @param message a human-readable description of the data error
     */
    public InvalidDatasetException(String message) {
        super(message);
        this.lineNumber = -1;
        this.rawRow = null;
    }

    /**
     * Constructs an {@code InvalidDatasetException} with full context.
     *
     * @param message    a human-readable description of the data error
     * @param lineNumber the 1-indexed line number in the CSV file where the error occurred
     * @param rawRow     the raw text of the offending CSV row
     */
    public InvalidDatasetException(String message, int lineNumber, String rawRow) {
        super(message);
        this.lineNumber = lineNumber;
        this.rawRow = rawRow;
    }

    /**
     * Constructs an {@code InvalidDatasetException} wrapping a root cause.
     *
     * @param message    a human-readable description of the data error
     * @param lineNumber the 1-indexed line number in the CSV file
     * @param rawRow     the raw text of the offending CSV row
     * @param cause      the underlying exception that triggered this error
     */
    public InvalidDatasetException(String message, int lineNumber, String rawRow, Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
        this.rawRow = rawRow;
    }

    /** @return the 1-indexed line number where the error occurred, or -1 if unknown */
    public int getLineNumber() {
        return lineNumber;
    }

    /** @return the raw CSV row text that caused the error, or null if unavailable */
    public String getRawRow() {
        return rawRow;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("InvalidDatasetException: ");
        sb.append(getMessage());
        if (lineNumber > 0) {
            sb.append(" [Line ").append(lineNumber).append("]");
        }
        if (rawRow != null) {
            sb.append("\n  Raw Data: ").append(rawRow);
        }
        return sb.toString();
    }
}
