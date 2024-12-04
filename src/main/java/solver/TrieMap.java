package solver;

import ast.Atom;
import ast.Program;
import ast.Rule;

import java.util.*;

public class TrieMap implements Map<Long, SimpleTrie> {
    /**
     * Map from predicate names to {@link SimpleTrie}. If the relation is empty, {@link #map} maps to <code>null</code>
     */
    public HashMap<Long, SimpleTrie> map;
    public Program p;

    public TrieMap(Program p) {
        map = new HashMap<>();
        this.p = p;
        for (var id : p.facts.keySet()) {
            if(!p.facts.get(id).isEmpty()) {
                map.put(id, SimpleTrie.trieFrom(p.facts.get(id)));
            }
        }
    }


    /**
     * Initialize with the map. Do not do anything based on the program, except store it
     *
     * @param map
     * @param p
     */
    public TrieMap(HashMap<Long, SimpleTrie> map, Program p) {
        this.map = map;
        this.p = p;
    }

    public Map<Long, Set<List<Long>>> solutionsToPredMap() {
        var res = new HashMap<Long, Set<List<Long>>>();
        var keys = new HashSet<>(p.facts.keySet());
        keys.addAll(p.rules.keySet());
        for (var key : keys) {
            if (map.get(key) == null) {
                res.put(key, new HashSet<>());
            } else {
                res.put(key, toStandardFormat(map.get(key)));
            }
        }
        return res;
    }

