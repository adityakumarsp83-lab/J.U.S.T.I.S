package com.justis.ds;

import com.justis.model.CitationEdge;
import com.justis.model.CitationEdge.RelationType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Directed Acyclic Graph (DAG) representing the citation network between legal cases.
 *
 * <p>Each node is a case ID, and each directed edge represents a citation relationship
 * (CITES, RELIES_ON, or OVERRULES). The graph enforces the temporal acyclicity
 * property of legal citations: a case can only cite cases decided before it.
 *
 * <p><b>Key Operations:</b>
 * <ul>
 *   <li>{@link #checkOverruled(int)} — DFS traversal to detect if any cited precedent
 *       has been overruled, returning graduated severity alerts</li>
 *   <li>{@link #hasCycle()} — Three-color DFS cycle detection for data integrity</li>
 *   <li>{@link #getInDegree(int)} — Returns the number of cases citing a given case</li>
 * </ul>
 *
 * <p><b>Complexity:</b> DFS traversals run in O(V + E).
 *
 * <p><b>OOP Concepts:</b> Encapsulated adjacency lists, inner result classes,
 * custom exception integration.
 *
 * <p><b>FODS Concepts:</b> Graph representation (adjacency lists), DFS traversal,
 * three-color cycle detection, topological properties.
 *
 * @author J.U.S.T.I.S. Team
 */
public class CitationGraph {

    /**
     * Severity levels for citation health alerts.
     */
    public enum Severity {
        /** The cited case itself has been directly overruled. */
        CRITICAL,
        /** The cited case RELIES_ON a case that has been overruled. */
        WARNING,
        /** The cited case CITES a case that has been overruled (informational). */
        INFO
    }

    /**
     * Represents a single alert from the citation health check.
     */
    public static class CitationAlert {
        private final int citingCaseId;
        private final int problematicCaseId;
        private final Severity severity;
        private final String chain;

        public CitationAlert(int citingCaseId, int problematicCaseId,
                              Severity severity, String chain) {
            this.citingCaseId = citingCaseId;
            this.problematicCaseId = problematicCaseId;
            this.severity = severity;
            this.chain = chain;
        }

        public int getCitingCaseId()       { return citingCaseId; }
        public int getProblematicCaseId()   { return problematicCaseId; }
        public Severity getSeverity()       { return severity; }
        public String getChain()            { return chain; }

        @Override
        public String toString() {
            String icon = severity == Severity.CRITICAL ? "❌" :
                          severity == Severity.WARNING  ? "⚠️" : "ℹ️";
            return String.format("%s [%s] Case %d → %s", icon, severity,
                                  citingCaseId, chain);
        }
    }

    // ─── Graph Storage ────────────────────────────────────────────
    /** Outgoing edges: source → list of edges from that source */
    private final Map<Integer, List<CitationEdge>> adjacencyList;

    /** Reverse index: target → list of edges pointing TO that target */
    private final Map<Integer, List<CitationEdge>> reverseIndex;

    /** Set of all node IDs in the graph */
    private final Set<Integer> nodes;

    /** Set of case IDs that have been directly overruled */
    private final Set<Integer> overruledCases;

    /**
     * Constructs an empty citation graph.
     */
    public CitationGraph() {
        this.adjacencyList = new HashMap<>();
        this.reverseIndex = new HashMap<>();
        this.nodes = new HashSet<>();
        this.overruledCases = new HashSet<>();
    }

    // ─── Graph Construction ───────────────────────────────────────

    /**
     * Adds a citation edge to the graph.
     *
     * @param edge the citation edge to add
     */
    public void addEdge(CitationEdge edge) {
        int source = edge.getSourceCaseId();
        int target = edge.getTargetCaseId();

        nodes.add(source);
        nodes.add(target);

        // Outgoing adjacency list
        adjacencyList.computeIfAbsent(source, k -> new ArrayList<>()).add(edge);

        // Reverse index (for in-degree computation)
        reverseIndex.computeIfAbsent(target, k -> new ArrayList<>()).add(edge);

        // Track overruled cases
        if (edge.getRelationType() == RelationType.OVERRULES) {
            overruledCases.add(target);
        }
    }

    /**
     * Loads a list of citation edges into the graph.
     *
     * @param edges list of citation edges from the CSV
     */
    public void loadEdges(List<CitationEdge> edges) {
        for (CitationEdge edge : edges) {
            addEdge(edge);
        }
    }

    // ─── Overrule Detection — DFS ─────────────────────────────────

    /**
     * Checks the citation health of a given case by performing DFS
     * through its cited precedents.
     *
     * <p>Returns a list of alerts with graduated severity:
     * <ul>
     *   <li>CRITICAL: A directly cited case has been overruled</li>
     *   <li>WARNING: A cited case RELIES_ON an overruled case</li>
     *   <li>INFO: A cited case CITES an overruled case</li>
     * </ul>
     *
     * @param caseId the case ID to check
     * @return list of citation alerts (empty if all citations are healthy)
     */
    public List<CitationAlert> checkOverruled(int caseId) {
        List<CitationAlert> alerts = new ArrayList<>();
        List<CitationEdge> outgoing = adjacencyList.get(caseId);

        if (outgoing == null || outgoing.isEmpty()) {
            return alerts;  // no citations to check
        }

        for (CitationEdge edge : outgoing) {
            int targetId = edge.getTargetCaseId();
            Set<Integer> visited = new HashSet<>();
            dfsOverruleCheck(caseId, targetId, edge.getRelationType(),
                              String.valueOf(caseId), visited, alerts);
        }

        return alerts;
    }

    /**
     * Recursive DFS to find overruled cases in the citation chain.
     */
    private void dfsOverruleCheck(int originalCaseId, int currentId,
                                    RelationType incomingRelation, String path,
                                    Set<Integer> visited, List<CitationAlert> alerts) {
        if (visited.contains(currentId)) return;
        visited.add(currentId);

        String currentPath = path + " --[" + incomingRelation + "]--> " + currentId;

        // Check if this case has been overruled
        if (overruledCases.contains(currentId)) {
            // Find which case overruled it
            List<CitationEdge> incoming = reverseIndex.get(currentId);
            String overruler = "";
            if (incoming != null) {
                for (CitationEdge e : incoming) {
                    if (e.getRelationType() == RelationType.OVERRULES) {
                        overruler = " [OVERRULED by Case " + e.getSourceCaseId() + "]";
                        break;
                    }
                }
            }

            // Determine severity based on how this case was reached
            Severity severity;
            if (incomingRelation == RelationType.RELIES_ON) {
                severity = Severity.WARNING;
            } else if (incomingRelation == RelationType.CITES) {
                severity = Severity.INFO;
            } else {
                severity = Severity.CRITICAL;
            }

            // Direct citation from the original case is always CRITICAL
            if (path.equals(String.valueOf(originalCaseId))) {
                severity = Severity.CRITICAL;
            }

            alerts.add(new CitationAlert(originalCaseId, currentId,
                                          severity, currentPath + overruler));
        }

        // Continue DFS through outgoing edges of currentId
        List<CitationEdge> outgoing = adjacencyList.get(currentId);
        if (outgoing != null) {
            for (CitationEdge edge : outgoing) {
                dfsOverruleCheck(originalCaseId, edge.getTargetCaseId(),
                                  edge.getRelationType(), currentPath, visited, alerts);
            }
        }
    }

    // ─── Cycle Detection — Three-Color DFS ────────────────────────

    private static final int WHITE = 0;  // unvisited
    private static final int GRAY  = 1;  // in current recursion stack
    private static final int BLACK = 2;  // fully explored

    /**
     * Detects cycles in the citation graph using three-color DFS.
     *
     * <p>In a valid legal citation graph, cycles should never exist because
     * citations are temporally ordered. This method validates data integrity.
     *
     * @return true if a cycle is detected (data integrity violation), false if acyclic
     */
    public boolean hasCycle() {
        Map<Integer, Integer> color = new HashMap<>();
        for (int node : nodes) {
            color.put(node, WHITE);
        }

        for (int node : nodes) {
            if (color.get(node) == WHITE) {
                if (dfsCycleDetect(node, color)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfsCycleDetect(int node, Map<Integer, Integer> color) {
        color.put(node, GRAY);

        List<CitationEdge> outgoing = adjacencyList.get(node);
        if (outgoing != null) {
            for (CitationEdge edge : outgoing) {
                int neighbor = edge.getTargetCaseId();
                Integer neighborColor = color.get(neighbor);
                if (neighborColor == null) continue;

                if (neighborColor == GRAY) {
                    return true;  // back edge = cycle
                }
                if (neighborColor == WHITE && dfsCycleDetect(neighbor, color)) {
                    return true;
                }
            }
        }

        color.put(node, BLACK);
        return false;
    }

    // ─── Utility Methods ──────────────────────────────────────────

    /**
     * Returns the in-degree of a case (how many other cases cite it).
     *
     * @param caseId the case ID to query
     * @return the number of incoming citation edges
     */
    public int getInDegree(int caseId) {
        List<CitationEdge> incoming = reverseIndex.get(caseId);
        return incoming == null ? 0 : incoming.size();
    }

    /**
     * Returns the set of case IDs that have been overruled.
     *
     * @return set of overruled case IDs
     */
    public Set<Integer> getOverruledCases() {
        return new HashSet<>(overruledCases);
    }

    /**
     * Returns the total number of nodes in the graph.
     *
     * @return node count
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * Returns the total number of edges in the graph.
     *
     * @return edge count
     */
    public int getEdgeCount() {
        int count = 0;
        for (List<CitationEdge> edges : adjacencyList.values()) {
            count += edges.size();
        }
        return count;
    }

    /**
     * Checks if a specific case has been overruled.
     *
     * @param caseId the case ID to check
     * @return true if the case has been overruled
     */
    public boolean isOverruled(int caseId) {
        return overruledCases.contains(caseId);
    }
}
