package solver;

import ast.AdornedAtom;

import java.util.Set;

public class SIPEntry {
    Set<AdornedAtom> leftSide;
    AdornedAtom rightSide;

    public SIPEntry(Set<AdornedAtom> leftSide, AdornedAtom rightSide) {
        this.leftSide = leftSide;
        this.rightSide = rightSide;
    }
}
