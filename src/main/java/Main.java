import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws Exception {
        String projectPath = "C:\\Users\\adamt\\Desktop\\datalog-engine\\";
        //var is = new FileInputStream(projectPath + "src\\test\\test1.datalog");
        var is = new FileInputStream(projectPath + "src\\test\\test2.datalog");
//        "src/test/test1.datalog"

        var parser = new Parser(is);
        var p = parser.parse();
        Transformer.setEqSet(p);
//        var solution = Solver.naiveEval(p);
        var solution = Solver.semiNaiveEval(p);
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
