package solver;

import ast.Program;
import ast.Rule;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleSolver implements Solver<Set<List<Long>>> {
    public Program p;
    private Map<Long, Set<List<Long>>> solutions;

    public SimpleSolver(Program p) {
        Transformer.setEqSet(p);
        this.p = p;
        solutions = initializeWithFacts();
    }

    public Map<Long, Set<List<Long>>> initializeWithFacts() {
        Map<Long, Set<List<Long>>> solutions = new HashMap<>();
        for (var x : p.rules.keySet()) {
            solutions.putIfAbsent(x, new HashSet<>());
        }
        for (var x : p.facts.entrySet()) {
            var facts = solutions.computeIfAbsent(x.getKey(), k -> new HashSet<>());
            facts.addAll(x.getValue());
        }
        return solutions;
    }

    public Map<Long, Set<List<Long>>> initializeEmpty() {
        Map<Long, Set<List<Long>>> solutions = new HashMap<>();
        for (var x : p.rules.keySet()) {
            solutions.computeIfAbsent(x, y -> new HashSet<>());
        }
        for (var x : p.facts.keySet()) {
            solutions.computeIfAbsent(x, y -> new HashSet<>());
        }
        return solutions;
    }

    public Map<Long, Set<List<Long>>> naiveEval() {
//        Map<Long, Set<List<Long>>> solutions = initializeEmpty();
        boolean done;
        do {
            done = true;
            Map<Long, Set<List<Long>>> newSolutions = new HashMap<>();
            for (var r : p.rules.keySet()) {
                newSolutions.put(r, eval(p, r, solutions));
            }
            loop:
            for (var x : newSolutions.keySet()) {
                var set = newSolutions.get(x);
                for (var y : set) {
                    if (!solutions.get(x).contains(y)) {
                        done = false;
                        break loop;
                    }
                }
            }
            // Add all new solutions to the old.
            if (!done) {
                for (var x : newSolutions.keySet()) {
                    solutions.get(x).addAll(newSolutions.get(x));
                }
            }
        } while (!done);
        return solutions;
    }

    public Map<Long, Set<List<Long>>> semiNaiveEval() {
        Map<Long, Set<List<Long>>> deltaSolutions = initializeEmpty();
        for (var p_i : p.rules.keySet()) {
            deltaSolutions.put(p_i, eval(p, p_i, solutions));
        }
        Map<Long, Set<List<Long>>> temp = deltaSolutions;
        deltaSolutions.keySet().forEach(e ->
                {
                    solutions.computeIfAbsent(e, x -> new HashSet<>());
                    solutions.get(e).addAll(temp.get(e));
                }
        );

        boolean done;
        do {
            done = true;
            Map<Long, Set<List<Long>>> deltaPrimeSolutions = deltaSolutions;
            deltaSolutions = initializeEmpty();

            for (var p_i : p.rules.keySet()) {
                deltaSolutions.put(p_i, evalIncremental(p, p_i, solutions, deltaPrimeSolutions));
                deltaSolutions.get(p_i).removeAll(solutions.get(p_i));
            }
            for (var x : deltaSolutions.keySet()) {
                if (!deltaSolutions.get(x).isEmpty()) {
                    done = false;
                    break;
                }
            }
            // Add all new solutions to the old.
            if (!done) {
                for (var p_i : p.rules.keySet()) {
                    solutions.get(p_i).addAll(deltaSolutions.get(p_i));
                }
            }
        } while (!done);
        return solutions;
    }

    @Override
    public SimpleSolver resetWithProgram(Program p) {
//        TODO: make less stupid
        return new SimpleSolver(p);
    }

    @Override
    public Solver<Set<List<Long>>> resetWithProgramAndFacts(Program p, Map<Long, Set<List<Long>>> facts) {
        var res = new SimpleSolver(p);
        res.solutions = facts;
        return res;
    }

    @Override
    public Map<Long, Set<List<Long>>> solutionsToPredMap() {
        return solutions;
    }

    public static Set<List<Long>> eval(Program p, long p_i, Map<Long, Set<List<Long>>> solutions) {
        Set<List<Long>> sol = new HashSet<>();
        for (var r : p.rules.get(p_i)) {
            sol.addAll(evalRule(p, r, r.body.stream().map(x -> solutions.get(x.pred)).toList()));
        }
        return sol;
    }

    public static Set<List<Long>> evalIncremental(Program p, long p_i, Map<Long, Set<List<Long>>> solutions, Map<Long, Set<List<Long>>> deltaSolutions) {
        Set<List<Long>> sol = new HashSet<>();
        for (var r : p.rules.get(p_i)) {
            sol.addAll(evalRuleIncremental(p, r, solutions, deltaSolutions));
        }
        return sol;
    }

    public static Set<List<Long>> evalRule(Program p, Rule r, List<Set<List<Long>>> solutions) {
        if (solutions.stream().anyMatch(Objects::isNull)) {
            return new HashSet<>();
        }
        var satisfyingTuples = join(p, r, solutions);
        return project(r, satisfyingTuples);
    }

    public static Set<List<Long>> evalRuleIncremental(Program p, Rule r, Map<Long, Set<List<Long>>> solutions, Map<Long, Set<List<Long>>> newSolutions) {
        Set<List<Long>> newNewSolutions = new HashSet<>();

        var sols = r.body.stream().map(x -> solutions.containsKey(x.pred) ? solutions.get(x.pred) : p.facts.get(x.pred)).collect(Collectors.toCollection(ArrayList::new));

        for(var i = 0; i < sols.size(); i++) {
            var atomID = r.body.get(i).pred;
            sols.set(i, newSolutions.get(atomID));
            newNewSolutions.addAll(evalRule(p, r, sols));
            sols.set(i, solutions.get(atomID));
        }

//        var meaningFullRelations = r.body.stream().map(x -> x.pred).filter(x -> p.rules.containsKey(x)).collect(Collectors.toSet());
//        meaningFullRelations.forEach(x -> {
//            var temp = solutions.get(x);
//            solutions.put(x, newSolutions.get(x));
//            newNewSolutions.addAll(evalRule(p, r, solutions));
//            solutions.put(x, temp);
//        });
        return newNewSolutions;
    }

    public static Set<List<Long>> project(Rule r, HashSet<ArrayList<List<Long>>> tuples) {
        return tuples.stream().map(tuple -> r.head.ids.stream().map(id -> {
            if (!id.isVar) {
                return id.value;
            }
            var listID = r.equalitySet.eqSet.get(id.value).get(0);
            var listIndex = r.equalitySet.eqSet.get(id.value).get(1);
            return tuple.get(listID).get(listIndex);
        }).collect(Collectors.toCollection(ArrayList::new))).collect(Collectors.toCollection(HashSet::new));
    }

    public static HashSet<ArrayList<List<Long>>> join(Program p, Rule r, List<Set<List<Long>>> solutions) {
        var cartProd = cartesianProduct(p, r, solutions, r.body.size() - 1);
//        TODO: Create a better version where this is pushed down to as early in the cartesian product as possible.
        var satisfyingTuples = cartProd.stream().filter(tuple -> {
//            Assert constant constraints
            for (int i = 0; i < r.equalitySet.constCollection.size(); i += 3) {
                var value = r.equalitySet.constCollection.get(i);
                var listID = r.equalitySet.constCollection.get(i + 1);
                var listIndex = r.equalitySet.constCollection.get(i + 2);
                if (tuple.get(listID).get(listIndex) != (long) value) {
                    return false;
                }
            }
//            Assert equality constraints
            return r.equalitySet.eqSet.values().stream().noneMatch(eqList -> {
                if (eqList.size() <= 2) {
                    return false;
                }
                var value = tuple.get(eqList.get(0)).get(eqList.get(1));
                for (int i = 2; i < eqList.size(); i += 2) {
                    if (!Objects.equals(tuple.get(eqList.get(i)).get(eqList.get(i + 1)), value)) {
                        return true;
                    }
                }
                return false;
            });
        }).collect(Collectors.toCollection(HashSet::new));

        return satisfyingTuples;
    }

    public static Set<ArrayList<List<Long>>> cartesianProduct(Program p, Rule r, List<Set<List<Long>>> solutions, int i) {
        if (i == 0) {
            Set<List<Long>> factSet;
            factSet = solutions.get(i);
            var wrapped = factSet.stream().map(toBeWrapped -> {
                var list = new ArrayList<List<Long>>();
                list.add(toBeWrapped);
                return list;
            }).collect(Collectors.toCollection(HashSet::new));
            return wrapped;
        }
        var prev = cartesianProduct(p, r, solutions, i - 1);
        Set<ArrayList<List<Long>>> result = new HashSet<>();
        Set<List<Long>> factSet;
        factSet = solutions.get(i);
        for (var x : factSet) {
            for (var y : prev) {
                var clone = (ArrayList<List<Long>>) y.clone(); // Stupid backwards compatible.
                clone.add(x);
                result.add(clone);
            }
        }
        return result;
    }
}
