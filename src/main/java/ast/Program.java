package ast;

import java.util.List;

public class Program {
    List<Atom> facts;
    List<Rule> rules;

    public Program(List<Atom> facts, List<Rule> rules) {
        this.facts = facts;
        this.rules = rules;
    }
}
