package graph;

import ast.Program;

public class Util {
    public static Graph graphFromProgram(Program p) throws Exception {
        int n = (int)p.maxPred;
        Graph graph = new Graph(n);
        for (long i = 0; i < n; i++) {
            var rules = p.rules.get(i);
            if (rules == null) continue;
            for (var rule : rules) {
                for (var atom: rule.body) {
                    graph.addEdge((int)(long)atom.pred, (int)i);
                }
            }
        }
        return graph;
    }

    public static int[][] toAdjMatrix(Program p) {
        int n = (int)p.maxPred;
        int[][] adjMatrix = new int[n][n];
        for (long i = 0; i < n; i++) {
            var rules = p.rules.get(i);
            if (rules == null) continue;
            for (var rule : rules) {
                for (var atom: rule.body) {
                    adjMatrix[(int)(long)atom.pred][(int)i] = 1;
                }
            }
        }
        return adjMatrix;
    }

    public static int[][] transposeGraph(int[][] adjMatrix) {
        int n = adjMatrix.length;
        int[][] transAdjMatrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (adjMatrix[i][j] == 1) {
                    transAdjMatrix[j][i] = 1;
                }
            }
        }
        return transAdjMatrix;
    }


}
