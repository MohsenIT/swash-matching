package dao.vertex;

import dao.vertex.V;

import static com.google.common.base.Preconditions.checkArgument;

public class ElementV extends V {
    //region Fields, Getters & Setters
    private Integer clusterCount;

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
    //endregion

    public ElementV(String id, String val, String type, String weight, Integer clusterCount) {
        super(id, val, type, weight);
        this.clusterCount = clusterCount;
    }
}
