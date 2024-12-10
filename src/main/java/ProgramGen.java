import ast.Program;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class ProgramGen {
    public static Program hardProblem(int n) throws Exception {
        var res = "";
        for (var i = 0; i < n; i++) {
            res += "edge(" + i + "," + (i + 1) + ").";
        }
        res += "\nedge(X,Y) :- edge(Y,X).\n";
        res += "reachable(X,Y) :- edge(X,Y).\n";
        res += "reachable(X,Y) :- reachable(X,Z), edge(Z,Y).\n";
        res += "?-reachable(X,3)";
        return parseStringToProgram(res);
    }

    public static Program clusterProblem(int numClusters, int clusterSize) throws Exception {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < numClusters; i++) {
            for (int j = 0; j < clusterSize; j++) {
                int offset = i * clusterSize;
                int u = (j + i * clusterSize);
                int v = ((u + 1) % clusterSize) + offset;
                res.append("edge(").append(u).append(",").append(v).append(").");
            }
        }
        res.append("\nedge(X,Y) :- edge(Y,X).\n");
        res.append("reachable(X,Y) :- edge(X,Y).\n");
        res.append("reachable(X,Y) :- reachable(X,Z), edge(Z,Y).\n");
        res.append("?-reachable(X,3)");
        return parseStringToProgram(res.toString());
    }

    public static Program reachableSCCProblem(int numNodes, int depth) throws RuntimeException {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            for (int j = i; j < numNodes; j += (int) Math.pow(2, i + 1)) {
                res.append("edge").append(i).append("(").append(j).append(",").append(j + 1).append(").");
            }
            res.append("\nedge").append(i).append("(X,Y) :- edge").append(i).append("(Y,X).\n");
            if (i > 0)
                res.append("edge").append(i).append("(X,Y) :- reachable").append(i - 1).append("(X,Y).\n");
            res.append("reachable").append(i).append("(X,Y) :- edge").append(i).append("(X,Y).\n");
            res.append("reachable").append(i).append("(X,Y) :- reachable").append(i).append("(X,Z), edge").append(i).append("(Z,Y).\n");
        }
        return parseStringToProgram(res.toString());
    }

    public static Program parseStringToProgram(String s) throws RuntimeException {
        try {
            var is = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
            var parser = new Parser(is);
            return parser.parse();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
