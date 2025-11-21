package org.fog.application;

/**
 * Created by Samodha Pallewatta on 9/15/2019.
 */
import java.util.*;

// This class represents a directed graph using adjacency
// list representation
public class DAG {
    private int V;   // No. of vertices
    private List<String> vertices = new ArrayList<>();
    private HashMap<String,LinkedList<String>> adj = new HashMap<>(); // Adjacency List

    //Constructor
    public DAG(List<String> vertices) {
        V = vertices.size();
        this.vertices =vertices;
        for(String vertice:vertices){
            adj.put(vertice,new LinkedList<>());
       }

    }

    // Function to add an edge into the graph
    public void addEdge(String v, String w) {
        if(adj.containsKey(v) && adj.containsKey(w))
        adj.get(v).add(w);
    }

    // A recursive function used by topologicalSort
    public  void topologicalSortUtil(String v, Map<String,Boolean> visited,
                             Stack stack) {
        // Mark the current node as visited.
        visited.put(v,true);
        String i;

        // Recur for all the vertices adjacent to this
        // vertex
        Iterator<String> it = adj.get(v).iterator();
        while (it.hasNext()) {
            i = it.next();
            if (!visited.get(i))
                topologicalSortUtil(i, visited, stack);
        }

        // Push current vertex to stack which stores result
        stack.push(v);
    }

    // The function to do Topological Sort. It uses
    // recursive topologicalSortUtil()
    public Stack topologicalSort() {
        Stack stack = new Stack();

        // Mark all the vertices as not visited
        Map<String,Boolean> visited = new HashMap<>();
        for (String vertice:adj.keySet())
            visited.put(vertice,false);

        // Call the recursive helper function to store
        // Topological Sort starting from all vertices
        // one by one
        for (String vertice:adj.keySet())
            if (visited.get(vertice) == false)
                topologicalSortUtil(vertice, visited, stack);

        // Print contents of stack
        while (stack.empty() == false)
            System.out.print(stack.pop() + " ");

        return stack;
    }

    public List<String> getSources(List<String> placed,List<String> failed){
        Stack stack = new Stack();
        HashMap<String,LinkedList<String>> adj_temp = new HashMap<>(adj);
        for(String placedM :placed){
            adj_temp.remove(placedM);
        }

        String i;
        for(String failedM:failed){
           removeUnplacedFromAdjacencyList(adj_temp,failedM);
        }

        Map<String,Boolean> visited = new HashMap<>();
        for (String vertice:adj_temp.keySet())
            visited.put(vertice,false);

       //buitd intverse map
        HashMap<String,Boolean> adj_inverse = new HashMap<>();
        for(String vertice:adj_temp.keySet()){
            adj_inverse.put(vertice,true);
        }

        for(String vertice:adj_temp.keySet()){
            Iterator<String> it = adj_temp.get(vertice).iterator();
            while (it.hasNext()){
                i = it.next();
                adj_inverse.put(i,false);
            }
        }

//        for(String failedM:failed){
//            adj_inverse.remove(failedM);
//        }

        List<String> sources = new ArrayList<>();
        for(String module:adj_inverse.keySet()){
            if(adj_inverse.get(module))
                sources.add(module);
        }

        return sources;

    }

    public void removeUnplacedFromAdjacencyList(HashMap<String,LinkedList<String>> adj_temp,String module){
        if(adj_temp.containsKey(module)) {
            Iterator<String> it = adj_temp.get(module).iterator();
            String i;
            while (it.hasNext()) {
                i = it.next();
                removeUnplacedFromAdjacencyList(adj_temp, i);
            }
            if (adj_temp.containsKey(module))
                adj_temp.remove(module);
        }
        return;
    }


}
