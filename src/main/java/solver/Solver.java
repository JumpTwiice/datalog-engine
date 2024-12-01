package solver;

import ast.Program;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Solver<V> {
    Map<Long, V> naiveEval();
    Map<Long, V> semiNaiveEval();
    Solver<V> resetWithProgram(Program p);
    Solver<V> resetWithProgramAndFacts(Program p, Map<Long, V> facts);
    Map<Long, Set<List<Long>>> solutionsToPredMap();

    static String formatSolution(Map<Long, Set<List<Long>>> solutions, Program p) {
        List<String> preds = new ArrayList<>();
        for (var id: solutions.keySet()) {
            var tuples = solutions.get(id);
            StringBuilder out = new StringBuilder();
            for (List<Long> tuple: tuples) {
                out.append("(");
                for (long constant: tuple) {
                    out.append(constant).append(",");
                }
                out = new StringBuilder(out.substring(0, out.length() - 1));
                out.append("), ");
            }
            var toAdd = p.idToVar.get(id) + " = {";
            if(!out.isEmpty()) {
                System.out.println(out);
                toAdd += out.substring(0, out.length() - 2);
            }
            toAdd +=  "}";
            preds.add(toAdd);
        }
        return String.join("\n", preds);
    }
}
