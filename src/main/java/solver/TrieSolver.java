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
//        TrieMap solutions = new TrieMap(p);
        boolean done;
        do {
//            System.out.println("ITERATING");
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
        solutions.meld(deltaSolutions.cloneTrieSet());
//        solutions = deltaSolutions.cloneTrieSet();

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
//        var sol = new SimpleTrie(p.rules.get(p_i).size());
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
//        System.out.println("JOINING");
//        System.out.println(relations);
//        System.out.println(r.toString(p));
        var join = join(r, relations);
//        System.out.println("JOIN");
//        System.out.println(join);
        if (join == null) {
            return null;
        }
//        System.out.println("PROJECTION");
//        System.out.println(join.projectTo(r));
        return join.projectTo(r);
    }


//    public SimpleTrie evalRule(Rule r, TrieMap solutions) {
//        if(r.body.stream().anyMatch(x -> solutions.get(x.pred) == null)) {
//            return null;
//        }
//        var join = join(r, solutions);
//        if (join == null) {
//            return null;
//        }
//        return join.projectTo(r);
//    }

    public SimpleTrie evalRuleIncremental(Rule r, TrieMap solutions, TrieMap newSolutions) {
        SimpleTrie result = null;
        var sols = r.body.stream().map(x -> solutions.get(x.pred)).collect(Collectors.toCollection(ArrayList::new));
        for (var i = 0; i < sols.size(); i++) {
            var atomID = r.body.get(i).pred;
            sols.set(i, newSolutions.get(atomID));
//            solutions.put(x, newSolutions.get(x));
//            var res = evalRule(r, sols);
            result = solutions.meldSimpleTries(result, evalRule(r, sols));
            sols.set(i, solutions.get(atomID));
        }
//        var result = meaningFullRelations.stream().map(x -> {
////        var result = p.rules.keySet().stream().map(x -> {
////        var result = solutions.keySet().stream().map(x -> {
////            TODO: If we want to do it in parallel we need to make a shallow clone of the map.
//            var temp = sols.get(x);
//            solutions.put(x, newSolutions.get(x));
//            var res = evalRule(r, solutions);
//            solutions.put(x, temp);
//            return res;
//        }).reduce(null, solutions::meldSimpleTries);

        return result;
    }

//    public SimpleTrie join(Rule r, List<SimpleTrie> solutions) {
//        return generateConstraints(r, solutions);
//    }

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
        var prev = TrieMap.cloneForTrie(source, constBool, constArr, sameArr);
        for (var i = 1; i < solutions.size(); i++) {
            prev = TrieMap.combine(prev, r.body.get(i), solutions.get(i), r);
            if (prev == null) {
                return null;
            }
        }
        return prev;
    }
}
