import solver.*;

import java.io.FileInputStream;

/*
let n = 100;
let res = '';
for(let i = 0; i < n; i++) {
    res += `edge(${i},${i+1}).`;
}
res += '\nedge(X,Y) :- edge(Y,X).\n';
res+='reachable(X,Y) :- edge(X,Y).\n';
res+='reachable(X,Y) :- reachable(X,Z), reachable(Z,Y).';
res+='?-reachable(X,3)';
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

*/

public class Main {
    public static String testPath = System.getProperty("user.dir") + "/src/test/";

    public static void main(String[] args) throws Exception {
//        Testing.runAllTests();
//        System.exit(0);


//        runAllTests();
//        System.exit(0);
//        TODO: Bug when a variable occurs for the first time and multiple times in an atom.
//         Solution ala project. Move to precomputation on atoms also.
//        beepTest();
//        System.exit(0);

//        var is = new FileInputStream(projectPath + "test1.datalog");
        var is = new FileInputStream(testPath + "test2.datalog");
//        var is = new FileInputStream(projectPath + "test3.datalog");
//        var is = new FileInputStream(projectPath + "test4.datalog");
//        var is = new FileInputStream(projectPath + "test5.datalog");
//        var is = new FileInputStream(projectPath + "test6.datalog");
//        var is = new FileInputStream(projectPath + "test7.datalog");
//        var is = new FileInputStream(projectPath + "test8.datalog");
//        var is = new FileInputStream(projectPath + "test9.datalog");
//        var is = new FileInputStream(projectPath + "test10.datalog");
//        var is = new FileInputStream(projectPath + "MagicSetsOriginal.datalog");
//        var is = new FileInputStream(projectPath + "MagicSetsMagic.datalog");

        var parser = new Parser(is);
        var p = parser.parse();
        System.out.println(p);
        p = Transformer.magicSets(p);
        System.out.println(p);
        System.exit(0);

        p.setupForTrieSolver();
//        p.setupForSimpleSolver();
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
