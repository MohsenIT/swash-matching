package ds;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.koloboke.collect.map.hash.HashObjObjMaps;

import java.util.*;

import static ds.ClusterV.AncestorEquity.ResultType.*;
import static ds.TokenE.NamePart.*;
import static helper.CollectionHelper.or;

/**
 * Vertex class of cluster representative in the graph.
 */
public class ClusterV extends V{

    private Map<TokenE.NamePart, Map<V, Boolean>> profileMap;

    //region Getters & Setters
    /**
     * Gets profile Map of {@code TOKEN}s vertices
     *
     * @return Map of {@code TOKEN} vertices and type of them
     */
    public Map<TokenE.NamePart, Map<V, Boolean>> getProfileMap() {
        return profileMap;
    }

    public void setProfileMap(Map<TokenE.NamePart, Map<V, Boolean>> profileMap) {
        this.profileMap = profileMap;
    }
    //endregion

    public ClusterV(Long id, V refV) {
        super(id, refV.getVal(), Type.CLUSTER, 1L);
        profileMap = getNamePartsMap(refV);
    }

    /**
     * Check if the input <i>REFERENCE</i> vertex is consistent with cluster profile or not?
     * @param refV a <i>REFERENCE</i> vertex
     * @return true if consistent and false if not
     */
    public Boolean isConsistent(V refV) {
        Map<TokenE.NamePart, Map<V, Boolean>> namePartsMap = getNamePartsMap(refV);
        // TODO: 13/08/2018 consider Shift-Left, Shift-Right & Inverse
        // TODO: 13/08/2018 implement first find path and then check name part

        // lastname check: equal or hash & not_abbr
        AncestorEquity lnameEq = isEqual(LASTNAME, namePartsMap.getOrDefault(LASTNAME, Collections.emptyMap()).keySet(), Type.TOKEN);
        if(lnameEq.result == IS_NOT_EQUAL)
            lnameEq = isEqual(LASTNAME, lnameEq.onlyInInput, Type.HASH);
        if(lnameEq.result != IS_EQUAL) return false;

        // firstname check: equal or hash or abbr but not removed
        // TODO: 14/08/2018 it is possible that a firstname token is abbr and another not!
        Map<V, Boolean> fnameMap = namePartsMap.getOrDefault(FIRSTNAME, Collections.emptyMap());
        AncestorEquity fnameEq = isEqual(FIRSTNAME, fnameMap.keySet(), Type.TOKEN);
        if(fnameEq.result == IS_NOT_EQUAL)
            fnameEq = isEqual(FIRSTNAME, fnameEq.onlyInInput, Type.HASH);
        if(fnameEq.result == IS_NOT_EQUAL && (or(fnameMap.values()) || or(profileMap.getOrDefault(FIRSTNAME, null).values()))) // has abbreviated
            fnameEq = isEqual(FIRSTNAME, fnameEq.onlyInInput, Type.ABBREVIATED);
        if(fnameEq.result != IS_EQUAL) return false;

        // middlename check: equal or hash or abbr or removed but not non-aligned
        // TODO: 14/08/2018 it is possible that a middlename token is abbr and another not!
        Map<V, Boolean> mnameMap = namePartsMap.getOrDefault(MIDDLENAME, Collections.emptyMap());
        AncestorEquity mnameEq = isEqual(MIDDLENAME, mnameMap.keySet(), Type.TOKEN);
        if(mnameEq.result == IS_NOT_EQUAL)
            mnameEq = isEqual(MIDDLENAME, mnameEq.onlyInInput, Type.HASH);
        if(fnameEq.result == IS_NOT_EQUAL && (or(mnameMap.values()) || or(profileMap.getOrDefault(MIDDLENAME, null).values()))) // has abbreviated
            mnameEq = isEqual(MIDDLENAME, mnameEq.onlyInInput, Type.ABBREVIATED);
        if(mnameEq.result == IS_NOT_EQUAL) return false;

        // suffix check: equal or hash or abbr or removed but not non-aligned
        // TODO: 14/08/2018 it is possible that a suffix token is abbr and another not!
        Map<V, Boolean> suffixMap = namePartsMap.getOrDefault(SUFFIX, Collections.emptyMap());
        AncestorEquity suffixEq = isEqual(SUFFIX, suffixMap.keySet(), Type.TOKEN);
        if(suffixEq.result == IS_NOT_EQUAL)
            suffixEq = isEqual(SUFFIX, suffixEq.onlyInInput, Type.HASH);
        if(fnameEq.result == IS_NOT_EQUAL && (or(suffixMap.values()) || or(profileMap.getOrDefault(SUFFIX, null).values()))) // has abbreviated
            suffixEq = isEqual(SUFFIX, suffixEq.onlyInInput, Type.ABBREVIATED);
        if(suffixEq.result == IS_NOT_EQUAL) return false;


        // TODO: 14/08/2018 update profile if matched
        return true;
    }

