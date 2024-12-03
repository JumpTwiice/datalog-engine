package solver;

import ast.Rule;

import java.util.*;

public class SimpleTrie {
    public Map<Long, SimpleTrie> children;
    public Set<Long> leaves;

    public SimpleTrie(int depth) {
        if (depth <= 0) {
            return;
        }
        if (depth == 1) {
            leaves = new HashSet<>();
        } else {
            children = new HashMap<>();
        }
    }

    /**
     * Sets {@link #leaves} to be empty if it is currently <node>null</node>
     *
     * @return <code>true</code> iff {@link #leaves} was <code>null</code>
     */
    public synchronized boolean initializeLeavesIfNull() {
        if (leaves == null) {
            this.leaves = new HashSet<>();
            return true;
        }
        return false;
    }

    /**
     * If <code>this</code> is empty, clone references to <code>source</code>'s attributes.
     *
     * @param source The source node
     * @return Whether <code>source</code>'s attributes were cloned
     */
    public synchronized boolean cloneIfEmpty(SimpleTrie source) {
        if (source == null) {
            return false;
        }
        if (children == null && leaves == null) {
            this.children = source.children;
            this.leaves = source.leaves;
            return source.leaves != null || source.children != null;
        }
        return false;
    }


    /**
     * If {@link #children} is currently null, set it to <code>newChild</code>
     *
     * @param newChild The new {@link #children} attribute for <code>this</code>
     * @return Whether {@link #children} was changed
     */
    public synchronized boolean setChildrenIfNull(Map<Long, SimpleTrie> newChild) {
        if (children == null) {
            this.children = newChild;
            return true;
        }
        return false;
    }

    /**
     * If {@link #leaves} is currently null, set it to <code>newLeaves</code>
     *
     * @param newLeaves The new {@link #leaves} attribute for <code>this</code>
     * @return Whether {@link #leaves} was changed
     */
    public synchronized boolean setLeavesIfNull(Set<Long> newLeaves) {
        if (leaves == null) {
            this.leaves = newLeaves;
            return true;
        }
        return false;
    }

    /**
     * Sets {@link #children} to empty if they are currently <code>null</code>
     *
     * @return <code>true</code> iff the children were <code>null</code>
     */

    public synchronized boolean initializeChildrenIfNull() {
        if (children == null) {
            this.children = new HashMap<>();
            return true;
        }
        return false;
    }


    /**
     * Create a {@link SimpleTrie} from a predicate (set of tuples)
     *
     * @param lists the predicate to create a {@link SimpleTrie} from, i.e., a set of tuples
     * @return A {@link SimpleTrie} over the constants of the tuples
     */
    public static SimpleTrie trieFrom(Set<List<Long>> lists) {
        var returnTrie = new SimpleTrie(lists.iterator().next().size());
        lists.forEach(returnTrie::add);
        return returnTrie;
    }

    /**
     * Check if <code>this</code> is a subset of <code>s</code>
     *
     * @param s The source {@link SimpleTrie}
     * @return <code>true</code> if <code>this</code> is a subset of <code>s</code>
     */
    public boolean subsetOf(SimpleTrie s) {
        if (s == null) {
            return false;
        }
        if (leaves != null) {
            return s.leaves.containsAll(leaves);
        }
        for (var x : children.keySet()) {
            if (!s.children.containsKey(x)) {
                return false;
            }
            if (!children.get(x).subsetOf(s.children.get(x))) {
                return false;
            }
        }
        return true;
    }

    public void removeAll(SimpleTrie source) {
        if (source == null) {
            return;
        }

        if (leaves != null) {
            leaves.removeAll(source.leaves);
            if(leaves.isEmpty()) {
                leaves = null;
            }
        }
        if(source.children == null || children == null) {
            return;
        }

//        TODO: Looping through this might be more efficient
        for (var c : source.children.keySet()) {
//                TODO: Make safe for concurrency
            if (children.containsKey(c)) {
                children.get(c).removeAll(source.children.get(c));
                if(children.get(c).children == null && children.get(c).leaves == null) {
                    children.remove(c);
                }
            }
        }
        if(children.isEmpty()) {
            children = null;
        }
    }


