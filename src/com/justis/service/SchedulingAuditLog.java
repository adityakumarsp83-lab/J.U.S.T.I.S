package com.justis.service;

import com.justis.model.Case;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Transparent judicial scheduling audit trail recorder.
 *
 * <p>Judicial scheduling algorithms must remain accountable, auditable,
 * and explainable to eliminate bias or arbitrary prioritization.
 *
 * <p>Appends scheduling decisions to {@code data/audit_log.csv}.
 *
 * <p><b>OOP Concept:</b> Single Responsibility Principle (SRP), resource management (I/O).
 */
public class SchedulingAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String logFilePath;

    public SchedulingAuditLog(String logFilePath) {
        this.logFilePath = logFilePath;
        initializeFile();
    }

    private void initializeFile() {
        File file = new File(logFilePath);
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                writer.write("timestamp,case_id,case_type,tier,priority_score,reasoning");
                writer.newLine();
            } catch (IOException e) {
                System.err.println("[AuditLog] Warning: Unable to initialize audit log file: " + e.getMessage());
            }
        }
    }

    /**
     * Records a single scheduling allocation decision.
     *
     * @param c Case that was scheduled
     * @param tier Scheduling tier (e.g., TIER_1_URGENT_HEAP, TIER_2_BACKLOG_FIFO, TIER_3_FRESH)
     * @param score Calculated priority score
     * @param reasoning Plain text explanation for why the case was placed in this slot
     */
    public synchronized void record(Case c, String tier, double score, String reasoning) {
        String ts = LocalDateTime.now().format(FORMATTER);
        String cleanReasoning = reasoning.replace(",", ";").replace("\"", "'");
        String line = String.format("%s,%d,%s,%s,%.2f,\"%s\"",
                ts, c.getCaseId(), c.getCaseType(), tier, score, cleanReasoning);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[AuditLog] Warning: Failed to write to audit log: " + e.getMessage());
        }
    }

    public String getLogFilePath() {
        return logFilePath;
    }
}
