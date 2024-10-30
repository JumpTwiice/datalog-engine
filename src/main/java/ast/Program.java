package ast;

import java.util.List;

public class Program {
    public List<Atom> facts;
    public List<Rule> rules;
    public Atom query;

    public Program(List<Atom> facts, List<Rule> rules, Atom query) {
        this.facts = facts;
        this.rules = rules;
        this.query = query;
    }
}
