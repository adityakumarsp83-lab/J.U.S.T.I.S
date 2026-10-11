package com.justis.model;

/**
 * Concrete subclass representing a criminal case in the Indian judicial system.
 *
 * <p>Criminal cases involve offences under the Bharatiya Nyaya Sanhita (BNS),
 * Bharatiya Nagarik Suraksha Sanhita (BNSS), and other penal statutes.
 * They carry an additional {@code custodyDays} field tracking undertrial detention.
 *
 * <p><b>Priority Scoring Formula:</b>
 * <pre>
 *   P_criminal = (3.0 × custodyDays) + (2.0 × caseAgeDays) + (10.0 × urgencyFlag)
 * </pre>
 * This formula heavily weights custody duration (reflecting the fundamental right
 * to liberty under Article 21) and urgency flags (for bail/habeas corpus matters).
 *
 * <p><b>OOP Concepts:</b> Inheritance, method overriding (polymorphism),
 * constructor chaining via {@code super()}, encapsulation of custody-specific state.
 *
 * @author J.U.S.T.I.S. Team
 */
public class CriminalCase extends Case {

    private static final long serialVersionUID = 1L;

    // ─── Scoring Weights ──────────────────────────────────────────
    private static final double WEIGHT_CUSTODY   = 3.0;
    private static final double WEIGHT_CASE_AGE  = 2.0;
    private static final double WEIGHT_URGENCY   = 10.0;

    // ─── Criminal-Specific Field ──────────────────────────────────
    private final int custodyDays;

    /**
     * Constructs a new {@code CriminalCase}.
     *
     * @param caseId       unique case identifier
     * @param title        case title / style
     * @param filingYear   year of filing
     * @param caseAgeDays  total days since filing
     * @param custodyDays  days the accused has been in custody (undertrial detention)
     * @param urgencyFlag  urgency rating (1–5)
     * @param statuteCode  primary statutory provision code
     * @param citedCaseIds array of cited case IDs
     */
    public CriminalCase(int caseId, String title, int filingYear,
                         int caseAgeDays, int custodyDays, int urgencyFlag,
                         String statuteCode, String[] citedCaseIds) {
        super(caseId, CaseType.CRIMINAL, title, filingYear, caseAgeDays,
              urgencyFlag, statuteCode, citedCaseIds);
        this.custodyDays = custodyDays;
    }

    /**
     * Calculates the priority score for this criminal case.
     *
     * <p>Formula: {@code P = (3.0 × custodyDays) + (2.0 × caseAgeDays) + (10.0 × urgencyFlag)}
     *
     * @return the calculated priority score
     */
    @Override
    public double calculatePriorityScore() {
        return (WEIGHT_CUSTODY * custodyDays)
             + (WEIGHT_CASE_AGE * getCaseAgeDays())
             + (WEIGHT_URGENCY * getUrgencyFlag());
    }

    /** @return the number of days the accused has spent in custody */
    public int getCustodyDays() {
        return custodyDays;
    }

    @Override
    public String toString() {
        return super.toString() + " | Custody: " + custodyDays + " days";
    }
}
