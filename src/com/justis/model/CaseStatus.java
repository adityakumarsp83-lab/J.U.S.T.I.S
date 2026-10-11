package com.justis.model;

/**
 * Enum representing the lifecycle status of a legal case.
 *
 * <p>Tracks the current procedural stage of a case through the judicial system:
 * <ul>
 *   <li>{@code PENDING} — Case is active and awaiting hearing or judgment</li>
 *   <li>{@code RESERVED_FOR_JUDGMENT} — Arguments concluded; judgment pending delivery</li>
 *   <li>{@code DISPOSED} — Case has been finally adjudicated (allowed or dismissed)</li>
 *   <li>{@code TRANSFERRED} — Case transferred to another court or bench</li>
 * </ul>
 *
 * <p>All cases default to {@code PENDING} upon ingestion from CSV datasets.
 *
 * <p><b>OOP Concepts:</b> Enum with descriptive labels, clean encapsulation
 * of lifecycle states.
 *
 * @author J.U.S.T.I.S. Team
 */
public enum CaseStatus {

    PENDING("Pending Adjudication"),
    RESERVED_FOR_JUDGMENT("Reserved for Judgment"),
    DISPOSED("Disposed / Final Order Passed"),
    TRANSFERRED("Transferred to Another Court");

    private final String displayLabel;

    /**
     * Constructs a {@code CaseStatus} with a human-readable display label.
     *
     * @param displayLabel the user-facing description of this status
     */
    CaseStatus(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    /**
     * Returns the human-readable display label for this status.
     *
     * @return the display label string
     */
    public String getDisplayLabel() {
        return displayLabel;
    }

    @Override
    public String toString() {
        return displayLabel;
    }
}
