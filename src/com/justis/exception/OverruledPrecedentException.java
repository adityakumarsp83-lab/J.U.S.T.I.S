package com.justis.exception;

/**
 * Checked exception thrown when the Citation DAG's DFS overrule traversal
 * detects that a case cites a precedent that has been overruled.
 *
 * <p>This exception is thrown during precedent validation and carries:
 * <ul>
 *   <li>The case ID that made the problematic citation</li>
 *   <li>The overruled precedent's case ID</li>
 *   <li>The full citation chain leading from the citing case to the overruled precedent</li>
 * </ul>
 *
 * <p><b>OOP Concepts:</b> Custom checked exception, encapsulated context fields,
 * rich error reporting via {@code toString()}.
 *
 * <p><b>Judicial Context:</b> Courts must not rely on overruled precedents.
 * This exception serves as an automated safeguard against inadvertent citation
 * of invalidated legal authority.
 *
 * @author J.U.S.T.I.S. Team
 */
public class OverruledPrecedentException extends Exception {

    private final int citingCaseId;
    private final int overruledCaseId;
    private final String citationChain;

    /**
     * Constructs an {@code OverruledPrecedentException} with full citation context.
     *
     * @param citingCaseId    the ID of the case that made the problematic citation
     * @param overruledCaseId the ID of the precedent that has been overruled
     * @param citationChain   a string representation of the citation path
     *                        (e.g., "102 -> 101 [OVERRULED by 109]")
     */
    public OverruledPrecedentException(int citingCaseId, int overruledCaseId, String citationChain) {
        super("Case " + citingCaseId + " cites overruled precedent " + overruledCaseId
              + ". Chain: " + citationChain);
        this.citingCaseId = citingCaseId;
        this.overruledCaseId = overruledCaseId;
        this.citationChain = citationChain;
    }

    /** @return the case ID that made the problematic citation */
    public int getCitingCaseId() {
        return citingCaseId;
    }

    /** @return the case ID of the overruled precedent */
    public int getOverruledCaseId() {
        return overruledCaseId;
    }

    /** @return the full citation chain from citing case to overruled precedent */
    public String getCitationChain() {
        return citationChain;
    }

    @Override
    public String toString() {
        return "OverruledPrecedentException: " + getMessage();
    }
}
