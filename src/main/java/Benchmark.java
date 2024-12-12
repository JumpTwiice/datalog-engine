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
    private static final int timeOutSeconds = 60;
    private static ExecutorService executor;
    private static final boolean verbose = false;
    private static final Solv defaultSolver = Solv.SCC_TRIE;

    public static void main(String[] args) throws Exception {
        executor = Executors.newFixedThreadPool(8);
        if (args[0].equals("benchmark-solver")) {
            String[] programs = new String[]{"cartesian"};
            benchmarkSolverOnPrograms(programs, defaultSolver, true);
            benchmarkSolverOnPrograms(programs, defaultSolver, false);
        } else if (args[0].equals("reachable-scc")) {
            Solv[] solvers = new Solv[]{Solv.TRIE, Solv.SCC_TRIE};
            int max = 16;
            int nodes = (int) Math.pow(2, max);
            benchmarkHardProblem("scc-reachable.json", solvers, x
                    -> ProgramGen.reachableSCCProblem(nodes, x),
                    1, max, 1, true);
        } else if (args[0].equals("hard-problem")) {
            Solv[] solvers = new Solv[]{Solv.TRIE, Solv.SIMPLE};
            benchmarkHardProblem("hard-problem.json", solvers, ProgramGen::reachableProblem, 30, 130, 10, true);
            benchmarkHardProblem("hard-problem.json", solvers, ProgramGen::reachableProblem, 30, 130, 10, false);
        } else if (args[0].equals("magic-sets")) {
            String[] programs = new String[]{
                    "reachable-magic", "reachable-magic2", "reachable-original"
            };
//            benchmarkMagicSetsOnPrograms(programs, defaultSolver, true);
            benchmarkMagicSetsOnPrograms(programs, defaultSolver, false);
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
                p = ProgramGen.clusterProblemFromTemplate("magic-sets/" + filename, n/3, 3);
            } else {
                int n = 2000;
                p = ProgramGen.reachableProblemFromTemplate("magic-sets/" + filename, n);
            }
            p.setupForTrieSolver();

            System.out.println((withClusters ? "Clusters" : "Normal") + " for " + filename + "...");
            System.out.println("----------------------------");
            System.out.println("Warming up...");

            for (int i = 0; i < 5; i++) {
//                var x = runWithSolverAndTimeOut(solver, p, 10, true);
            }

            System.out.println("Benchmarking...\n");
            long avg = getAvgFromTrials(solver, p, true, true);
            jsonObject.put(program, avg);
        }
        String outputFileName = (withClusters ? "clusters" : "reachable") + "-magic-sets.json";
        writeJSONFile(true, outputFileName, jsonObject);
    }

    private static void benchmarkHardProblem(String outputFileName, Solv[] solvers, Function<Integer, Program> func,
                                             int min, int max, int step, boolean withSemi) throws Exception {
        JSONObject jsonObject = new JSONObject();
        for (Solv solver : solvers) {
            JSONObject jsonObjectSolver = new JSONObject();
            jsonObject.put(solver.toString(), jsonObjectSolver);
            for (int n = min; n <= max; n += step) {
                Program p = func.apply(n);
                long avg = getAvgFromTrials(solver, p, withSemi, false);
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
            var p = ProgramGen.fileToProgram(benchmarkPath + filename);

            System.out.println("Profile for " + filename + "...");
            System.out.println("----------------------------");
            System.out.println("Warming up...");

            for (int i = 0; i < 5; i++) {
                var x = runWithSolverAndTimeOut(solver, p, 5, withSemi);
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
        var time = System.currentTimeMillis();
        Map<Long, ?> x = withSemi ? solver.semiNaiveEval() : solver.naiveEval();
        time = System.currentTimeMillis() - time;
        System.out.println();
        System.out.println(Solver.formatSolution(solver.solutionsToPredMap(), p));
        return time;
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
