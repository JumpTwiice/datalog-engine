package ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Program {
    public List<Atom> facts;
    public List<Rule> rules;
    public Atom query;
    public Map<Long, String> idToVar;
    public Map<Long, List<Integer>> idToRuleSet;
    public Program(List<Atom> facts, List<Rule> rules, Atom query, Map<Long, String> idToVar) {
        this.facts = facts;
        this.rules = rules;
        this.query = query;
        this.idToVar = idToVar;
        this.idToRuleSet = new HashMap<>();
        for (int i = 0; i < rules.size(); i++) {
            var r = rules.get(i);
            idToRuleSet.putIfAbsent(r.head.pred, new ArrayList<>());
            idToRuleSet.get(r.head.pred).add(i);
        }
    }
}
