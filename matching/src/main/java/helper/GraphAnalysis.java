package helper;

import com.google.common.graph.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GraphAnalysis
{
    /**
     * Compute Connected components of a {@link com.google.common.graph.ValueGraph} using BFS.
     *
     * @param g the input {@link com.google.common.graph.ValueGraph}
     * @return a components collection of the {@link com.google.common.graph.ImmutableValueGraph}
     */
    public static Collection<ImmutableValueGraph<Long, Double>> connectedComponents(ValueGraph<Long, Float> g) {
        Collection<ImmutableValueGraph<Long, Double>> components = new ArrayList<>();
        Map<Long, Boolean> refIdToNotVisited = g.nodes().stream().collect(Collectors.toMap(Function.identity(), x -> true));
        for (Long r : refIdToNotVisited.keySet()) {
            if (!refIdToNotVisited.get(r))
                continue;
            Queue<Long> queue = new LinkedList<>();
            Collection<weightedEdge> componentEs = new ArrayList<>();
            queue.add(r);
            refIdToNotVisited.put(r, false);
            while (!queue.isEmpty()) {
                Long u = queue.remove();
                g.adjacentNodes(u).stream().filter(refIdToNotVisited::get).forEach(v -> {
                    queue.add(v);
                    refIdToNotVisited.put(v, false);
                    componentEs.add(new weightedEdge(u, v, g.edgeValueOrDefault(u, v, 0f)));
                });
            }
            components.add(createGraph(componentEs));
        }
        return components;
    }

    /**
     * Create a {@link com.google.common.graph.ValueGraph} using a collection of weighted edges
     * @param es a collection of weightedEdges. A weightedEdge consist of u and v nodes and the weight of an edge
     * @return a new {@link com.google.common.graph.ImmutableValueGraph} with the offered edges structure
     */
    private static ImmutableValueGraph<Long,Double> createGraph(Collection<weightedEdge> es) {
        MutableValueGraph<Long, Double> sg = ValueGraphBuilder.directed().build();
        for (weightedEdge e : es)
            sg.putEdgeValue(e.u, e.v, e.w);
        return ImmutableValueGraph.copyOf(sg);
    }

    /**
     * Extract a Guava {@link com.google.common.graph.ValueGraph} using offered edges (es)
     *
     * @param g {@link com.google.common.graph.ValueGraph} which subgraph should be extracted from it
     * @param es edges collections of subgraph
     * @return a new {@link com.google.common.graph.ImmutableValueGraph} with the offered edges structure
     */
    private static ImmutableValueGraph<Long, Double> extractSubGraph(ValueGraph<Long, Double> g, Collection<EndpointPair<Long>> es) {
        MutableValueGraph<Long, Double> sg = ValueGraphBuilder.directed().build();
        for (EndpointPair<Long> e : es)
            sg.putEdgeValue(e.nodeU(), e.nodeV(), g.edgeValueOrDefault(e.nodeU(), e.nodeV(), 0.));
        return ImmutableValueGraph.copyOf(sg);
    }



    public static class weightedEdge {
        public long u;
        public long v;
        public double w;

        public weightedEdge(long u, long v, double w) {
            this.u = u;
            this.v = v;
            this.w = w;
        }
    }
}
