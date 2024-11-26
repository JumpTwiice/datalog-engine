package graph;

import java.util.*;

public class Algorithms {

    public static void main(String[] args) throws Exception {
        Graph graph = new Graph(9);
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        graph.addEdge(2, 5);
        graph.addEdge(5, 0);
        graph.addEdge(5, 2);
        graph.addEdge(3, 4);
        graph.addEdge(3, 0);
        graph.addEdge(3, 6);
        graph.addEdge(6, 5);
        graph.addEdge(6, 2);
        graph.addEdge(7, 6);
        graph.addEdge(8, 6);

        int i = 0;
        List<Set<Integer>> components = computeSCCOrder(graph);
        assert components != null;
        for (Set<Integer> component : components) {
            System.out.println("SCC " + i++ + ":");
            for (int v : component) {
                System.out.println(v);
            }
        }
    }

    /**
     * Computes the topological sort of the strongly connected components
     * of the graph
     * @param graph
     * @return a topologically sorted list of strongly connected components
     */
    public static List<Set<Integer>> computeSCCOrder(Graph graph) {
        List<Set<Integer>> components = computeSCC(graph);

        Map<Integer, Integer> rootMap = new HashMap<>();
        Map<Integer, Set<Integer>> componentMap = new HashMap<>();
        int node = 0;
        for (var component : components) {
            componentMap.put(node, component);
            for (var u : component) {
                rootMap.put(u, node);
            }
            node++;
        }

        Graph contractedGraph = new Graph(components.size());
        for (int u : graph.getVertices()) {
            int root = rootMap.get(u);
            for (int v : graph.getNeighbors(u) ) {
                int otherRoot = rootMap.get(v);
                if (root == otherRoot) continue;
                if (contractedGraph.hasEdge(root, otherRoot)) continue;

                try {
                    contractedGraph.addEdge(root, otherRoot);
                } catch (Exception e) {
                    System.out.println(e.toString());
                    return null;
                }
            }
        }

        List<Integer> sorted;
        try {
            sorted = topologicalSort(contractedGraph);
        } catch (Exception e) {
            System.out.println(e.toString());
            return null;
        }

        List<Set<Integer>> result = new ArrayList<>();
        for (var root : sorted) {
            result.add(componentMap.get(root));
        }

        return result;
    }

    public static List<Set<Integer>> computeSCC(Graph graph) {
        int n = graph.size();
        int[] disc = new int[n];
        int[] low = new int[n];
        for (int i = 0; i < n; i++) {
            disc[i] = -1;
            low[i] = -1;
        }

        boolean[] onStack = new boolean[n];
        Stack<Integer> stack = new Stack<>();
        List<Set<Integer>> components = new ArrayList<>();

        int time = 0;
        for (int u : graph.getVertices()) {
            if (disc[u] == -1)
                time = SCCUtil(graph, u, disc, low, onStack, stack, components, time);
        }
        return components;
    }

    private static int SCCUtil(Graph graph, int u, int[] disc, int[] low, boolean[] onStack,
                               Stack<Integer> stack, List<Set<Integer>> components, int time) {
        disc[u] = time;
        low[u] = time++;
        onStack[u] = true;
        stack.push(u);

        for (int v : graph.getNeighbors(u)) {
            if (disc[v] == -1) {
                time = SCCUtil(graph, v, disc, low, onStack, stack, components, time);
                low[u] = Math.min(low[u], low[v]);
            } else if (onStack[v]) {
                low[u] = Math.min(low[u], disc[v]);
            }
        }

        if (low[u] == disc[u]) {
            int w;
            Set<Integer> set = new HashSet<>();
            do {
                w = stack.pop();
                onStack[w] = false;
                set.add(w);
            } while (w != u);
            components.add(set);
        }

        return time;
    }

    public static List<Integer> topologicalSort(Graph graph) throws Exception {
        List<Integer> sorted = new LinkedList<>();
        Color[] colors = new Color[graph.size()];
        Arrays.fill(colors, Color.WHITE);
        for (int u : graph.getVertices()) {
            if (colors[u] == Color.WHITE) {
                topologicalSortVisit(graph, u, colors, sorted);
            }
        }
        return sorted;
    }

    private static void topologicalSortVisit(Graph graph, int node, Color[] colors, List<Integer> sorted) throws Exception {
        colors[node] = Color.GRAY;
        for (int v : graph.getNeighbors(node)) {
            if (colors[v] == Color.WHITE) {
                topologicalSortVisit(graph, v, colors, sorted);
            } else if (colors[v] == Color.GRAY) {
                throw new Exception("Cycle detected");
            }
        }
        colors[node] = Color.BLACK;
        sorted.addFirst(node);
    }
}


