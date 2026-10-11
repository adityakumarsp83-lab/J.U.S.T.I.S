package com.justis.model;

/**
 * Concrete subclass representing a constitutional writ petition.
 *
 * <p>Writ petitions are filed under Articles 226 (High Courts) and 32 (Supreme Court)
 * of the Constitution of India. They include:
 * <ul>
 *   <li><b>Habeas Corpus</b> — challenging unlawful detention</li>
 *   <li><b>Mandamus</b> — compelling a public authority to perform a duty</li>
 *   <li><b>Certiorari</b> — quashing orders of inferior courts/tribunals</li>
 *   <li><b>Prohibition</b> — preventing a court from exceeding jurisdiction</li>
 *   <li><b>Quo Warranto</b> — challenging the authority of a public office holder</li>
 * </ul>
 *
 * <p><b>Priority Scoring Formula:</b>
 * <pre>
 *   P_writ = (5.0 × caseAgeDays) + (15.0 × urgencyFlag) + WRIT_CEILING_BONUS
 * </pre>
 * Where {@code WRIT_CEILING_BONUS = 500.0} ensures that constitutional writs
 * are always near the top of the court's daily cause list. This reflects
 * the constitutional mandate that fundamental rights petitions receive
 * expedited hearing.
 *
 * <p><b>OOP Concepts:</b> Inheritance, method overriding (polymorphism),
 * constructor chaining, use of class-level constants.
 *
 * <p><b>Judicial Context:</b> The Supreme Court in <i>Hussainara Khatoon v. State of Bihar</i>
 * (1979) established that the right to a speedy trial is a fundamental right under
 * Article 21. Habeas corpus petitions must therefore receive highest scheduling priority.
 *
 * @author J.U.S.T.I.S. Team
 */
public class WritCase extends Case {

    private static final long serialVersionUID = 1L;

    // ─── Scoring Weights ──────────────────────────────────────────
    private static final double WEIGHT_CASE_AGE   = 5.0;
    private static final double WEIGHT_URGENCY    = 15.0;
    private static final double WRIT_CEILING_BONUS = 500.0;

    // ─── Writ-Specific Field ──────────────────────────────────────
    private final int custodyDays;  // for Habeas Corpus cases where petitioner is detained

    /**
     * Constructs a new {@code WritCase}.
     *
     * @param caseId       unique case identifier
     * @param title        case title / style
     * @param filingYear   year of filing
     * @param caseAgeDays  total days since filing
     * @param custodyDays  days of detention (relevant for Habeas Corpus; 0 for other writs)
     * @param urgencyFlag  urgency rating (1–5)
     * @param statuteCode  primary constitutional article (e.g., "CONST_226", "CONST_32")
     * @param citedCaseIds array of cited case IDs
     */
    public WritCase(int caseId, String title, int filingYear,
                     int caseAgeDays, int custodyDays, int urgencyFlag,
                     String statuteCode, String[] citedCaseIds) {
        super(caseId, CaseType.WRIT, title, filingYear, caseAgeDays,
              urgencyFlag, statuteCode, citedCaseIds);
        this.custodyDays = custodyDays;
    }

    /**
     * Calculates the priority score for this writ petition.
     *
     * <p>Formula: {@code P = (5.0 × caseAgeDays) + (15.0 × urgencyFlag) + 500.0}
     *
     * <p>The ceiling bonus ensures writs always outrank routine civil/criminal matters
     * of comparable age.
     *
     * @return the calculated priority score
     */
    @Override
    public double calculatePriorityScore() {
        return (WEIGHT_CASE_AGE * getCaseAgeDays())
             + (WEIGHT_URGENCY * getUrgencyFlag())
             + WRIT_CEILING_BONUS;
    }

    /** @return the number of days of detention (for Habeas Corpus) */
    public int getCustodyDays() {
        return custodyDays;
    }

    @Override
    public String toString() {
        String extra = custodyDays > 0 ? " | Detention: " + custodyDays + " days" : "";
        return super.toString() + extra;
    }
}
