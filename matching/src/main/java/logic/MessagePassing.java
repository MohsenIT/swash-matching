package logic;

import com.google.common.graph.*;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import dao.*;
import dao.edge.E;
import dao.edge.TokenE;
import dao.vertex.ElementV;
import dao.vertex.RefV;
import dao.vertex.V;
import helper.GraphAnalysis;
import logic.matching.ClusterProfile;
import logic.matching.MatchResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class MessagePassing {

    //region Fields
    private G g;
    private Map<Message, V> currentPosition;
    //endregion

    public MessagePassing(G g) {
        this.g = g;
    }


    //region Traversal & Message Passing Steps
    public MessagePassing V(Collection<V> sourceVs){
        currentPosition =  HashObjObjMaps.newMutableMap(sourceVs.size());
        sourceVs.forEach(v -> currentPosition.put(new Message((RefV) v, 1.0f), v));
        return this;
    }

    public MessagePassing V(V.Type type){
        return this.V(g.getVs().values().stream().filter(e -> e.getType() == type).collect(Collectors.toList()));
    }

    public MessagePassing out(E.Type type){
        Map<Message, V> nextPosition = HashObjObjMaps.newMutableMap();
        for (Map.Entry<Message, V> entry :currentPosition.entrySet()) {
            for (V v : entry.getValue().getOutV(type)) { // foreach out V
                ElementV elementV = (ElementV)v;
                Message m = entry.getKey().clone();
                m.incMaxLevel();
                m.setSimilarity(m.getSimilarity()/elementV.getClusterCount());
                if(elementV.getType() == V.Type.TOKEN)
                    m.setFirstToken(elementV);
                nextPosition.put(m, elementV);
            }
        }
        currentPosition = nextPosition;
        return this;
    }

    public MessagePassing in(E.Type type){
        Map<Message, V> nextPosition = HashObjObjMaps.newMutableMap();
        for (Map.Entry<Message, V> entry :currentPosition.entrySet()) {
            for (V v : (Set<V>) entry.getValue().getInV(type)) { // foreach out V
                Message m = entry.getKey().clone();
                if(v.getType() == V.Type.REFERENCE)
                    m.setDestRefV((RefV) v);
                else {
                    m.setSimilarity(m.getSimilarity()/((ElementV)v).getClusterCount());
                }
                nextPosition.put(m, v);
            }
        }
        currentPosition = nextPosition;
        return this;
    }

    public Map<V, List<Candidate>> aggRefVsTerminal(int minCommonMessages) {
        Map<RefV, Map<RefV, List<Message>>> result = currentPosition.keySet().stream()
                .collect(Collectors.groupingBy(Message::getDestRefV,
                        Collectors.groupingBy(Message::getOriginRefV)));

        Map<V, List<Candidate>> strongMap = HashObjObjMaps.newMutableMap();
        for (Map.Entry<RefV, Map<RefV, List<Message>>> dst: result.entrySet()) {
            Double simThreshold = getSimilarityThreshold(dst);
            List<Candidate> candidateList = new ArrayList<>();

            for (Map.Entry<RefV, List<Message>> org: dst.getValue().entrySet()) {
                Candidate can = new Candidate(dst.getKey(), org.getKey(), org.getValue());
                if(can.sumSimilarity >= simThreshold && can.messageList.size() >= minCommonMessages)
                    candidateList.add(can);
            }
            if(candidateList.size() > 1)
                strongMap.put((RefV)dst.getKey(), candidateList.stream()
                        .sorted(Comparator.comparing(Candidate::getSumSimilarity, Comparator.reverseOrder()))
                        .collect(Collectors.toList()));
        }
        return strongMap;
    }

    private Double getSimilarityThreshold(Map.Entry<RefV, Map<RefV, List<Message>>> messagesReceivedToRefV) {
        Double selfSim = messagesReceivedToRefV.getValue().get(messagesReceivedToRefV.getKey())
                .stream().mapToDouble(Message::getSimilarity).sum();
        return Candidate.SIM_PROPORTION_THRESHOLD * selfSim;
    }
    //endregion

    //region Clustering Candidates
    /**
     * Compute Connected components of CandidateLists using BFS.
     * CandidateLists convert to a guava graph and use BFS on this graph to compute Connected Components.
     *
     * @param candidates Map of Candidate Collections
     * @return Collection of {@link com.google.common.graph.ImmutableValueGraph}s
     */
    public Collection<ImmutableValueGraph<Long, Double>> connectedCandidatesGuavaGraphs(Map<V, List<Candidate>> candidates) {
        MutableValueGraph<Long, Float> graph = ValueGraphBuilder.directed().build();
        candidates.values().stream().flatMap(Collection::stream).forEach(c -> {
            if(c.getDestRefV() != c.getOriginRefV())
                graph.putEdgeValue(c.getDestRefV().getId(), c.getOriginRefV().getId(), c.sumSimilarity);
        });
        return GraphAnalysis.connectedComponents(graph);
    }

    /**
     * Get a collection of {@link com.google.common.graph.ImmutableValueGraph}s and return the corresponding
     * vertices of nodes in the ValueGraph by their ids.
     *
     * @param componentGraphs Collection of {@link com.google.common.graph.ImmutableValueGraph}s
     * @return Map of representative V (currently most frequent) to connected components Vs
     */
    public Map<V, Collection<V>> graphsToClusters(Collection<ImmutableValueGraph<Long, Double>> componentGraphs) {
        Map<V, Collection<V>> components = HashObjObjMaps.newMutableMap();
        for (ImmutableValueGraph<Long, Double> componentGraph : componentGraphs) {
            List<V> componentVs = componentGraph.nodes().stream().map(g::getV).collect(Collectors.toList());
            components.put(componentVs.stream().max(Comparator.comparing(V::getWeight)).get(), componentVs);
        }
        return components;
    }

    /**
     * Get a collection of candidates and cluster them based on a rule-based greedy approach
     *
     * @param candidates Map of the V vertex to their candidates
     * @return Map of representative V (currently most frequent) to connected components Vs
     */
    public void greedyClustering(Map<V, List<Candidate>> candidates) {
        // add similarity edges between tokens (REF_REF edges)
        candidates.values().stream().flatMap(Collection::stream).forEach(c -> {
            if(c.getDestRefV() != c.getOriginRefV())
                g.addE(c.getDestRefV(), c.getOriginRefV(), E.Type.REF_REF, c.sumSimilarity);
        });

        // collect REFs and prioritize them
        List<RefV> sortedVs = g.getVs(V.Type.REFERENCE).stream().map(v -> (RefV)v)
                .filter(v -> v.hasInOutE(E.Type.REF_REF)).sorted(
                        Comparator.comparing((RefV v) -> v.getOutE(E.Type.REF_TKN).size())
                                .thenComparing(t -> t.getOutE(E.Type.REF_TKN).stream().filter(e -> ((TokenE)e).getIsAbbr()).count())
                                .thenComparing(V::getWeight, Comparator.reverseOrder())
                ).collect(Collectors.toList());

        // BFS traversal on REF_REF edges
        Map<RefV, Boolean> refsToNotVisited = sortedVs.stream().collect(Collectors.toMap(Function.identity(), x -> true, (a,b)->a, LinkedHashMap::new));
        long maxId = g.getVs().keySet().stream().max(Comparator.naturalOrder()).orElse(1L);
        for (RefV v : refsToNotVisited.keySet()) {
            if (!refsToNotVisited.get(v))
                continue;
            Queue<RefV> queue = new LinkedList<>(Collections.singletonList(v));
            refsToNotVisited.put(v, false);
            ClusterProfile clusterProfile = v.getRefClusterV().getProfile();
            while (!queue.isEmpty()) {
                RefV u = queue.remove();
                u.getInOutV(E.Type.REF_REF).stream().map(e -> (RefV)e)
                        .filter(refsToNotVisited::get).forEach((RefV adj) -> {
                    MatchResult result = clusterProfile.match(adj);
                    boolean isConsistent = result.isConsistent();
                    if(!isConsistent){
                        result.canBecomeConsistent();
                    }
                    if(isConsistent) {
                        queue.add(adj);
                        refsToNotVisited.put(adj, false);
                        adj.replaceReferenceCluster(u);
                        clusterProfile.merge(result);
                    }
                    if(isConsistent && u.getRefResolvedIdV() != adj.getRefResolvedIdV()){
                        System.out.printf("%s\t%s\t%s%n", u.getVal(), adj.getVal(), clusterProfile);
                    }
                });
            }
        }
    }
    //endregion



    public class Message implements Cloneable{
        //region Fields
        private RefV originRefV;
        private RefV destRefV;
        private Float similarity;
        private ElementV firstToken;
        private Integer maxLevel = 0;
        //endregion

        //region Getters & Setters
        /**
         * Gets origin REFERENCE vertex of Message
         *
         * @return Vertex of message origin
         */
        public RefV getOriginRefV() {
            return originRefV;
        }

        public void setOriginRefV(RefV originRefV) {
            this.originRefV = originRefV;
        }

        /**
         * Gets similarity
         *
         * @return value of similarity
         */
        public Float getSimilarity() {
            return similarity;
        }

        public void setSimilarity(Float similarity) {
            this.similarity = similarity;
        }

        /**
         * Gets destRefV
         *
         * @return value of destRefV
         */
        public RefV getDestRefV() {
            return destRefV;
        }

        public void setDestRefV(RefV destRefV) {
            this.destRefV = destRefV;
        }

        /**
         * Gets firstToken
         *
         * @return value of firstToken
         */
        public ElementV getFirstToken() {
            return firstToken;
        }

        public void setFirstToken(ElementV firstToken) {
            this.firstToken = firstToken;
        }

        /**
         * Gets maxLevel
         *
         * @return value of maxLevel
         */
        public Integer getMaxLevel() {
            return maxLevel;
        }

        public void setMaxLevel(Integer maxLevel) {
            this.maxLevel = maxLevel;
        }

        public void incMaxLevel() {
            this.maxLevel = maxLevel + 1;
        }
        //endregion

        public Message(RefV originRefV, Float similarity) {
            this.originRefV = originRefV;
            this.similarity = similarity;
        }

        public Message(RefV originRefV, Float similarity, RefV destRefV, ElementV firstToken, Integer maxLevel) {
            this.originRefV = originRefV;
            this.similarity = similarity;
            this.destRefV = destRefV;
            this.firstToken = firstToken;
            this.maxLevel = maxLevel;
        }

        @Override
        public Message clone() {
            return new Message(this.originRefV, this.similarity, this.destRefV, this.firstToken, this.maxLevel);
        }

        @Override
        public String toString() {
            return String.format("MSG{origin=%s, sim=%s, firstToken=%s}", originRefV, similarity, firstToken);
        }
    }

    public class Candidate {

        public static final float SIM_PROPORTION_THRESHOLD = 0.5f;
        public static final int MIN_COMMON_TOKENS_THRESHOLD = 2;

        //region Fields
        private RefV destRefV;
        private RefV originRefV;
        private Float sumSimilarity;
        private List<Message> messageList;
        //endregion


        //region Getters & Setters
        /**
         * Gets REFERENCE vertex that the message is received to it.
         *
         * @return V with REFERENCE type of destination
         */
        public RefV getDestRefV() {
            return destRefV;
        }

        /**
         * Gets REFERENCE vertex that the message is send from it.
         *
         * @return V with REFERENCE type of origin
         */
        public RefV getOriginRefV() {
            return originRefV;
        }

        /**
         * Gets count of message from the origin that received to the current V.
         *
         * @return Integer Message Count
         */
        public Integer getCntMessage() {
            return messageList.size();
        }

        /**
         * Gets sum of all messages similarity from the destination V.
         *
         * @return Float number of Messages similarity
         */
        public Float getSumSimilarity() {
            return sumSimilarity;
        }

        /**
         * Gets All of Message from the origin received to destination V.
         *
         * @return List of Messages
         */
        public List<Message> getMessageList() {
            return messageList;
        }
        //endregion


        public Candidate(RefV destRefV, RefV originRefV, List<Message> messageList) {
            this.destRefV = destRefV;
            this.originRefV = originRefV;
            this.messageList = messageList;
            this.sumSimilarity = (float)messageList.stream().mapToDouble(Message::getSimilarity).sum();
        }


        @Override
        public String toString() {
            return String.format("CAN{origin=%s, cnt=%d, sim=%.3f, messages=%s}"
                    , originRefV, getCntMessage(), sumSimilarity
                    , messageList.stream().map(Message::getFirstToken).collect(Collectors.toList()));
        }
    }
}
