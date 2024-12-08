import ast.Program;
import solver.*;

import java.io.*;
import java.util.*;

public class Benchmark {
    public static String projectPath = System.getProperty("user.dir") + "/src/test/benchmark/";
    public static String resPath = System.getProperty("user.dir") + "/src/main/resources/result/";
    public static int numTrials = 5;

    public static void main(String[] args) throws Exception {
        String[] programs = new String[]{"reachable", "clusters"};
        Solv[] solvers = new Solv[]{Solv.SIMPLE, Solv.TRIE}; // Temp for at undgå SCC
        for (String program : programs) {
            String filename = program + ".datalog";
            var is = new FileInputStream(projectPath + filename);
            var parser = new Parser(is);
            var p = parser.parse();
            is.close();

            System.out.println("Running for " + filename + "...");
            System.out.println("Warming up...");
            for (Solv solver: solvers) {
                for (int i = 0; i < 3; i++) {
//                    var x = runNaiveWithSolver(solver, p);
                    var y = runSemiNaiveWithSolver(solver, p);
                }
            }

            System.out.println("Benchmarking...");
            String outputFileNaive = resPath + "naive/" + program + "-java.txt";
            String outputFileSemi = resPath + "semi-naive/" + program + "-java.txt";
            PrintWriter naiveWriter = new PrintWriter(outputFileNaive, "UTF-8");
            PrintWriter semiWriter = new PrintWriter(outputFileSemi, "UTF-8");
            for (Solv solver: solvers) {
                long naiveSum = 0;
                long semiSum = 0;
                naiveWriter.print(solver + ":");
                semiWriter.print(solver + ":");
                for (int i = 0; i < numTrials; i++) {
                    long res1 = 0;
//                    long res1 = runNaiveWithSolver(solver, p);
                    long res2 = runSemiNaiveWithSolver(solver, p);
                    naiveSum += res1;
                    semiSum += res2;
                    naiveWriter.println(res1);
                    semiWriter.println(res2);
                }
                long avg1 = naiveSum / numTrials;
                long avg2 = semiSum / numTrials;
                naiveWriter.println("Avg " + avg1);
                semiWriter.println("Avg " + avg2);
                System.out.println("Naive for " + solver + ": " + avg1);
                System.out.println("Semi naive for " + solver + ": " + avg2);
            }
            naiveWriter.close();
            semiWriter.close();
        }
    }

    private static long runSemiNaiveWithSolver(Solv s, Program p) throws Exception {
        Solver<?> solver;
        var time = System.currentTimeMillis();
        switch (s) {
            case SIMPLE -> solver = new SimpleSolver(p);
            case TRIE -> {
                p.setupForTrieSolver();
                solver = new TrieSolver(p);
            }
            case SCC_SIMPLE -> solver = new SCCSolverDecorator<>(p, new SimpleSolver(p));
            default -> throw new Exception("hej");
        }
        Map<Long, ?> x = solver.semiNaiveEval();
        time = System.currentTimeMillis() - time;
        return time;
    }

    // Kodeduplikering men vi er ligeglade
    private static long runNaiveWithSolver(Solv s, Program p) throws Exception {
        Solver<?> solver;
        var time = System.currentTimeMillis();
        switch (s) {
            case SIMPLE -> solver = new SimpleSolver(p);
            case TRIE -> {
                p.setupForTrieSolver();
                solver = new TrieSolver(p);
            }
            case SCC_SIMPLE -> solver = new SCCSolverDecorator<>(p, new SimpleSolver(p));
            default -> throw new Exception("hej");
        }
        Map<Long, ?> x = solver.naiveEval();
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
