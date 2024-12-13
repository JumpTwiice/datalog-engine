import ast.Program;
import org.json.simple.JSONObject;
import solver.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class Benchmark {
    public static final String benchmarkPath = System.getProperty("user.dir") + "/src/test/benchmark/";
    private static final String resPath = System.getProperty("user.dir") + "/src/main/resources/result/";
    private static final int numTrials = 5;
    private static final int timeOutSeconds = 30;
    private static final ExecutorService executor = Executors.newFixedThreadPool(8);;
    private static final boolean verbose = false;
    private static final Solv defaultSolver = Solv.SCC_TRIE;

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "benchmark-solver" -> {
                String[] programs = new String[]{"cartesian", "reachable", "reachable-flipped", "clusters"};
                benchmarkSolverOnPrograms(programs, defaultSolver, true);
            }
            case "reachable-scc" -> {
                Solv[] solvers = new Solv[]{Solv.TRIE, Solv.SCC_TRIE};
                int max = 16;
                int nodes = (int) Math.pow(2, max);
                benchmarkProblem("scc-reachable.json", solvers, x
                                -> ProgramGen.reachableSCCProblem(nodes, x),
                        1, max, 1, true, false);
            }
            case "reachable-problem" -> {
                Solv[] solvers = new Solv[]{Solv.TRIE, Solv.SIMPLE};
                benchmarkProblem("reachable.json", solvers, ProgramGen::reachableProblem, 30, 130, 10, true, true);
                benchmarkProblem("reachable.json", solvers, ProgramGen::reachableProblem, 30, 130, 10, false, true);
            }
            case "reachable-simple-vs-trie" -> {
                Solv[] solvers = new Solv[]{Solv.TRIE, Solv.SIMPLE};
                benchmarkProblem("reachable-large-simple-vs-true.json", solvers, ProgramGen::reachableProblem, 50, 1000, 50, true, true);
            }
            case "magic-sets" -> {
                String[] programs = new String[]{
                        "reachable-magic", "reachable-magic2", "reachable-original"
                };
                benchmarkMagicSetsOnPrograms(programs, defaultSolver, true);
                benchmarkMagicSetsOnPrograms(programs, defaultSolver, false);
            }
            case "benchmark-problems" -> {
                Solv[] solvers = new Solv[]{defaultSolver};
                benchmarkProblem("default-clusters.json", solvers, x -> ProgramGen.clusterProblem(x, 10), 1000, 5000, 100, true, false);
                benchmarkProblem("default-clusters.json", solvers, x -> ProgramGen.clusterProblem(x, 10), 1000, 5000, 100, false, false);
                benchmarkProblem("default-reachable.json", solvers, ProgramGen::reachableProblem, 100, 500, 10, true, false);
                benchmarkProblem("default-reachable.json", solvers, ProgramGen::reachableProblem, 100, 500, 10, false, false);
            }
        }
        executor.shutdownNow();
    }

    private static void benchmarkMagicSetsOnPrograms(String[] programs, Solv solver, boolean withClusters) throws Exception {
        JSONObject jsonObject = new JSONObject();
        for (String program : programs) {
            String filename = program + ".datalog";
            Program p;
            if (withClusters) {
                int n = 10000;
                int clusterSize = 100;
                p = ProgramGen.clusterProblemFromTemplate("magic-sets/" + filename, n/clusterSize, clusterSize);
            } else {
                int n = 1000;
                p = ProgramGen.reachableProblemFromTemplate("magic-sets/" + filename, n);
            }
            p.setupForTrieSolver();

            System.out.println((withClusters ? "Clusters" : "Normal") + " for " + filename + "...");
            System.out.println("----------------------------");
            System.out.println("Warming up...");

            for (int i = 0; i < 5; i++) {
                var x = runWithSolverAndTimeOut(solver, p, 10, true);
            }

            System.out.println("Benchmarking...\n");
            long avg = getAvgFromTrials(solver, p, true, true);
            jsonObject.put(program, avg);
        }
        String outputFileName = (withClusters ? "clusters" : "reachable") + "-magic-sets.json";
        writeJSONFile(true, outputFileName, jsonObject);
    }

    private static void benchmarkProblem(String outputFileName, Solv[] solvers, Function<Integer, Program> problem,
                                         int min, int max, int step, boolean withSemi, boolean withTimeout) throws Exception {
        JSONObject jsonObject = new JSONObject();
        for (Solv solver : solvers) {
            JSONObject jsonObjectSolver = new JSONObject();
            jsonObject.put(solver.toString(), jsonObjectSolver);
            for (int n = min; n <= max; n += step) {
                Program p = problem.apply(n);
                long avg = getAvgFromTrials(solver, p, withSemi, withTimeout);
                if (avg == -1L)
                    break;
                System.out.println("(" + solver + ") " + "Average time (n=" + n + "): " + avg + " ms");
                jsonObjectSolver.put(n, avg);
            }
        }
        writeJSONFile(withSemi, outputFileName, jsonObject);
    }

    private static void writeJSONFile(boolean withSemi, String outputFileName, JSONObject outerJSON) throws IOException {
        String outputFile = resPath + (withSemi ? "semi-naive/" : "naive/") + outputFileName;
        PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8);
        outerJSON.writeJSONString(writer);
        writer.close();
    }

    private static void benchmarkSolverOnPrograms(String[] programs, Solv solver, boolean withSemi) throws Exception {
        JSONObject jsonObject = new JSONObject();
        for (String program : programs) {
            String filename = program + ".datalog";
            var p = ProgramGen.fileToProgram("benchmark/" + filename);

            System.out.println("Profile for " + filename + "...");
            System.out.println("----------------------------");
            System.out.println("Warming up...");

            for (int i = 0; i < numTrials; i++) {
                var x = runWithSolverAndTimeOut(solver, p, 10, withSemi);
            }

            System.out.println("Benchmarking...");
            long avg = getAvgFromTrials(solver, p, withSemi, true);
            jsonObject.put(program, avg);
        }
        String outputFileName = solver.toFileName() + ".json";
        writeJSONFile(withSemi, outputFileName, jsonObject);
    }

    private static long getAvgFromTrials(Solv solver, Program p, boolean withSemi, boolean withTimeout) throws Exception {
        int trialsSucceeded = 0;
        long sum = 0;
        for (int i = 0; i < numTrials; i++) {
            long time;
            if (withTimeout) {
                time = runWithSolverAndTimeOut(solver, p, timeOutSeconds, withSemi);
            } else {
                time = runWithSolver(solver, p, withSemi);
            }
            if (time == -1L) {
                if (trialsSucceeded == 0)
                    sum = -1L;
                break;
            } else {
                sum += time;
                trialsSucceeded++;
            }
        }
        return sum / Math.max(trialsSucceeded, 1);
    }

    private static long runWithSolverAndTimeOut(Solv s, Program p, int timeOutSeconds, boolean withSemi) throws Exception {
        if (verbose) {
            String eval = withSemi ? "semi-naive" : "naive";
            System.out.print("Running " + s + " with " + eval + "... ");
        }
        final Future<Long> handler = executor.submit(() -> runWithSolver(s, p, withSemi));
        Long res = -1L;
        try {
            res = handler.get(timeOutSeconds, TimeUnit.SECONDS);
            if (verbose)
                System.out.println("Took " + res + " ms");
        } catch (TimeoutException e) {
            if (verbose)
                System.out.println("Timed out");
        } catch (Exception e) {
            System.out.println(e.toString());
            executor.shutdownNow();
        } finally {
            handler.cancel(true);
        }
        return res;
    }

    private static long runWithSolver(Solv s, Program p, boolean withSemi) throws Exception {
        Solver<?> solver;
        p.setupForSimpleSolver();
        p.setupForTrieSolver();
        switch (s) {
            case SIMPLE -> solver = new SimpleSolver(p);
            case TRIE -> solver = new TrieSolver(p);
            case SCC_SIMPLE -> solver = new SCCSolverDecorator<>(p, new SimpleSolver(p));
            case SCC_TRIE -> solver = new SCCSolverDecorator<>(p, new TrieSolver(p));
            default -> throw new Exception("Impossible");
        }
        var startTime = System.currentTimeMillis();
        Map<Long, ?> x = withSemi ? solver.semiNaiveEval() : solver.naiveEval();
        var endTime = System.currentTimeMillis();
        return endTime - startTime;
    }
}

enum Solv {
    SIMPLE("Simple Solver"),
    TRIE("Trie Solver"),
    SCC_SIMPLE("SCC Simple Solver"),
    SCC_TRIE("SCC Trie Solver");

    private final String text;

    Solv(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public String toFileName() {
        return text.toLowerCase().replace(" ", "-");
    }
}
