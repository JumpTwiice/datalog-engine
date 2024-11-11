package ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EqualitySet2 {
    public Map<Long, ArrayList<Integer>> eqSet;
    public List<Integer> constCollection;
    public EqualitySet2() {
        eqSet = new HashMap<>();
        constCollection = new ArrayList<>();
    }

    public void setConstant(int listNumber, int listIndex, int constant) {
        constCollection.add(constant);
        constCollection.add(listNumber);
        constCollection.add(listIndex);
    }


    public void setEqual(int listNumber, int listIndex, long predicateSymbol) {
        eqSet.putIfAbsent(predicateSymbol, new ArrayList<>());
        var list = eqSet.get(predicateSymbol);
        list.add(listNumber);
        list.add(listIndex);
    }
}
