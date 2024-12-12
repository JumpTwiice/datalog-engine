package ast;

import solver.Solver;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class Program {
    public Map<Long, Set<List<Long>>> facts;
    public Map<Long, List<Rule>> rules;
    public Atom query;
    public Map<Long, String> idToVar;
    public long nextPred;
    public Program(Map<Long, Set<List<Long>>> facts, Map<Long, List<Rule>> rules, Atom query, Map<Long, String> idToVar, long counter) {
        this.nextPred = counter;
        this.facts = facts;
        this.rules = rules;
        this.query = query;
        this.idToVar = idToVar;
    }

    public void setupForQueryAnswering() {
        if(query == null){
            throw new RuntimeException("Query was null when asked to answer query.");
        }
        var id = nextPred++;
        ArrayList<Atom> body = new ArrayList<>();
        var queryClone = new Atom(id, query.ids);
        idToVar.put(id, idToVar.get(query.pred) + "_query");
        body.add(query);
        var newRule = new Rule(queryClone, body);
        var ruleSet = new ArrayList<Rule>();
        ruleSet.add(newRule);
        rules.put(id, ruleSet);
//        Should just return all values of query predicate.
        this.query = new Atom(queryClone.pred, LongStream.range(0, queryClone.ids.size()).mapToObj(x -> new Term(x, false)).collect(Collectors.toCollection(ArrayList::new)));
    }

    public void setupForTrieSolver() {
        for(var ruleSet: rules.values()) {
            for(var r: ruleSet) {
                r.setupForTrieSolver();
            }
        }
    }

    public void setupForSimpleSolver() {
        for (var ruleSet : rules.values()) {
            for (var r : ruleSet) {
                r.equalitySet = new EqualitySet();
                for (int i = 0; i < r.body.size(); i++) {
                    var currentRelation = r.body.get(i);
                    for (int j = 0; j < currentRelation.ids.size(); j++) {
                        var currentTerm = currentRelation.ids.get(j);
                        if (currentTerm.isVar) {
                            r.equalitySet.setEqual(i, j, currentTerm.value);
                        } else {
                            r.equalitySet.setConstant(i, j, (int) currentTerm.value);
                        }
                    }
                }
            }
        }
    }


    @Override
    public String toString() {
        return "Program{" +
                "query=" + ((query == null) ? "": query.toString(this)) + "\n" +
                "facts=\n\t" + Solver.formatSolution(facts, this).replace("\n", "\n\t") +
                "\nrules=\n" +
                (rules.isEmpty() ? "" : rules.values().stream().map(ruleset -> ruleset.stream().map(r -> r.toString(this)).reduce("", (acc, cur) -> acc + "\n\t" + cur)).reduce("", (acc, cur) -> acc + "\n" + cur).substring(2)) +
//                rules +
//                ", query=" + query.toString(this) +
//                ", idToVar=" + idToVar +
//                ", nextPred=" + nextPred +
                "\n}";
    }

    public Program cloneProgram() {
        return new Program(new HashMap<>(facts), new HashMap<>(rules), query, idToVar, nextPred);
    }
}
