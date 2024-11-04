import ast.Atom;
import ast.Program;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Checker {

    private static void checkAtomArity(Atom a, Map<Long, Long> arities) {
        if(arities.containsKey(a.pred)) {
            assert(arities.get(a.pred) == a.ids.size());
        } else {
            arities.put(a.pred, (long) a.ids.size());
        }
    }

    public static void checkArity(Program p) {
        Map<Long, Long> arities = new HashMap<>();
        for (var a: p.facts) {
            checkAtomArity(a, arities);
        }
        for (var r: p.rules) {
            checkAtomArity(r.head, arities);
            for(var bodyAtom: r.body) {
                checkAtomArity(bodyAtom, arities);
            }
        }
        checkAtomArity(p.query, arities);
    }

    public static void checkBoundness(Program p) {
        for(var r: p.rules) {
            Set<Long> bodyVars = new HashSet<>();
            r.body.forEach(x -> x.ids.forEach(u -> {
                if(u.isVar) {
                    bodyVars.add(u.value);
                }
            }));
            for(var id: r.head.ids) {
                if(id.isVar) {
                    assert(bodyVars.contains(id.value));
                }
            }
        }
    }

    public static void checkProgram(Program p)  {
        checkArity(p);
        checkBoundness(p);
    }
}
