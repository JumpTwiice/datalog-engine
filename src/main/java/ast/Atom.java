package ast;

import java.util.List;

public class Atom {
    public Term pred;
    public List<Term> ids;

    public Atom(Term pred, List<Term> ids) {
        this.pred = pred;
        this.ids = ids;
    }
}
