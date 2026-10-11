package com.justis.model;

import com.justis.exception.InvalidDatasetException;

/**
 * Enum representing the classification of a legal case in the Indian judicial system.
 * 
 * <p>Each case type corresponds to a distinct jurisdictional category:
 * <ul>
 *   <li>{@code CRIMINAL} — Offences under BNS, BNSS, BSA, and special criminal statutes</li>
 *   <li>{@code CIVIL} — Property disputes, contracts, torts, and commercial litigation</li>
 *   <li>{@code WRIT} — Constitutional petitions under Articles 226 and 32 (Habeas Corpus, Mandamus, etc.)</li>
 * </ul>
 *
 * <p><b>OOP Concepts:</b> Enum with static factory method, encapsulation of parsing logic,
 * custom exception integration.
 *
 * @author J.U.S.T.I.S. Team
 */
public enum CaseType {

    CRIMINAL,
    CIVIL,
    WRIT;

    /**
     * Parses a raw string from the CSV dataset into a {@code CaseType} enum constant.
     * Performs case-insensitive matching after trimming whitespace.
     *
     * @param raw the raw string value from the CSV (e.g., "CRIMINAL", "civil", " Writ ")
     * @return the matching {@code CaseType} enum constant
     * @throws InvalidDatasetException if the string does not match any known case type
     */
    public static CaseType fromString(String raw) throws InvalidDatasetException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidDatasetException("Case type is null or empty.");
        }
        String normalized = raw.trim().toUpperCase();
        try {
            return CaseType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new InvalidDatasetException(
                "Unknown case type: '" + raw.trim() + "'. Expected one of: CRIMINAL, CIVIL, WRIT."
            );
        }
    }
}
