package ast;

import java.util.*;

public class Program {
    public Map<Long, Set<List<Long>>> facts;
    public Map<Long, List<Rule>> rules;
    public Atom query;
    public Map<Long, String> idToVar;
    public long maxPred;
    public Program(Map<Long, Set<List<Long>>> facts, Map<Long, List<Rule>> rules, Atom query, Map<Long, String> idToVar, long counter) {
        this.maxPred = counter;
        this.facts = facts;
        this.rules = rules;
        this.query = query;
        this.idToVar = idToVar;
    }
}
