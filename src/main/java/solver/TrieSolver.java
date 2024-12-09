package solver;

import ast.Program;
import ast.Rule;

import java.util.*;
import java.util.stream.Collectors;

public class TrieSolver implements Solver<SimpleTrie> {

    private Program p;
    private TrieMap solutions;

    public TrieSolver(Program p) {
        this.p = p;
        solutions = new TrieMap(p);
    }

    public TrieMap naiveEval() {
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
        TrieMap deltaSolutions = new TrieMap(p);
        for (var p_i : p.rules.keySet()) {
            deltaSolutions.put(p_i, eval(p_i, solutions));
        }
        solutions.meld(deltaSolutions.cloneTrieSet());

        boolean done;
        do {
            TrieMap deltaPrimeSolutions = deltaSolutions;
            deltaSolutions = new TrieMap(p);

            for (var p_i : p.rules.keySet()) {
                deltaSolutions.put(p_i, evalIncremental(p_i, solutions, deltaPrimeSolutions));
            }
            deltaSolutions.removeAll(solutions);
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
        var sol = new SimpleTrie(-1);
        for (var r : p.rules.get(p_i)) {
            sol.meld(evalRule(r, r.body.stream().map(x -> solutions.get(x.pred)).toList()));
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

    public SimpleTrie evalRule(Rule r, List<SimpleTrie> relations) {
        if (relations.stream().anyMatch(Objects::isNull)) {
            return null;
        }
        var join = join(r, relations);
        if (join == null) {
            return null;
        }
        return join.projectTo(r);
    }

    public SimpleTrie evalRuleIncremental(Rule r, TrieMap solutions, TrieMap newSolutions) {
        SimpleTrie result = null;
        var sols = r.body.stream().map(x -> solutions.get(x.pred)).collect(Collectors.toCollection(ArrayList::new));
        for (var i = 0; i < sols.size(); i++) {
            var atomID = r.body.get(i).pred;
            sols.set(i, newSolutions.get(atomID));
            result = solutions.meldSimpleTries(result, evalRule(r, sols));
            sols.set(i, solutions.get(atomID));
        }
        return result;
    }

    /**
     * Iteratively builds the trie for the rule.
     *
     * @param r
     * @param solutions
     * @return
     */
    public SimpleTrie join(Rule r, List<SimpleTrie> solutions) {
        var atom = r.body.getFirst();
        var constBool = atom.constBool;
        var constArr = atom.constArr;
        var sameArr = atom.sameNess;
        var source = solutions.getFirst();
        if (source == null) {
            return null;
        }
        var prev = TrieMap.query(source, constBool, constArr, sameArr);
        for (var i = 1; i < solutions.size(); i++) {
            prev = TrieMap.combine(prev, r.body.get(i), solutions.get(i), r);
            if (prev == null) {
                return null;
            }
        }
        return prev;
    }
}
