package dao.vertex;

import com.koloboke.collect.map.hash.HashObjObjMaps;
import com.koloboke.collect.set.hash.HashObjSets;
import dao.edge.E;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static dao.vertex.V.Type.*;

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
        SIMILAR("SIM", 2),
        NICKNAME("NCK", 2),
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

        public static boolean isElement(String type) {
            Type t = fromString(type);
            return t.level > 0;
        }

        public static boolean isReference(String type) {
            return fromString(type) == REFERENCE;
        }


    }


    //region Fields
    private Long id;
    private String val;
    private Type type;
    private Long weight;
    private Map<E.Type, List<E>> inE;
    private Map<E.Type, List<E>> outE;
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
     * @return {@code Map} of type and corresponding edge {@code List}
     */
    public Map<E.Type, List<E>> getInE() {
        return inE;
    }

    /**
     * Gets OUT edges of specific edge type
     *
     * @return {@code Map} of type and corresponding edge {@code List}
     */
    public Map<E.Type, List<E>> getOutE() {
        return outE;
    }


    //endregion


    //region Constructors
    public V(Long id, String val, Type type, Long weight) {
        this.id = id;
        this.val = val;
        this.type = type;
        this.weight = weight;
        inE = HashObjObjMaps.newMutableMap();
        outE = HashObjObjMaps.newMutableMap();
    }

    public V(String id, String val, String type, String weight) {
        this(Long.valueOf(id), val, fromString(type), Long.valueOf(weight));
    }
    //endregion


    //region Traversals Methods
    /**
     * Gets IN edges of specific edge type
     *
     * @param type edge type
     * @return {@code List} of in edges
     */
    public List<E> getInE(E.Type type) {
        return inE.getOrDefault(type, Collections.emptyList());
    }

    /**
     * Gets OUT edges of specific edge type
     *
     * @param type edge type
     * @return {@code List} of in edges
     */
    public List<E> getOutE(E.Type type) {
        return outE.getOrDefault(type, Collections.emptyList());
    }

    /**
     * Gets IN or OUT edges of specific edge type
     *
     * @param type edge type
     * @return {@code List} of in edges
     */
    public List<E> getInOutE(E.Type type) {
        List<E> list = getInE(type);
        list.addAll(getOutE(type));
        return list;
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
     * Gets OUT edges if next V is in the next level
     *
     * @return set of in vertices
     */
    public Set<V> getOutNextLevelV() {
        Integer level = this.type.level;
        List<E.Type> types = this.outE.keySet().stream()
                .filter(e -> e.getOutLevel() == level + 1).collect(Collectors.toList());
        if(types.size() == 0)
            return Collections.emptySet();
        return types.stream().flatMap(t -> getOutV(t).stream()).collect(Collectors.toSet());
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
        checkArgument(!vList.isEmpty(), "list of input vertices should not be empty.");
        Stream<Integer> levelStream = vList.stream().map(v -> v.getType().getLevel());
        checkArgument(levelStream.distinct().count() == 1, "list of input vertices should not be in same level.");
        int currentLevel = levelStream.findAny().orElse(10);
        checkArgument(currentLevel <= destLevel, "input level should not be less than vertices level.");

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
     * Add an in-edge to this vertex
     *
     * @param e : In-edge to the vertex
     */
    public void addInE(E e) {
        if(inE.containsKey(e.getType()))
            inE.get(e.getType()).add(e);
        else {
            inE.put(e.getType(), new ArrayList<E>() {{add(e);}});
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
            outE.put(e.getType(), new ArrayList<E>() {{add(e);}});
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
