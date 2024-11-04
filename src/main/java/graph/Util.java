package graph;

import ast.Program;

public class Util {
    public static int[][] toGraph(Program p) {
        int n = p.rules.size();
        int[][] adjMatrix = new int[n][n];
        for (int i = 0; i < p.rules.size(); i++) {
            var rule = p.rules.get(i);
            for (int j = 0; j < rule.body.size(); j++) {
                adjMatrix[i][j] = 1;
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
