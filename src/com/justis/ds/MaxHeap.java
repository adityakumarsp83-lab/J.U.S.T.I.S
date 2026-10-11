package com.justis.ds;

import com.justis.model.Case;

/**
 * Custom Binary Max-Heap implementation backed by a raw array.
 *
 * <p>This heap provides the core scheduling engine for J.U.S.T.I.S.,
 * maintaining cases in priority order so that the highest-priority case
 * (urgent bail, high custody duration) can be extracted in O(log N) time.
 *
 * <p><b>Underlying Structure:</b> 1D array where for every node at index i:
 * <pre>
 *   Parent(i) = floor((i - 1) / 2)
 *   Left(i)   = 2i + 1
 *   Right(i)  = 2i + 2
 * </pre>
 *
 * <p><b>Complexity:</b>
 * <ul>
 *   <li>insert:     O(log N) — sift up</li>
 *   <li>extractMax: O(log N) — sift down</li>
 *   <li>peekMax:    O(1)</li>
 *   <li>buildHeap:  O(N) — Floyd's bottom-up algorithm</li>
 * </ul>
 *
 * <p><b>OOP Concepts:</b> Encapsulation (private array, private sift methods),
 * polymorphic comparison via {@code Case.compareTo()}.
 *
 * <p><b>FODS Concepts:</b> Array-based heap, sift-up/sift-down, Floyd's build-heap.
 *
 * @author J.U.S.T.I.S. Team
 */
public class MaxHeap {

    private static final int DEFAULT_CAPACITY = 64;

    private Case[] heap;
    private int size;

    /**
     * Constructs an empty MaxHeap with default initial capacity (64).
     */
    public MaxHeap() {
        this.heap = new Case[DEFAULT_CAPACITY];
        this.size = 0;
    }

    /**
     * Constructs a MaxHeap from an existing array of cases using Floyd's
     * bottom-up build-heap algorithm in O(N) time.
     *
     * @param cases the array of cases to heapify
     */
    public MaxHeap(Case[] cases) {
        this.size = cases.length;
        this.heap = new Case[Math.max(size * 2, DEFAULT_CAPACITY)];
        System.arraycopy(cases, 0, this.heap, 0, size);
        buildHeap();
    }

    // ─── Public API ───────────────────────────────────────────────

    /**
     * Inserts a case into the heap. O(log N).
     *
     * @param c the case to insert
     * @throws IllegalArgumentException if the case is null
     */
    public void insert(Case c) {
        if (c == null) {
            throw new IllegalArgumentException("Cannot insert null case into heap.");
        }
        ensureCapacity();
        heap[size] = c;
        siftUp(size);
        size++;
    }

    /**
     * Removes and returns the highest-priority case from the heap. O(log N).
     *
     * @return the case with the highest priority score, or null if empty
     */
    public Case extractMax() {
        if (size == 0) return null;

        Case max = heap[0];
        size--;
        heap[0] = heap[size];
        heap[size] = null;  // help GC

        if (size > 0) {
            siftDown(0);
        }
        return max;
    }

    /**
     * Returns the highest-priority case without removing it. O(1).
     *
     * @return the case with the highest priority score, or null if empty
     */
    public Case peekMax() {
        return size == 0 ? null : heap[0];
    }

    /**
     * Returns the number of cases in the heap.
     *
     * @return the current size
     */
    public int size() {
        return size;
    }

    /**
     * Returns true if the heap contains no cases.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns all cases in the heap as an array (not necessarily sorted).
     * Useful for iteration without extraction.
     *
     * @return array of cases currently in the heap
     */
    public Case[] toArray() {
        Case[] result = new Case[size];
        System.arraycopy(heap, 0, result, 0, size);
        return result;
    }

    // ─── Floyd's Build-Heap — O(N) ───────────────────────────────

    /**
     * Constructs a valid max-heap from an unordered array using Floyd's
     * bottom-up algorithm. Runs siftDown from the last internal node
     * (index floor(N/2) - 1) down to index 0.
     *
     * <p>Time complexity: O(N) — provably linear via the geometric series
     * sum of sift distances at each level.
     */
        public void buildHeap(java.util.List<Case> cases) {
        if (cases == null || cases.isEmpty()) {
            this.size = 0;
            return;
        }
        this.size = cases.size();
        this.heap = new Case[Math.max(size * 2, DEFAULT_CAPACITY)];
        for (int i = 0; i < size; i++) {
            this.heap[i] = cases.get(i);
        }
        buildHeap();
    }

    public void buildHeap() {
        for (int i = (size / 2) - 1; i >= 0; i--) {
            siftDown(i);
        }
    }


    // ─── Sift Operations ──────────────────────────────────────────

    /**
     * Sift-Up: Restores heap property after insertion.
     * Swaps the node at index i upward with its parent while
     * the child has higher priority than the parent.
     *
     * @param i the index of the newly inserted node
     */
    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            // compareTo returns negative if heap[i] has HIGHER priority
            if (heap[i].compareTo(heap[parent]) < 0) {
                swap(i, parent);
                i = parent;
            } else {
                break;
            }
        }
    }

    /**
     * Sift-Down: Restores heap property after extraction.
     * Swaps the node at index i downward with the higher-priority child
     * while the parent has lower priority than either child.
     *
     * @param i the index to sift down from
     */
    private void siftDown(int i) {
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int highest = i;

            // Compare with left child
            if (left < size && heap[left].compareTo(heap[highest]) < 0) {
                highest = left;
            }
            // Compare with right child
            if (right < size && heap[right].compareTo(heap[highest]) < 0) {
                highest = right;
            }

            if (highest != i) {
                swap(i, highest);
                i = highest;
            } else {
                break;
            }
        }
    }

    // ─── Utility ──────────────────────────────────────────────────

    private void swap(int i, int j) {
        Case temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    private void ensureCapacity() {
        if (size >= heap.length) {
            Case[] newHeap = new Case[heap.length * 2];
            System.arraycopy(heap, 0, newHeap, 0, heap.length);
            heap = newHeap;
        }
    }
}
