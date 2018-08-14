package ds;

import com.koloboke.collect.map.hash.HashObjObjMaps;
import com.koloboke.collect.set.hash.HashObjSets;
import org.apache.tinkerpop.shaded.kryo.util.IntArray;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ds.V.Type.*;

/**
 * Vertex class to represent vertices in the graph. The weight attribute allows
 * you to represent weighted graph.
 */
public class V {
    public enum Type {
        RESOLVED_ID("RID", -1),
        CLUSTER("CLS", -1),
        REFERENCE("REF", 0),
        TOKEN("TKN", 1),
        HASH("HSH", 2),
        ABBREVIATED("ABR", 3);

        //region Fields & Getters
        private String text;
        private Integer level;
        public static final Integer maxLevel = Arrays.stream(values()).mapToInt(b -> b.level).max().orElse(3);

        public String getText() {return this.text;}
        public Integer getLevel() {return level;}
        //endregion

        Type(String text, Integer level) {
            this.text = text;
            this.level = level;
        }

        public static Type fromString(String text) {
            return Arrays.stream(values()).filter(b -> b.text.equalsIgnoreCase(text)).findFirst().orElse(null);
        }
    }


    //region Fields
    private Long id;
    private String val;
    private Type type;
    private Long weight;
    private Integer clusterCount;
    private Map<E.Type, Set<E>> inE;
    private Map<E.Type, Set<E>> outE;
    //endregion


    //region Getters & Setters
    /**
     * Gets vertex id
     *
     * @return value of id
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets string value of vertex content
     *
     * @return value of val
     */
    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    /**
     * Gets vertex type. each vertex has only one type.
     *
     * @return value of type
     */
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Derived property that show vertices level of each V based on its Type.
     *
     * @return Integer level of vertices
     */
    public Integer getLevel() {
        return type.getLevel();
    }

    /**
     * Gets vertex weight. The occurrence frequency of value is common the definition of weight.
     *
     * @return value of weight
     */
    public Long getWeight() {
        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }

    /**
     * Gets IN edges
     *
     * @return Map of type and corresponding edge set
     */
    public Map<E.Type, Set<E>> getInE() {
        return inE;
    }

    /**
     * Gets OUT edges of specific edge type
     *
     * @return Map of type and corresponding edge set
     */
    public Map<E.Type, Set<E>> getOutE() {
        return outE;
    }


    /**
     * Gets number of clusters that exist in the corresponding references.
     * For all of vertices type except CLS (CLUSTERS) AND RID (RESOLVED_ID),
     * this field is valid.
     *
     * @return Integer value of cluster count
     */
    public Integer getClusterCount() {
        return clusterCount;
    }

    public void setClusterCount(Integer clusterCount) {
        this.clusterCount = clusterCount;
    }

    /**
     * @return resolved_id if the type of vertex is REF else null
     */
    public String getRefResolvedId() {
        if (type == REFERENCE)
            return getInV(E.Type.RID_REF).iterator().next().getVal();
        return null;
    }

    //endregion


    //region Constructors
    public V(Long id, String val, Type type, Long weight, Integer clusterCount) {
        this.id = id;
        this.val = val;
        this.type = type;
        this.weight = weight;
        this.clusterCount = clusterCount;
        inE = HashObjObjMaps.newMutableMap();
        outE = HashObjObjMaps.newMutableMap();
    }

    public V(String id, String val, String type, String weight) {
        this(Long.valueOf(id), val, fromString(type), Long.valueOf(weight), 0);
    }

    public V(Long id, String val, Type type, Long weight){
        this(id, val, type, weight, null);
    }
    //endregion


    //region Traversals Methods
    /**
     * Gets IN edges of specific edge type
     *
     * @param type edge type
     * @return set of in edges
     */
    public Set<E> getInE(E.Type type) {
        return inE.get(type);
    }

    /**
     * Gets OUT edges of specific edge type
     *
     * @param type edge type
     * @return set of in edges
     */
    public Set<E> getOutE(E.Type type) {
        return outE.get(type);
    }

    /**
     * Gets IN or OUT edges of specific edge type
     *
     * @param type edge type
     * @return set of in edges
     */
    public Set<E> getInOutE(E.Type type) {
        Set<E> joinSet = getInE(type);
        joinSet.addAll(getOutE(type));
        return joinSet;
    }

