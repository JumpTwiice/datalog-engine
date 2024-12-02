package solver;

import ast.Program;
import ast.Rule;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrieSolver implements Solver<SimpleTrie> {

    private Program p;
    private TrieMap solutions;

    public TrieSolver(Program p) {
        this.p = p;
        solutions = new TrieMap(p);
    }

    public TrieMap naiveEval() {
//        TrieMap solutions = new TrieMap(p);
        boolean done;
        do {
            TrieMap newSolutions = new TrieMap(p);
            for (var p_i : p.rules.keySet()) {
                newSolutions.put(p_i, eval(p_i, solutions));
            }

            done = newSolutions.subsetOf(solutions);
            // Add all new solutions to the old.
            if (!done) {
                solutions.meld(newSolutions);
            }

        } while (!done);
        return solutions;
    }

    public TrieMap semiNaiveEval() {
//        Map<Long, Set<List<Long>>> solutions = initializeSolutions(p);
//        TrieMap solutions = new TrieMap(p);
        TrieMap deltaSolutions = new TrieMap(p);
        for (var p_i : p.rules.keySet()) {
            deltaSolutions.put(p_i, eval(p_i, solutions));
        }
        solutions = deltaSolutions.cloneTrieSet();

        boolean done;
        do {
//            done = true;
            TrieMap deltaPrimeSolutions = deltaSolutions;
            deltaSolutions = new TrieMap(p);

            for (var p_i : p.rules.keySet()) {
                deltaSolutions.put(p_i, evalIncremental(p_i, solutions, deltaPrimeSolutions));
//                deltaSolutions.get(p_i);
            }
            // Add all new solutions to the old.
            done = !solutions.meld(deltaSolutions);
        } while (!done);
        return solutions;
    }

    // TODO: Do this smarter
    @Override
    public TrieSolver resetWithProgram(Program p) {
        return new TrieSolver(p);
    }

    @Override
    public Solver<SimpleTrie> resetWithProgramAndFacts(Program p, Map<Long, SimpleTrie> facts) {
        var res = new TrieSolver(p);
        res.solutions = (TrieMap) facts;
        return res;
    }

    @Override
    public Map<Long, Set<List<Long>>> solutionsToPredMap() {
        return solutions.solutionsToPredMap();
    }

    public SimpleTrie eval(long p_i, TrieMap solutions) {
//        var sol = new SimpleTrie(p.rules.get(p_i).size());
        var sol = new SimpleTrie(-1);
        for (var r : p.rules.get(p_i)) {
            sol.meld(evalRule(r, solutions));
        }
        if (sol.leaves == null && sol.children == null) {
            return null;
        }
        return sol;
    }

    public SimpleTrie evalIncremental(long p_i, TrieMap solutions, TrieMap deltaSolutions) {
        SimpleTrie sol = null;
        for (var r : p.rules.get(p_i)) {
            sol = solutions.meldSimpleTries(sol, evalRuleIncremental(r, solutions, deltaSolutions));
        }
        return sol;
    }


    public SimpleTrie evalRule(Rule r, TrieMap solutions) {
        var join = join(r, solutions);
        if (join == null) {
            return null;
        }
        return join.projectTo(r);
    }

    public SimpleTrie evalRuleIncremental(Rule r, TrieMap solutions, TrieMap newSolutions) {
//        TODO: Should not be over solutions, but rules.
        var result = p.rules.keySet().stream().map(x -> {
//        var result = solutions.keySet().stream().map(x -> {
//            TODO: If we want to do it in parallel we need to make a shallow clone of the map.
            var temp = solutions.get(x);
            solutions.put(x, newSolutions.get(x));
            var res = evalRule(r, solutions);
            solutions.put(x, temp);
            return res;
        }).reduce(null, solutions::meldSimpleTries);

        return result;
    }

    public SimpleTrie join(Rule r, TrieMap solutions) {
        return generateConstraints(r, solutions, r.body.size() - 1);
    }

    /**
     * Recursively builds the trie for the rule.
     *
     * @param r
     * @param solutions
     * @param i
     * @return
     */
    public SimpleTrie generateConstraints(Rule r, TrieMap solutions, int i) {
        var atom = r.body.get(i);
        var boolConst = atom.getBoolAndConstArr();
        var constBool = boolConst.x();
        var constArr = boolConst.y();
        if (i == 0) {
            var source = solutions.get(r.body.getFirst().pred);
            if (source == null) {
                return null;
            }
            return solutions.cloneForTrie(r.body.getFirst(), constBool, constArr);
        }
        var prev = generateConstraints(r, solutions, i - 1);
        return solutions.combine(prev, r.body.get(i), r);
    }
}