    /**
     * Melds <code>this</code> with <code>source</code> potentially taking references
     * from <code>source</code> and adding them to <code>this</code>
     * Destructive for <code>this</code>
     *
     * @param source The source {@link SimpleTrie}
     * @return <code>true</code> if <code>source</code> contained values not in <code>this</code>
     */
    public boolean meld(SimpleTrie source) {
        if (source == null) {
            return false;
        }

        boolean wasEmpty = cloneIfEmpty(source);
        if (wasEmpty) {
            return true;
        }

        if (leaves != null) {
            int size = leaves.size();
            leaves.addAll(source.leaves);
            return size != leaves.size();
        }
        if (source.children == null) {
            return false;
        }


        boolean change = false;
        for (var c : source.children.keySet()) {
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

    /**
     * Add <code>list</code> to <code>this</code>
     *
     * @param list
     */
    public void add(List<Long> list) {
        addWithIndex(list, 0);
    }

    /**
     * Add <code>list</code> to <code>this</code>. Recursively goes down the {@link SimpleTrie}
     * while updating/creating the path
     *
     * @param list
     * @param index
     */
    private void addWithIndex(List<Long> list, int index) {
        if (list.size() - index == 1) {
            leaves.add(list.getLast());
        } else {
            var child = children.computeIfAbsent(list.get(index), x -> new SimpleTrie(list.size() - 1));
            child.addWithIndex(list, index + 1);
        }
    }

    /**
     * Add <code>list</code> to <code>this</code>
     *
     * @param list
     */
    public void add(long[] list) {
        addWithIndex(list, 0);
    }

    private void addWithIndex(long[] list, int index) {
        if (list.length - index == 1) {
            if (leaves == null) {
                initializeLeavesIfNull();
            }
            leaves.add(list[list.length - 1]);
        } else {
            initializeChildrenIfNull();
            var child = children.computeIfAbsent(list[index], x -> new SimpleTrie(list.length - 1));
            child.addWithIndex(list, index + 1);
        }
    }

    /**
     * Add <code>list::l</code> for all <code>l</code> in leaves to <code>this</code>
     *
     * @param list
     */
    public void add(long[] list, Set<Long> leaves) {
        addWithIndex(list, 0, leaves);
    }

    /**
     * Add <code>list::l</code> for all <code>l</code> in leaves to <code>this</code>.
     * Recursively goes down the trie while updating/creating the path.
     *
     * @param list
     */
    private void addWithIndex(long[] list, int index, Set<Long> newLeaves) {
        if (list.length - index == 0) {
            initializeLeavesIfNull();
            leaves.addAll(newLeaves);
        } else {
            initializeChildrenIfNull();
            var child = children.computeIfAbsent(list[index], x -> new SimpleTrie(list.length - 1));
            child.addWithIndex(list, index + 1, newLeaves);
        }
    }

    /**
     * Performs a relational projection from <code>r</code>'s body "attributes" to the resulting
     * tuple represented in the <code>r</code>'s head
     *
     * @param r A {@link Rule}
     * @return A {@link SimpleTrie} representing <code>r</code>'s head
     */
//    TODO: Possibly pass the to trie.
    public SimpleTrie projectTo(Rule r) {
        var res = new SimpleTrie(-1);
        projectTo(0, res, r.positions, r.headHelper);
        return res;
    }

    /**
     * Helper method that performs a relational projection
     *
     * @param index     the index of the current variable
     * @param result    the result of the projection which is being built
     * @param positions the sequence of "attributes" being projected to. Represented as
     *                  positions that the variables of the body appear in the head atom
     * @param temp      array representation of the relation being built
     */
//    Slow. Only use on unordered. TODO: Implement fast on ordered.
    private void projectTo(int index, SimpleTrie result, int[][] positions, long[] temp) {
        if (index == positions.length) {
            result.add(temp);
            return;
        }
        if (index == positions.length - 1) {
            if (positions[index] == null) {
                projectTo(index + 1, result, positions, temp);
                return;
            }
            for (var x : leaves) {
                for (int i = 0; i < positions[index].length; i++) {
                    temp[positions[index][i]] = x;
                }
                projectTo(index + 1, result, positions, temp);
            }

            return;
        }

        if (positions[index] != null) {
            if (result.children == null) {
                result.initializeChildrenIfNull();
            }
            for (var x : children.keySet()) {
                var callWith = temp.clone();
                for (int i = 0; i < positions[index].length; i++) {
                    callWith[positions[index][i]] = x;
                }
                children.get(x).projectTo(index + 1, result, positions, callWith);
            }
            return;
        }
//        Don't need to clone since we are not writing anything.
        for (var x : children.keySet()) {
            children.get(x).projectTo(index + 1, result, positions, temp);
        }
    }

    @Override
    public String toString() {
        if (leaves != null && children != null) {
            return "SimpleTrie{ERROR! Both children and leaves present}";
        }

        if (leaves == null && children == null) {
            return "SimpleTrie{Empty}";
        }

        if (leaves != null) {
            return "SimpleTrie{" + leaves + "}";
        }

        return "SimpleTrie{" + children + "}";
    }
}
