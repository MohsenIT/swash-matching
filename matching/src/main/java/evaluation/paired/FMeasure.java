package evaluation.paired;

import dao.G;
import dao.vertex.RefV;
import dao.vertex.V;
import helper.IO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FMeasure {

    //region field
    private Double Precision = 0.0;
    private Double Recall = 0.0;

    private Map<String, Long> resolvedIdCntMap;
    private G g;
    //endregion


    //region getter and setter

    /**
     * Gets Precision
     *
     * @return value of Precision
     */
    public Double getPrecision() {
        return Precision;
    }

    /**
     * Gets Recall
     *
     * @return value of Recall
     */
    public Double getRecall() {
        return Recall;
    }

    /**
     * Gets F1
     *
     * @return value of F1
     */
    public Double getF1() {
        return 2 * Precision * Recall / (Precision + Recall);
    }

    //endregion


    public FMeasure(G g) {
        resolvedIdCntMap = g.getVs(V.Type.RESOLVED_ID).stream().collect(Collectors.toMap(V::getVal, V::getWeight));
        this.g = g;
    }

    public void evaluate(String goldPairsFilePath) {
        List<String[]> goldPairs = IO.readCSVLines(goldPairsFilePath);
        double fp = 0, fn = 0, tp = 0;
        List<GoldPairs> golds = goldPairs.stream().skip(1).map(GoldPairs::new).collect(Collectors.toList());
        for (GoldPairs gold : golds) {
            if (gold.inSameCluster()) {
                if(gold.isMatched()) tp += gold.getIncValue();
                else fp += gold.getIncValue();
            }
            else {
                if (!gold.isMatched) tp += gold.getIncValue();
                else {
                    fn += gold.getIncValue();
                    System.out.printf("%s == %s\r\n", gold.refV1.getVal(), gold.refV2.getVal());
                }
            }

        }
        Precision = tp / (tp + fp);
        Recall = tp / (tp + fn);
    }

    @Override
    public String toString() {
        return String.format("FMeasure measures{F1=%s, Precision=%s, Recall=%s}"
                , getF1(), Precision, Recall);
    }

    public class GoldPairs{
        RefV refV1;
        RefV refV2;
        boolean isMatched;

        /**
         * Gets is two reference is matched or not?
         *
         * @return value of isMatched
         */
        public boolean isMatched() {
            return isMatched;
        }

        public long getIncValue() {
//            return 1;
            return refV1.getWeight() * refV2.getWeight();
        }

        public GoldPairs(String[] elements) {
            this.refV1 = (RefV) g.getV(Long.valueOf(elements[0]));
            this.refV2 = (RefV) g.getV(Long.valueOf(elements[1]));
            this.isMatched = elements.length <= 4 || Integer.valueOf(elements[4]).equals(1);
        }

        public boolean inSameCluster() {
            return refV1.getRefClusterV().equals(refV2.getRefClusterV());
        }
    }
}
