package solver;

import ast.Program;
import ast.Rule;

import java.util.*;
import java.util.stream.Collectors;

public abstract class GenericSolver<D extends DataStructure> {

    private Program p;/*

    public GenericSolver(Program p) {
        this.p = p;
    }

    public abstract D initializeEmpty();

    public Map<Long, Set<List<Long>>> naiveEval() {
        D solutions = initializeEmpty();
        boolean done;
        do {
            done = true;
            D newSolutions = initializeEmpty();
            for(var r: p.rules.keySet()) {
                newSolutions.put(r, eval(r, solutions));
            }
            loop: for (var x: newSolutions.keySet()) {
                var set = newSolutions.get(x);
                for(var y: set) {
                    if(!solutions.get(x).contains(y)) {
                        done = false;
                        break loop;
                    }
                }
            }
            // Add all new solutions to the old.
            if(!done) {
                for (var x: newSolutions.keySet()) {
                    solutions.get(x).addAll(newSolutions.get(x));
                }
            }
        } while(!done);
        return solutions;
    }

    public Map<Long, Set<List<Long>>> semiNaiveEval(Program p) {
//        Map<Long, Set<List<Long>>> solutions = initializeSolutions(p);
        D solutions = initializeEmpty();
        D deltaSolutions = initializeEmpty();
        for (var p_i: p.rules.keySet()) {
            deltaSolutions.put(p_i, eval(p_i, solutions));
        }
        Map<Long, Set<List<Long>>> temp = deltaSolutions;
        deltaSolutions.keySet().forEach(e ->
                {
                    solutions.putIfAbsent(e, new HashSet<>());
                    solutions.get(e).addAll(temp.get(e));
                }
        );
//        solutions = deltaSolutions;


        boolean done;
        do {
            done = true;
            Map<Long, Set<List<Long>>> deltaPrimeSolutions = deltaSolutions;
            deltaSolutions = new HashMap<>();

            for (var p_i: p.rules.keySet()) {
                deltaSolutions.put(p_i, evalIncremental(p_i, solutions, deltaPrimeSolutions));
                deltaSolutions.get(p_i).removeAll(solutions.get(p_i));
            }
            for (var x: deltaSolutions.keySet()) {
                var set = deltaSolutions.get(x);
                if(deltaSolutions.get(x).size() != 0) {
                    done = false;
                    break;
                }
            }
            // Add all new solutions to the old.
            if(!done) {
                for (var p_i: p.rules.keySet()) {
                    solutions.get(p_i).addAll(deltaSolutions.get(p_i));
                }
//                Ugly. Add all built in facts to deltaSolutions.
                Map<Long, Set<List<Long>>> temp_ = deltaSolutions;
            }
        } while(!done);
        return solutions;
    }

    public D eval(long p_i, Map<Long, Set<List<Long>>> solutions) {
        Set<List<Long>> sol = new HashSet<>();
        for (var r: p.rules.get(p_i)) {
            sol.addAll(evalRule(r, solutions));
        }
        return sol;
    }

    public Set<List<Long>> evalIncremental(long p_i, Map<Long, Set<List<Long>>> solutions, Map<Long, Set<List<Long>>> deltaSolutions) {
        Set<List<Long>> sol = new HashSet<>();
        for (var r: p.rules.get(p_i)) {
            sol.addAll(evalRuleIncremental(r, solutions, deltaSolutions));
        }
        return sol;
    }



    public D evalRule(Rule r, Map<Long, Set<List<Long>>> solutions) {
        var satisfyingTuples = join(r, solutions);
        return project(r, satisfyingTuples);
    }

    public Set<List<Long>> evalRuleIncremental(Rule r, Map<Long, Set<List<Long>>> solutions, Map<Long, Set<List<Long>>> newSolutions) {
        Set<List<Long>> newNewSolutions = new HashSet<>();
        solutions.keySet().forEach(x -> {
            var temp = solutions.get(x);
            solutions.put(x, newSolutions.get(x));
            newNewSolutions.addAll(evalRule(r, solutions));
            solutions.put(x, temp);
        });
        return newNewSolutions;
    }

    public Set<List<Long>> project(Rule r, HashSet<ArrayList<List<Long>>> tuples) {
        return tuples.stream().map(tuple -> r.head.ids.stream().map(id -> {
            if(!id.isVar) {
                return id.value;
            }
            var listID = r.equalitySet.eqSet.get(id.value).get(0);
            var listIndex = r.equalitySet.eqSet.get(id.value).get(1);
            return tuple.get(listID).get(listIndex);
        }).collect(Collectors.toCollection(ArrayList::new))).collect(Collectors.toCollection(HashSet::new));
    }

    public HashSet<ArrayList<List<Long>>> join(Rule r, Map<Long, Set<List<Long>>> solutions) {
        var cartProd = cartesianProduct(r, solutions, r.body.size() - 1);
//        TODO: Create a better version where this is pushed down to as early in the cartesian product as possible.
        var satisfyingTuples = cartProd.stream().filter(tuple -> {
//            Assert constant constraints
            for (int i = 0; i < r.equalitySet.constCollection.size(); i+=3) {
                var value = r.equalitySet.constCollection.get(i);
                var listID = r.equalitySet.constCollection.get(i+1);
                var listIndex =  r.equalitySet.constCollection.get(i+2);
                if(tuple.get(listID).get(listIndex) != (long) value) {
                    return false;
                }
            }
//            Assert equality constraints
            return !r.equalitySet.eqSet.values().stream().anyMatch(eqList -> {
                if(eqList.size() <= 2) {
                    return false;
                }
                var value = tuple.get(eqList.get(0)).get(eqList.get(1));
                for (int i = 2; i < eqList.size(); i+=2) {
                    if(tuple.get(eqList.get(i)).get(eqList.get(i+1)) != value) {
                        return true;
                    }
                }
                return false;
            });
        }).collect(Collectors.toCollection(HashSet::new));

        return satisfyingTuples;
    }

    public D cartesianProduct(Rule r, Map<Long, Set<List<Long>>> solutions, int i) {
        if(i == 0) {
            Set<List<Long>> factSet;
            if(solutions.containsKey(r.body.getFirst().pred)) {
                factSet = solutions.get(r.body.getFirst().pred);
            } else {
                factSet = p.facts.get(r.body.getFirst().pred);
            }
            var wrapped = factSet.stream().map(toBeWrapped -> {
                var list = new ArrayList<List<Long>>();
                list.add(toBeWrapped);
                return list;
            }).collect(Collectors.toCollection(HashSet::new));
            return wrapped;
        }
        var prev = cartesianProduct(r, solutions, i-1);
        Set<ArrayList<List<Long>>> result = new HashSet<>();
        Set<List<Long>> factSet;
        if(solutions.containsKey(r.body.get(i).pred)) {
            factSet = solutions.get(r.body.get(i).pred);
        } else {
            factSet = p.facts.get(r.body.get(i).pred);
        }
        for(var x: factSet) {
            for(var y: prev)  {
                var clone = (ArrayList<List<Long>>) y.clone(); // Stupid backwards compatible.
                clone.add(x);
                result.add(clone);
            }
        }
        return result;
    }*/
}
