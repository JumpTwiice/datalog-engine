package graph;

import ast.Program;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Util {
    public static Graph graphFromProgram(Program p) throws Exception {
        int n = (int)p.nextPred;
        Graph graph = new Graph(n);
        for (var i: p.rules.keySet()) {
            var rules = p.rules.get(i);
            if (rules == null) continue;
            for (var rule : rules) {
                for (var atom: rule.body) {
                    graph.addEdge((int)(long)atom.pred, (int)(long)rule.head.pred);
                }
            }
        }
        return graph;
    }

    public static List<Set<Integer>> negateNumbers(List<Set<Integer>> s) {
        var res = new ArrayList<Set<Integer>>();
        for(var set: s) {
            var newSet = set.stream().map(x -> -x).collect(Collectors.toCollection(HashSet::new));
            res.add(newSet);
        }
        return res;
    }

    public static int[][] toAdjMatrix(Program p) {
        int n = (int)p.nextPred;
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
