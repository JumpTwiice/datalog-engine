import ast.Program;
import org.json.simple.JSONObject;
import solver.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;

public class Benchmark {
    private static final String projectPath = System.getProperty("user.dir") + "/src/test/benchmark/";
    private static final String resPath = System.getProperty("user.dir") + "/src/main/resources/result/";
    private static final int numTrials = 5;
    private static final int timeOutSeconds = 60;
    private static ExecutorService executor;

    public static void main(String[] args) throws Exception {
        executor = Executors.newFixedThreadPool(8);
        compare(true);
        compare(false);
        System.exit(0);

        String[] programs = new String[]{"reachable", "clusters"};
        Solv solver = Solv.TRIE;
        benchmark(programs, solver, true);
        benchmark(programs, solver, false);

        executor.shutdownNow();
    }

    private static void compare(boolean withSemi) throws Exception {
        JSONObject outerJSON = new JSONObject();
        Solv[] solvers = new Solv[]{Solv.TRIE, Solv.SCC_TRIE, Solv.SCC_SIMPLE, Solv.SIMPLE};
        for (Solv solver : solvers) {
            JSONObject solverJSON = new JSONObject();
            outerJSON.put(solver.toString(), solverJSON);
            for (int n = 30; n <= 130; n += 10) {
                String program = Main.computeHardProblem(n);
                var is = new ByteArrayInputStream(program.getBytes(StandardCharsets.UTF_8));
                var parser = new Parser(is);
                var p = parser.parse();
                is.close();

                int trialsSucceeded = 0;
                long sum = 0;
                for (int i = 0; i < numTrials; i++) {
                    var time = runWithSolverAndTimeOut(solver, p, timeOutSeconds, withSemi);
                    if (time == -1L) {
                        if (trialsSucceeded == 0)
                            sum = -1L;
                        break;
                    } else {
                        sum += time;
                        trialsSucceeded++;
                    }
                }
                long avg = sum / Math.max(trialsSucceeded, 1);
                System.out.println("Average time (n=" + n + "): " + avg);
                solverJSON.put(n, avg);
            }
        }
        String out = resPath + (withSemi ? "semi-naive/" : "naive/") +  "hard-problem.json";
        PrintWriter writer = new PrintWriter(out, StandardCharsets.UTF_8);
        outerJSON.writeJSONString(writer);
        writer.close();
    }

    private static void benchmark(String[] programs, Solv solver, boolean withSemi) throws Exception {
        for (String program : programs) {
            String filename = program + ".datalog";
            var is = new FileInputStream(projectPath + filename);
            var parser = new Parser(is);
            var p = parser.parse();
            is.close();

            System.out.println("Profile for " + filename + "...");
            System.out.println("----------------------------");
            System.out.println("Warming up...");

            for (int i = 0; i < 5; i++) {
                var x = runWithSolverAndTimeOut(solver, p, 5, withSemi);
            }

            System.out.println("Benchmarking...");
            String outputFileName = program + solver.toFileSuffix() + ".txt";
            String outputFile = resPath + (withSemi ? "semi-naive/" : "naive/") + outputFileName;

            PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8);

            long sum = 0;

            int numTimedOut = 0;

            for (int i = 0; i < numTrials; i++) {
                long time = runWithSolverAndTimeOut(solver, p, timeOutSeconds, withSemi);

                if (time == -1L) {
                    numTimedOut++;
                    writer.println("T/O");
                } else {
                    sum += time;
                    writer.println(time);
                }
            }
            long avg = sum / Math.max(1, numTrials - numTimedOut);

            writer.println("Avg: " + avg);
            System.out.println((withSemi ? "Semi " : "") + "naive for " + solver + ": " + avg);
            writer.close();
        }
    }

    private static long runWithSolverAndTimeOut(Solv s, Program p, int timeOutSeconds, boolean withSemi) throws Exception {
        String eval = withSemi ? "semi-naive" : "naive";
        System.out.print("Running " + s + " with " + eval + "... ");
        final Future<Long> handler = executor.submit(() -> runWithSolver(s, p, withSemi));
        Long res = -1L;
        try {
            res = handler.get(timeOutSeconds, TimeUnit.SECONDS);
            System.out.println("Took " + res + " ms");
        } catch (TimeoutException e) {
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

    public String toFileSuffix() {
        return "-" + text.toLowerCase().replace(" ", "-");
    }
}
