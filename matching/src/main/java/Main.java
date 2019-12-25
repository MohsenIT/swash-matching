import analysis.Stats;
import com.google.common.graph.ImmutableValueGraph;
import dao.G;
import dao.edge.E;
import dao.vertex.RefV;
import dao.vertex.V;
import evaluation.collective.PairwiseFMeasure;
import logic.MessagePassing;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class Main {
    public static void main(String[] args) throws IOException {
        // TODO: 03/09/2018 Log4j
        String dataset = "arxiv"; //"citeseer";
        G g = new G();
        g.init(args[0].replace("XXX", dataset), args[1].replace("XXX", dataset));
        g.initNamesPart();
//        g.getVs(V.Type.TOKEN).stream().mapToInt(v -> v.getInE(E.Type.REF_TKN).size()).map(e -> e * e).sum();
        g.initClusters();
        g.updateAncestorClusterCnt(1);

        PairwiseFMeasure eval = new PairwiseFMeasure(g);
        eval.evaluate();
        System.out.println(eval);

        MessagePassing mp = new MessagePassing(g);
        Map<RefV, List<MessagePassing.Candidate>> candidates = mp.V(V.Type.REFERENCE)
                .out(E.Type.REF_TKN).in(E.Type.REF_TKN).aggRefVsTerminal(1, 0.5f);
        //helper.IO.writeSimilarityGraph(candidates, "matching/out", true, g);

        // ------ rule-based approach of clustering ---------------------------
        mp.clusterCandidates(candidates);
        // g.updateClusters(components);

        //region check upper bound of performance in this step
//        candidates.values().stream().flatMap(Collection::stream).forEach(c -> {
//            if(c.getDestRefV() != c.getOriginRefV())
//                g.addE(c.getDestRefV(), c.getOriginRefV(), E.Type.REF_REF, c.getSumSimilarity());
//        });
//        g.updateToMaxAchievableRecall(g);
        //endregion

        eval.evaluate();
        System.out.println(eval);
        Stats.calcStats(g);
    }

}
