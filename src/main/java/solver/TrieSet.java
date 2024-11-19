package solver;

import ast.Atom;
import ast.Program;
import ast.Rule;
import ast.Term;

import java.util.*;

public class TrieSet {
    public HashMap<Long, SimpleTrie> map;
    public Program p;
    public TrieSet(Program p) {
        map = new HashMap<>();
        this.p = p;
        for(var id: p.rules.keySet()) {
            int size = p.rules.get(id).getFirst().head.ids.size();
            map.put(id, null);
//            map.put(id, new SimpleTrie(size));
        }
        for(var id: p.facts.keySet()) {
            map.put(id, SimpleTrie.trieFrom(p.facts.get(id)));
        }
    }

    public boolean subsetOf(TrieSet s) {
        for (var x: map.keySet()) {
            var maybeSub = map.get(x);
            var maybeSup = s.map.get(x);
            if(maybeSub.children == null && maybeSub.leaves == null) {
                continue;
            }
            if(maybeSup == null) {
                return false;
            }
            if(!maybeSub.subsetOf(maybeSup)) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(long pred) {
        return false;
    }

    public boolean meld(TrieSet s) {
        boolean change = false;
        for(var x: map.keySet()) {
            var source = s.map.get(x);
            if(source == null) {
                continue;
            }
            var destination = map.get(x);
            if(destination == null) {
                map.put(x, s.map.get(x));
                change = true;
                continue;
            }
            change |= map.get(x).meld(s.map.get(x));
        }
        return change;
    }

//    public SimpleTrie cloneForTrie(Atom a) {
//        var isConst = new boolean[a.ids.size()];
//        var constArr = new long[a.ids.size()];
//        return getAll(a, map.get(a.pred), 0, isConst, constArr);
//    }

    public SimpleTrie cloneForTrie(Atom a, boolean[] isConstant, long[] constantArr) {
        return getAll(a, map.get(a.pred), 0, isConstant, constantArr);
    }

    /**
     * Clones the 'from' trie to the returned trie. If the returned value is null it means empty.
     * @param a
     * @param from
     * @param index
     * @param isConstant
     * @param constantArr
     * @return
     */
    private SimpleTrie getAll(Atom a, SimpleTrie from, int index, boolean[] isConstant, long[] constantArr) {
        System.out.println(index);
        if (index == a.ids.size() - 1) {
            if (isConstant[index]) {
                if(from.leaves.contains(constantArr[index])) {
                    return new SimpleTrie(-1);
                }
                return null;
            } else {
                System.out.println(from.leaves);
                System.out.println(from.children);
//                System.out.println(from.children.get(0L).leaves);
                if(!from.leaves.isEmpty()) {
                    var res = new SimpleTrie(1);
                    res.leaves = new HashSet<>(from.leaves);
                    return res;
                } else {
                    return null;
                }
            }
        }
        if(isConstant[index]) {
            return from.children.get(constantArr[index]);
        }

        var maybeChildren = new HashMap<Long, SimpleTrie>();
        var maybeLeaves = new HashSet<Long>();
        for (var x : from.children.keySet()) {
            var child = getAll(a, from.children.get(x), index + 1, isConstant, constantArr);
//            Assuming the construction of everything else is without error this check is not necessary.
            if(child == null) {
                continue;
            }

            if(child.leaves == null && child.children == null) {
                maybeLeaves.add(x);
            } else {
                maybeChildren.put(x, child);
            }
        }
        if(maybeChildren.isEmpty() && maybeLeaves.isEmpty()) {
            return null;
        }

        if(maybeChildren.isEmpty()) {
            var res = new SimpleTrie(1);
            res.leaves = maybeLeaves;
            return res;
        }
        var res = new SimpleTrie(2);
        res.children = maybeChildren;
        return res;
    }

//    private boolean getAll(Atom a, SimpleTrie to, SimpleTrie from, int index, boolean[] isConstant, long[] constantArr) {
//        if (index == a.ids.size() - 1) {
//            if (isConstant[index]) {
//                return from.leaves.contains(constantArr[index]);
//            } else {
//                if(!from.leaves.isEmpty()) {
//                    to.leaves = new HashSet<>(from.leaves);
//                    return true;
//                } else {
//                    return false;
//                }
//            }
//        }
//        if(isConstant[index]) {
//            var possibly = from.children.get(constantArr[index]);
//            if(possibly == null) {
//                return false;
//            }
//            var child = new SimpleTrie(a.ids.size() - index);
//            var success = getAll(a, child, possibly, index + 1, isConstant, constantArr);
//            if(success) {
//                if(isConstant[index+1]) {
//                    to.children = new HashMap<>();
//                    to.children.put(constantArr[index], child.children.get(constantArr[index+1]));
//                }
//                to.children = new HashMap<>();
//                to.children.put(constantArr[index], child);
//            }
//            return success;
//        }
//
//        var maybeChildren = new HashMap<Long, SimpleTrie>();
//        var success = false;
//        for (var x : from.children.keySet()) {
//            var child = new SimpleTrie(a.ids.size() - index);
//            success |= getAll(a, new SimpleTrie(a.ids.size() - index), from.children.get(x), index, isConstant, constantArr);
////            Assuming the construction of everything else is without error this check is not necessary.
//            if(child.leaves != null || child.children != null) {
//                maybeChildren.put(x, child);
//            }
//        }
//        if(maybeChildren.isEmpty()) {
//            to.children = maybeChildren;
//        }
//        return success;
//    }

//    public void getAll(Atom a, SimpleTrie to, SimpleTrie from, int index) {
//        if(from.leaves != null) {
//            for (var x: from.leaves) {
//                to.leaves = new HashSet<>(from.leaves);
//            }
//        } else {
//            to.children = new HashMap<>();
//            for (var x: from.children.keySet()) {
//                var child = new SimpleTrie(a.ids.size() - index);
//                getAll(a, new SimpleTrie(a.ids.size() - index), from.children.get(x), index);
//                to.children.put(x, child);
//            }
//        }
//    }



    public void combine(SimpleTrie other, Atom atom, Rule rule) {
        outerCombine(other, atom, rule, 0, new ArrayList<>());
    }

    private void outerCombine(SimpleTrie to, Atom atom, Rule rule, int index, List<Long> soFar) {
        if(index == atom.ids.size() -1) {
            SimpleTrie s = innerCombine(atom, rule, soFar);
            if (s.children == null && s.leaves == null) {
                to.leaves.remove(soFar.getLast());
                if(to.leaves.isEmpty()) {
                    to.leaves = null;
                }
                return;
            }
            if(to.children == null) {
                to.children = new HashMap<>();
            }
            to.children.put(soFar.getLast(), s);
            return;
        }
        for(var x: to.children.keySet()) {
            soFar.add(x);
            var child = to.children.get(x);
            outerCombine(child, atom, rule, index+1, soFar);
            soFar.removeLast();
            if (child.children == null || child.leaves == null) {
                to.children.remove(x);
            }
        }
    }

    /**
     *
     * @param atom
     * @param rule
     * @param soFar
     * @return
     */
    private SimpleTrie innerCombine(Atom atom, Rule rule, List<Long> soFar) {
        boolean[] isConstant = new boolean[atom.ids.size()];
        long[] constantArr = new long[atom.ids.size()];
        for(int i = 0; i < isConstant.length; i++) {
            var t = atom.ids.get(i);
            isConstant[i] = !t.isVar || (soFar.size() > rule.varMap.get(t.value));
            constantArr[i] = !t.isVar ? t.value : soFar.get((int) (long) rule.varMap.get(t.value));
        }

//        TODO: Check that clone is correct for constants.
        return this.cloneForTrie(atom, isConstant, constantArr);
    }


//    public TrieSet evaluateConstraints(D constraints, Rule r) {
//
//    }
}
