package ast;

import java.util.List;

public class Rule {
    Atom head;
    List<Atom> body;

    public Rule(Atom head, List<Atom> body) {
        this.head = head;
        this.body = body;
    }
}
