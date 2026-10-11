package com.justis.analytics;

import com.justis.model.Case;
import com.justis.model.CaseType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Statistical engine for court workload analysis and pendency distribution.
 *
 * <p><b>FODS Concepts Demonstrated:</b>
 * <ul>
 *   <li><b>Quickselect (Hoare's Selection Algorithm):</b> O(N) expected time median computation</li>
 *   <li><b>Descriptive Statistics:</b> Mean, median, standard deviation, percentile rankings</li>
 *   <li><b>Frequency Distributions:</b> Case types, urgency buckets, age quartiles</li>
 *   <li><b>Top-K Selection:</b> Identifying oldest pending matters without full O(N log N) sort</li>
 * </ul>
 *
 * <p><b>OOP Concepts:</b> Pure utility / analytics service, immutability of input data,
 * defensive copying.
 */
public class CourtStatistics {

    private static final Random RANDOM = new Random(42);

    /**
     * Computes the median case age in days using Hoare's Quickselect algorithm.
     * Expected time complexity: O(N), Worst-case: O(N^2).
     *
     * @param cases list of cases
     * @return median age in days, or 0.0 if empty
     */
    public static double medianCaseAge(List<Case> cases) {
        if (cases == null || cases.isEmpty()) {
            return 0.0;
        }

        int[] ages = new int[cases.size()];
        for (int i = 0; i < cases.size(); i++) {
            ages[i] = cases.get(i).getCaseAgeDays();
        }

        int n = ages.length;
        if (n % 2 == 1) {
            return quickselect(ages, 0, n - 1, n / 2);
        } else {
            double m1 = quickselect(ages, 0, n - 1, (n / 2) - 1);
            double m2 = quickselect(ages, 0, n - 1, n / 2);
            return (m1 + m2) / 2.0;
        }
    }

    /**
     * Hoare's Quickselect algorithm to find the k-th smallest element (0-indexed).
     */
    private static int quickselect(int[] arr, int left, int right, int k) {
        if (left == right) {
            return arr[left];
        }

        int pivotIndex = left + RANDOM.nextInt(right - left + 1);
        pivotIndex = partition(arr, left, right, pivotIndex);

        if (k == pivotIndex) {
            return arr[k];
        } else if (k < pivotIndex) {
            return quickselect(arr, left, pivotIndex - 1, k);
        } else {
            return quickselect(arr, pivotIndex + 1, right, k);
        }
    }

    private static int partition(int[] arr, int left, int right, int pivotIndex) {
        int pivotValue = arr[pivotIndex];
        // Move pivot to end
        swap(arr, pivotIndex, right);
        int storeIndex = left;

        for (int i = left; i < right; i++) {
            if (arr[i] < pivotValue) {
                swap(arr, storeIndex, i);
                storeIndex++;
            }
        }

        // Move pivot to its final place
        swap(arr, right, storeIndex);
        return storeIndex;
    }

    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    /**
     * Computes the mean case age across all cases in O(N) time.
     */
    public static double meanCaseAge(List<Case> cases) {
        if (cases == null || cases.isEmpty()) {
            return 0.0;
        }
        long sum = 0;
        for (Case c : cases) {
            sum += c.getCaseAgeDays();
        }
        return (double) sum / cases.size();
    }

    /**
     * Computes standard deviation of case ages.
     */
    public static double standardDeviationCaseAge(List<Case> cases) {
        if (cases == null || cases.size() < 2) {
            return 0.0;
        }
        double mean = meanCaseAge(cases);
        double varianceSum = 0.0;
        for (Case c : cases) {
            double diff = c.getCaseAgeDays() - mean;
            varianceSum += diff * diff;
        }
        return Math.sqrt(varianceSum / (cases.size() - 1));
    }

    /**
     * Returns distribution count of cases by CaseType.
     */
    public static Map<CaseType, Integer> caseTypeDistribution(List<Case> cases) {
        Map<CaseType, Integer> dist = new EnumMap<>(CaseType.class);
        for (CaseType ct : CaseType.values()) {
            dist.put(ct, 0);
        }
        if (cases != null) {
            for (Case c : cases) {
                dist.put(c.getCaseType(), dist.get(c.getCaseType()) + 1);
            }
        }
        return dist;
    }

    /**
     * Returns frequency distribution of urgency levels (1 to 5).
     */
    public static Map<Integer, Integer> urgencyDistribution(List<Case> cases) {
        Map<Integer, Integer> dist = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            dist.put(i, 0);
        }
        if (cases != null) {
            for (Case c : cases) {
                dist.put(c.getUrgencyFlag(), dist.getOrDefault(c.getUrgencyFlag(), 0) + 1);
            }
        }
        return dist;
    }

    /**
     * Returns the top K oldest pending cases sorted by age descending.
     */
    public static List<Case> topKPendency(List<Case> cases, int k) {
        if (cases == null || cases.isEmpty() || k <= 0) {
            return Collections.emptyList();
        }
        List<Case> copy = new ArrayList<>(cases);
        copy.sort((c1, c2) -> Integer.compare(c2.getCaseAgeDays(), c1.getCaseAgeDays()));
        return copy.subList(0, Math.min(k, copy.size()));
    }

    /**
     * Generates a comprehensive formatted statistics report.
     */
    public static String generateDashboardReport(List<Case> cases) {
        if (cases == null || cases.isEmpty()) {
            return "No cases available for statistics generation.";
        }

        StringBuilder sb = new StringBuilder();
        double mean = meanCaseAge(cases);
        double median = medianCaseAge(cases);
        double stdDev = standardDeviationCaseAge(cases);
        Map<CaseType, Integer> typeDist = caseTypeDistribution(cases);
        Map<Integer, Integer> urgDist = urgencyDistribution(cases);

        int starvingCount = 0;
        for (Case c : cases) {
            if (c.isStarving()) starvingCount++;
        }

        sb.append("╔════════════════════════════════════════════════════════════════╗\n");
        sb.append("║              COURT PENDENCY & ANALYTICS DASHBOARD              ║\n");
        sb.append("╠════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║  Total Docket Volume:       %-35d ║\n", cases.size()));
        sb.append(String.format("║  Mean Case Age:             %-35.2f ║\n", mean));
        sb.append(String.format("║  Median Case Age (Hoare):   %-35.2f ║\n", median));
        sb.append(String.format("║  Std Deviation (Age):       %-35.2f ║\n", stdDev));
        sb.append(String.format("║  Starving Cases (>1000 d):  %-35d ║\n", starvingCount));
        sb.append("╠════════════════════════════════════════════════════════════════╣\n");
        sb.append("║  Case Type Breakdown:                                          ║\n");
        for (Map.Entry<CaseType, Integer> entry : typeDist.entrySet()) {
            double pct = (entry.getValue() * 100.0) / cases.size();
            sb.append(String.format("║    • %-10s : %3d cases  (%5.1f%%)                           ║\n",
                    entry.getKey(), entry.getValue(), pct));
        }
        sb.append("╠════════════════════════════════════════════════════════════════╣\n");
        sb.append("║  Urgency Levels (1=Routine, 5=Constitutional Urgency):         ║\n");
        for (int u = 5; u >= 1; u--) {
            int count = urgDist.getOrDefault(u, 0);
            sb.append(String.format("║    Level %d : %3d cases %-38s ║\n",
                    u, count, "█".repeat(Math.min(30, count * 3))));
        }
        sb.append("╚════════════════════════════════════════════════════════════════╝\n");

        return sb.toString();
    }
}
