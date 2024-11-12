package graph;

import java.util.*;

public class Algorithms {

    public static void main(String[] args) throws Exception {
        Graph graph = new Graph(9);
        graph.addEdge(0,1);
        graph.addEdge(0,2);
        graph.addEdge(1,2);
        graph.addEdge(2,3);
        graph.addEdge(2,5);
        graph.addEdge(5,0);
        graph.addEdge(5,2);
        graph.addEdge(3,4);
        graph.addEdge(3,0);
        graph.addEdge(3,6);
        graph.addEdge(6,5);
        graph.addEdge(6,2);
        graph.addEdge(7,6);
        graph.addEdge(8,6);

        int i = 0;
        Set<Set<Integer>> components = TarjanSCC(graph);
        for (Set<Integer> component : components) {
            System.out.println("SCC " + i++ + ":");
            for (int v : component) {
                System.out.println(v);
            }
        }

    }

    public static Set<Set<Integer>> TarjanSCC(Graph graph) {
        int n = graph.size();
        int[] disc = new int[n];
        int[] low = new int[n];
        for (int i = 0; i < n; i++) {
            disc[i] = -1;
            low[i] = -1;
        }

        boolean[] onStack = new boolean[n];
        Stack<Integer> stack = new Stack<>();
        Set<Set<Integer>> components = new HashSet<>();

        int time = 0;
        for (int u : graph.getVertices()) {
            if (disc[u] == -1)
                time = TarjanSCCUtil(graph, u, disc, low, onStack, stack, components, time);
        }
        return components;
    }

    private static int TarjanSCCUtil(Graph graph, int u, int[] disc, int[] low, boolean[] onStack,
                                     Stack<Integer> stack, Set<Set<Integer>> components, int time) {
        disc[u] = time;
        low[u] = time++;
        onStack[u] = true;
        stack.push(u);

        for (int v : graph.getEdgesFrom(u)) {
            if (disc[v] == -1) {
                time = TarjanSCCUtil(graph, v, disc, low, onStack, stack, components, time);
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

//    public static void KosarajuSCC(int[][] adjMatrix) {
//        // Line 1 CLRS
//        int n = adjMatrix.length;
//        int[] ordering = new int[n];
//        for (int i = 0; i < n; i++) {
//            ordering[i] = i;
//        }
//        int[] times = new int[n];
//        DepthFirstSearch(adjMatrix, times, ordering, null, false);
//
//        // Line 2
//        int[][] transAdjMatrix = transposeGraph(adjMatrix);
//
//        // Line 3
//        Arrays.sort(times);
//        ordering = new int[n];
//        for (int i = 0; i < n; i++) {
//            ordering[i] = times[n - i - 1];
//        }
//        times = new int[n];
//
//        List<DepthFirstTree> trees = new ArrayList<>();
//        for (int i = 0; i < n; i++) {
//            trees.add(new DepthFirstTree(-1));
//        }
//        DepthFirstSearch(transAdjMatrix, times, ordering, trees, false);
//
//        // Line 4
//    }
//
//    public static void DepthFirstSearch(int[][] adjMatrix,
//                                        int[] times,
//                                        List<DepthFirstTree> trees,
//                                        boolean createTrees) {
//        int n = adjMatrix.length;
//        boolean[] visited = new boolean[n];
//        int time = 0;
//        for (int i = 0; i < n; i++) {
//            if (visited[i]) continue;
//            for (int j = 0; j < n; j++) {
//                if (adjMatrix[i][j] == 1) {
//                    time = DFSVisit(adjMatrix, i, visited, times, time, trees, createTrees);
//                }
//            }
//        }
//    }
//
//    public static void DepthFirstSearch(int[][] adjMatrix,
//                                        int[] times,
//                                        int[] ordering,
//                                        List<DepthFirstTree> trees,
//                                        boolean createTrees) {
//        int n = adjMatrix.length;
//        boolean[] visited = new boolean[n];
//        int time = 0;
//        for (int i : ordering) {
//            if (visited[i]) continue;
//            for (int j = 0; j < n; j++) {
//                if (adjMatrix[i][j] == 1) {
//                    time = DFSVisit(adjMatrix, i, visited, times, time, trees, createTrees);
//                }
//            }
//        }
//    }
//
//    private static int DFSVisit(int[][] adjMatrix,
//                                int node,
//                                boolean[] visited,
//                                int[] times,
//                                int time,
//                                List<DepthFirstTree> trees,
//                                boolean createTrees) {
//        int n = adjMatrix.length;
//        for (int j = 0; j < n; j++) {
//            if (adjMatrix[node][j] == 1) {
//                if (!visited[j]) continue;
//                time = DFSVisit(adjMatrix, j, visited, times, time);
//            }
//        }
//        visited[node] = true;
//        times[node] = time++;
//        return time;
//    }
}

