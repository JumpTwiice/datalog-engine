package ast;

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

    public Program cloneProgram() {
//        facts.clone();
        return new Program(new HashMap<>(facts), new HashMap<>(rules), query, idToVar, nextPred);
    }
}
