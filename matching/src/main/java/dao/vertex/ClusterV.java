package dao.vertex;

import logic.matching.ClusterProfile;

/**
 * Vertex class of cluster representative in the graph.
 */
public class ClusterV extends V {

    private ClusterProfile profile;

    //region Getters & Setters

    /**
     * Gets profile Map of {@code TOKEN}s vertices
     *
     * @return Map of {@code TOKEN} vertices and type of them
     */
    public ClusterProfile getProfile() {
        return profile;
    }

    public void setProfile(ClusterProfile profile) {
        this.profile = profile;
    }

    //endregion

    public ClusterV(Long id, RefV refV) {
        super(id, refV.getVal(), Type.CLUSTER, 1L);
        profile = refV.buildClusterProfile();
    }
}
