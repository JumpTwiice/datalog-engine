package ast;

import java.util.List;

public class AdornedAtom extends Atom {
    public List<Boolean> isBoundArray;
    public boolean isMagic;
    public AdornedAtom(Long pred, List<Term> ids, List<Boolean> adornment) {
        super(pred, ids);
        this.isBoundArray = adornment;
    }
    public AdornedAtom(Long pred, List<Term> ids, List<Boolean> adornment, boolean magic) {
        super(pred, ids);
        this.isBoundArray = adornment;
        this.isMagic = magic;
    }

    public String generatePred(Program p) {
        String magicPart = isMagic ? "_m" : "";
        String adornmentPar = isBoundArray == null ? "": isBoundArray.stream().map(b -> b ? "b" : "f").reduce("^", String::concat);
        return p.idToVar.get(pred) + adornmentPar + magicPart;
    }


    public String toString(Program p) {
        String magicPart = isMagic ? "_m" : "";
        String adornmentPar = isBoundArray == null ? "": isBoundArray.stream().map(b -> b ? "b" : "f").reduce("^", String::concat);
        return p.idToVar.get(pred) + adornmentPar + magicPart + '(' + ids + ')';
    }
}
