import solver.TrieSolver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public static void main(String[] args) throws Exception {
//        presentationTest();
//        presentationTestTree();
//        System.exit(0);

        String projectPath = System.getProperty("user.dir") + "\\src\\test\\";
//        var is = new FileInputStream(projectPath + "test1.datalog");
//        var is = new FileInputStream(projectPath + "test2.datalog");
        var is = new FileInputStream(projectPath + "test3.datalog");
//        var is = new FileInputStream(projectPath + "MagicSetsOriginal.datalog");
//        "src/test/test1.datalog"

        var parser = new Parser(is);
        var p = parser.parse();
//        Checker.checkProgram(p);
        var solver = new TrieSolver(p);
        var x = solver.naiveEval();
        for(var id: x.map.keySet()) {
            System.out.println(p.idToVar.get(id));
//            System.out.println(id);
            System.out.println(x.map.get(id).leaves);
        }


//        solver.Transformer.setEqSet(p);
//        solver.Transformer.setEqSet2(p);
////        var solution = Solver.naiveEval(p);
//        var solution = solver.Solver.semiNaiveEval(p);
//        for (var x: solution.keySet()) {
//            System.out.println(p.idToVar.get(x));
////            System.out.println(x);
//            System.out.println(solution.get(x));
//        }
//        System.out.println(p.facts.get(0).ids.get(0).value);
//        System.out.println(p.facts.get(0).ids.get(0).value);
        is.close();

    }

    public static void presentationTest() throws Exception {
        String projectPath = System.getProperty("user.dir") + "\\src\\test\\";
        for (var filePath: List.of("MagicSetsMagic.datalog", "MagicSetsOriginal.datalog")) {
            long time = System.currentTimeMillis();
            var is = new FileInputStream(projectPath + filePath);
            var parser = new Parser(is);
            var p = parser.parse();
            solver.Transformer.setEqSet(p);
            solver.Transformer.setEqSet2(p);
            var solution = solver.Solver.semiNaiveEval(p);
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
            solver.Transformer.setEqSet2(p);
            var solution = solver.Solver.semiNaiveEval(p);
            for (var x: solution.keySet()) {
                System.out.println(p.idToVar.get(x));
                System.out.println(solution.get(x));
            }

            System.out.println("Computed using " + filePath.substring("MagicSets".length(), filePath.indexOf('_')) + ". Took: " + ((System.currentTimeMillis() - time) / 1000) + " seconds");
        }
    }
}
