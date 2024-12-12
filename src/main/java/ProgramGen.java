import ast.Program;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProgramGen {
    public static void main(String[] args) {
        System.out.println(cartesianProductProblem(1000));
    }

    public static Program reachableProblem(int n) throws RuntimeException {
        StringBuilder res = new StringBuilder();
        createChainEdgeFacts(n, res);
        res.append("\nedge(X,Y) :- edge(Y,X).\n");
        res.append("reachable(X,Y) :- edge(X,Y).\n");
        res.append("reachable(X,Y) :- reachable(X,Z), edge(Z,Y).\n");
        res.append("?-reachable(X,3)");
        return parseStringToProgram(res.toString());
    }

    private static void createChainEdgeFacts(int n, StringBuilder res) {
        for (var i = 0; i < n; i++) {
            res.append("edge(").append(i).append(",").append(i + 1).append(").");
        }
    }

    public static Program clusterProblem(int numClusters, int clusterSize) throws RuntimeException {
        StringBuilder res = new StringBuilder();
        createClusterFacts(numClusters, clusterSize, res);
        res.append("\nedge(X,Y) :- edge(Y,X).\n");
        res.append("reachable(X,Y) :- edge(X,Y).\n");
        res.append("reachable(X,Y) :- reachable(X,Z), edge(Z,Y).\n");
        res.append("?-reachable(X,3)");
        return parseStringToProgram(res.toString());
    }

    private static void createClusterFacts(int numClusters, int clusterSize, StringBuilder res) {
        for (int i = 0; i < numClusters; i++) {
            for (int j = 0; j < clusterSize; j++) {
                int offset = i * clusterSize;
                int u = (j + i * clusterSize);
                int v = ((u + 1) % clusterSize) + offset;
                res.append("edge(").append(u).append(",").append(v).append(").");
            }
        }
    }

    public static String cartesianProductProblem(int numFacts) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < numFacts/2; i++) {
            res.append("p(").append(i).append(",").append(i+1).append(").");
            res.append("q(").append(i).append(",").append(i+1).append(").");
        }
        res.append("\nr(X,Y,Z,W) :- p(X,Y), q(Z,W).\n");
        res.append("?-r(1,Y,Z,W)");
        return res.toString();
    }

    public static Program reachableProblemFromTemplate(String filename, int n) {
        StringBuilder res = new StringBuilder();
        createChainEdgeFacts(n, res);

        try {
            String content = Files.readString(Path.of(Benchmark.benchmarkPath + filename), StandardCharsets.UTF_8);
            res.append(content);
            return parseStringToProgram(res.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Program clusterProblemFromTemplate(String filename, int numClusters, int clusterSize) {
        StringBuilder res = new StringBuilder();
        createClusterFacts(numClusters, clusterSize, res);

        try {
            String content = Files.readString(Path.of(Benchmark.benchmarkPath + filename), StandardCharsets.UTF_8);
            res.append(content);
            return parseStringToProgram(res.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public static Program fileToProgram(String name) throws RuntimeException {
        try {
            var is = new FileInputStream(Main.projectPath + name);
            return new Parser(is).parse();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
