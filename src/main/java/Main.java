import solver.*;

import java.io.FileInputStream;
import java.util.List;

/*
let n = 100;
let res = '';
for(let i = 0; i < n; i++) {
    res += `edge(${i},${i+1}).`
}
res += 'edge(X,Y) :- edge(Y,X).\n';
res+='reachable(X,Y) :- edge(X,Y).\n';
res+='reachable(X,Y) :- reachable(X,Z), reachable(Z,Y).';
console.log(res);



 */


/*
*
let res = '';
let n = 60;
for(let i = 0; i < n; i++) {
    res += `person(${i}).person(${i+100}).`
}
res += `person(${n}).`
for(let i = 0; i < n; i+=4) {
    res += `parent(${i},${i+100}).parent(${i+1},${i+100}).parent(${i+2},${i+100}).parent(${i+3},${i+100}).`
}
for(let i = 4; i < n; i++) {
    res += `parent(${i},${101}).`
}
res;





let res = '';
function generateTree(height, counter) {
    let self = counter;
    res += `person(${counter}).`;
    if(height === 0) {
        return counter + 1;
    }
    res += `parent(${++counter}, ${self}).`;
    counter = generateTree(height - 1, counter);
    res += `parent(${++counter}, ${self}).`;
    counter = generateTree(height - 1, counter);
    return counter;
}

generateTree(6, 0);
res;

*
* */

public class Main {
    public static String projectPath = System.getProperty("user.dir") + "\\src\\test\\";

    public static void main(String[] args) throws Exception {
//        presentationTest();
//        presentationTestTree();
//        System.exit(0);
//        runAllTests();
//        System.exit(0);

//        String projectPath = System.getProperty("user.dir") + "\\src\\test\\";
        var is = new FileInputStream(projectPath + "test1.datalog");
//        var is = new FileInputStream(projectPath + "test2.datalog");
//        var is = new FileInputStream(projectPath + "test3.datalog");
//        var is = new FileInputStream(projectPath + "test4.datalog");
//        var is = new FileInputStream(projectPath + "test5.datalog");
//        var is = new FileInputStream(projectPath + "test6.datalog");
//        var is = new FileInputStream(projectPath + "test7.datalog");
//        var is = new FileInputStream(projectPath + "test8.datalog");
//        var is = new FileInputStream(projectPath + "MagicSetsOriginal.datalog");
//        var is = new FileInputStream(projectPath + "MagicSetsMagic.datalog");
//        "src/test/test1.datalog"

        var parser = new Parser(is);
        var p = parser.parse();
        System.out.println(p);
//        System.out.println("IDTOVAR");
//        System.out.println(p.idToVar);
//        p = Transformer.magicSets(p);
//        System.out.println(p.idToVar);
        System.out.println(p);
//        System.out.println(p.rules);
//        System.out.println(p.facts);

        p.setupPositionsForRules();
//        Transformer.setEqSet(p);
//        Checker.checkProgram(p);
//        var solver = new SCCSolverDecorator<>(p, new SimpleSolver(p));
//        var solver = new SCCSolverDecorator<>(p, new TrieSolver(p));
        var solver = new TrieSolver(p);
//        var solver = new SimpleSolver(p);
//        var x = solver.naiveEval();
        var x = solver.semiNaiveEval();
//        System.out.println(x.map);
        var predMap = solver.solutionsToPredMap();
        System.out.println(Solver.formatSolution(predMap, p));
//        for(var id: x.keySet()) {
//            System.out.println(p.idToVar.get(id));
////            System.out.println(id);
//            System.out.println(x.get(id));
//        }
        is.close();
    }

    private static void runAllTests() throws Exception {
        var x = new String[] {
                "test1.datalog",
                "test2.datalog",
                "test3.datalog",
                "test4.datalog",
                "test5.datalog",
                "test6.datalog",
                "test7.datalog",};
        for(var i = 0; i < x.length; i++) {
            System.out.println("RUNNING ON " + x[i]);
            var is = new FileInputStream(projectPath + x[i]);
            var parser = new Parser(is);
            var p = parser.parse();
            p = Transformer.magicSets(p);
            p.setupPositionsForRules();
            var solver = new TrieSolver(p);
            solver.semiNaiveEval();
            solver.solutionsToPredMap();
        }
    }

    public static void presentationTest() throws Exception {
        String projectPath = System.getProperty("user.dir") + "\\src\\test\\";
        for (var filePath: List.of("MagicSetsMagic.datalog", "MagicSetsOriginal.datalog")) {
            long time = System.currentTimeMillis();
            var is = new FileInputStream(projectPath + filePath);
            var parser = new Parser(is);
            var p = parser.parse();
            solver.Transformer.setEqSet(p);
            var solver = new SimpleSolver(p);
            var solution = solver.semiNaiveEval();
            for (var x: solution.keySet()) {
                System.out.println(p.idToVar.get(x));
                System.out.println(solution.get(x));
            }
            System.out.println("Computed using " + filePath.substring("MagicSets".length(), filePath.indexOf('.')) + ". Took: " + ((System.currentTimeMillis() - time) / 1000) + " seconds");
        }
    }

    public static void presentationTestTree() throws Exception {
        String projectPath = System.getProperty("user.dir") + "\\src\\test\\";
        for (var filePath: List.of("MagicSetsMagic_tree.datalog", "MagicSetsOriginal_tree.datalog")) {
            long time = System.currentTimeMillis();
            var is = new FileInputStream(projectPath + filePath);
            var parser = new Parser(is);
            var p = parser.parse();
            solver.Transformer.setEqSet(p);
            SimpleSolver solver = new SimpleSolver(p);
            var solution = solver.semiNaiveEval();
            for (var x: solution.keySet()) {
                System.out.println(p.idToVar.get(x));
                System.out.println(solution.get(x));
            }

            System.out.println("Computed using " + filePath.substring("MagicSets".length(), filePath.indexOf('_')) + ". Took: " + ((System.currentTimeMillis() - time) / 1000) + " seconds");
        }
    }
}
