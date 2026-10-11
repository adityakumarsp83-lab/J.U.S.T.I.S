package com.justis.ds;

import java.util.ArrayList;
import java.util.List;

/**
 * Trie (Prefix Tree) for O(K) statutory code search and autocomplete.
 *
 * <p>Provides instant lookup and prefix-based autocomplete for statutory codes
 * like "BNS_103", "BNSS_479", "CONST_226". Query time is O(K) where K is the
 * length of the query string, completely independent of total database volume N.
 *
 * <p><b>Internal Representation:</b> Each node contains an array of 128 child
 * pointers (ASCII range) and a boolean terminal flag. When a code is fully inserted,
 * the terminal node stores the full statutory code string.
 *
 * <p><b>Complexity:</b>
 * <ul>
 *   <li>insert:       O(K)</li>
 *   <li>search:       O(K)</li>
 *   <li>autocomplete: O(K + M) where M = total characters in all matching suffixes</li>
 * </ul>
 *
 * <p><b>OOP Concepts:</b> Information hiding via private inner class {@code TrieNode},
 * encapsulation, recursive DFS collector.
 *
 * <p><b>FODS Concepts:</b> Trie data structure, prefix matching, DFS subtree traversal.
 *
 * @author J.U.S.T.I.S. Team
 */
public class Trie {

    /**
     * Internal node representation. Private to enforce information hiding.
     * Each node can have up to 128 children (ASCII character set).
     */
    private static class TrieNode {
        TrieNode[] children = new TrieNode[128];
        boolean isEndOfWord = false;
        String fullCode = null;  // stored at terminal nodes
    }

    private final TrieNode root;
    private int size;

    /**
     * Constructs an empty Trie.
     */
    public Trie() {
        this.root = new TrieNode();
        this.size = 0;
    }

    // ─── Public API ───────────────────────────────────────────────

    /**
     * Inserts a statutory code into the Trie. O(K).
     *
     * @param code the statutory code to insert (e.g., "BNS_103")
     * @throws IllegalArgumentException if the code is null or empty
     */
    public void insert(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot insert null or empty code.");
        }
        String normalized = code.trim().toUpperCase();
        TrieNode current = root;

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= 128) continue;  // skip non-ASCII characters
            if (current.children[c] == null) {
                current.children[c] = new TrieNode();
            }
            current = current.children[c];
        }

        if (!current.isEndOfWord) {
            current.isEndOfWord = true;
            current.fullCode = normalized;
            size++;
        }
    }

    /**
     * Searches for an exact statutory code in the Trie. O(K).
     *
     * @param code the statutory code to search for
     * @return true if the exact code exists in the Trie
     */
    public boolean search(String code) {
        if (code == null || code.trim().isEmpty()) return false;
        String normalized = code.trim().toUpperCase();
        TrieNode node = findNode(normalized);
        return node != null && node.isEndOfWord;
    }

    /**
     * Returns all statutory codes that match the given prefix. O(K + M).
     *
     * <p>After matching the prefix characters, performs a DFS over all
     * downstream branches to collect terminal nodes.
     *
     * @param prefix the prefix to match (e.g., "BNS" matches "BNS_103", "BNS_111", etc.)
     * @return list of all matching statutory codes, or empty list if no matches
     */
    public List<String> autocomplete(String prefix) {
        List<String> results = new ArrayList<>();
        if (prefix == null || prefix.trim().isEmpty()) return results;

        String normalized = prefix.trim().toUpperCase();
        TrieNode node = findNode(normalized);

        if (node != null) {
            collectAll(node, results);
        }
        return results;
    }

    /**
     * Returns the total number of statutory codes stored in the Trie.
     *
     * @return the number of inserted codes
     */
    public int size() {
        return size;
    }

    /**
     * Returns true if the Trie contains no codes.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    // ─── Private Helpers ──────────────────────────────────────────

    /**
     * Traverses the Trie following the given string, returning the
     * final node if the path exists, or null if any character is missing.
     */
    private TrieNode findNode(String s) {
        TrieNode current = root;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 128 || current.children[c] == null) {
                return null;
            }
            current = current.children[c];
        }
        return current;
    }

    /**
     * DFS collector: traverses all children of the given node and
     * adds terminal nodes' fullCode to the results list.
     */
    private void collectAll(TrieNode node, List<String> results) {
        if (node == null) return;

        if (node.isEndOfWord && node.fullCode != null) {
            results.add(node.fullCode);
        }

        for (int i = 0; i < 128; i++) {
            if (node.children[i] != null) {
                collectAll(node.children[i], results);
            }
        }
    }
}
