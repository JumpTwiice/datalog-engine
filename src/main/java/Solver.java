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
            solutions.put(x.head.pred, new HashSet<>());
        }
        for(var x: p.facts) {
            solutions.putIfAbsent(x.pred, new HashSet<>());
            solutions.get(x.pred).add(x.ids.stream().map(x_ -> x_.value).collect(Collectors.toCollection(ArrayList::new)));
        }
        return solutions;
    }

    public static void naiveEval(Program p) {
        Map<Long, Set<List<Long>>> solutions = initializeSolutions(p);
        boolean done;
        do  {
            done = true;
            Map<Long, Set<List<Long>>> newSolutions = new HashMap<>();
            for (var x: p.rules) {
                newSolutions.put(x.head.pred, eval(x, solutions));
            }
            for (var x: newSolutions.keySet()) {
                var set = newSolutions.get(x);
                solutions.get(x);
            }
//            for (var x: p.rules) {
//
//                newSolutions.put(x.head.pred, computeSet(x, solutions));
//            }
        } while(!done);
    }

    public static Set<List<Long>> eval(Rule r, Map<Long, Set<List<Long>>> solutions) {
        var satisfyingTuples = join(r, solutions);
        return project(r, satisfyingTuples);
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
            return r.equalitySet.eqSet.values().stream().anyMatch(eqList -> {
                if(eqList.size() <= 1) {
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
