package com.justis.model;

/**
 * Concrete subclass representing a civil case in the Indian judicial system.
 *
 * <p>Civil cases encompass property disputes, contractual claims, torts,
 * and commercial litigation. They carry an additional {@code disputeValueINR}
 * field representing the monetary value at stake.
 *
 * <p><b>Priority Scoring Formula:</b>
 * <pre>
 *   P_civil = (1.5 × caseAgeDays) + (disputeValueINR / 100,000)
 * </pre>
 * Civil cases are weighted primarily by pendency age (right to speedy trial)
 * and secondarily by dispute monetary value (proportionate judicial attention).
 *
 * <p><b>OOP Concepts:</b> Inheritance, method overriding (polymorphism),
 * constructor chaining via {@code super()}, encapsulation of civil-specific state.
 *
 * @author J.U.S.T.I.S. Team
 */
public class CivilCase extends Case {

    private static final long serialVersionUID = 1L;

    // ─── Scoring Weights ──────────────────────────────────────────
    private static final double WEIGHT_CASE_AGE    = 1.5;
    private static final double DISPUTE_DIVISOR    = 100_000.0;

    // ─── Civil-Specific Field ─────────────────────────────────────
    private final double disputeValueINR;

    /**
     * Constructs a new {@code CivilCase}.
     *
     * @param caseId          unique case identifier
     * @param title           case title / style
     * @param filingYear      year of filing
     * @param caseAgeDays     total days since filing
     * @param urgencyFlag     urgency rating (1–5)
     * @param statuteCode     primary statutory provision code
     * @param disputeValueINR monetary value of the dispute in Indian Rupees
     * @param citedCaseIds    array of cited case IDs
     */
    public CivilCase(int caseId, String title, int filingYear,
                      int caseAgeDays, int urgencyFlag, String statuteCode,
                      double disputeValueINR, String[] citedCaseIds) {
        super(caseId, CaseType.CIVIL, title, filingYear, caseAgeDays,
              urgencyFlag, statuteCode, citedCaseIds);
        this.disputeValueINR = disputeValueINR;
    }

    /**
     * Calculates the priority score for this civil case.
     *
     * <p>Formula: {@code P = (1.5 × caseAgeDays) + (disputeValueINR / 100,000)}
     *
     * @return the calculated priority score
     */
    @Override
    public double calculatePriorityScore() {
        return (WEIGHT_CASE_AGE * getCaseAgeDays())
             + (disputeValueINR / DISPUTE_DIVISOR);
    }

    /** @return the monetary value of the dispute in INR */
    public double getDisputeValueINR() {
        return disputeValueINR;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" | Dispute: ₹%.2f", disputeValueINR);
    }
}
