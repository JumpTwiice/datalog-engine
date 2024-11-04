package ast;

import java.util.List;

public class Atom {
    public Long pred;
    public List<Term> ids;

    public Atom(Long pred, List<Term> ids) {
        this.pred = pred;
        this.ids = ids;
    }
}
