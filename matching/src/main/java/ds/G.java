package ds;

import com.koloboke.collect.map.hash.HashLongObjMaps;
import com.koloboke.collect.set.hash.HashObjSets;
import ds.TokenE.NamePart;
import helper.IO;

import java.util.*;
import java.util.stream.Collectors;

public class G {

    //region Fields
    private Set<E> es;
    private Map<Long, V> vs;
    //endregion

    //region Getters & Setters

    /**
     * Gets all vertices of the Graph.
     *
     * @return Map of graph vertices. The key is numeric vertex id and value is V.
     */
    public Map<Long, V> getVs() {
        return vs;
    }

    /**
     * Gets all graph vertices with specified type.
     *
     * @param type : a specific type of vertices
     * @return Set of graph vertices
     */
    public Set<V> getVs(V.Type type) {
        return vs.values().stream().filter(v -> v.getType()==type).collect(Collectors.toSet());
    }

    /**
     * Gets a vertex with id.
     *
     * @param id : Long id of a specific vertex
     * @return a vertex if exist else null
     */
    public V getV(Long id) {
        return vs.getOrDefault(id, null);
    }

    public void setVs(Map<Long, V> vs) {
        this.vs = vs;
    }

    /**
     * Add a single vertex to the Graph vertices.
     *
     * @param v : a vertex
     */
    public void addV(V v) {
        this.vs.put(v.getId(), v);
    }





    /**
     * Gets all edges of the Graph.
     *
     * @return Set of graph edges
     */
    public Set<E> getEs() {
        return es;
    }

    public void setEs(Set<E> es) {
        this.es = es;
    }

    /**
     * Add an edge to the Graph.
     *
     * @param inV from vertex of edge
     * @param outV to vertex of edge
     * @param type type of edge
     * @param weight weight of edge    */
    public void addE(V inV, V outV, E.Type type, Float weight) {
        this.es.add(new E(inV, outV, type, weight));
    }

    /**
     * Add an edge to the Graph Edges.
     *
     * @param e from vertex of edge    */
    public void addE(E e) {
        this.es.add(e);
    }

    //endregion

    /**
     * Generate graph using vertices and edges adjacency list file
     *
     * @param vertexFilePath csv file contains vertices fields
     * @param edgeFilePath csv file that store edges fields
     */
    public void init(String vertexFilePath, String edgeFilePath) {
        List<String[]> vertices = IO.readCSVLines(vertexFilePath);
        vs = HashLongObjMaps.newMutableMap(40000);
        assert vertices != null;
        for (int i = 1; i < vertices.size(); i++) {
            String[] l = vertices.get(i);
            vs.put(Long.valueOf(l[0]), new V(l[0], l[1], l[2], l[3]));
        }

        List<String[]> edges = IO.readCSVLines(edgeFilePath);
        assert edges != null;
        es = HashObjSets.newMutableSet(80000);
        for (int i = 1; i < edges.size(); i++) {
            String[] l = edges.get(i);

            V inV = vs.get(Long.valueOf(l[0]));
            V outV = vs.get(Long.valueOf(l[1]));
            E e = l[4].equals("REF_TKN") ? new TokenE(inV, outV, l[4], l[5]) : new E(inV, outV, l[4], l[5]);
            inV.addOutE(e);
            outV.addInE(e);
            es.add(e);
        }
        System.out.println("\tGraph object is initiated successfully.");
    }


    /**
     * Assign a cluster vertex to each REFERENCE vertices.
     * The clusters change during resolution.
     *
     */
    public void initClusters() {
        long maxId = vs.keySet().stream().mapToLong(Long::longValue).max().orElse(1);
        Set<V> refVs = vs.values().stream().filter(e -> e.getType() == V.Type.REFERENCE).collect(Collectors.toSet());
        for (V refV : refVs) {
            ClusterV clusV = new ClusterV(++maxId, refV);
            E clusE = new E(clusV, refV, E.Type.CLS_REF, 1.0f);
            refV.addInE(clusE);
            clusV.addOutE(clusE);
            this.addE(clusE);
            this.addV(clusV);
        }
        System.out.println("\tA Cluster vertex is assigned to each REF vertex");
    }

