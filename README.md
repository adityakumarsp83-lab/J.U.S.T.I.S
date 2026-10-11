<div align="center">

# ⚖️ J.U.S.T.I.S.

### **Judicial Unified Search, Triaging & Intelligent Scheduling**

A high-performance, algorithmic legal-technology platform engineered in **pure Java (JDK 17+)** — designed to eliminate procedural delays, prevent citation of overruled precedents, and automate court cause-list generation in the Indian judicial system.

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Platform](https://img.shields.io/badge/Platform-macOS%20ARM64-000000?style=for-the-badge&logo=apple&logoColor=white)](https://www.apple.com/macos/)
[![License](https://img.shields.io/badge/License-Academic-blue?style=for-the-badge)](LICENSE)

</div>

---

## 🎯 Problem Statement

The Indian judicial system faces a structural processing crisis:

| Bottleneck | Impact |
|------------|--------|
| **~5 Crore Pending Cases** | Manual FCFS docketing buries urgent bail hearings |
| **Undertrial Crisis (~70%)** | Inmates detained beyond statutory limits without automated tracking |
| **Precedent Cognitive Overload** | Courts inadvertently rely on overruled judgments |
| **Information Retrieval Latency** | Unindexed legal text corpora cause O(N×L) search times |

## 🏗️ Architecture Overview

```
                    ┌─────────────────────────┐
                    │      Main.java (CLI)     │
                    │    Interactive Terminal   │
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │   SchedulerService.java  │
                    │     (Facade Pattern)     │
                    └────────────┬────────────┘
                                 │
          ┌──────────┬───────────┼───────────┬──────────┐
          ▼          ▼           ▼           ▼          ▼
     ┌─────────┐ ┌────────┐ ┌───────┐ ┌──────────┐ ┌────────┐
     │ MaxHeap │ │Citation│ │ Trie  │ │ Inverted │ │Court   │
     │ (Cause  │ │  Graph │ │(Stat  │ │  Index   │ │Stats   │
     │  List)  │ │ (DAG)  │ │Search)│ │(Keyword) │ │Engine  │
     └─────────┘ └────────┘ └───────┘ └──────────┘ └────────┘
```

## 📋 Dual Academic Track

This project simultaneously demonstrates concepts from two academic subjects:

### OOPS (Object-Oriented Programming Systems)
- Abstract class hierarchy with polymorphic priority scoring
- Factory Pattern, Facade Pattern, Singleton Pattern
- Custom checked & unchecked exceptions
- Interface-driven design (`Comparable<Case>`, `Serializable`)

### FODS (Foundations of Data Science & Custom Data Structures)
- **Binary Max-Heap** — O(log N) priority queue for cause-list generation
- **Directed Acyclic Graph (DAG)** — Citation network with DFS overrule detection
- **Trie (Prefix Tree)** — O(K) statutory code autocomplete
- **Inverted Index** — Hash table with separate chaining for keyword search
- **Quickselect** — O(N) median computation for court statistics

## 🗂️ Project Structure

```
J.U.S.T.I.S./
├── src/
│   └── com/justis/
│       ├── model/              # Domain entities
│       │   ├── Case.java       # Abstract base class
│       │   ├── CriminalCase.java
│       │   ├── CivilCase.java
│       │   ├── WritCase.java
│       │   ├── CaseType.java   # Enum
│       │   ├── CaseStatus.java # Enum
│       │   └── CitationEdge.java
│       ├── ds/                 # Custom data structures
│       ├── analytics/          # Statistical engine
│       ├── service/            # Business logic
│       │   ├── CaseFactory.java
│       │   └── DataWrangler.java
│       └── exception/          # Custom exceptions
│           ├── InvalidDatasetException.java
│           ├── CaseNotFoundException.java
│           └── OverruledPrecedentException.java
├── data/                       # CSV datasets
│   ├── cases_master.csv
│   ├── statutes_catalog.csv
│   └── citations_dag.csv
├── test/                       # Test classes
├── compile.sh                  # Build & run script
└── README.md
```

## 🚀 Quick Start

### Prerequisites
- **Java JDK 17+** (OpenJDK recommended)
- **macOS** with Apple Silicon (M1/M2/M3/M4) or any Unix-like system

### Installation

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/J.U.S.T.I.S..git
cd J.U.S.T.I.S.

# Compile all sources
./compile.sh

# Run the smoke tests
./compile.sh test

# Run the main application (when available)
./compile.sh run

# Clean build artifacts
./compile.sh clean
```

### Manual Compilation

```bash
# Compile
javac -d out src/com/justis/exception/*.java \
             src/com/justis/model/*.java \
             src/com/justis/service/*.java

# Run smoke test
java -cp out com.justis.Phase2SmokeTest
```

## 📊 Dataset

Built on the **ILDC (Indian Legal Documents Corpus)** benchmark:

> *"ILDC for CJPE: Indian Legal Documents Corpus for Court Judgment Prediction and Explanation"*
> Vijit Malik et al. (IIT Kanpur, IISER Kolkata, WBNUJS — ACL-IJCNLP 2021)

| Sub-Corpus | Documents | Avg. Tokens |
|------------|-----------|-------------|
| ILDC_multi | 34,816 | 3,231 |
| ILDC_single | 5,082 | 3,884 |
| ILDC_expert | 56 | Expert-annotated |

## 📈 Development Roadmap

- [x] **Phase 1:** Research & Modeling
- [x] **Phase 2:** Domain Model & Ingestion Pipeline
- [ ] **Phase 3:** Custom Data Structures (MaxHeap, DAG, Trie, InvertedIndex)
- [ ] **Phase 4:** Analytics Engine & Scheduler Facade
- [ ] **Phase 5:** Interactive CLI Application
- [ ] **Phase 6:** Testing & Documentation

## 🧮 Priority Scoring Formulas

| Case Type | Formula |
|-----------|---------|
| **Criminal** | `P = (3.0 × custodyDays) + (2.0 × caseAgeDays) + (10.0 × urgencyFlag)` |
| **Civil** | `P = (1.5 × caseAgeDays) + (disputeValueINR / 100,000)` |
| **Writ** | `P = (5.0 × caseAgeDays) + (15.0 × urgencyFlag) + 500.0` |

## 👥 Authors

- **Aditya Kumar** — [adityakumarsp83@gmail.com](mailto:adityakumarsp83@gmail.com)

## 📄 License

This project is developed for academic purposes as part of the OOPS & FODS coursework.
