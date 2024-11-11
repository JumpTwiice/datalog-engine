package graph;

import ast.Program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Util {
    public static Map<Integer, List<Integer>> toAdjList(Program p) {
        int n = (int)p.maxPred;
        Map<Integer, List<Integer>> adjList = new HashMap<>();
        for (long i = 0; i < n; i++) {
            var rules = p.rules.get(i);
            if (rules == null) continue;
            for (var rule : rules) {
                for (var atom: rule.body) {
                    adjList.putIfAbsent((int)(long)atom.pred, new ArrayList<>());
                    var list = adjList.get((int)i);
                    list.add((int)i);
                }
            }
        }
        return adjList;
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
