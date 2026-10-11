package com.justis.exception;

/**
 * Unchecked exception thrown when a case lookup by ID fails to find
 * any matching case in the system's data structures.
 *
 * <p>This is an unchecked (runtime) exception because a missing case ID
 * typically indicates a programming error or corrupt data reference,
 * not a recoverable condition.
 *
 * <p><b>OOP Concepts:</b> Custom unchecked exception extending {@code RuntimeException},
 * encapsulated case ID field, overridden {@code toString()}.
 *
 * @author J.U.S.T.I.S. Team
 */
public class CaseNotFoundException extends RuntimeException {

    private final int caseId;

    /**
     * Constructs a {@code CaseNotFoundException} for the given case ID.
     *
     * @param caseId the case ID that was not found
     */
    public CaseNotFoundException(int caseId) {
        super("Case not found with ID: " + caseId);
        this.caseId = caseId;
    }

    /**
     * Constructs a {@code CaseNotFoundException} with a custom message.
     *
     * @param message a descriptive error message
     * @param caseId  the case ID that was not found
     */
    public CaseNotFoundException(String message, int caseId) {
        super(message);
        this.caseId = caseId;
    }

    /** @return the case ID that could not be located */
    public int getCaseId() {
        return caseId;
    }

    @Override
    public String toString() {
        return "CaseNotFoundException: " + getMessage() + " (caseId=" + caseId + ")";
    }
}
