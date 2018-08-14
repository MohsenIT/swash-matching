package analysis;

import ds.E;
import ds.G;
import ds.V;

public class Stats {
    public static void calcStats(G g) {
        // count of REF = 58515
        g.getVs(V.Type.REFERENCE).stream().mapToLong(V::getWeight).sum();

        // count of distinct NAME in REFs = 12872
        g.getVs(V.Type.REFERENCE).size();

        //count of NAMEs with more than 1 representations = 3089
        g.getVs(V.Type.RESOLVED_ID).stream().filter(v -> v.getOutE(E.Type.RID_REF).size()>1).count();
    }
}