    /**
     * Checks whether <code>this</code> is a subset of <code>s</code>
     *
     * @param s the possible superset
     * @return <code>true</code> iff <code>this</code> is a subset of <code>s</code>
     */
    public boolean subsetOf(TrieMap s) {
        for (var x : map.keySet()) {
            var maybeSub = map.get(x);
            var maybeSup = s.map.get(x);
            if (maybeSub == null) {
                continue;
            }
            if (maybeSup == null) {
                return false;
            }
            if (!maybeSub.subsetOf(maybeSup)) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(long pred) {
        return false;
    }

    public TrieMap cloneTrieSet() {
        HashMap<Long, SimpleTrie> newMap = new HashMap<>();
        for (var e : map.entrySet()) {
            newMap.put(e.getKey(), this.cloneTrie(e.getValue()));
        }
        return new TrieMap(newMap, p);
    }

    private SimpleTrie cloneTrie(SimpleTrie value) {
        if (value == null) {
            return null;
        }
        if (value.leaves != null) {
            var res = new SimpleTrie(1);
            res.leaves.addAll(value.leaves);
            return res;
        }
        var res = new SimpleTrie(2);
        for (var e : value.children.entrySet()) {
            res.children.put(e.getKey(), cloneTrie(e.getValue()));
        }
        return res;
    }

    public void removeAll(TrieMap s) {
        for (var x : s.map.keySet()) {
            var source = s.map.get(x);
            if (source == null) {
                continue;
            }
            var destination = map.get(x);
            if (destination == null) {
                continue;
            }
            destination.removeAll(source);
            if(destination.children == null && destination.leaves == null) {
                map.remove(x);
            }
        }
    }

    /**
     * Melds <code>this</code> with <code>s</code> possibly stealing references from <code>s</code>.
     * Destructive for <code>this</code>
     *
     * @param s The source for the meld
     * @return <code>true</code> iff <code>this</code> was changed
     */
    public boolean meld(TrieMap s) {
        boolean change = false;
        for (var x : s.map.keySet()) {
            var source = s.map.get(x);
            if (source == null) {
                continue;
            }
            var destination = map.get(x);
            if (destination == null) {
                map.put(x, s.map.get(x));
                change = true;
                continue;
            }
            change |= destination.meld(source);
        }
        return change;
    }

    /**
     * Melds <code>to</code> with <code>from</code>, possibly stealing references from <code>from</code>.
     * Destructive for <code>to</code>
     *
     * @param to   The destination for the meld
     * @param from The source for the meld
     * @return <code>true</code> iff <code>this</code> was changed
     */
    public SimpleTrie meldSimpleTries(SimpleTrie to, SimpleTrie from) {
        if (to == null) {
            return from;
        }
        if (from == null) {
            return to;
        }
        to.meld(from);
        return to;
    }

    /**
     * Clones the {@link SimpleTrie} associated with <code>a</code>. The result is a deep clone. Example:
     * <code>isConstant=[true,false,false]</code> and <code>constantArr[42,0,0]</code> will copy the
     * source trie, but only the path that starts with 42
     *
     * @param isConstant  Describes which of the values in the source trie that are constants.
     *                    Must have constants described by <code>constantArr</code>
     * @param constantArr Describes the values in the source trie that are constants
     * @return A clone of the source trie under the restrictions posed by <code>isConstant</code> and
     * <code>constantArr</code>. If nothing satisfies the requirements it returns null. If it is satisfiable,
     * but everything is constants, a {@link SimpleTrie} with <code>leaves=children=null</code> is returned
     */
    public static SimpleTrie cloneForTrie(SimpleTrie source, boolean[] isConstant, long[] constantArr, int[][] sameArr) {
        return getAll(source, 0, isConstant, constantArr, sameArr);
    }


    /**
     * Clones the {@link SimpleTrie} associated with <code>a</code>. The result is a deep clone. Example:
     * <code>isConstant=[true,false,false]</code> and <code>constantArr[42,0,0]</code> will copy the
     * source trie, but only the path that starts with 42
     *
     * @param a           {@link Atom} describing the source trie
     * @param isConstant  Describes which of the values in the source trie that are constants.
     *                    Must have constants described by <code>constantArr</code>
     * @param constantArr Describes the values in the source trie that are constants
     * @return A clone of the source trie under the restrictions posed by <code>isConstant</code> and
     * <code>constantArr</code>. If nothing satisfies the requirements it returns null. If it is satisfiable,
     * but everything is constants, a {@link SimpleTrie} with <code>leaves=children=null</code> is returned
     */
    public SimpleTrie cloneForTrie(Atom a, boolean[] isConstant, long[] constantArr, int[][] sameArr) {
        return getAll(map.get(a.pred), 0, isConstant, constantArr, sameArr);
    }

    /**
     * Recursively clones <code>from</code> to the returned {@link SimpleTrie}.
     * If the clone is unsatisfiable, <code>null</code> is returned.
     * If it is satisfiable, but everything is constants, a {@link SimpleTrie} with <code>leaves=children=null</code> is returned
     * Clones <code>from</code> to the returned {@link SimpleTrie}. If the returned value is <code>null</code> it means empty.
     *
     * @param from
     * @param index
     * @param isConstant
     * @param constantArr
     * @return
     */
    private static SimpleTrie getAll(SimpleTrie from, int index, boolean[] isConstant, long[] constantArr, int[][] sameArr) {
        if (from == null) {
            return null;
        }
        if (index == isConstant.length - 1) {
            if (isConstant[index]) {
                if (from.leaves.contains(constantArr[index])) {
                    return new SimpleTrie(-1);
                }
                return null;
            } else {
                if (!from.leaves.isEmpty()) {
                    var res = new SimpleTrie(1);
                    res.leaves = new HashSet<>(from.leaves);
                    return res;
                } else {
                    return null;
                }
            }
        }
        if (isConstant[index]) {
            return getAll(from.children.get(constantArr[index]), index + 1, isConstant, constantArr, sameArr);
        }

        var maybeChildren = new HashMap<Long, SimpleTrie>();
        var maybeLeaves = new HashSet<Long>();
        for (var x : from.children.keySet()) {
//            If we have a requirement that the same variable must occur later on bind it.
            if(sameArr != null && sameArr[index] != null) {
                for(var i = 0; i < sameArr[index].length; i++) {
                    constantArr[sameArr[index][i]] = x;
                }
            }
            var child = getAll(from.children.get(x), index + 1, isConstant, constantArr, sameArr);
//            Assuming the construction of everything else is without error this check is not necessary.
            if (child == null) {
                continue;
            }

            if (child.leaves == null && child.children == null) {
                maybeLeaves.add(x);
            } else {
                maybeChildren.put(x, child);
            }
        }
        if (maybeChildren.isEmpty() && maybeLeaves.isEmpty()) {
            return null;
        }

        if (maybeChildren.isEmpty()) {
            var res = new SimpleTrie(1);
            res.leaves = maybeLeaves;
            return res;
        }
        var res = new SimpleTrie(2);
        res.children = maybeChildren;
        return res;
    }

    /**
     * Combines the previous solution with a new {@link Atom}.
     * The new SimpleTrie will have potentially new variables added as children of the previous leaf-nodes
     *
     * @param old  a {@link SimpleTrie} describing the previous solution
     * @param atom the new {@link Atom}
     * @param rule
     * @return a new {@link SimpleTrie} combining <code>atom</code> with <code>old</code>
     */
    public static SimpleTrie combine(SimpleTrie old, Atom atom, SimpleTrie atomSolution, Rule rule) {
//        If the previous conditions are not satisfiable adding more cannot help
        if (old == null) {
            return null;
        }
//        If there were no variables so far we just return the matches for the next atom
        if (old.children == null && old.leaves == null) {
            return cloneForTrie(atomSolution, atom.constBool, atom.constArr, atom.sameNess);
        }
        return outerCombine(old, atom, atomSolution, rule, 0, new ArrayList<>());
    }

    /**
     * Recurses through <code>old</code>, adding the new paths matching with the
     * {@link SimpleTrie} of <code>atom</code> and pruning branches that do not match
     *
     * @param old   a {@link SimpleTrie} describing the previous solution
     * @param atom  the new {@link Atom}
     * @param rule
     * @param index
     * @param soFar
     */
    private static SimpleTrie outerCombine(SimpleTrie old, Atom atom, SimpleTrie atomSolution, Rule rule, int index, List<Long> soFar) {
        if (old.leaves != null) {
//        if (index == atom.ids.size() - 1) {
//            Iterate over the leaves and get the solutions associated with 'atom'
//            If nothing matches the leaf
//            If all leaves are removed set it to null
            var it = old.leaves.iterator();
//            TODO: Optimize this. Can from length of soFar and at which point in the rule a variable first occurs.
            while (it.hasNext()) {
                var x = it.next();
                soFar.add(x);
                SimpleTrie s = innerCombine(atom, atomSolution, rule, soFar);
                soFar.removeLast();
                if (s == null) {
                    it.remove();
                    continue;
                }
                if (s.children == null && s.leaves == null) {
                    continue;
                }
                if (old.children == null) {
                    old.initializeChildrenIfNull();
                }
                old.children.put(x, s);
//                    it.remove();
            }
            if (old.leaves.isEmpty()) {
                return null;
            }
            if (old.children != null) {
                old.leaves = null;
            }

            return old;
        }
        var newChildren = new HashMap<Long, SimpleTrie>();
        for (var x : old.children.keySet()) {
            soFar.add(x);
            var child = old.children.get(x);
            var newChild = outerCombine(child, atom, atomSolution, rule, index + 1, soFar);
            if (newChild != null) {
                newChildren.put(x, newChild);
            }
            soFar.removeLast();
        }
        old.children = newChildren;
        if (old.children.isEmpty()) {
            return null;
        }
        return old;
    }

    /**
     * @param atom
     * @param rule
     * @param soFar
     * @return
     */
    private static SimpleTrie innerCombine(Atom atom, SimpleTrie atomSolution, Rule rule, List<Long> soFar) {
        var isConstant = atom.constBool;
        var constantArr = atom.constArr;
        var sameArr = atom.sameNess;
//        boolean[] isConstant = new boolean[atom.ids.size()];
//        long[] constantArr = new long[atom.ids.size()];
        for (int i = 0; i < isConstant.length; i++) {
            var t = atom.ids.get(i);
            if (!t.isVar) {
                continue;
            }
            if ((soFar.size() > rule.varMap.get(t.value))) {
                isConstant[i] = true;
                constantArr[i] = soFar.get((int) (long) rule.varMap.get(t.value));
            }
        }
////        TODO: Check that clone is correct for constants.
        return cloneForTrie(atomSolution, isConstant, constantArr, sameArr);
    }

    public Set<List<Long>> toStandardFormat(SimpleTrie s) {
        var res = new HashSet<List<Long>>();
        toStandardFormat(s, res, new ArrayList<>());
        return res;
    }


    private void toStandardFormat(SimpleTrie s, Set<List<Long>> result, ArrayList<Long> soFar) {
        if (s.leaves != null) {
            for (var l : s.leaves) {
                var clone = (ArrayList<Long>) soFar.clone();
                clone.add(l);
                result.add(clone);
            }
            return;
        }

        for (var child : s.children.keySet()) {
            var clone = (ArrayList<Long>) soFar.clone();
            clone.add(child);
            toStandardFormat(s.children.get(child), result, clone);
        }
    }

    @Override
    public String toString() {
        return "TrieSet{" +
                "map=" + map +
                '}';
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public SimpleTrie get(Object key) {
        return map.get(key);
    }

    @Override
    public SimpleTrie put(Long key, SimpleTrie value) {
        return map.put(key, value);
    }

    @Override
    public SimpleTrie remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends Long, ? extends SimpleTrie> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<Long> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<SimpleTrie> values() {
        return map.values();
    }

    @Override
    public Set<Entry<Long, SimpleTrie>> entrySet() {
        return map.entrySet();
    }
}
