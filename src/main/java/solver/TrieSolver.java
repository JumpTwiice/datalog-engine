package solver;

import ast.Program;
import ast.Rule;

import java.util.*;

public class TrieSolver {

    private Program p;

    public TrieSolver(Program p) {
        this.p = p;
    }

    public TrieSet naiveEval() {
        TrieSet solutions = new TrieSet(p);
        boolean done;
        System.out.println(p.idToVar);
        do {
            TrieSet newSolutions = new TrieSet(p);
            for(var p_i: p.rules.keySet()) {
                newSolutions.map.put(p_i, eval(p_i, solutions));
            }

            done = newSolutions.subsetOf(solutions);
            // Add all new solutions to the old.
            if(!done) {
                solutions.meld(newSolutions);
            }

        } while(!done);
        return solutions;
    }

    public TrieSet semiNaiveEval() {
//        Map<Long, Set<List<Long>>> solutions = initializeSolutions(p);
        TrieSet solutions = new TrieSet(p);
        TrieSet deltaSolutions =  new TrieSet(p);
        for (var p_i: p.rules.keySet()) {
            deltaSolutions.map.put(p_i, eval(p_i, solutions));
        }
        TrieSet temp = deltaSolutions;
        deltaSolutions.map.keySet().forEach(e -> {
//            TODO: Maybe not like this.
                    var x = new SimpleTrie(-1);
                    x.cloneIfEmpty(temp.map.get(e));
                    solutions.map.put(e, x);
                }
        );

        boolean done;
        do {
            System.out.println("SOLUTIONS");
            System.out.println(solutions);
//            done = true;
            TrieSet deltaPrimeSolutions = deltaSolutions;
            System.out.println(deltaPrimeSolutions.map);
            deltaSolutions = new TrieSet(p);

            for (var p_i: p.rules.keySet()) {
                deltaSolutions.map.put(p_i, evalIncremental(p_i, solutions, deltaPrimeSolutions));
            }
            // Add all new solutions to the old.
            done = !solutions.meld(deltaSolutions);
        } while(!done);
        return solutions;
    }

    public SimpleTrie eval(long p_i, TrieSet solutions) {
//        var sol = new SimpleTrie(p.rules.get(p_i).size());
        var sol = new SimpleTrie(-1);
        for (var r: p.rules.get(p_i)) {
            sol.meld(evalRule(r, solutions));
        }
        if(sol.leaves == null && sol.children == null) {
            return null;
        }
//        if(p.idToVar.get(p_i).equals("reachable")) {
//            System.out.println(sol);
//        }
        return sol;
    }

    public SimpleTrie evalIncremental(long p_i, TrieSet solutions, TrieSet deltaSolutions) {
//        Set<List<Long>> sol = new HashSet<>();
        var sol = new SimpleTrie(-1);
        for (var r: p.rules.get(p_i)) {
            sol.meld(evalRuleIncremental(r, solutions, deltaSolutions));
        }
        return sol;
    }



    public SimpleTrie evalRule(Rule r, TrieSet solutions) {
        var join = join(r, solutions);
        if(join == null) {
            return null;
        }
        System.out.println(join);
        System.out.println(p.idToVar.get(r.head.pred));
        return join.projectTo(r);
    }

    public SimpleTrie evalRuleIncremental(Rule r, TrieSet solutions, TrieSet newSolutions) {
        var newNewSolutions = new SimpleTrie(-1);
//        TODO: Should not be over solutions, but rules.
        solutions.map.keySet().forEach(x -> {
            var temp = solutions.map.get(x);
            solutions.map.put(x, newSolutions.map.get(x));
            newNewSolutions.meld(evalRule(r, solutions));
            solutions.map.put(x, temp);
        });
        return newNewSolutions;
    }

    public SimpleTrie join(Rule r, TrieSet solutions) {
        return generateConstraints(r, solutions, r.body.size() - 1);
    }

    /**
     * Recursively builds the trie for the rule.
     * @param r
     * @param solutions
     * @param i
     * @return
     */
    public SimpleTrie generateConstraints(Rule r, TrieSet solutions, int i) {
        var atom = r.body.get(i);
        var constBool = new boolean[atom.ids.size()];
        var constArr = new long[atom.ids.size()];
//      Unsure of whether branch prediction will be a problem, but might as well try to avoid it.
        for (int j = 0; j < constBool.length; j++) {
            constBool[j] = !atom.ids.get(j).isVar;
            constArr[j] = atom.ids.get(j).value;
        }
        if(i == 0) {
            var source = solutions.map.get(r.body.getFirst().pred);
            if(source == null) {
                return null;
            }
            System.out.println(p.idToVar.get(r.body.getFirst().pred));
            System.out.println(source);
            return solutions.cloneForTrie(r.body.getFirst(), constBool, constArr);
        }
        var prev = generateConstraints(r, solutions, i-1);
        if(prev == null) {
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
