import ast.Program;
import solver.*;

import java.util.*;
import java.util.stream.Collectors;
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
    public static double constantProbability = 0.3;
    public static int minFacts = 1;
    public static int maxFacts = 8;

    public static int minHerbrand = 1;
    public static int maxHerbrand = 10;

    public static Random rand = new Random(1);

    public static String generateRandomRawProgram() {
        var predNumber = rand.nextInt(minPreds, maxPreds);
        var predArities = IntStream.range(0, predNumber).map(x -> rand.nextInt(minArity, maxArity)).toArray();
        var ruleNumber = rand.nextInt(minRules, maxRules);
        var result = new StringBuilder();
        result.append(makeFacts(rand, predNumber, predArities));
        result.append(makeRules(ruleNumber, rand, predNumber, predArities));
        result.append(makeQuery(rand, predNumber, predArities));
        return result.toString();
    }

    private static StringBuilder makeFacts(Random rand, int predNumber, int[] predArities) {
        var res = new StringBuilder();
        for (var i = 0; i < predNumber; i++) {
            if (factProbability < rand.nextDouble()) {
                continue;
            }
            var factNum = rand.nextInt(minFacts, maxFacts);
            for (var j = 0; j < factNum; j++) {
                res.append('p').append(i).append('(');
                for (var k = 0; k < predArities[i]; k++) {
                    res.append(rand.nextInt(1, maxHerbrand - minHerbrand + 2));
                    if (k != predArities[i] - 1) {
                        res.append(',');
                    }
                }
                res.append(')').append('.');
            }
            res.append('\n');
        }
        return res;
    }

    private static StringBuilder makeQuery(Random rand, int predNumber, int[] predArities) {
        var res = new StringBuilder();
        var pred = rand.nextInt(0, predNumber);
        res.append("?-").append('p').append(pred).append('(');
        for (var k = 0; k < predArities[pred]; k++) {
            if (constantProbability < rand.nextDouble()) {
                res.append(rand.nextInt(1, maxHerbrand - minHerbrand + 2));
            } else {
                res.append('X').append(rand.nextInt(minVariables, maxVariables));
            }

            if (k != predArities[pred] - 1) {
                res.append(',');
            }
        }
        res.append(')');
        return res;
    }

    private static StringBuilder makeRules(int ruleNumber, Random rand, int predNumber, int[] predArities) {
        var result = new StringBuilder();
        for (var i = 0; i < ruleNumber; i++) {
            List<Integer> variables = new ArrayList<>();
            var variableNum = rand.nextInt(minVariables, maxVariables);
            var bodyNumber = rand.nextInt(minBodyAtoms, maxBodyAtoms);
            var bodyBuilder = new StringBuilder();
            for (var j = 0; j < bodyNumber; j++) {
                var bodyPredicate = rand.nextInt(0, predNumber);
                bodyBuilder.append('p').append(bodyPredicate).append('(');
                for (var k = 0; k < predArities[bodyPredicate]; k++) {
                    var variable = rand.nextInt(0, variableNum);
                    if (!variables.contains(variable)) {
                        variables.add(variable);
                    }
                    bodyBuilder.append('X').append(variable);
                    if (k != predArities[bodyPredicate] - 1) {
                        bodyBuilder.append(',');
                    }
                }
                bodyBuilder.append(')');
                if (j != bodyNumber - 1) {
                    bodyBuilder.append(',');
                } else {
                    bodyBuilder.append('.');
                }
            }
            bodyBuilder.append('\n');

            var headBuilder = new StringBuilder();
            var headPredicate = rand.nextInt(0, predNumber);
            headBuilder.append('p').append(headPredicate).append('(');
            for (var k = 0; k < predArities[headPredicate]; k++) {
                var variable = variables.get(rand.nextInt(0, variables.size()));
                headBuilder.append('X').append(variable);
                if (k != predArities[headPredicate] - 1) {
                    headBuilder.append(',');
                }
            }
            headBuilder.append(')').append(':').append('-');
            result.append(headBuilder).append(bodyBuilder);
        }
        return result;
    }

    public static Program generateRandomParsedProgram() throws Exception {
        return ProgramGen.parseStringToProgram(generateRandomRawProgram());
    }

    public static void ensureEquality(List<Map<Long, Set<List<Long>>>> solutions) {
        for (var i = 1; i < solutions.size(); i++) {
            var prev = solutions.get(i - 1);
            var cur = solutions.get(i);
            assert (prev.keySet().equals(cur.keySet()));
            for (var k : prev.keySet()) {
                assert (prev.get(k).equals(cur.get(k)));
            }
        }
    }

    public static void ensureQueryEquality(List<Set<List<Long>>> queryAnswers) {
        for (var i = 1; i < queryAnswers.size(); i++) {
            var prev = queryAnswers.get(i - 1);
            var cur = queryAnswers.get(i);
            assert (prev.equals(cur));
        }
    }



    public static void runRandomTests(int n) throws Exception {
        for (var i = 0; i < n; i++) {
            System.out.println(i);
            List<Map<Long, Set<List<Long>>>> solutions = new ArrayList<>();
            var p = generateRandomParsedProgram();
            p.setupForSimpleSolver();
            p.setupForTrieSolver();
            Solver<?> solver = new SimpleSolver(p);
            solver.naiveEval();
            solutions.add(solver.solutionsToPredMap());
            solver = new SimpleSolver(p);
            solver.semiNaiveEval();
            solutions.add(solver.solutionsToPredMap());

            solver = new TrieSolver(p);
            solver.naiveEval();
            solutions.add(solver.solutionsToPredMap());
            solver = new TrieSolver(p);
            solver.semiNaiveEval();
            solutions.add(solver.solutionsToPredMap());

            solver = new SCCSolverDecorator<>(p, new SimpleSolver(p));
            solver.naiveEval();
            solutions.add(solver.solutionsToPredMap());
            solver = new SCCSolverDecorator<>(p, new SimpleSolver(p));
            solver.semiNaiveEval();
            solutions.add(solver.solutionsToPredMap());


            solver = new SCCSolverDecorator<>(p, new TrieSolver(p));
            solver.naiveEval();
            solutions.add(solver.solutionsToPredMap());
            solver = new SCCSolverDecorator<>(p, new TrieSolver(p));
            solver.semiNaiveEval();
            solutions.add(solver.solutionsToPredMap());

            ensureEquality(solutions);
            System.gc();
        }
    }

    public static void runAllTests() throws Exception {
        System.out.println("Testing trie solver");
        TrieTester.runAllTests();
        System.out.println("Testing simple solver");
        SimpleSolverTester.runAllTests();
        System.out.println("Testing magic sets");
        MagicSetsTester.runAllTests();
        System.out.println("Comparing output of solvers");
        Testing.runRandomTests(1000);
    }

    private static class TrieTester {
        public static void runAllTests() {
            testInsertAndRemove();
            testQuery();
            testMeld();
        }

        public static void testMeld() {
            var testTrie = new SimpleTrie(3);
            testTrie.add(new long[] {1,2,3});
            testTrie.add(new long[] {1,3,4});
            var testTrie2 = new SimpleTrie(3);
            testTrie2.add(new long[]{1,3,6});
            testTrie2.add(new long[]{4,1,9});
            testTrie.meld(testTrie2);
            assert(testTrie.children.get(1L).children.get(2L).leaves.contains(3L));
            assert(testTrie.children.get(1L).children.get(3L).leaves.contains(4L));
            assert(testTrie.children.get(1L).children.get(3L).leaves.contains(6L));
            assert(testTrie.children.get(4L).children.get(1L).leaves.contains(9L));
        }

        public static void testQuery() {
            var testTrie = new SimpleTrie(3);
            testTrie.add(new long[] {1,2,3});
            testTrie.add(new long[] {1,3,4});
            var q = TrieMap.query(testTrie, new boolean[3], new long[3], null);
            assert(q.children.get(1L).children.get(2L).leaves.contains(3L));
            assert(q.children.get(1L).children.get(3L).leaves.contains(4L));
            assert(q.children.size() == 1 && q.children.get(1L).children.size() == 2 && q.children.get(1L).children.get(2L).leaves.size() == 1 && q.children.get(1L).children.get(3L).leaves.size() == 1);
            q = TrieMap.query(testTrie, new boolean[] {false, true, false}, new long[] {29,2,32}, null);
            assert(q.children.get(1L).leaves.contains(3L));
            assert(q.children.size() == 1 && q.children.get(1L).leaves.size() == 1);
            testTrie.add(new long[] {1,4,1});
            q = TrieMap.query(testTrie, new boolean[]{false,false,true}, new long[3], new int[][]{new int[] {2}, null, null});
            assert(q.children.get(1L).leaves.contains(4L));
            assert(q.children.size() == 1 && q.children.get(1L).leaves.size() == 1);
        }

        public static void testInsertAndRemove() {
            var testTrie = new SimpleTrie(3);
            testTrie.add(new long[] {1,2,3});
            assert(testTrie.children.get(1L).children.get(2L).leaves.contains(3L));
            assert(testTrie.children.size() == 1 && testTrie.children.get(1L).children.size() == 1 && testTrie.children.get(1L).children.get(2L).leaves.size() == 1);
            testTrie.add(new long[] {1,3,4});
            assert(testTrie.children.get(1L).children.get(2L).leaves.contains(3L));
            assert(testTrie.children.get(1L).children.get(3L).leaves.contains(4L));
            assert(testTrie.children.size() == 1 && testTrie.children.get(1L).children.size() == 2 && testTrie.children.get(1L).children.get(2L).leaves.size() == 1 && testTrie.children.get(1L).children.get(3L).leaves.size() == 1);
            var removeTrie = new SimpleTrie(3);
            removeTrie.add(new long[]{1, 3, 4});
            testTrie.removeAll(removeTrie);
            assert(testTrie.children.get(1L).children.get(2L).leaves.contains(3L));
            assert(testTrie.children.size() == 1 && testTrie.children.get(1L).children.size() == 1 && testTrie.children.get(1L).children.get(2L).leaves.size() == 1);
        }
    }


    private static class MagicSetsTester {
        public static void runRandomTests(int n) throws Exception {
            for (var i = 0; i < n; i++) {
                System.out.println(i);
                List<Set<List<Long>>> solutions = new ArrayList<>();
                var p = generateRandomParsedProgram();
                var magicP = Transformer.magicSets(p);
                p.setupForQueryAnswering();
                p.setupForTrieSolver();
                magicP.setupForQueryAnswering();
                magicP.setupForTrieSolver();
                Solver<?> solver = new TrieSolver(p);
                solver.semiNaiveEval();
                solutions.add(Solver.getQueryAnswer(solver.solutionsToPredMap(), p));
                solver = new TrieSolver(magicP);
                solver.semiNaiveEval();
                solutions.add(Solver.getQueryAnswer(solver.solutionsToPredMap(), magicP));

                ensureQueryEquality(solutions);
            }

        }

        public static void runAllTests() throws Exception {
            runRandomTests(1000);
        }
    }

    private static class SimpleSolverTester {
        private static final Program dummyProgram = new Program(new HashMap<>(), new HashMap<>(), null, new HashMap<>(), 0);

        private static Set<List<Long>> createSimpleSet() {
            var x = Arrays.asList(1L, 2L, 3L);
            var y = Arrays.asList(4L, 5L, 6L);
            var z = Arrays.asList(7L, 8L, 9L);
            Set<List<Long>> res = new HashSet<>();
            res.add(x);
            res.add(y);
            res.add(z);
            return res;
        }

        private static Set<ArrayList<List<Long>>> createCartesianOfTwoSimple() {
            Set<ArrayList<List<Long>>> res = new HashSet<>();
            var x = Arrays.asList(1L, 2L, 3L);
            var y = Arrays.asList(4L, 5L, 6L);
            var z = Arrays.asList(7L, 8L, 9L);
            ArrayList<List<Long>> innerList = new ArrayList<>();
            innerList.add(x);
            innerList.add(x);
            res.add(innerList);
            innerList = new ArrayList<>();
            innerList.add(x);
            innerList.add(y);
            res.add(innerList);
            innerList = new ArrayList<>();
            innerList.add(x);
            innerList.add(z);
            res.add(innerList);
            innerList = new ArrayList<>();
            innerList.add(y);
            innerList.add(x);
            res.add(innerList);
            innerList = new ArrayList<>();
            innerList.add(y);
            innerList.add(y);
            res.add(innerList);
            innerList = new ArrayList<>();
            innerList.add(y);
            innerList.add(z);
            res.add(innerList);
            innerList = new ArrayList<>();
            innerList.add(z);
            innerList.add(x);
            res.add(innerList);
            innerList = new ArrayList<>();
            innerList.add(z);
            innerList.add(y);
            res.add(innerList);
            innerList = new ArrayList<>();
            innerList.add(z);
            innerList.add(z);
            res.add(innerList);
            return res;
        }

        private static Set<ArrayList<List<Long>>> wrapInList(Collection<List<Long>> toWrap) {
            return toWrap.stream().map(x -> {
                ArrayList<List<Long>> res = new ArrayList<>();
                res.add(x);
                return res;
            }).collect(Collectors.toCollection(HashSet::new));
        }

        /**
         * Run (primarily) 'unit' tests. To avoid diving too deep into the various constructions the parser and so on will be used to generate inputs
         */
        public static void runAllTests() throws Exception {
            testCartesian();
            testJoin();
            testEvalRule();
            testEval();
            testNaiveSolver();
        }

        /**
         * Test that the eval correctly combines the result of multiple rules
         */
        public static void testNaiveSolver() throws Exception {
            test1();
            test3();
            test4();
            test5();
            test6();
//            test7();
//            test8();
//            test9();
        }

        /**
         * Test that the eval correctly combines the result of multiple rules
         */
        public static void testEval() throws Exception {
            testCombining();
        }

        /**
         * Test that the eval-rule is performed correctly
         */
        public static void testEvalRule() throws Exception {
            testCopyEval();
            testConstantEval();
        }

        /**
         * Test that the join (Cartesian product and selection) is performed correctly
         */
        public static void testJoin() throws Exception {
            testCopySelection();
            testCopySelection2();
            testConstantSelection();
            testVariableSelection();
            testVariableSelection2();
        }


        /**
         * Test that the Cartesian product is performed correctly.
         */
        public static void testCartesian() {
            testCartesianOnlyOne();
            testCartesianOnlyTwo();
        }

        public static void test1() throws Exception {
            var p = ProgramGen.fileToProgram("test1.datalog");
            p.setupForSimpleSolver();
            var solver = new SimpleSolver(p);
            solver.naiveEval();
            solver.solutionsToPredMap().forEach((key, value) -> {
                switch (p.idToVar.get(key)) {
                    case "hej":
                        assert (value.equals(new HashSet<>(Arrays.asList(List.of(2L), List.of(9L)))));
                        break;
                    case "med":
                        assert (value.equals(new HashSet<>(Arrays.asList(List.of(2L), List.of(6L), List.of(9L)))));
                }
            });
        }

        public static void test3() throws Exception {
            var p = ProgramGen.fileToProgram("test3.datalog");
            p.setupForSimpleSolver();
            var solver = new SimpleSolver(p);
            solver.naiveEval();
            solver.solutionsToPredMap().forEach((key, value) -> {
                switch (p.idToVar.get(key)) {
                    case "hej", "med":
                        assert (value.equals(new HashSet<>(List.of(List.of(1L, 2L)))));
                        break;
                }
            });
        }

        public static void test4() throws Exception {
            var p = ProgramGen.fileToProgram("test4.datalog");
            p.setupForSimpleSolver();
            var solver = new SimpleSolver(p);
            solver.naiveEval();
            solver.solutionsToPredMap().forEach((key, value) -> {
                switch (p.idToVar.get(key)) {
                    case "hej":
                        assert (value.equals(new HashSet<>(Arrays.asList(List.of(1L, 2L), List.of(2L, 3L)))));
                        break;
                    case "med":
                        assert (value.equals(new HashSet<>(List.of(List.of(1L, 3L)))));
                }
            });
        }

        public static void test5() throws Exception {
            var p = ProgramGen.fileToProgram("test5.datalog");
            p.setupForSimpleSolver();
            var solver = new SimpleSolver(p);
            solver.naiveEval();
            solver.solutionsToPredMap().forEach((key, value) -> {
                switch (p.idToVar.get(key)) {
                    case "hej":
                        assert (value.equals(new HashSet<>(List.of(List.of(1L, 2L)))));
                        break;
                    case "med":
                        assert (value.equals(new HashSet<>(List.of(List.of(7L, 8L)))));
                }
            });
        }

        public static void test6() throws Exception {
            var p = ProgramGen.fileToProgram("test6.datalog");
            p.setupForSimpleSolver();
            var solver = new SimpleSolver(p);
            solver.naiveEval();
            solver.solutionsToPredMap().forEach((key, value) -> {
                switch (p.idToVar.get(key)) {
                    case "hej", "med":
                        assert (value.equals(new HashSet<>(List.of(List.of(1L, 1L)))));
                        break;
                }
            });
        }

        public static void testCombining() throws Exception {
            String s = "f(1,2).f(2,3).r(1,1):-f(X,Y).r(2,2):-f(X,Y).";
            var p = ProgramGen.parseStringToProgram(s);
            p.setupForSimpleSolver();
            var x = new SimpleSolver(p);

            var p_i = p.rules.keySet().stream().findFirst().get();
            var factSet = p.facts.values().stream().findFirst().get();
            List<Set<List<Long>>> arr = new ArrayList<>();
            arr.add(factSet);
            HashSet<List<Long>> res = new HashSet<>();
            List<Long> first = new ArrayList<>();
            first.add(1L);
            first.add(1L);
            res.add(first);
            List<Long> second = new ArrayList<>();
            second.add(2L);
            second.add(2L);
            res.add(second);
            var sols = SimpleSolver.class.getDeclaredField("solutions"); // Reflection
            sols.setAccessible(true);

            assert (x.eval(p_i, (Map<Long, Set<List<Long>>>) sols.get(x)).equals(res));
        }

        public static void testCopyEval() throws Exception {
            String s = "f(1,2).f(2,3).r(X,Y):-f(X,Y).";
            var p = ProgramGen.parseStringToProgram(s);
            p.setupForSimpleSolver();
            var x = new SimpleSolver(p);
            var rule = p.rules.values().stream().findFirst().get().getFirst();
            var factSet = p.facts.values().stream().findFirst().get();
            List<Set<List<Long>>> arr = new ArrayList<>();
            arr.add(factSet);
            HashSet<List<Long>> res = new HashSet<>();
            List<Long> first = new ArrayList<>();
            first.add(1L);
            first.add(2L);
            res.add(first);
            List<Long> second = new ArrayList<>();
            second.add(2L);
            second.add(3L);
            res.add(second);
            assert (x.evalRule(rule, arr).equals(res));
        }

        public static void testConstantEval() throws Exception {
            String s = "f(1,2).f(2,3).r(5,Y):-f(X,Y).";
            var p = ProgramGen.parseStringToProgram(s);
            p.setupForSimpleSolver();
            var x = new SimpleSolver(p);
            var rule = p.rules.values().stream().findFirst().get().getFirst();
            var factSet = p.facts.values().stream().findFirst().get();
            List<Set<List<Long>>> arr = new ArrayList<>();
            arr.add(factSet);
            HashSet<List<Long>> res = new HashSet<>();
            List<Long> first = new ArrayList<>();
            first.add(5L);
            first.add(2L);
            res.add(first);
            List<Long> second = new ArrayList<>();
            second.add(5L);
            second.add(3L);
            res.add(second);
            assert (x.evalRule(rule, arr).equals(res));
        }


        public static void testVariableSelection2() throws Exception {
            String s = "f(1,2).f(2,3).f(6,7).r(X,Y):-f(X,Z),f(Z,Y).";
            var p = ProgramGen.parseStringToProgram(s);
            p.setupForSimpleSolver();
            var x = new SimpleSolver(p);
            var rule = p.rules.values().stream().findFirst().get().getFirst();
            var factSet = p.facts.values().stream().findFirst().get();
            List<Set<List<Long>>> arr = new ArrayList<>();
            arr.add(factSet);
            arr.add(factSet);
            HashSet<ArrayList<List<Long>>> res = new HashSet<>();
            ArrayList<List<Long>> innerRes = new ArrayList<>();
            List<Long> first = new ArrayList<>();
            first.add(1L);
            first.add(2L);
            innerRes.add(first);
            List<Long> second = new ArrayList<>();
            second.add(2L);
            second.add(3L);
            innerRes.add(second);
            res.add(innerRes);
            assert (x.join(rule, arr).equals(res));
        }

        public static void testVariableSelection() throws Exception {
            String s = "f(1,1).f(2,2).f(1,4).f(4,5).f(5,4).r(X,X):-f(X,X).";
            var p = ProgramGen.parseStringToProgram(s);
            p.setupForSimpleSolver();
            var x = new SimpleSolver(p);
            var rule = p.rules.values().stream().findFirst().get().getFirst();
            var factSet = p.facts.values().stream().findFirst().get();
            List<Set<List<Long>>> arr = new ArrayList<>();
            arr.add(factSet);
            HashSet<List<Long>> res = new HashSet<>();
            List<Long> first = new ArrayList<>();
            first.add(1L);
            first.add(1L);
            res.add(first);
            List<Long> second = new ArrayList<>();
            second.add(2L);
            second.add(2L);
            res.add(second);
            assert (x.join(rule, arr).equals(wrapInList(res)));
        }

        public static void testCopySelection2() throws Exception {
            String s = "f(1,2).f(2,3).r(X,Y):-f(X,Y),f(Z,A).";
            var p = ProgramGen.parseStringToProgram(s);
            var x = new SimpleSolver(p);
            p.setupForSimpleSolver();
            var rule = p.rules.values().stream().findFirst().get().getFirst();
            var factSet = p.facts.values().stream().findFirst().get();
            List<Set<List<Long>>> arr = new ArrayList<>();
            arr.add(factSet);
            arr.add(factSet);
            HashSet<ArrayList<List<Long>>> res = new HashSet<>();
            ArrayList<List<Long>> innerRes = new ArrayList<>();
            List<Long> first = new ArrayList<>();
            first.add(1L);
            first.add(2L);
            innerRes.add(first);
            List<Long> second = new ArrayList<>();
            second.add(1L);
            second.add(2L);
            innerRes.add(second);
            res.add(innerRes);
            innerRes = new ArrayList<>();
            first = new ArrayList<>();
            first.add(1L);
            first.add(2L);
            innerRes.add(first);
            second = new ArrayList<>();
            second.add(2L);
            second.add(3L);
            innerRes.add(second);
            res.add(innerRes);
            innerRes = new ArrayList<>();
            first = new ArrayList<>();
            first.add(2L);
            first.add(3L);
            innerRes.add(first);
            second = new ArrayList<>();
            second.add(1L);
            second.add(2L);
            innerRes.add(second);
            res.add(innerRes);
            innerRes = new ArrayList<>();
            first = new ArrayList<>();
            first.add(2L);
            first.add(3L);
            innerRes.add(first);
            second = new ArrayList<>();
            second.add(2L);
            second.add(3L);
            innerRes.add(second);
            res.add(innerRes);
            assert (x.join(rule, arr).equals(res));
        }

        public static void testCopySelection() throws Exception {
            String s = "f(1,2).f(2,3).f(1,4).r(X,Y):-f(X,Y).";
            var p = ProgramGen.parseStringToProgram(s);
            p.setupForSimpleSolver();
            var x = new SimpleSolver(p);
            var rule = p.rules.values().stream().findFirst().get().getFirst();
            var factSet = p.facts.values().stream().findFirst().get();
            List<Set<List<Long>>> arr = new ArrayList<>();
            arr.add(factSet);
            HashSet<List<Long>> res = new HashSet<>();
            List<Long> first = new ArrayList<>();
            first.add(1L);
            first.add(2L);
            res.add(first);
            List<Long> second = new ArrayList<>();
            second.add(1L);
            second.add(4L);
            res.add(second);
            List<Long> third = new ArrayList<>();
            third.add(2L);
            third.add(3L);
            res.add(third);
            assert (x.join(rule, arr).equals(wrapInList(res)));
        }

        public static void testConstantSelection() throws Exception {
            String s = "f(1,2).f(2,3).f(1,4).r(X):-f(1,X).";
            var p = ProgramGen.parseStringToProgram(s);
            p.setupForSimpleSolver();
            var x = new SimpleSolver(p);
            var rule = p.rules.values().stream().findFirst().get().getFirst();
            var factSet = p.facts.values().stream().findFirst().get();
            List<Set<List<Long>>> arr = new ArrayList<>();
            arr.add(factSet);
            HashSet<List<Long>> res = new HashSet<>();
            List<Long> first = new ArrayList<>();
            first.add(1L);
            first.add(2L);
            res.add(first);
            List<Long> second = new ArrayList<>();
            second.add(1L);
            second.add(4L);
            res.add(second);
            assert (x.join(rule, arr).equals(wrapInList(res)));
        }

        public static void testCartesianOnlyTwo() {
            var testSolver = new SimpleSolver(dummyProgram);
            List<Set<List<Long>>> currentTest = new ArrayList<>();
            Set<List<Long>> first = createSimpleSet();
            currentTest.add(first);
            Set<List<Long>> second = createSimpleSet();
            currentTest.add(second);
            var x = testSolver.cartesianProduct(currentTest);
            assert (x.equals(createCartesianOfTwoSimple()));
        }

        public static void testCartesianOnlyOne() {
            var testSolver = new SimpleSolver(dummyProgram);
            List<Set<List<Long>>> currentTest = new ArrayList<>();
            Set<List<Long>> first = createSimpleSet();
            currentTest.add(first);
            var x = testSolver.cartesianProduct(currentTest);
            assert (x.equals(wrapInList(createSimpleSet())));
        }
    }
}
