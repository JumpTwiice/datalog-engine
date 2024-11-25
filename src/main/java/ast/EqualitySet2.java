package ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EqualitySet2 {
    public Map<Long, ArrayList<Integer>> eqSet;
    public EqualitySet2() {
        eqSet = new HashMap<>();
    }

    public void setEqual(int listNumber, int listIndex, long predicateSymbol) {
        if(!eqSet.containsKey(predicateSymbol)) {
            ArrayList<Integer> list = new ArrayList<>();
            list.add(listNumber);
            list.add(listIndex);
            eqSet.put(predicateSymbol, list);
        }
    }
}
