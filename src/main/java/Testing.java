import ast.Program;
import solver.SimpleSolver;
import solver.Solver;
import solver.Transformer;
import solver.TrieSolver;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

public class Testing {
    public static int minPreds = 2;
    public static int maxPreds = 10;

    public static int minRules = 2;
    public static int maxRules = 10;

    public static int minArity = 1;
    public static int maxArity = 4;

    public static int minBodyAtoms = 1;
    public static int maxBodyAtoms = 4;

    public static int minVariables = 1;
    public static int maxVariables = 8;

    public static double factProbability = 0.5;
    public static int minFacts = 1;
    public static int maxFacts = 20;

    public static int minHerbrand = 1;
    public static int maxHerbrand = 20;

    public static Random rand = new Random(1);


    public static String generateRandomRawProgram() {
        var predNumber =  rand.nextInt(minPreds, maxPreds);
        var predArities = IntStream.range(0, predNumber).map(x -> rand.nextInt(minArity, maxArity)).toArray();
        var ruleNumber =  rand.nextInt(minRules, maxRules);
        var result = new StringBuilder();
        result.append(makeFacts(rand, predNumber, predArities));
        result.append(makeRules(ruleNumber, rand, predNumber, predArities));
        return result.toString();
    }

    private static StringBuilder makeQuery(Random rand, int predNumber, int[] predArities) {
        var res = new StringBuilder();

        for(var i = 0; i < predNumber; i++) {
            if(factProbability < rand.nextDouble()) {
                continue;
            }
            var factNum =  rand.nextInt(minFacts, maxFacts);
            for(var j = 0; j < factNum; j++) {
                res.append('p').append(i).append('(');
                for(var k = 0; k < predArities[i]; k++) {
                    res.append(rand.nextInt(1, maxHerbrand - minHerbrand + 2));
                    if(k != predArities[i] - 1) {
                        res.append(',');
                    }
                }
                res.append(')').append('.');
            }
            res.append('\n');
        }
        return res;
    }

    private static StringBuilder makeFacts(Random rand, int predNumber, int[] predArities) {
        var res = new StringBuilder();
        for(var i = 0; i < predNumber; i++) {
            if(factProbability < rand.nextDouble()) {
                continue;
            }
            var factNum =  rand.nextInt(minFacts, maxFacts);
            for(var j = 0; j < factNum; j++) {
                res.append('p').append(i).append('(');
                for(var k = 0; k < predArities[i]; k++) {
                    res.append(rand.nextInt(1, maxHerbrand - minHerbrand + 2));
                    if(k != predArities[i] - 1) {
                        res.append(',');
                    }
                }
                res.append(')').append('.');
            }
            res.append('\n');
        }
        return res;
    }

    private static StringBuilder makeRules(int ruleNumber, Random rand, int predNumber, int[] predArities) {
        var result = new StringBuilder();
        for(var i = 0; i < ruleNumber; i++) {
            List<Integer> variables = new ArrayList<>();
            var variableNum = rand.nextInt(minVariables, maxVariables);
            var bodyNumber = rand.nextInt(minBodyAtoms, maxBodyAtoms);
            var bodyBuilder = new StringBuilder();
            for(var j = 0; j < bodyNumber; j++) {
                var bodyPredicate = rand.nextInt(0, predNumber);
                bodyBuilder.append('p').append(bodyPredicate).append('(');
                for(var k = 0; k < predArities[bodyPredicate]; k++) {
                    var variable = rand.nextInt(0, variableNum);
                    if(!variables.contains(variable)) {
                        variables.add(variable);
                    }
                    bodyBuilder.append('X').append(variable);
                    if(k != predArities[bodyPredicate] - 1) {
                        bodyBuilder.append(',');
                    }
                }
                bodyBuilder.append(')');
                if(j != bodyNumber - 1) {
                    bodyBuilder.append(',');
                } else {
                    bodyBuilder.append('.');
                }
            }
            bodyBuilder.append('\n');

            var headBuilder = new StringBuilder();
            var headPredicate = rand.nextInt(0, predNumber);
            headBuilder.append('p').append(headPredicate).append('(');
            for(var k = 0; k < predArities[headPredicate]; k++) {
                var variable = variables.get(rand.nextInt(0, variables.size()));
                headBuilder.append('X').append(variable);
                if(k != predArities[headPredicate] - 1) {
                    headBuilder.append(',');
                }
            }
            headBuilder.append(')').append(':').append('-');
            result.append(headBuilder).append(bodyBuilder);
        }
        return result;
    }

    public static Program generateRandomParsedProgram() throws Exception {
        var is = new ByteArrayInputStream(generateRandomRawProgram().getBytes(StandardCharsets.UTF_8));
        var parser = new Parser(is);
        return parser.parse();
    }

    public static void ensureEquality(List<Map<Long, Set<List<Long>>>> solutions) {
        for(var i = 1; i < solutions.size(); i++) {
            var prev = solutions.get(i-1);
            var cur = solutions.get(i);
            assert(prev.equals(cur));
        }
    }


    public static void runRandomTests(int n) throws Exception {
        for(var i = 0; i < n; i++) {
            System.out.println(i);
            List<Map<Long, Set<List<Long>>>> solutions = new ArrayList<>();
//            System.out.println(i);
            var p = generateRandomParsedProgram();
//            System.out.println(p);
            Transformer.setEqSet(p);
//            p.setupForTrieSolver();
            Solver<?> solver = new SimpleSolver(p);
            solver.naiveEval();
            solutions.add(solver.solutionsToPredMap());
            solver = new SimpleSolver(p);
            solver.semiNaiveEval();
            solutions.add(solver.solutionsToPredMap());
            p.setupForTrieSolver();
            solver = new TrieSolver(p);
            solver.naiveEval();
            solutions.add(solver.solutionsToPredMap());
            solver = new TrieSolver(p);
            solver.semiNaiveEval();
            solutions.add(solver.solutionsToPredMap());

//            TODO: Do for SCC

            ensureEquality(solutions);
        }
    }

}
