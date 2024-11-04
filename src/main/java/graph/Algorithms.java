package graph;

import ast.Program;

import java.util.*;

import static graph.Util.toGraph;
import static graph.Util.transposeGraph;

public class Algorithms {
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
            ordering[i] = times[n-i-1];
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
                                        int[] ordering,
                                        List<DepthFirstTree> trees,
                                        boolean createTrees) {
        int n = adjMatrix.length;
        boolean[] visited = new boolean[n];
        for (int i: ordering) {
            if (visited[i]) continue;
            for (int j = 0; j < n; j++) {
                if (adjMatrix[i][j] == 1) {
                    DFSVisit(adjMatrix, i, visited, times, 0, trees, createTrees);
                }
            }
        }
    }

    public static int DFSVisit(int[][] adjMatrix,
                               int node,
                               boolean[] visited,
                               int[] times,
                               int time,
                               List<DepthFirstTree> trees,
                               boolean createTrees) {
        int n = adjMatrix.length;
        for (int j = 0; j < n; j++) {
            if(adjMatrix[node][j] == 1) {
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