    /**
     * Gets IN edges of specific edge type
     *
     * @param type edge type
     * @return set of in vertices
     */
    public Set<V> getInV(E.Type type) {
        return getInE(type).stream().map(E::getInV).collect(Collectors.toSet());
    }

    /**
     * Gets OUT edges of specific edge type
     *
     * @param type edge type
     * @return set of in vertices
     */
    public Set<V> getOutV(E.Type type) {
        return getOutE(type).stream().map(E::getOutV).collect(Collectors.toSet());
    }

    /**
     * Gets IN or OUT edges of specific edge type
     *
     * @param type edge type
     * @return set of in vertices
     */
    public Set<V> getInOutV(E.Type type) {
        Set<V> joinSet = getInV(type);
        joinSet.addAll(getOutV(type));
        return joinSet;
    }

    /**
     * Has IN or OUT edges or not?
     *
     * @param type edge type
     * @return boolean variable that has or not?
     */
    public Boolean hasInOutE(E.Type type) {
        return getInOutV(type).size() > 0;
    }

    /**
     * traverse out edges until reach to the specified level.
     *
     * @param vList vertex collection that traverse should start from it
     * @param destLevel level of destination of traversal
     * @return set of vertices in the destLevel
     */
    public static Set<V> outVsUntil(Collection<V> vList, int destLevel) {
        assert !vList.isEmpty() : "list of input vertices should not be empty.";
        Stream<Integer> levelStream = vList.stream().map(v -> v.getType().getLevel());
        assert levelStream.distinct().count() == 1 : "list of input vertices should not be in same level.";
        int currentLevel = levelStream.findAny().orElse(10);
        assert currentLevel <= destLevel : "input level should not be less than vertices level.";

        Set<V> vs = HashObjSets.newMutableSet(vList);
        while (destLevel ==currentLevel){
            E.Type eType = E.Type.getTypeByLevels(currentLevel, ++currentLevel);
            vs = vs.stream().flatMap(v -> v.getOutV(eType).stream()).collect(Collectors.toSet());
        }
        return vs;
    }

    //endregion


    //region Edit Vertex's Edge Methods

    /**
     * replace CLUSTER vertex of a this REFERENCE vertex
     *
     * @param clusterV new cluster of the reference, the type of this parameter should be CLUSTER or REFERENCE.
     *                 If REFERENCE vertex is provided, the CLUSTER of this vertex is used.
     */
    public void replaceReferenceCluster(V clusterV) {
        assert this.type != REFERENCE : "The vertex should have REFERENCE type.";
        assert clusterV.type != CLUSTER && clusterV.type != REFERENCE : "The input parameter type should be REFERENCE or CLUSTER.";

        E e = this.getInE(E.Type.CLS_REF).iterator().next();
        V clstV = clusterV.type == REFERENCE ? clusterV.getInV(E.Type.CLS_REF).iterator().next() : clusterV;
        if (e.getInV() == clstV) return;

        e.getInV().removeOutE(e);
        e.setInV(clstV);
        clstV.addOutE(e);
    }

    /**
     * Add an in-edge to this vertex
     *
     * @param e : In-edge to the vertex
     */
    public void addInE(E e) {
        if(inE.containsKey(e.getType()))
            inE.get(e.getType()).add(e);
        else {
            inE.put(e.getType(), HashObjSets.newMutableSetOf(e));
        }
    }

    /**
     * Remove an in-edge of this vertex
     *
     * @param e : In-edge to the vertex
     */
    public void removeInE(E e) {
        inE.get(e.getType()).remove(e);
    }

    /**
     * Add an out-edge to this vertex
     *
     * @param e : out-edge to the vertex
     */
    public void addOutE(E e) {
        if(outE.containsKey(e.getType()))
            outE.get(e.getType()).add(e);
        else {
            outE.put(e.getType(), HashObjSets.newMutableSetOf(e));
        }
    }

    /**
     * Remove an out-edge of this vertex
     *
     * @param e : Out-edge to the vertex
     */
    public void removeOutE(E e) {
        outE.get(e.getType()).remove(e);
    }
    //endregion


    @Override
    public String toString() {
        return String.format("V[%s] %s: %d", type.getText(), val, weight);
    }
}
