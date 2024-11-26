package solver;

import ast.Program;
import graph.Graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static graph.Algorithms.computeSCCOrder;

public class SCCSolverDecorator<V> implements Solver<V> {
    Program p;
    Solver<V> childSolver;
    public SCCSolverDecorator(Program p, Solver<V> childSolver) {
        this.p = p;
        this.childSolver = childSolver;
    }

    private Map<Long, V> evalWithFunc(Function<Solver<V>, Map<Long, V>> evalFunc) {
        Graph g;
        try {
            g = graph.Util.graphFromProgram(p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        var components = computeSCCOrder(g);

        var clone = p.cloneProgram();
        clone.rules = new HashMap<>();
        childSolver.resetWithProgram(clone);
//        TODO: A method: Transform/initialize with facts would be more nice
        var childFacts = evalFunc.apply(childSolver);
//        var childFacts = childSolver.naiveEval();
        for (var cur : components) {
            for (var v : cur) {
                if(p.rules.containsKey((long)v)) {
                    clone.rules.put((long) v, p.rules.get((long) v));
                }
            }
            childSolver = childSolver.resetWithProgramAndFacts(clone, childFacts);
            childFacts = evalFunc.apply(childSolver);
//            childFacts = childSolver.naiveEval();
            for (var v : cur) {
                clone.rules.remove((long) v);
            }
        }
        return childFacts;
    }

    @Override
    public Map<Long, V> naiveEval() {
        return evalWithFunc(Solver::naiveEval);
    }

    @Override
    public Map<Long, V> semiNaiveEval() {
        return evalWithFunc(Solver::semiNaiveEval);
//        return null;
    }

    @Override
    public SCCSolverDecorator<V> resetWithProgram(Program p) {
        return null;
    }

    @Override
    public Solver<V> resetWithProgramAndFacts(Program p, Map<Long, V> facts) {
        return null;
    }

    @Override
    public Map<Long, Set<List<Long>>> solutionsToPredMap() {
        return childSolver.solutionsToPredMap();
    }
}
