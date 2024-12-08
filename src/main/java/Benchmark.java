import ast.Program;
import solver.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;

public class Benchmark {
    private static final String projectPath = System.getProperty("user.dir") + "/src/test/benchmark/";
    private static final String resPath = System.getProperty("user.dir") + "/src/main/resources/result/";
    private static final int numTrials = 5;
    private static final int timeOutSeconds = 180;
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) throws Exception {
        String[] programs = new String[]{"reachable", "clusters"};
        Solv[] solvers = new Solv[]{Solv.SIMPLE, Solv.TRIE}; // Temp for at undgå SCC

        for (String program : programs) {
            String filename = program + ".datalog";

            System.out.println("Profile for " + filename + "...");
            System.out.println("----------------------------");
            System.out.println("Warming up...");

            for (Solv solver: solvers) {
                for (int i = 0; i < 2; i++) {
                    var x = runWithSolverAndTimeOut(solver, filename, 5, false);
                    var y = runWithSolverAndTimeOut(solver, filename, 5, true);
                }
            }

            System.out.println("Benchmarking...");
            String outputFileNaive = resPath + "naive/" + program + "-java.txt";
            String outputFileSemi = resPath + "semi-naive/" + program + "-java.txt";

            PrintWriter naiveWriter = new PrintWriter(outputFileNaive, StandardCharsets.UTF_8);
            PrintWriter semiWriter = new PrintWriter(outputFileSemi, StandardCharsets.UTF_8);

            for (Solv solver: solvers) {
                long naiveSum = 0;
                long semiSum = 0;

                naiveWriter.println(solver + ":");
                semiWriter.println(solver + ":");

                int numTimedOutNaive = 0;
                int numTimedOutSemi = 0;

                for (int i = 0; i < numTrials; i++) {
                    long timeNaive = runWithSolverAndTimeOut(solver, filename, timeOutSeconds, false);
                    long timeSemi = runWithSolverAndTimeOut(solver, filename, timeOutSeconds, true);

                    if (timeNaive == -1L) {
                        numTimedOutNaive++;
                        naiveWriter.println("T/O");
                    } else {
                        naiveSum += timeNaive;
                        naiveWriter.println(timeNaive);
                    }
                    if (timeSemi == -1L) {
                        numTimedOutSemi++;
                        semiWriter.println("T/O");
                    } else {
                        semiSum += timeSemi;
                        semiWriter.println(timeSemi);
                    }
                }
                long avgNaive = naiveSum / Math.max(1, numTrials - numTimedOutNaive);
                long avgSemi = semiSum / Math.max(1, numTrials - numTimedOutSemi);

                naiveWriter.println("Avg: " + avgNaive);
                semiWriter.println("Avg: " + avgSemi);
                System.out.println("Naive for " + solver + ": " + avgNaive);
                System.out.println("Semi naive for " + solver + ": " + avgSemi);
            }
            naiveWriter.close();
            semiWriter.close();
        }
    }

    private static long runWithSolverAndTimeOut(Solv s, String filename, int timeOutSeconds, boolean withSemi) throws Exception {
        String eval = withSemi ? "semi-naive" : "naive";
        System.out.print("Running " + s + " with " + eval + "... ");
        final Future<Long> handler = executor.submit(() -> runWithSolver(s, filename, withSemi));
        Long res;
        try {
            res = handler.get(timeOutSeconds, TimeUnit.SECONDS);
            System.out.println("Took " + res + " ms");
        } catch (TimeoutException e) {
            res = -1L;
            System.out.println("Timed out");
        } finally {
            executor.shutdownNow();
        }
        return res;
    }

    private static long runWithSolver(Solv s, String filename, boolean withSemi) throws Exception {
        var is = new FileInputStream(projectPath + filename);
        var parser = new Parser(is);
        var p = parser.parse();
        is.close();
        Solver<?> solver;
        var time = System.currentTimeMillis();
        switch (s) {
            case SIMPLE -> solver = new SimpleSolver(p);
            case TRIE -> {
                p.setupForTrieSolver();
                solver = new TrieSolver(p);
            }
            default -> throw new Exception("hej");
        }
        Map<Long, ?> x = withSemi ? solver.semiNaiveEval() : solver.naiveEval();
        time = System.currentTimeMillis() - time;
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
}
