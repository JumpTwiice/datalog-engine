package ast;

import common.Tuple;

import java.util.List;

public class Atom {
    public Long pred;
    public List<Term> ids;

    public Atom(Long pred, List<Term> ids) {
        this.pred = pred;
        this.ids = ids;
    }

    public Tuple<boolean[], long[]> getBoolAndConstArr() {
        var constBool = new boolean[ids.size()];
        var constArr = new long[ids.size()];
//      Unsure of whether branch prediction will be a problem, but might as well try to avoid it.
        for (int j = 0; j < constBool.length; j++) {
            constBool[j] = !ids.get(j).isVar;
            constArr[j] = ids.get(j).value;
        }
        return new Tuple<>(constBool, constArr);
    }

    public String idsToString() {
        return ids.stream().map(Term::toString).reduce("", (acc, cur) -> acc + ", " + cur).substring(2);
    }

    public String toString(Program p) {
        return p.idToVar.get(pred) + '(' + idsToString() + ')';
    }
}
