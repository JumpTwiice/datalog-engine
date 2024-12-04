import solver.*;

import java.io.*;
import java.util.*;

public class Benchmark {
    public static String projectPath = System.getProperty("user.dir") + "/src/test/";

    public static void main(String[] args) throws Exception {
        String[] programs = new String[]{"test2", "test3", "test4", "test5", "test6", "test7", "test8"};
        for (String program : programs) {
            String filename = program + ".datalog";

            System.out.println("Profile for " + filename);
            System.out.println("-------------------------");

            runWithSolver(Solv.SIMPLE, filename);
            runWithSolver(Solv.TRIE, filename);
            runWithSolver(Solv.SCC_SIMPLE, filename);
        }
    }

    private static void runWithSolver(Solv s, String filename) throws Exception {
        var is = new FileInputStream(projectPath + filename);
        var parser = new Parser(is);
        var p = parser.parse();
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
        System.out.println("Runtime for " + s + ": " + time + "ms");
        is.close();
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
