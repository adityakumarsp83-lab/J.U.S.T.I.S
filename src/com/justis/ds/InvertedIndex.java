package com.justis.ds;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom Inverted Index implemented using a Hash Table with Separate Chaining.
 *
 * <p>FODS Concept:
 * Maps tokenized keywords from legal case titles/summaries to posting lists (Case IDs).
 * Demonstrates:
 * <ul>
 *   <li>Custom separate chaining hash table (dynamic array of singly linked bucket nodes)</li>
 *   <li>Load factor monitoring with automatic rehashing (doubling capacity at > 0.75)</li>
 *   <li>O(1) average lookup per keyword</li>
 *   <li>Posting list intersection (searchAnd) in O(L1 + L2) time</li>
 * </ul>
 */
public class InvertedIndex implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int INITIAL_CAPACITY = 64;
    private static final double LOAD_FACTOR_THRESHOLD = 0.75;
    private static final int MIN_KEYWORD_LENGTH = 3;

    /**
     * Linked list node for separate chaining collision resolution.
     */
    private static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;

        final String keyword;
        final List<Integer> postingList; // Sorted list of case IDs
        Entry next;

        Entry(String keyword, int initialCaseId) {
            this.keyword = keyword;
            this.postingList = new ArrayList<>();
            this.postingList.add(initialCaseId);
            this.next = null;
        }

        void addCaseId(int caseId) {
            if (!postingList.contains(caseId)) {
                postingList.add(caseId);
                Collections.sort(postingList);
            }
        }
    }

    private Entry[] buckets;
    private int entryCount;
    private int capacity;

    public InvertedIndex() {
        this(INITIAL_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public InvertedIndex(int initialCapacity) {
        this.capacity = Math.max(16, initialCapacity);
        this.buckets = new Entry[this.capacity];
        this.entryCount = 0;
    }

    /**
     * Hashes keyword using polynomial rolling hash and bounds it to table capacity.
     */
    private int hash(String key) {
        int h = key.hashCode();
        h ^= (h >>> 16);
        return Math.abs(h % capacity);
    }

    /**
     * Tokenizes text and indexes each token for the given caseId.
     * Tokens are lowercased, stripped of non-word characters, and filtered for min length.
     *
     * @param caseId The ID of the case
     * @param text The text (e.g. title, summary) to index
     */
    public void index(int caseId, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        String[] tokens = text.toLowerCase().split("\\W+");
        for (String token : tokens) {
            token = token.trim();
            if (token.length() >= MIN_KEYWORD_LENGTH) {
                insertToken(token, caseId);
            }
        }
    }

    /**
     * Inserts a single token and case ID into the hash table.
     */
    private synchronized void insertToken(String keyword, int caseId) {
        if ((double) entryCount / capacity >= LOAD_FACTOR_THRESHOLD) {
            rehash();
        }

        int index = hash(keyword);
        Entry current = buckets[index];

        while (current != null) {
            if (current.keyword.equals(keyword)) {
                current.addCaseId(caseId);
                return;
            }
            current = current.next;
        }

        // Keyword not found in bucket; prepend new entry
        Entry newEntry = new Entry(keyword, caseId);
        newEntry.next = buckets[index];
        buckets[index] = newEntry;
        entryCount++;
    }

    /**
     * Searches for a single keyword and returns the posting list of matching case IDs.
     *
     * @param keyword The search term
     * @return List of matching case IDs, or empty list if no matches
     */
    public List<Integer> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = keyword.trim().toLowerCase();
        int index = hash(normalized);
        Entry current = buckets[index];

        while (current != null) {
            if (current.keyword.equals(normalized)) {
                return new ArrayList<>(current.postingList);
            }
            current = current.next;
        }

        return Collections.emptyList();
    }

    /**
     * Performs conjunctive boolean search (AND query) between two keywords.
     * Implements linear posting list intersection in O(|L1| + |L2|) time.
     *
     * @param keyword1 First search term
     * @param keyword2 Second search term
     * @return List of case IDs containing both keywords
     */
    public List<Integer> searchAnd(String keyword1, String keyword2) {
        List<Integer> list1 = search(keyword1);
        List<Integer> list2 = search(keyword2);

        if (list1.isEmpty() || list2.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> intersection = new ArrayList<>();
        int i = 0;
        int j = 0;

        while (i < list1.size() && j < list2.size()) {
            int id1 = list1.get(i);
            int id2 = list2.get(j);

            if (id1 == id2) {
                intersection.add(id1);
                i++;
                j++;
            } else if (id1 < id2) {
                i++;
            } else {
                j++;
            }
        }

        return intersection;
    }

    /**
     * Performs multi-word query search: parses all tokens and intersects their posting lists.
     *
     * @param query Multi-word search query
     * @return List of case IDs matching all non-trivial tokens
     */
    public List<Integer> searchPhrase(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] tokens = query.toLowerCase().split("\\W+");
        List<String> validTokens = new ArrayList<>();
        for (String t : tokens) {
            if (t.trim().length() >= MIN_KEYWORD_LENGTH) {
                validTokens.add(t.trim());
            }
        }

        if (validTokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> result = search(validTokens.get(0));
        for (int i = 1; i < validTokens.size(); i++) {
            if (result.isEmpty()) break;
            List<Integer> next = search(validTokens.get(i));
            result = intersect(result, next);
        }

        return result;
    }

    private List<Integer> intersect(List<Integer> l1, List<Integer> l2) {
        List<Integer> out = new ArrayList<>();
        int i = 0, j = 0;
        while (i < l1.size() && j < l2.size()) {
            int id1 = l1.get(i);
            int id2 = l2.get(j);
            if (id1 == id2) {
                out.add(id1);
                i++;
                j++;
            } else if (id1 < id2) {
                i++;
            } else {
                j++;
            }
        }
        return out;
    }

    /**
     * Resizes the internal hash table when load factor exceeds the threshold.
     * Doubles capacity and redistributes all entries into new buckets.
     */
    @SuppressWarnings("unchecked")
    private void rehash() {
        int oldCapacity = this.capacity;
        Entry[] oldBuckets = this.buckets;

        this.capacity = oldCapacity * 2;
        this.buckets = new Entry[this.capacity];
        this.entryCount = 0;

        for (int b = 0; b < oldCapacity; b++) {
            Entry current = oldBuckets[b];
            while (current != null) {
                for (int caseId : current.postingList) {
                    insertToken(current.keyword, caseId);
                }
                current = current.next;
            }
        }
    }

    public int getEntryCount() {
        return entryCount;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getLoadFactor() {
        return (double) entryCount / capacity;
    }
}
