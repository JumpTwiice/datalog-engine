import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/*
*
let res = '';
let n = 100;
for(let i = 0; i < 20; i++) {
    res += `person(${i}).person(${i+100}).`
}
res += `person(${n}).`
for(let i = 0; i < n; i++) {
    res += `parent(${i+100},${i}).`
}
for(let i = 0; i < n; i+=4) {
    res += `parent(${i+100+1},${i}).parent(${i+100+2},${i}).parent(${i+100+3},${i}).`
}
*
* */

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println(System.getProperty("user.dir"));
//        String projectPath = "C:\\Users\\caspe\\OneDrive\\Skrivebord\\Uni\\9. semester\\Programming languages\\datalog-engine\\";
        String projectPath = "C:\\Users\\adamt\\Desktop\\datalog-engine\\";
//        var is = new FileInputStream(projectPath + "src\\test\\test1.datalog");
        var is = new FileInputStream(projectPath + "src\\test\\test2.datalog");
//        var is = new FileInputStream(projectPath + "src\\test\\MagicSetsOriginal.datalog");
//        var is = new FileInputStream(projectPath + "src\\test\\MagicSetsMagic.datalog");
//        "src/test/test1.datalog"

        var parser = new Parser(is);
        var p = parser.parse();
//        Checker.checkProgram(p);
        solver.Transformer.setEqSet(p);
//        var solution = Solver.naiveEval(p);
        var solution = solver.Solver.semiNaiveEval(p);
        for (var x: solution.keySet()) {
            System.out.println(p.idToVar.get(x));
//            System.out.println(x);
            System.out.println(solution.get(x));
        }
//        System.out.println(p.facts.get(0).ids.get(0).value);
//        System.out.println(p.facts.get(0).ids.get(0).value);
        is.close();

    }

}
