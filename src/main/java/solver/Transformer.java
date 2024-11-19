package solver;

import ast.*;

import java.util.*;
import java.util.stream.Collectors;

public class Transformer {

    public static long changeFactsAndRulesToEDGFormat(Map<Long, List<Atom>> facts, Map<Long, List<Rule>> rules, long counter) {
        Set<Long> problematicPreds = new HashSet<>(rules.keySet());
        problematicPreds.retainAll(facts.keySet());
        for(var pred: problematicPreds) {
            var newPred = counter--;
            facts.put(newPred, facts.get(pred));
            var factList = facts.remove(pred);
            List<Term> ids = new ArrayList<>();
            for(var ignored : factList.getFirst().ids) {
                ids.add(new Term(counter--, true));
            }
            rules.get(pred).add(new Rule(new Atom(pred, ids), new ArrayList<>(List.of(new Atom(newPred, ids)))));
        }
        return counter;
    }

    public static void setEqSet2(Program p) {
        for(var ruleSet: p.rules.values()) {
            for(var r: ruleSet) {
                r.equalitySet2 = new EqualitySet2();
                for(int i = 0; i < r.body.size(); i++) {
                    var currentRelation = r.body.get(i);
                    for(int j = 0; j < currentRelation.ids.size(); j++) {
                        var currentTerm = currentRelation.ids.get(j);
                        if(currentTerm.isVar) {
                            r.equalitySet2.setEqual(i, j, currentTerm.value);
                        }
                    }
                }
            }
        }
    }


    public static void setEqSet(Program p) {
        for(var ruleSet: p.rules.values()) {
            for(var r: ruleSet) {
                r.equalitySet = new EqualitySet();
                for(int i = 0; i < r.body.size(); i++) {
                    var currentRelation = r.body.get(i);
                    for(int j = 0; j < currentRelation.ids.size(); j++) {
                        var currentTerm = currentRelation.ids.get(j);
                        if(currentTerm.isVar) {
                            r.equalitySet.setEqual(i, j, currentTerm.value);
                        } else {
                            r.equalitySet.setConstant(i, j, (int) currentTerm.value);
                        }
                    }
                }
            }
        }
    }
}
