import analysis.Stats;
import dao.G;
import dao.edge.E;
import dao.vertex.RefV;
import dao.vertex.V;
import evaluation.paired.FMeasure;
import logic.MessagePassing;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class MainPairedEval {
    public static void main(String[] args) throws IOException {
        // TODO: 03/09/2018 Log4j
        String dataset = "dblp_reuther";
        //String dataset = "dblp_naumann";
        String goldPairsFilePath = args[2].replace("XXX", dataset);
        G g = new G();
        g.init(args[0].replace("XXX", dataset), args[1].replace("XXX", dataset));
        g.initNamesPart();
        g.initClusters();
        FMeasure eval = new FMeasure(g);

        g.updateAncestorClusterCnt(1);

        MessagePassing mp = new MessagePassing(g);
        Map<RefV, List<MessagePassing.Candidate>> candidates = mp.V(V.Type.REFERENCE)
                .out(E.Type.REF_TKN).in(E.Type.REF_TKN).aggRefVsTerminal(1, 0.5f);

        // ------ rule-based approach of clustering ---------------------------
        mp.clusterCandidates(candidates);

        //region check upper bound of performance in this step
//        candidates.values().stream().flatMap(Collection::stream).forEach(c -> {
//            if(c.getDestRefV() != c.getOriginRefV()) {
//                g.addE(c.getDestRefV(), c.getOriginRefV(), E.Type.REF_REF, c.getSumSimilarity());
//            }
//        });
//        g.updateToMaxAchievableRecallPairwise(g, goldPairsFilePath);
        //endregion

        eval.evaluate(goldPairsFilePath);
        //helper.IO.writeSimilarityGraph(candidates, "matching/out", false, g);
        System.out.println(eval);
        Stats.calcStats(g);
    }

}
