package solver;

import ast.Rule;

import java.util.*;

public class SimpleTrie {
    public Map<Long, SimpleTrie> children;
    public Set<Long> leaves;


//    public SimpleTrie(Atom head) {
////        head.ids
//    }

    public SimpleTrie(int size) {
        if(size == -1) {
            return;
        }
        if(size == 1) {
            leaves = new HashSet<>();
        } else {
            children = new HashMap<>();
        }
    }

    public synchronized boolean initializeLeavesIfNull() {
        if(leaves == null) {
            this.leaves = new HashSet<>();
            return true;
        }
        return false;
    }

    public synchronized boolean cloneIfEmpty(SimpleTrie source) {
        if(children == null && leaves == null) {
            this.children = source.children;
            this.leaves = source.leaves;
            return true;
        }
        return false;
    }


    public synchronized boolean setChildrenIfNull(Map<Long, SimpleTrie> newChild) {
        if(children == null) {
            this.children = newChild;
            return true;
        }
        return false;
    }

    public synchronized boolean setLeavesIfNull(Set<Long> newLeaves) {
        if(leaves == null) {
            this.leaves = newLeaves;
            return true;
        }
        return false;
    }

    public synchronized void initializeChildrenIfNull() {
        if(children == null) {
            this.children = new HashMap<>();
        }
    }



    public static SimpleTrie trieFrom(Set<List<Long>> lists) {
        var returnTrie = new SimpleTrie(lists.iterator().next().size());
        lists.forEach(returnTrie::add);
        return returnTrie;
    }

    public boolean subsetOf(SimpleTrie s) {
        if(leaves != null) {
            return s.leaves.containsAll(leaves);
        }
        for(var x: children.keySet()) {
            if(!s.children.containsKey(x)) {
                return false;
            }
            if(!children.get(x).subsetOf(s.children.get(x))) {
                return false;
            }
        }
        return true;
    }


    public boolean meld(SimpleTrie source) {
        if(source == null) {
            return false;
        }

        boolean wasEmpty = cloneIfEmpty(source);
        if(wasEmpty) {
            return true;
        }

//        if(leaves == null && children == null) {
//            setChildrenIfNull(source.children);
//            leaves = source.leaves;
//            children = source.children;
//            return;
//        }
        if(leaves != null) {
            int size = leaves.size();
            leaves.addAll(source.leaves);
            return size != leaves.size();
        } else {
            boolean change = false;
            for (var c: source.children.keySet()) {
//                TODO: Make safe for concurrency
                if (!children.containsKey(c)) {
                    children.put(c, source.children.get(c));
                    change = true;
                } else {
                    change |= children.get(c).meld(source.children.get(c));
                }
            }
            return change;
        }
    }

    public void add(List<Long> list) {
        addWithIndex(list, 0);
    }

    private void addWithIndex(List<Long> list, int index) {
        if(list.size() - index == 1) {
            leaves.add(list.getLast());
        } else {
            var child = children.computeIfAbsent(list.get(index), x -> new SimpleTrie(list.size() - 1));
            child.addWithIndex(list, index+1);
        }
    }

    public void add(long[] list) {
        addWithIndex(list, 0);
    }

    private void addWithIndex(long[] list, int index) {
        if(list.length - index == 1) {
            leaves.add(list[0]);
        } else {
            var child = children.computeIfAbsent(list[index], x -> new SimpleTrie(list.length - 1));
            child.addWithIndex(list, index+1);
        }
    }

    public void add(long[] list, Set<Long> leaves) {
        addWithIndex(list, 0, leaves);
    }

    private void addWithIndex(long[] list, int index, Set<Long> newLeaves) {
        if(list.length - index == 0) {
            initializeLeavesIfNull();
            leaves.addAll(newLeaves);
        } else {
            initializeChildrenIfNull();
            var child = children.computeIfAbsent(list[index], x -> new SimpleTrie(list.length - 1));
            child.addWithIndex(list, index+1, newLeaves);
        }
    }

//    TODO: Possibly pass the to trie.
    public SimpleTrie projectTo(Rule r) {
        var res = new SimpleTrie(r.positions.length);
        if(children == null && leaves == null) {
            return res;
        }
        projectTo(0, res, r.positions, new long[r.positions.length - 1]);
        return res;
    }

//    Slow. Only use on unordered. TODO: Implement fast on ordered.
    private void projectTo(int index, SimpleTrie result, int[][] positions, long[] temp) {
        System.out.println(index);
        if(index == positions.length -1) {
            if(positions[index] != null) {
                result.add(temp, leaves);
            }
            return;
        }

        if(positions[index] != null) {
//            if(!children.isEmpty()) {
//                result.initializeChildrenIfNull();
//            }
            for(var x: children.keySet()) {
                var callWith = temp.clone();
                for (int i = 0; i < positions[index].length; i++) {
                    callWith[positions[index][i]] = x;
                }
                children.get(x).projectTo(index + 1, result.children.computeIfAbsent(x, ignored -> new SimpleTrie(-1)), positions, callWith);
//                projectTo(index + 1, result.children.get(x), positions, callWith);
            }
            return;
        }

//        Don't need to clone since we are not writing anything.
        for(var x: children.keySet()) {
            projectTo(index + 1, result.children.get(x), positions, temp);
        }
    }

//    public void projectTo(SimpleTrie result, int index, Long[] choices, boolean[] useAsConstant) {
//        if(index == choices.length - 1) {
//            if(useAsConstant[index]) {
//                result.leaves.add(choices[index]);
//            } else {
//                result.leaves = leaves;
//            }
//        }
//        if(useAsConstant[index]) {
//            result.children = new HashMap<>();
//            var child = new SimpleTrie(choices.length - index);
//            projectTo(child, index+1, choices,useAsConstant);
//            result.children.put(choices[index], child);
//            return;
//        }
//        for(var x: children.keySet()) {
//            var child = new SimpleTrie(choices.length - index);
//            children.get(x).projectTo(child, index+1,choices,useAsConstant);
//            children.put(x,child);
//        }
//    }

}
