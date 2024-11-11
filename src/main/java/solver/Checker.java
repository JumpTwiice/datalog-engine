package solver;

import ast.Atom;
import ast.Program;
import ast.Rule;

import java.util.*;

public class Checker {

    private static void checkAtomArity(Atom a, Map<Long, Long> arities) {
        if(arities.containsKey(a.pred)) {
            assert(arities.get(a.pred) == a.ids.size());
        } else {
            arities.put(a.pred, (long) a.ids.size());
        }
    }

    public static void checkArity(Map<Long, List<Atom>> facts, Map<Long, List<Rule>> rules, Atom query) {
        Map<Long, Long> arities = new HashMap<>();
        for (var factSet: facts.values()) {
            for (var atom: factSet) {
                checkAtomArity(atom, arities);
            }
        }
        for (var ruleSet: rules.values()) {
            for (var r : ruleSet) {
                checkAtomArity(r.head, arities);
                for (var bodyAtom : r.body) {
                    checkAtomArity(bodyAtom, arities);
                }
            }
        }
        if(query != null) {
            checkAtomArity(query, arities);
        }
    }

    public static void checkBoundness(Map<Long, List<Rule>> rules) {
        for (var ruleSet: rules.values()) {
            for (var r : ruleSet) {
                Set<Long> bodyVars = new HashSet<>();
                r.body.forEach(x -> x.ids.forEach(u -> {
                    if (u.isVar) {
                        bodyVars.add(u.value);
                    }
                }));
                for (var id : r.head.ids) {
                    if (id.isVar) {
                        assert (bodyVars.contains(id.value));
                    }
                }
            }
        }
    }

    public static void checkProgram(Map<Long, List<Atom>> facts, Map<Long, List<Rule>> rules, Atom query)  {
        checkArity(facts, rules, query);
        checkBoundness(rules);
    }
}
