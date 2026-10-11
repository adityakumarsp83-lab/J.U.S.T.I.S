package com.justis.service;

import java.util.List;

/**
 * Interface defining statutory and keyword search capabilities.
 *
 * <p><b>OOP Concept:</b> Interface-driven design, polymorphism, Open-Closed Principle (OCP).
 */
public interface SearchEngine {

    /**
     * Executes a keyword query and returns matching case IDs.
     *
     * @param query Search query string
     * @return List of matching case IDs
     */
    List<Integer> search(String query);
}
