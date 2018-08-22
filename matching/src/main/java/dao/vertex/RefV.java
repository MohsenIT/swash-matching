package dao.vertex;

import com.koloboke.collect.map.hash.HashObjObjMaps;
import dao.edge.E;
import dao.edge.TokenE;

import java.util.Collections;
import java.util.Map;

import static dao.vertex.V.Type.REFERENCE;

public class RefV extends V {

    public RefV(String id, String val, String weight) {
        super(Long.valueOf(id), val, REFERENCE, Long.valueOf(weight));
    }

    /**
     * @return resolved_id if the type of vertex is REF else null
     */
    public String getRefResolvedId() {
        return getInV(E.Type.RID_REF).iterator().next().getVal();
    }


    // TODO: 20/08/2018 migrate V to ElementV
    public Map<TokenE.NamePart, Map<V, Boolean>> getNamePartsMap() {
        Map<TokenE.NamePart, Map<V, Boolean>> partsMap = HashObjObjMaps.newMutableMap();
        this.getOutE(E.Type.REF_TKN).stream().map(e -> (TokenE) e).forEach(e -> {
            if(partsMap.containsKey(e.getPart()))
                partsMap.get(e.getPart()).put(e.getOutV(), e.getIsAbbr());
            else partsMap.put(e.getPart(), HashObjObjMaps.newMutableMap(Collections.singletonMap(e.getOutV(), e.getIsAbbr())));
        });
        return partsMap;
    }


    /**
     * replace CLUSTER vertex of a this REFERENCE vertex by {@code clusterV} of {@code refV} parameter.
     *
     * @param refV reference vertex that its cluster should be replaced by old one.
     */
    public void replaceReferenceCluster(RefV refV) {
        replaceReferenceCluster((ClusterV) refV.getInV(E.Type.CLS_REF).iterator().next());
    }

    /**
     * replace CLUSTER vertex of a this REFERENCE vertex by {@code clusterV} parameter.
     *
     * @param clusterV new cluster vertex of the reference.
     */
    public void replaceReferenceCluster(ClusterV clusterV) {
        E e = this.getInE(E.Type.CLS_REF).iterator().next();
        if (e.getInV() == clusterV) return;

        e.getInV().removeOutE(e);
        e.setInV(clusterV);
        clusterV.addOutE(e);
    }



}
