import ast.Program;
import ast.Rule;

import java.util.*;
import java.util.stream.Collectors;

public class Solver {
    public static void solve(Program p) {

    }

    public static Map<Long, Set<List<Long>>> initializeSolutions(Program p) {
        Map<Long, Set<List<Long>>> solutions = new HashMap<>();
        for(var x: p.rules) {
            solutions.putIfAbsent(x.head.pred, new HashSet<>());
        }
        for(var x: p.facts) {
            solutions.putIfAbsent(x.pred, new HashSet<>());
            solutions.get(x.pred).add(x.ids.stream().map(x_ -> x_.value).collect(Collectors.toCollection(ArrayList::new)));
        }
        return solutions;
    }

    public static Map<Long, Set<List<Long>>> initializeEmpty(Program p) {
        Map<Long, Set<List<Long>>> solutions = new HashMap<>();
        for(var x: p.rules) {
            solutions.putIfAbsent(x.head.pred, new HashSet<>());
        }
        for(var x: p.facts) {
            solutions.putIfAbsent(x.pred, new HashSet<>());
        }
        return solutions;
    }

    public static Map<Long, Set<List<Long>>> naiveEval(Program p) {
        Map<Long, Set<List<Long>>> solutions = initializeSolutions(p);
        boolean done;
        do {
            done = true;
            Map<Long, Set<List<Long>>> newSolutions = new HashMap<>();
            for(var r: p.idToRuleSet.keySet()) {
                newSolutions.put(r, eval(p, r, solutions));
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

    public static Map<Long, Set<List<Long>>> semiNaiveEval(Program p) {
        Map<Long, Set<List<Long>>> solutions = initializeSolutions(p);
        Map<Long, Set<List<Long>>> deltaSolutions = initializeEmpty(p);
        for (var p_i: p.idToRuleSet.keySet()) {
            deltaSolutions.put(p_i, eval(p, p_i, solutions));
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

            for (var p_i: p.idToRuleSet.keySet()) {
                deltaSolutions.put(p_i, evalIncremental(p, p_i, solutions, deltaPrimeSolutions));
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
                for (var p_i: p.idToRuleSet.keySet()) {
                    solutions.get(p_i).addAll(deltaSolutions.get(p_i));
                }
//                Ugly. Add all built in facts to deltaSolutions.
                Map<Long, Set<List<Long>>> temp_ = deltaSolutions;
                p.facts.forEach(e -> {
                    temp_.putIfAbsent(e.pred, new HashSet<>());
                    temp_.get(e.pred).add(e.ids.stream().map(x -> x.value).collect(Collectors.toCollection(ArrayList::new)));
                });
            }
        } while(!done);
        return solutions;
    }

    public static Set<List<Long>> eval(Program p, long p_i, Map<Long, Set<List<Long>>> solutions) {
        Set<List<Long>> sol = new HashSet<>();
        for (var r: p.idToRuleSet.get(p_i)) {
            sol.addAll(evalRule(p.rules.get(r), solutions));
        }
        return sol;
    }

    public static Set<List<Long>> evalIncremental(Program p, long p_i, Map<Long, Set<List<Long>>> solutions, Map<Long, Set<List<Long>>> deltaSolutions) {
        Set<List<Long>> sol = new HashSet<>();
        for (var r: p.idToRuleSet.get(p_i)) {
            sol.addAll(evalRuleIncremental(p.rules.get(r), solutions, deltaSolutions));
        }
        return sol;
    }



    public static Set<List<Long>> evalRule(Rule r, Map<Long, Set<List<Long>>> solutions) {
        var satisfyingTuples = join(r, solutions);
        return project(r, satisfyingTuples);
    }

    public static Set<List<Long>> evalRuleIncremental(Rule r, Map<Long, Set<List<Long>>> solutions, Map<Long, Set<List<Long>>> newSolutions) {
        Set<List<Long>> newNewSolutions = new HashSet<>();
        solutions.keySet().forEach(x -> {
            var temp = solutions.get(x);
            solutions.put(x, newSolutions.get(x));
            newNewSolutions.addAll(evalRule(r, solutions));
            solutions.put(x, temp);
        });
        return newNewSolutions;
    }

    public static Set<List<Long>> project(Rule r, HashSet<ArrayList<List<Long>>> tuples) {
        return tuples.stream().map(tuple -> r.head.ids.stream().map(id -> {
            if(!id.isVar) {
                return id.value;
            }
            var listID = r.equalitySet.eqSet.get(id.value).get(0);
            var listIndex = r.equalitySet.eqSet.get(id.value).get(1);
            return tuple.get(listID).get(listIndex);
        }).collect(Collectors.toCollection(ArrayList::new))).collect(Collectors.toCollection(HashSet::new));
    }

    public static HashSet<ArrayList<List<Long>>> join(Rule r, Map<Long, Set<List<Long>>> solutions) {
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

    public static Set<ArrayList<List<Long>>> cartesianProduct(Rule r, Map<Long, Set<List<Long>>> solutions, int i) {
        if(i == 0) {
            var wrapped = solutions.get(r.body.getFirst().pred).stream().map(toBeWrapped -> {
                var list = new ArrayList<List<Long>>();
                list.add(toBeWrapped);
                return list;
            }).collect(Collectors.toCollection(HashSet::new));
            return wrapped;
        }
        var prev = cartesianProduct(r, solutions, i-1);
        Set<ArrayList<List<Long>>> result = new HashSet<>();
        for(var x: solutions.get(r.body.get(i).pred)) {
            for(var y: prev)  {
                var clone = (ArrayList<List<Long>>) y.clone(); // Stupid backwards compatible.
                clone.add(x);
                result.add(clone);
            }
        }
        return result;
    }
}
