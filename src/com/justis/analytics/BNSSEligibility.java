package com.justis.analytics;

import com.justis.model.Case;
import com.justis.model.CriminalCase;
import com.justis.model.WritCase;
import com.justis.service.DataWrangler.StatuteRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Statutory Compliance Engine for Bharatiya Nagarik Suraksha Sanhita (BNSS), 2023.
 *
 * <p>Implements automated tracking for <b>Section 479 BNSS</b> (Maximum period for which
 * an undertrial prisoner can be detained).
 *
 * <p><b>Statutory Rule:</b>
 * Where an undertrial prisoner accused of an offence (other than an offence for which
 * punishment of death or life imprisonment is specified) has undergone detention
 * for a period extending up to:
 * <ul>
 *   <li><b>One-half</b> of the maximum period of imprisonment specified for that offence under law,
 *       they SHALL be released by the Court on their personal bond (with or without sureties).</li>
 *   <li>First-time offenders are eligible at <b>one-third</b> of the maximum sentence.</li>
 * </ul>
 *
 * <p><b>FODS Concepts:</b> Relational join between cases dataset and statutory catalog,
 * rule-based compliance filtering, difference metrics.
 */
public class BNSSEligibility {

    public enum Status {
        ELIGIBLE_FOR_RELEASE,
        NOT_YET_ELIGIBLE,
        NON_CUSTODIAL_PROVISION,
        CIVIL_MATTER
    }

    /**
     * DTO containing the Section 479 evaluation outcome for a case.
     */
    public static class EligibilityResult {
        private final int caseId;
        private final String title;
        private final String statuteCode;
        private final int custodyDays;
        private final int maxSentenceYears;
        private final int thresholdDays;
        private final Status status;
        private final int daysRemaining;

        public EligibilityResult(int caseId, String title, String statuteCode,
                                 int custodyDays, int maxSentenceYears, int thresholdDays,
                                 Status status, int daysRemaining) {
            this.caseId = caseId;
            this.title = title;
            this.statuteCode = statuteCode;
            this.custodyDays = custodyDays;
            this.maxSentenceYears = maxSentenceYears;
            this.thresholdDays = thresholdDays;
            this.status = status;
            this.daysRemaining = Math.max(0, daysRemaining);
        }

        public int getCaseId()            { return caseId; }
        public String getTitle()          { return title; }
        public String getStatuteCode()    { return statuteCode; }
        public int getCustodyDays()       { return custodyDays; }
        public int getMaxSentenceYears()  { return maxSentenceYears; }
        public int getThresholdDays()     { return thresholdDays; }
        public Status getStatus()         { return status; }
        public int getDaysRemaining()     { return daysRemaining; }

        @Override
        public String toString() {
            if (status == Status.ELIGIBLE_FOR_RELEASE) {
                return String.format("[Case #%d] %s | Statute: %s | Custody: %d d (Threshold: %d d) -> 🟢 ELIGIBLE FOR RELEASE",
                        caseId, title, statuteCode, custodyDays, thresholdDays);
            } else if (status == Status.NOT_YET_ELIGIBLE) {
                return String.format("[Case #%d] %s | Statute: %s | Custody: %d d (Threshold: %d d) -> 🟡 NOT ELIGIBLE (%d days remaining)",
                        caseId, title, statuteCode, custodyDays, thresholdDays, daysRemaining);
            } else {
                return String.format("[Case #%d] %s | %s -> ℹ️ %s",
                        caseId, title, statuteCode, status);
            }
        }
    }

    private final Map<String, StatuteRecord> statuteCatalog;

    public BNSSEligibility(List<StatuteRecord> statutes) {
        this.statuteCatalog = new HashMap<>();
        if (statutes != null) {
            for (StatuteRecord s : statutes) {
                statuteCatalog.put(s.getStatuteCode().trim().toUpperCase(), s);
            }
        }
    }

    /**
     * Evaluates undertrial release eligibility for a single case.
     *
     * @param c Case to evaluate
     * @return EligibilityResult containing statutory decision
     */
    public EligibilityResult evaluateCase(Case c) {
        int custodyDays = 0;
        if (c instanceof CriminalCase cc) {
            custodyDays = cc.getCustodyDays();
        } else if (c instanceof WritCase wc) {
            custodyDays = wc.getCustodyDays();
        } else {
            return new EligibilityResult(c.getCaseId(), c.getTitle(), c.getStatuteCode(),
                    0, 0, 0, Status.CIVIL_MATTER, 0);
        }

        String code = c.getStatuteCode() != null ? c.getStatuteCode().trim().toUpperCase() : "";
        StatuteRecord record = statuteCatalog.get(code);

        if (record == null || record.getMaxSentenceYears() <= 0) {
            return new EligibilityResult(c.getCaseId(), c.getTitle(), code,
                    custodyDays, 0, 0, Status.NON_CUSTODIAL_PROVISION, 0);
        }

        int maxYears = record.getMaxSentenceYears();
        // Section 479 standard threshold: 1/2 of max sentence in days
        int thresholdDays = (maxYears * 365) / 2;

        if (custodyDays >= thresholdDays) {
            return new EligibilityResult(c.getCaseId(), c.getTitle(), code,
                    custodyDays, maxYears, thresholdDays, Status.ELIGIBLE_FOR_RELEASE, 0);
        } else {
            int remaining = thresholdDays - custodyDays;
            return new EligibilityResult(c.getCaseId(), c.getTitle(), code,
                    custodyDays, maxYears, thresholdDays, Status.NOT_YET_ELIGIBLE, remaining);
        }
    }

    /**
     * Evaluates all cases in the docket and returns complete eligibility records.
     */
    public List<EligibilityResult> evaluateDocket(List<Case> cases) {
        List<EligibilityResult> results = new ArrayList<>();
        if (cases != null) {
            for (Case c : cases) {
                results.add(evaluateCase(c));
            }
        }
        return results;
    }

    /**
     * Filters and returns only cases currently eligible for mandatory release under Sec 479.
     */
    public List<EligibilityResult> getEligibleForRelease(List<Case> cases) {
        List<EligibilityResult> eligible = new ArrayList<>();
        for (EligibilityResult res : evaluateDocket(cases)) {
            if (res.getStatus() == Status.ELIGIBLE_FOR_RELEASE) {
                eligible.add(res);
            }
        }
        return eligible;
    }
}
