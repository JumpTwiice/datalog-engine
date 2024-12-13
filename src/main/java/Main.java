import solver.*;

import java.io.FileInputStream;
public class Main {
    public static String testPath = System.getProperty("user.dir") + "/src/test/";

    public static void main(String[] args) throws Exception {
        Testing.runAllTests();
        System.exit(0);

        var is = new FileInputStream(testPath + "test1.datalog");
        var parser = new Parser(is);
        var p = parser.parse();
//        System.out.println(p);
//        p = Transformer.magicSets(p);
//        System.out.println(p);
//        System.exit(0);

        p.setupForTrieSolver();
        p.setupForSimpleSolver();
//        Checker.checkProgram(p);
//        var solver = new SCCSolverDecorator<>(p, new SimpleSolver(p));
        var solver = new SCCSolverDecorator<>(p, new TrieSolver(p));
//        var solver = new TrieSolver(p);
//        var solver = new SimpleSolver(p);
//        var x = solver.naiveEval();
        solver.semiNaiveEval();
        var predMap = solver.solutionsToPredMap();
        System.out.println(Solver.formatSolution(predMap, p));
        is.close();
    }
}
