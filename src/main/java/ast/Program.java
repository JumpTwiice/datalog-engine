package ast;

import solver.Solver;

import java.util.*;

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

    public void setupForTrieSolver() {
        for(var ruleSet: rules.values()) {
            for(var r: ruleSet) {
                r.setupForTrieSolver();
            }
        }
    }

    @Override
    public String toString() {
        return "Program{" +
                "query=" + ((query == null) ? "": query.toString(this)) + "\n" +
                "facts=\n\t" + Solver.formatSolution(facts, this).replace("\n", "\n\t") +
                "\nrules=\n" +
                rules.values().stream().map(ruleset -> ruleset.stream().map(r -> r.toString(this)).reduce("", (acc, cur) -> acc + "\n\t" + cur)).reduce("", (acc, cur) -> acc + "\n" + cur).substring(2) +
//                rules +
//                ", query=" + query.toString(this) +
//                ", idToVar=" + idToVar +
//                ", nextPred=" + nextPred +
                "\n}";
    }

    public Program cloneProgram() {
//        facts.clone();
        return new Program(new HashMap<>(facts), new HashMap<>(rules), query, idToVar, nextPred);
    }
}
