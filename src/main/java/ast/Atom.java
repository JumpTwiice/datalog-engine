package ast;

import java.util.List;

public class Atom {
    public long pred;
    public List<Term> ids;
    public int[][] sameNess;
    public boolean[] constBool;
    public long[] constArr;

    public Atom(Long pred, List<Term> ids) {
        this.pred = pred;
        this.ids = ids;
    }

//    public void setupForTrieSolver() {
//        constBool = new boolean[ids.size()];
//        constArr = new long[ids.size()];
////      Unsure of whether branch prediction will be a problem, but might as well try to avoid it.
//        for (int j = 0; j < constBool.length; j++) {
//            constBool[j] = !ids.get(j).isVar;
//            constArr[j] = ids.get(j).value;
//        }
//        HashMap<Long, Integer> idToFirstPos = new HashMap<>();
//        HashMap<Long, List<Integer>> posToWriteList = new HashMap<>();
//        for (int j = ids.size()-1; j >= 0; j--) {
//            var id = ids.get(j).value;
//            if(ids.get(j).isVar) {
//                if(idToFirstPos.containsKey(id)) {
//                    var list = posToWriteList.computeIfAbsent(id, x -> new ArrayList<>());
//                    list.add(j);
//                    constBool[j] = true;
//                } else {
//                    idToFirstPos.put(id, j);
//                }
//            }
//        }
//
//        sameNess = new int[ids.size()][];
//        posToWriteList.forEach((key, val) -> {
//            sameNess[idToFirstPos.get(key)] = val.stream().mapToInt(x -> x).toArray();
//        });
//    }

//    public Tuple<boolean[], long[]> getBoolAndConstArr() {
//        var constBool = new boolean[ids.size()];
//        var constArr = new long[ids.size()];
////      Unsure of whether branch prediction will be a problem, but might as well try to avoid it.
//        for (int j = 0; j < constBool.length; j++) {
//            constBool[j] = !ids.get(j).isVar;
//            constArr[j] = ids.get(j).value;
//        }
//        return new Tuple<>(constBool, constArr);
//    }

    public String idsToString() {
        return ids.stream().map(Term::toString).reduce("", (acc, cur) -> acc + ", " + cur).substring(2);
    }

    public static String formatPredicate(long pred, Program p) {
        return p.idToVar.get(pred) + "@" + pred;
    }

    public String toString(Program p) {
        return formatPredicate(this.pred, p) + '(' + idsToString() + ')';
    }
}
