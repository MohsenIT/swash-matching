package evaluation;

import ds.E;
import ds.G;
import ds.V;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class Pairwise {

    //region field
    private Double pairwisePrecision = 0.0;
    private Double pairwiseRecall = 0.0;

    private Map<String, Long> resolvedIdCntMap;
    private G g;
    //endregion


    //region getter and setter

    /**
     * Gets Pairwise Precision
     *
     * @return value of pairwisePrecision
     */
    public Double getPairwisePrecision() {
        return pairwisePrecision;
    }

    /**
     * Gets pairwiseRecall
     *
     * @return value of pairwiseRecall
     */
    public Double getPairwiseRecall() {
        return pairwiseRecall;
    }

    /**
     * Gets PairwiseF1
     *
     * @return value of pairwiseF1
     */
    public Double getPairwiseF1() {
        return 2 * pairwisePrecision * pairwiseRecall / (pairwisePrecision + pairwiseRecall);
    }

    //endregion


    public Pairwise(G g) {
        resolvedIdCntMap = g.getVs(V.Type.RESOLVED_ID).stream().collect(Collectors.toMap(V::getVal, V::getWeight));
        this.g = g;
    }

    public void evaluate() {
        double fp = 0, fn = 0, tp = 0;
        for (V clusterV : g.getVs(V.Type.CLUSTER)) {
            Collection<V> clusterRefVs = clusterV.getOutV(E.Type.CLS_REF);

            Map<String, Long> clusterResolvedIdCntMap = clusterRefVs.stream()
                    .flatMap(r -> r.getInE(E.Type.RID_REF).stream())
                    .map((E e) -> new SimpleEntry<>(e.getInV().getVal(), e.getWeight().longValue()))
                    .collect(Collectors.groupingBy(SimpleEntry::getKey, Collectors.summingLong(Map.Entry::getValue)));

            long clusterRefCnt = clusterResolvedIdCntMap.values().stream().mapToLong(Long::longValue).sum();
            for (Map.Entry<String, Long> c : clusterResolvedIdCntMap.entrySet()) {
                tp += c.getValue() * (c.getValue()-1)/2;
                fn += c.getValue() * (resolvedIdCntMap.get(c.getKey()) - c.getValue());
                fp += c.getValue() * (clusterRefCnt - c.getValue());
            }
        }
        pairwisePrecision = tp / (tp + fp);
        pairwiseRecall = tp / (tp + fn);
    }

    @Override
    public String toString() {
        return String.format("Pairwise measures{F1=%s, Precision=%s, pairwiseRecall=%s}"
                , getPairwiseF1(), pairwisePrecision, pairwiseRecall);
    }
}
