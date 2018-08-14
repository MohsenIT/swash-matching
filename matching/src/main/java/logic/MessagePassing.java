package logic;

import com.google.common.graph.*;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import ds.*;
import helper.GraphAnalysis;

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
        sourceVs.forEach(v -> currentPosition.put(new Message(v, 1.0f), v));
        return this;
    }

    public MessagePassing V(V.Type type){
        return this.V(g.getVs().values().stream().filter(e -> e.getType() == type).collect(Collectors.toList()));
    }


    public MessagePassing out(E.Type type){
        Map<Message, V> nextPosition = HashObjObjMaps.newMutableMap();
        for (Map.Entry<Message, V> entry :currentPosition.entrySet()) {
            for (V v : (Set<V>) entry.getValue().getOutV(type)) { // foreach out V
                Message m = entry.getKey().clone();
                m.incMaxLevel();
                m.setSimilarity(m.getSimilarity()/v.getClusterCount());
                if(v.getType() == V.Type.TOKEN)
                    m.setFirstToken(v);
                nextPosition.put(m, v);
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
                    m.setDestRefV(v);
                else {
                    m.setSimilarity(m.getSimilarity()/v.getClusterCount());
                }
                nextPosition.put(m, v);
            }
        }
        currentPosition = nextPosition;
        return this;
    }

    public Map<V, List<Candidate>> aggregateTerminal() {
        Map<V, Map<V, List<Message>>> result = currentPosition.keySet().stream()
                .collect(Collectors.groupingBy(Message::getDestRefV,
                        Collectors.groupingBy(Message::getOriginRefV)));

        Map<V, List<Candidate>> strongMap = HashObjObjMaps.newMutableMap();
        for (Map.Entry<V, Map<V, List<Message>>> dst: result.entrySet()) {
            Double simThreshold = getSimilarityThreshold(dst);
            List<Candidate> candidateList = new ArrayList<>();

            for (Map.Entry<V, List<Message>> org: dst.getValue().entrySet()) {
                Candidate can = new Candidate(dst.getKey(), org.getKey(), org.getValue());
                if(can.sumSimilarity >= simThreshold && can.messageList.size() >= Candidate.MIN_COMMON_TOKENS_THRESHOLD)
                    candidateList.add(can);
            }
            if(candidateList.size() > 1)
                strongMap.put(dst.getKey(), candidateList.stream()
                        .sorted(Comparator.comparing(Candidate::getSumSimilarity, Comparator.reverseOrder()))
                        .collect(Collectors.toList()));
        }
        return strongMap;
    }

    private Double getSimilarityThreshold(Map.Entry<V, Map<V, List<Message>>> messagesReceivedToRefV) {
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
    public Map<V, Collection<V>> greedyClustering(Map<V, List<Candidate>> candidates) {
        // add similarity edges between tokens (REF_REF edges)
        candidates.values().stream().flatMap(Collection::stream).forEach(c -> {
            if(c.getDestRefV() != c.getOriginRefV())
                g.addE(c.getDestRefV(), c.getOriginRefV(), E.Type.REF_REF, c.sumSimilarity);
        });

        // collect REFs and prioritize them
        List<V> sortedVs = g.getVs(V.Type.REFERENCE).stream()
                .filter(v -> v.hasInOutE(E.Type.REF_REF)).sorted(
                        Comparator.comparing((V v) -> v.getOutE(E.Type.REF_TKN).size())
                                .thenComparing(t -> t.getOutE(E.Type.REF_TKN).stream().filter(e -> ((TokenE)e).getIsAbbr()).count())
                                .thenComparing(V::getWeight, Comparator.reverseOrder())
                ).collect(Collectors.toList());

        // BFS traversal on REF_REF edges
        Map<V, Collection<V>> components = HashObjObjMaps.newMutableMap();
        Map<V, Boolean> refsToNotVisited = sortedVs.stream().collect(Collectors.toMap(Function.identity(), x -> true));
        long maxId = g.getVs().keySet().stream().max(Comparator.naturalOrder()).orElse(1L);
        for (V v : refsToNotVisited.keySet()) {
            if (!refsToNotVisited.get(v))
                continue;
            Queue<V> queue = new LinkedList<>(Collections.singletonList(v));
            refsToNotVisited.put(v, false);
            ClusterV clusterV = (ClusterV) v.getInV(E.Type.CLS_REF);
            Map<TokenE.NamePart, Map<V, Boolean>> reprMap = clusterV.getProfileMap();
            while (!queue.isEmpty()) {
                V u = queue.remove();
                u.getInOutV(E.Type.REF_REF).stream().filter(refsToNotVisited::get).forEach(adj -> {
                    // TODO: 07/08/2018 update token types if increase consensus and cluster again
                    if(true) {
                        queue.add(adj);
                        refsToNotVisited.put(adj, false);
                        u.replaceReferenceCluster(v);
                    }
                });
            }
            clusterV.setProfileMap(reprMap);
        }
        return components;
    }
    //endregion



    public class Message implements Cloneable{
        //region Fields
        private V originRefV;
        private V destRefV;
        private Float similarity;
        private V firstToken;
        private Integer maxLevel = 0;
        //endregion

        //region Getters & Setters
        /**
         * Gets origin REFERENCE vertex of Message
         *
         * @return Vertex of message origin
         */
        public V getOriginRefV() {
            return originRefV;
        }

        public void setOriginRefV(V originRefV) {
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
        public V getDestRefV() {
            return destRefV;
        }

        public void setDestRefV(V destRefV) {
            this.destRefV = destRefV;
        }

        /**
         * Gets firstToken
         *
         * @return value of firstToken
         */
        public V getFirstToken() {
            return firstToken;
        }

        public void setFirstToken(V firstToken) {
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

        public Message(V originRefV, Float similarity) {
            this.originRefV = originRefV;
            this.similarity = similarity;
        }

        public Message(V originRefV, Float similarity, V destRefV, V firstToken, Integer maxLevel) {
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
        public static final int MIN_COMMON_TOKENS_THRESHOLD = 1;

        //region Fields
        private V destRefV;
        private V originRefV;
        private Float sumSimilarity;
        private List<Message> messageList;
        //endregion


        //region Getters & Setters
        /**
         * Gets REFERENCE vertex that the message is received to it.
         *
         * @return V with REFERENCE type of destination
         */
        public V getDestRefV() {
            return destRefV;
        }

        /**
         * Gets REFERENCE vertex that the message is send from it.
         *
         * @return V with REFERENCE type of origin
         */
        public V getOriginRefV() {
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


        public Candidate(V destRefV, V originRefV, List<Message> messageList) {
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
