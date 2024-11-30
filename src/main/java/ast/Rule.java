package ast;

import java.util.*;

public class Rule {
    public Atom head;
    public ArrayList<Atom> body;
    public EqualitySet equalitySet;
    public EqualitySet2 equalitySet2;

    public long[] varList; // List of variables in body. First variable is the leftmost and so on.
    public Map<Long, Integer> varMap; // Maps variables to the number they are in the varList.
    public int[][] positions; // Array of indices of variables from the head in varList
    public long[] headHelper; // Array of values to be used in the head

    public Rule(Atom head, ArrayList<Atom> body) {
        this.head = head;
        this.body = body;
    }

    public void setupPositions() {
        HashSet<Long> seen = new HashSet<>();
        List<Long> vars = new ArrayList<>();
        varMap = new HashMap<>();
        for (var a: body) {
            for(var id: a.ids) {
                if(id.isVar && !seen.contains(id.value)) {
                    vars.add(id.value);
                    varMap.put(id.value, vars.size() - 1);
                    seen.add(id.value);
                }
            }
        }
        varList = new long[vars.size()];
        for(int i = 0; i < vars.size(); i++) {
            varList[i] = vars.get(i);
        }

        headHelper = new long[head.ids.size()];
        var variablePositionToHeadPosition = new HashMap<Integer, ArrayList<Integer>>();
        for (int i = 0; i < head.ids.size(); i++) {
            var x = head.ids.get(i);
            if(!x.isVar) {
                headHelper[i] = x.value;
            } else {
                var list = variablePositionToHeadPosition.computeIfAbsent(varMap.get(x.value), x_ -> new ArrayList<>());
                list.add(i);
            }
        }
        positions = new int[varList.length][];
        for (var x: variablePositionToHeadPosition.keySet()) {
            var current = variablePositionToHeadPosition.get(x);
            var temp = new int[current.size()];
            for(int i = 0; i < current.size(); i++) {
                temp[i] = current.get(i);
            }
            positions[x] = temp;
        }
//        positions = new int[head.ids.size()][];

    }

    public String toString(Program p) {
        return head.toString(p) + " :- " + body.stream().map(x -> x.toString(p)).reduce("", (acc, cur) -> acc + ", " + cur).substring(2) + '.';
    }
}
