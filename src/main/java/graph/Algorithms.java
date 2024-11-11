package graph;

import java.util.*;

import static graph.Util.transposeGraph;

public class Algorithms {

    public static void main(String[] args) {
        int[][] adjMatrix = {
                new int[]{0,1,1,0,0,0,0,0,0},
                new int[]{0,0,1,0,0,0,0,0,0},
                new int[]{0,0,0,1,0,1,0,0,0},
                new int[]{1,0,0,0,1,0,1,0,0},
                new int[]{0,0,0,0,0,0,0,0,0},
                new int[]{1,0,1,0,0,0,0,0,0},
                new int[]{0,0,1,0,0,1,0,0,0},
                new int[]{0,0,0,0,0,0,1,0,0},
                new int[]{0,0,0,0,0,0,1,0,0}
        };
        int i = 0;
        Set<Set<Integer>> components = TarjanSCC(adjMatrix);
        for (Set<Integer> component : components) {
            System.out.println("SCC " + i++ + ":");
            for (int v : component) {
                System.out.println(v);
            }
        }
    }

    public static Set<Set<Integer>> TarjanSCC(Map<Integer, List<Integer>> adjList) {
        int n = adjList.size();
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
        for (int u : adjList.keySet()) {
            if (disc[u] == -1)
                time = TarjanSCCUtil(adjList, u, disc, low, onStack, stack, components, time);
        }
        return components;
    }

    private static int TarjanSCCUtil(Map<Integer, List<Integer>> adjList, int u, int[] disc, int[] low, boolean[] onStack,
                                     Stack<Integer> stack, Set<Set<Integer>> components, int time) {
        disc[u] = time;
        low[u] = time++;
        onStack[u] = true;
        stack.push(u);

        for (int v : adjList.get(u)) {
            if (disc[u] == -1) {
                time = TarjanSCCUtil(adjList, v, disc, low, onStack, stack, components, time);
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

    public static Set<Set<Integer>> TarjanSCC(int[][] adjMatrix) {
        int n = adjMatrix.length;
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
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (adjMatrix[i][j] == 0) continue;
                if (disc[i] == -1)
                    time = TarjanSCCUtil(adjMatrix, i, disc, low, onStack, stack, components, time);
            }
        }
        return components;
    }

    private static int TarjanSCCUtil(int[][] adjMatrix, int u, int[] disc, int[] low, boolean[] onStack,
                                     Stack<Integer> stack, Set<Set<Integer>> components, int time) {
        int n = adjMatrix.length;
        disc[u] = time;
        low[u] = time++;
        onStack[u] = true;
        stack.push(u);

        for (int v = 0; v < n; v++) {
            if (adjMatrix[u][v] == 0) continue;
            if (disc[u] == -1) {
                time = TarjanSCCUtil(adjMatrix, v, disc, low, onStack, stack, components, time);
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

    public static void KosarajuSCC(int[][] adjMatrix) {
        // Line 1 CLRS
        int n = adjMatrix.length;
        int[] ordering = new int[n];
        for (int i = 0; i < n; i++) {
            ordering[i] = i;
        }
        int[] times = new int[n];
        DepthFirstSearch(adjMatrix, times, ordering, null, false);

        // Line 2
        int[][] transAdjMatrix = transposeGraph(adjMatrix);

        // Line 3
        Arrays.sort(times);
        ordering = new int[n];
        for (int i = 0; i < n; i++) {
            ordering[i] = times[n - i - 1];
        }
        times = new int[n];

        List<DepthFirstTree> trees = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            trees.add(new DepthFirstTree(-1));
        }
        DepthFirstSearch(transAdjMatrix, times, ordering, trees, false);

        // Line 4
    }

    public static void DepthFirstSearch(int[][] adjMatrix,
                                        int[] times,
                                        List<DepthFirstTree> trees,
                                        boolean createTrees) {
        int n = adjMatrix.length;
        boolean[] visited = new boolean[n];
        int time = 0;
        for (int i = 0; i < n; i++) {
            if (visited[i]) continue;
            for (int j = 0; j < n; j++) {
                if (adjMatrix[i][j] == 1) {
                    time = DFSVisit(adjMatrix, i, visited, times, time, trees, createTrees);
                }
            }
        }
    }

    public static void DepthFirstSearch(int[][] adjMatrix,
                                        int[] times,
                                        int[] ordering,
                                        List<DepthFirstTree> trees,
                                        boolean createTrees) {
        int n = adjMatrix.length;
        boolean[] visited = new boolean[n];
        int time = 0;
        for (int i : ordering) {
            if (visited[i]) continue;
            for (int j = 0; j < n; j++) {
                if (adjMatrix[i][j] == 1) {
                    time = DFSVisit(adjMatrix, i, visited, times, time, trees, createTrees);
                }
            }
        }
    }

    private static int DFSVisit(int[][] adjMatrix,
                                int node,
                                boolean[] visited,
                                int[] times,
                                int time,
                                List<DepthFirstTree> trees,
                                boolean createTrees) {
        int n = adjMatrix.length;
        for (int j = 0; j < n; j++) {
            if (adjMatrix[node][j] == 1) {
                if (!visited[j]) continue;
                if (createTrees) trees.get(j).parent = node;
                time = DFSVisit(adjMatrix, j, visited, times, time, trees, createTrees);
            }
        }
        visited[node] = true;
        times[node] = time++;
        return time;
    }
}