    /**
     * Assign name's part type (firstname, lastname , ...) to the REF_TKN edges
     */
    public void initNamesPart() {
        List<V> refVs = vs.values().stream().filter(e -> e.getType() == V.Type.REFERENCE).collect(Collectors.toList());
        for (V refV : refVs) {
            List<TokenE> tokenEs = refV.getOutE(E.Type.REF_TKN).stream().map(e -> (TokenE) e).sorted(
                    Comparator.comparing(TokenE::getIsAbbr)
                            .thenComparing(TokenE::getOrder, Comparator.reverseOrder()))
                    .collect(Collectors.toList());

            TokenE lname, fname;
            lname = tokenEs.get(0);
            lname.setPart(NamePart.LASTNAME);
            tokenEs.remove(lname);
            fname = tokenEs.stream().sorted(Comparator.comparing(TokenE::getOrder)).findFirst().orElse(null);
            if (fname != null) {
                fname.setPart(NamePart.FIRSTNAME);
                tokenEs.remove(fname);
                for (TokenE e : tokenEs) {
                    if(e.getOrder() > lname.getOrder())
                        e.setPart(NamePart.SUFFIX);
                    else if(e.getOrder() > fname.getOrder() && e.getOrder() < lname.getOrder())
                        e.setPart(NamePart.MIDDLENAME);
                    else e.setPart(NamePart.PREFIX);
                }
            }

        }
        System.out.println("\tInitial Name's part is assigned to REF_TKN edges.");
//        List<TokenE> eList = es.stream().filter(e -> e.getType() == E.Type.REF_TKN).map(e -> (TokenE) e).collect(Collectors.toList());
//        System.out.println(eList.size());
    }

    /**
     * update cluster edges according to the clustering result
     *
     * @param clusters a map of cluster Vs and their representative
     */
    public void updateClusters(Map<V, Collection<V>> clusters) {
        for (Map.Entry<V, Collection<V>> cluster : clusters.entrySet()) {
           for (V v : cluster.getValue())
                v.replaceReferenceCluster(cluster.getKey());
        }
    }

    /**
     * update cluster edges according to the their actual resolved_id to calculate max achievable F1.
     *
     * @param allCandidatesVs all vertices in the candidates collection
     */
    public void updateClustersToRealClusters(Collection<V> allCandidatesVs) {
        Queue<V> queue = new LinkedList<>(allCandidatesVs);
        while (!queue.isEmpty()) {
            V refV = queue.peek();
            List<V> vsInRID = refV.getInV(E.Type.RID_REF).iterator().next().getOutV(E.Type.RID_REF).stream()
                    .filter(queue::contains).collect(Collectors.toList());
            for (V v : vsInRID)
                v.replaceReferenceCluster(refV);
            queue.removeAll(vsInRID);
        }
    }

    /**
     * update the clusterCnt field of all vertices with level >= 0
     * (all type except CLS & RID).
     *
     * @param maxUpdateLevel max level to update cluster count in the graph
     *                       note that level 0 is REF type, 1 is TKN type , and etc.
     *                       if this parameter is null, max possible level is considered.
     */
    public void updateAncestorClusterCnt(Integer maxUpdateLevel){
        Integer maxLevel = maxUpdateLevel != null ? maxUpdateLevel : V.Type.maxLevel;
        List<V> allNotNegativeLevelVs = vs.values().stream()
                .filter(v -> v.getLevel() >= 0 & v.getLevel() <= maxLevel)
                .sorted(Comparator.comparing(V::getLevel))
                .collect(Collectors.toList());
        for (V v : allNotNegativeLevelVs) {
            int clusterCnt = v.getInE().entrySet().stream()
                    .filter(t -> t.getKey().isInterLevel())
                    .flatMapToInt(e -> e.getValue().stream()
                            .mapToInt(x -> x.getInV().getClusterCount())
                    ).sum();
            v.setClusterCount(clusterCnt);
        }
        System.out.printf("\tClusterCount property of every vertex of level>0 is updated up to level %d.", maxLevel);
    }
}
