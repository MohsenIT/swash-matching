package dao.vertex;

import dao.edge.E;
import dao.edge.TokenE;
import logic.matching.ClusterProfile;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static dao.vertex.V.Type.REFERENCE;

public class RefV extends V {

    //region Getters & Setters
    /**
     * @return ResolvedIdV of this <i>REFERENCE<i/> vertex if exist else null
     */
    public V getRefResolvedIdV() {
        return getInV(E.Type.RID_REF).iterator().next();
    }

    /**
     * @return {@code String} resolved_id of this <i>REFERENCE</i> vertex
     */
    public String getRefResolvedId() {
        return getRefResolvedIdV().getVal();
    }

    /**
     * @return clusterV of this <i>REFERENCE<i/> vertex if exist else null
     */
    public ClusterV getRefClusterV() {
        Iterator<V> iterator = getInV(E.Type.CLS_REF).iterator();
        return iterator.hasNext()? (ClusterV) iterator.next() : null;
    }

    /**
     * @return list of {@code TokenE}s of RefV
     */
    public List<TokenE> getTokenEs() {
        return getOutE(E.Type.REF_TKN).stream().map(TokenE.class::cast).collect(Collectors.toList());
    }
    //endregion


    public RefV(String id, String val, String weight) {
        super(Long.valueOf(id), val, REFERENCE, Long.valueOf(weight));
    }


    //region Methods

    /**
     * Build a ClusterProfile from <i>TokenE</i>s of this <i>refV</i>.
     *
     * @return the built {@code ClusterProfile}
     */
    public ClusterProfile buildClusterProfile() {
        ClusterProfile profile = new ClusterProfile();
        this.getTokenEs().forEach(e -> profile.addEntry(new ClusterProfile.Entry(e)));
        return profile;
    }

    /**
     * replace CLUSTER vertex of a this REFERENCE vertex by {@code clusterV} of {@code targetV} parameter.
     *
     * @param targetV reference vertex that its cluster should be replaced by old one.
     */
    public void replaceReferenceCluster(RefV targetV) {
        replaceReferenceCluster(targetV.getRefClusterV());
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
    //endregion
}
