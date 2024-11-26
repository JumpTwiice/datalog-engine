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
                newSolutions.map.put(p_i, eval(p_i, solutions));
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
            deltaSolutions.map.put(p_i, eval(p_i, solutions));
        }
        solutions = deltaSolutions.cloneTrieSet();

        boolean done;
        do {
//            done = true;
            TrieMap deltaPrimeSolutions = deltaSolutions;
            deltaSolutions = new TrieMap(p);

            for (var p_i : p.rules.keySet()) {
                deltaSolutions.map.put(p_i, evalIncremental(p_i, solutions, deltaPrimeSolutions));
//                deltaSolutions.map.get(p_i);
            }
            // Add all new solutions to the old.
            done = !solutions.meld(deltaSolutions);
        } while (!done);
        return solutions;
    }

    // TODO
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
//        if(p.idToVar.get(p_i).equals("reachable")) {
//            System.out.println(sol);
//        }
        return sol;
    }

    public SimpleTrie evalIncremental(long p_i, TrieMap solutions, TrieMap deltaSolutions) {
//        Set<List<Long>> sol = new HashSet<>();
        SimpleTrie sol = null;
        for (var r : p.rules.get(p_i)) {
            sol = solutions.meldSimpleTries(sol, evalRuleIncremental(r, solutions, deltaSolutions));
//            sol.meld(evalRuleIncremental(r, solutions, deltaSolutions));
        }
        return sol;
    }


    public SimpleTrie evalRule(Rule r, TrieMap solutions) {
        var join = join(r, solutions);
        if (join == null) {
            return null;
        }
//        System.out.println(join);
//        System.out.println(p.idToVar.get(r.head.pred));
        return join.projectTo(r);
    }

    public SimpleTrie evalRuleIncremental(Rule r, TrieMap solutions, TrieMap newSolutions) {
//TODO        var newNewSolutions = solutions.initializeEmptyTrie();
//        SimpleTrie newNewSolutions = null;

//        var newNewSolutions = new SimpleTrie(-1);
//        TODO: Should not be over solutions, but rules.
        var result = solutions.map.keySet().stream().map(x -> {
//            TODO: If we want to do it in parallel we need to make a shallow clone of the map.
            var temp = solutions.map.get(x);
            solutions.map.put(x, newSolutions.map.get(x));
            var res = evalRule(r, solutions);
            solutions.map.put(x, temp);
            return res;
        }).reduce(null, solutions::meldSimpleTries);

        return result;


//        solutions.map.keySet().forEach(x -> {
//            var temp = solutions.map.get(x);
//            solutions.map.put(x, newSolutions.map.get(x));
//            newNewSolutions = solutions.meldSimpleTries(newNewSolutions, evalRule(r, solutions));
////            newNewSolutions.meld(evalRule(r, solutions));
//            solutions.map.put(x, temp);
//        });
//        return newNewSolutions;
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
        var constBool = new boolean[atom.ids.size()];
        var constArr = new long[atom.ids.size()];
//      Unsure of whether branch prediction will be a problem, but might as well try to avoid it.
        for (int j = 0; j < constBool.length; j++) {
            constBool[j] = !atom.ids.get(j).isVar;
            constArr[j] = atom.ids.get(j).value;
        }
        if (i == 0) {
            var source = solutions.map.get(r.body.getFirst().pred);
            if (source == null) {
                return null;
            }
            return solutions.cloneForTrie(r.body.getFirst(), constBool, constArr);
        }
        var prev = generateConstraints(r, solutions, i - 1);
        if (prev == null) {
            return null;
        }
//        if(r.head.pred == -2) {
//            System.out.println("Before");
//            System.out.println(prev.leaves);
//        }
        return solutions.combine(prev, r.body.get(i), r);
//        return solutions.combine(prev, r.body.get(i), r);
//        if(r.head.pred == -2) {
//            System.out.println("After");
//            System.out.println(prev.leaves);
//        }
//        return prev;
    }
}
