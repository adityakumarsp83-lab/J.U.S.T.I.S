package com.justis.model;

import java.io.Serializable;

/**
 * Abstract base class representing a legal case in the Indian judicial system.
 *
 * <p>This class serves as the root of the case domain hierarchy:
 * <pre>
 *              Case (abstract)
 *             /      |       \
 *    CriminalCase  CivilCase  WritCase
 * </pre>
 *
 * <p>Each concrete subclass overrides {@link #calculatePriorityScore()} to implement
 * its own domain-specific scoring formula, enabling <b>runtime polymorphism</b>
 * when the {@code MaxHeap} compares cases via {@link #compareTo(Case)}.
 *
 * <p><b>OOP Concepts Demonstrated:</b>
 * <ul>
 *   <li><b>Abstraction:</b> Abstract class with abstract method</li>
 *   <li><b>Encapsulation:</b> Private fields with public getters/setters</li>
 *   <li><b>Polymorphism:</b> {@code calculatePriorityScore()} dispatched at runtime</li>
 *   <li><b>Comparable Interface:</b> Natural ordering by priority score</li>
 *   <li><b>Serializable:</b> Enables checkpoint persistence</li>
 * </ul>
 *
 * <p><b>FODS Concepts:</b> Vectorized feature representation (priority score),
 * comparator-driven heap ordering.
 *
 * @author J.U.S.T.I.S. Team
 */
public abstract class Case implements Comparable<Case>, Serializable {

    private static final long serialVersionUID = 1L;

    /** Starvation threshold in days — cases older than this are flagged as starving (Fix 2). */
    public static final int STARVATION_THRESHOLD_DAYS = 1000;

    // ─── Core Fields ──────────────────────────────────────────────
    private final int caseId;
    private CaseType caseType;
    private String title;
    private int filingYear;
    private int caseAgeDays;
    private int urgencyFlag;          // 1 (lowest) to 5 (highest)
    private String statuteCode;
    private String[] citedCaseIds;    // raw IDs from CSV, split on '|'

    // ─── Lifecycle & Starvation ───────────────────────────────────
    private CaseStatus status;
    private boolean isStarving;

    /**
     * Constructs a new {@code Case} with the given core attributes.
     *
     * @param caseId       unique integer identifier for this case
     * @param caseType     the classification of this case (CRIMINAL, CIVIL, WRIT)
     * @param title        the short title / style of the case (e.g., "State of MH v. Rahul Sharma")
     * @param filingYear   the year this case was originally filed
     * @param caseAgeDays  the total number of days since filing
     * @param urgencyFlag  urgency rating from 1 (routine) to 5 (constitutionally urgent)
     * @param statuteCode  the primary statutory provision under which this case is registered
     * @param citedCaseIds array of case IDs cited by this case (may be empty or contain "NONE")
     */
    protected Case(int caseId, CaseType caseType, String title, int filingYear,
                    int caseAgeDays, int urgencyFlag, String statuteCode, String[] citedCaseIds) {
        this.caseId = caseId;
        this.caseType = caseType;
        this.title = title;
        this.filingYear = filingYear;
        this.caseAgeDays = caseAgeDays;
        this.urgencyFlag = urgencyFlag;
        this.statuteCode = statuteCode;
        this.citedCaseIds = citedCaseIds;
        this.status = CaseStatus.PENDING;  // default on ingestion
        refreshStarvationStatus();
    }

    // ─── Abstract Method (Polymorphism) ───────────────────────────

    /**
     * Calculates the priority score for this case based on its domain-specific attributes.
     *
     * <p>Each subclass implements its own scoring formula:
     * <ul>
     *   <li>{@code CriminalCase}: weighted sum of custody days, case age, and urgency</li>
     *   <li>{@code CivilCase}: weighted sum of case age and dispute monetary value</li>
     *   <li>{@code WritCase}: highest urgency ceiling for constitutional writs</li>
     * </ul>
     *
     * @return a non-negative priority score (higher = more urgent)
     */
    public abstract double calculatePriorityScore();

    // ─── Comparable Implementation ────────────────────────────────

    /**
     * Compares this case with another for priority ordering (descending).
     *
     * <p>Primary comparison: higher priority score wins.<br>
     * Tiebreaker 1: starving cases win over non-starving.<br>
     * Tiebreaker 2: older cases win over newer ones.
     *
     * @param other the other case to compare against
     * @return negative if this case has higher priority, positive if lower, zero if equal
     */
    @Override
    public int compareTo(Case other) {
        // Primary: compare priority scores (higher score = higher priority)
        int cmp = Double.compare(other.calculatePriorityScore(), this.calculatePriorityScore());
        if (cmp != 0) {
            return cmp;
        }

        // Tiebreaker 1: starving cases get preference
        if (this.isStarving && !other.isStarving) {
            return -1;  // this case wins
        }
        if (!this.isStarving && other.isStarving) {
            return 1;   // other case wins
        }

        // Tiebreaker 2: older case gets preference
        return Integer.compare(other.caseAgeDays, this.caseAgeDays);
    }

    // ─── Starvation Logic (Fix 2 Circuit Breaker) ─────────────────

    /**
     * Refreshes the starvation flag based on the current case age.
     * A case is considered "starving" if its age exceeds {@value #STARVATION_THRESHOLD_DAYS} days.
     */
    public void refreshStarvationStatus() {
        this.isStarving = this.caseAgeDays > STARVATION_THRESHOLD_DAYS;
    }

    // ─── Getters ──────────────────────────────────────────────────

    /** @return the unique integer identifier for this case */
    public int getCaseId() {
        return caseId;
    }

    /** @return the classification type (CRIMINAL, CIVIL, WRIT) */
    public CaseType getCaseType() {
        return caseType;
    }

    /** @return the short title / style of the case */
    public String getTitle() {
        return title;
    }

    /** @return the year this case was originally filed */
    public int getFilingYear() {
        return filingYear;
    }

    /** @return total days since filing */
    public int getCaseAgeDays() {
        return caseAgeDays;
    }

    /** @return urgency rating (1–5) */
    public int getUrgencyFlag() {
        return urgencyFlag;
    }

    /** @return the primary statutory provision code */
    public String getStatuteCode() {
        return statuteCode;
    }

    /** @return array of case IDs cited by this case */
    public String[] getCitedCaseIds() {
        return citedCaseIds;
    }

    /** @return the current lifecycle status of this case */
    public CaseStatus getStatus() {
        return status;
    }

    /** @return {@code true} if this case has been pending beyond the starvation threshold */
    public boolean isStarving() {
        return isStarving;
    }

    // ─── Setters ──────────────────────────────────────────────────

    /**
     * Updates the lifecycle status of this case.
     *
     * @param status the new status
     */
    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    /**
     * Updates the case age and refreshes the starvation flag.
     *
     * @param caseAgeDays the updated age in days
     */
    public void setCaseAgeDays(int caseAgeDays) {
        this.caseAgeDays = caseAgeDays;
        refreshStarvationStatus();
    }

    // ─── Display ──────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("[%s] Case #%d | %s | Age: %d days | Urgency: %d | Score: %.2f | %s%s",
                caseType, caseId, title, caseAgeDays, urgencyFlag,
                calculatePriorityScore(), status,
                isStarving ? " [STARVING]" : "");
    }
}
