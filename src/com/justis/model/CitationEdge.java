package com.justis.model;

/**
 * Represents a directed edge in the Citation DAG (Directed Acyclic Graph).
 *
 * <p>Each edge connects a source case to a target case with a specific
 * legal relationship type. The three supported relation types are:
 * <ul>
 *   <li>{@code CITES} — The source case references the target as persuasive authority</li>
 *   <li>{@code RELIES_ON} — The source case builds its core legal argument upon the target</li>
 *   <li>{@code OVERRULES} — The source case explicitly reverses or invalidates the target's holding</li>
 * </ul>
 *
 * <p><b>OOP Concepts:</b> Encapsulation (private fields, public getters), inner enum,
 * immutable value object, defensive constructor validation.
 *
 * <p><b>FODS Concepts:</b> Graph edge representation for adjacency list storage.
 *
 * @author J.U.S.T.I.S. Team
 */
public class CitationEdge implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Enum representing the legal relationship type between two cases.
     */
    public enum RelationType {
        /** Persuasive reference to a prior judgment. */
        CITES,
        /** Core legal reliance on a prior judgment's ratio decidendi. */
        RELIES_ON,
        /** Explicit reversal or invalidation of a prior judgment. */
        OVERRULES;

        /**
         * Parses a raw string into a {@code RelationType} enum constant.
         *
         * @param raw the raw string from the CSV (e.g., "CITES", "RELIES_ON", "OVERRULES")
         * @return the matching {@code RelationType}
         * @throws IllegalArgumentException if the string does not match any known relation type
         */
        public static RelationType fromString(String raw) {
            if (raw == null || raw.trim().isEmpty()) {
                throw new IllegalArgumentException("Relation type is null or empty.");
            }
            return RelationType.valueOf(raw.trim().toUpperCase());
        }
    }

    private final int sourceCaseId;
    private final int targetCaseId;
    private final RelationType relationType;

    /**
     * Constructs a new citation edge.
     *
     * @param sourceCaseId  the ID of the citing case (the case that references another)
     * @param targetCaseId  the ID of the cited case (the precedent being referenced)
     * @param relationType  the nature of the citation relationship
     * @throws IllegalArgumentException if either case ID is non-positive or relationType is null
     */
    public CitationEdge(int sourceCaseId, int targetCaseId, RelationType relationType) {
        if (sourceCaseId <= 0) {
            throw new IllegalArgumentException("Source case ID must be positive: " + sourceCaseId);
        }
        if (targetCaseId <= 0) {
            throw new IllegalArgumentException("Target case ID must be positive: " + targetCaseId);
        }
        if (relationType == null) {
            throw new IllegalArgumentException("Relation type cannot be null.");
        }
        this.sourceCaseId = sourceCaseId;
        this.targetCaseId = targetCaseId;
        this.relationType = relationType;
    }

    /** @return the case ID of the citing (source) case */
    public int getSourceCaseId() {
        return sourceCaseId;
    }

    /** @return the case ID of the cited (target) precedent */
    public int getTargetCaseId() {
        return targetCaseId;
    }

    /** @return the legal relationship type of this citation */
    public RelationType getRelationType() {
        return relationType;
    }

    @Override
    public String toString() {
        return "CitationEdge{" + sourceCaseId + " --[" + relationType + "]--> " + targetCaseId + "}";
    }
}
