package custom;

//import com.sun.deploy.util.StringUtils;

import java.util.*;

public class Dijkstras {

    public static void main(String[] args) {
        Graph g = new Graph();
        g.addVertex("A", Arrays.asList(new Vertex("B", 7), new Vertex("C", 8)));
        g.addVertex("B", Arrays.asList(new Vertex("A", 1), new Vertex("F", 2)));
        g.addVertex("C", Arrays.asList(new Vertex("A", 2), new Vertex("F", 6), new Vertex("G", 4)));
        g.addVertex("D", Arrays.asList(new Vertex("F", 8)));
        g.addVertex("E", Arrays.asList(new Vertex("H", 1)));
        g.addVertex("F", Arrays.asList(new Vertex("B", 10), new Vertex("C", 6), new Vertex("D", 8), new Vertex("G", 9), new Vertex("H", 3)));
        g.addVertex("G", Arrays.asList(new Vertex("C", 4), new Vertex("F", 9)));
        g.addVertex("H", Arrays.asList(new Vertex("E", 1), new Vertex("F", 3)));

        String start = "H";
        String finish = "A";
        List<String> path = g.getShortestPath(start, finish);

        Collections.reverse(path);
        //System.out.println(start+"->"+ StringUtils.join(path,"->"));
    }

}

class Vertex implements Comparable<Vertex> {

    private String id;
    private double distance;

    public Vertex(String id, double distance) {
        super();
        this.id = id;
        this.distance = distance;
    }

    public String getId() {
        return id;
    }

    public double getDistance() {
        return distance;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vertex vertex = (Vertex) o;
        return Double.compare(vertex.distance, distance) == 0 &&
                Objects.equals(id, vertex.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, distance);
    }

    @Override
    public String toString() {
        return "Vertex [id=" + id + ", distance=" + distance + "]";
    }

    @Override
    public int compareTo(Vertex o) {
        if (this.distance < o.distance)
            return -1;
        else if (this.distance > o.distance)
            return 1;
        else
            return this.getId().compareTo(o.getId());
    }

}

class Graph {

    private final Map<String, List<Vertex>> vertices;

    public Graph() {
        this.vertices = new HashMap<>();
    }

    public void addVertex(String character, List<Vertex> vertex) {
        this.vertices.put(character, vertex);
    }

    public List<String> getShortestPath(String start, String finish) {
        final Map<String, Double> distances = new HashMap<>();
        final Map<String, Vertex> previous = new HashMap<>();
        PriorityQueue<Vertex> nodes = new PriorityQueue<>();

        distances.put(start, (double) Integer.MAX_VALUE);

        for (String vertex : vertices.keySet()) {
            if (vertex.equalsIgnoreCase(start)) {
                distances.put(vertex, (double) 0);
                nodes.add(new Vertex(vertex, 0));
            } else {
                distances.put(vertex, (double) Integer.MAX_VALUE);
                nodes.add(new Vertex(vertex, Integer.MAX_VALUE));
            }
            previous.put(vertex, null);
        }

        while (!nodes.isEmpty()) {
            Vertex smallest = nodes.poll();
            if (smallest.getId().equalsIgnoreCase(finish)) {
                final List<String> path = new ArrayList<>();
                while (previous.get(smallest.getId()) != null) {
                    path.add(smallest.getId());
                    smallest = previous.get(smallest.getId());
                }
                return path;
            }

            if (distances.get(smallest.getId()) == Integer.MAX_VALUE) {
                break;
            }

            for (Vertex neighbor : vertices.get(smallest.getId())) {
                double alt = distances.get(smallest.getId()) + neighbor.getDistance();
                if (alt < distances.get(neighbor.getId())) {
                    distances.put(neighbor.getId(), alt);
                    previous.put(neighbor.getId(), smallest);

                    forloop:
                    for (Vertex n : nodes) {
                        if (n.getId().equalsIgnoreCase(neighbor.getId())) {
                            nodes.remove(n);
                            n.setDistance(alt);
                            nodes.add(n);
                            break forloop;
                        }
                    }
                }
            }
        }

        return new ArrayList<>(distances.keySet());
    }

}