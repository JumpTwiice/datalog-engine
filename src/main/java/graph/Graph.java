package graph;

import java.util.*;

public class Graph {
    private int size;
    private Map<Integer, List<Integer>> adjList;

    public Graph(int size) {
        this.size = size;
        adjList = new HashMap<>();
        for (int i = 0; i < size; i++) {
            adjList.put(i, new LinkedList<>());
        }
    }

    public void addEdge(int u, int v) throws Exception {
        if (u < size && v < size) {
            List<Integer> list = adjList.get(u);
            list.add(v);
        } else {
            throw new Exception("Attempting to add edges between non-existent vertices");
        }
    }

    public boolean hasEdge(int u, int v) {
        if (u < size) {
            List<Integer> list = adjList.get(u);
            return list.contains(v);
        }
        return false;
    }

    public int size() {
        return size;
    }

    public Set<Integer> getVertices() {
        return adjList.keySet();
    }

    public List<Integer> getNeighbors(int u) {
        return adjList.get(u);
    }

    public Map<Integer, List<Integer>> getAdjList() {
        return adjList;
    }
}