    public static Map<TokenE.NamePart, Map<V, Boolean>> getNamePartsMap(V refV) {
        assert refV.getType() == Type.REFERENCE : "The parameter should be REFERENCE vertex.";
        Map<TokenE.NamePart, Map<V, Boolean>> partsMap = HashObjObjMaps.newMutableMap();
        refV.getOutE(E.Type.REF_TKN).stream().map(e -> (TokenE) e).forEach(e -> {
            if(partsMap.containsKey(e.getPart()))
                partsMap.get(e.getPart()).put(e.getOutV(), e.getIsAbbr());
            else partsMap.put(e.getPart(), HashObjObjMaps.newMutableMap(Collections.singletonMap(e.getOutV(), e.getIsAbbr())));
        });
        return partsMap;
    }


    /**
     * check the equity of {@code inputVs} with the profile Vs of specified {@code part}. for example
     * if the {@code inputVs} types are <i>TOKEN</i>  and {@code vType} are <i>ABBREVIATED</i>, {@code inputVs} and
     * profile Vs are traversed out util <i>ABBREVIATED</i> level and then equity of vertices are checked.
     *
     * @param part name part of cluster profile that its vertices should be checked.
     * @param inputVs Collection of Vertices their equity should be checked.
     * @param vType levels of equity checked.
     *
     * @return
     */
    private AncestorEquity isEqual(TokenE.NamePart part, Collection<V> inputVs, V.Type vType) {
        int level = vType.getLevel();
        assert level > 0 : "level is not valid. level should be TOKEN, HASH or ABBREVIATED.";

        Set<V> vs = V.outVsUntil(inputVs, level);
        Set<V> profileVs = profileMap.containsKey(part) ? V.outVsUntil(profileMap.get(part).keySet(), level) : Collections.EMPTY_SET;

        AncestorEquity eq = new AncestorEquity(IS_EQUAL);
        eq.onlyInInput = Lists.newArrayList(Sets.difference(vs, profileVs));
        eq.onlyInProfile = Lists.newArrayList(Sets.difference(profileVs, vs));
        eq.inBoth = Lists.newArrayList(Sets.intersection(vs, profileVs));

        if(!eq.onlyInInput.isEmpty() && !eq.onlyInProfile.isEmpty())
            eq.result = IS_NOT_EQUAL;
        else if (!eq.onlyInProfile.isEmpty())
            eq.result = PROFILE_HAS_MORE;
        else if (!eq.onlyInInput.isEmpty())
            eq.result = INPUT_HAS_MORE;
        return eq;
    }


    /**
     * Inner static class for report the equity result of two vertex collections or their ancestor.
     */
    public static class AncestorEquity {
        public enum ResultType {IS_NOT_EQUAL, PROFILE_HAS_MORE, INPUT_HAS_MORE, IS_EQUAL}

        //region Public Fields
        public ResultType result;
        public List<V> inBoth;
        public List<V> onlyInProfile;
        public List<V> onlyInInput;
        //endregion

        //region Constructors
        public AncestorEquity(ResultType result) {
            this.result = result;
        }
        //endregion
    }

}
