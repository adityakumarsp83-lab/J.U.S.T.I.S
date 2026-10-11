package com.justis.service;

import com.justis.model.Case;
import com.justis.model.CitationEdge;
import com.justis.service.DataWrangler.StatuteRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Checkpoint persistence manager using pure Java object serialization.
 *
 * <p>Persists the complete state of the judicial scheduling engine (cases, citation edges,
 * and statutes) to a binary checkpoint file ({@code data/checkpoint.dat}).
 *
 * <p><b>OOP Concept:</b> Serialization, checkpointing, crash-resilience.
 */
public class CheckpointManager {

    public static class CheckpointData implements Serializable {
        private static final long serialVersionUID = 1L;

        private final List<Case> cases;
        private final List<CitationEdge> edges;
        private final List<StatuteRecord> statutes;

        public CheckpointData(List<Case> cases, List<CitationEdge> edges, List<StatuteRecord> statutes) {
            this.cases = new ArrayList<>(cases);
            this.edges = new ArrayList<>(edges);
            this.statutes = new ArrayList<>(statutes);
        }

        public List<Case> getCases()           { return cases; }
        public List<CitationEdge> getEdges()   { return edges; }
        public List<StatuteRecord> getStatutes(){ return statutes; }
    }

    /**
     * Saves checkpoint data to disk.
     */
    public static void save(SchedulerService service, String filePath) throws IOException {
        CheckpointData data = new CheckpointData(
                service.getAllCases(),
                service.getAllEdges(),
                service.getAllStatutes()
        );

        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(data);
        }
    }

    /**
     * Loads checkpoint data from disk and constructs a new SchedulerService.
     */
    public static SchedulerService load(String filePath) throws IOException, ClassNotFoundException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Checkpoint file not found: " + filePath);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            CheckpointData data = (CheckpointData) ois.readObject();
            return new SchedulerService(data.getCases(), data.getEdges(), data.getStatutes());
        }
    }
}
