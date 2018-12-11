import analysis.Stats;
import dao.G;
import dao.edge.E;
import dao.vertex.RefV;
import dao.vertex.V;
import evaluation.paired.FMeasure;
import logic.MessagePassing;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public class MainPairedEval {
    public static void main(String[] args) throws IOException {
        // TODO: 03/09/2018 Log4j
//        String dataset = "dblp_reuther";
        String dataset = "dblp_naumann";
        G g = new G();
        g.init(args[0].replace("XXX", dataset), args[1].replace("XXX", dataset));
        g.initNamesPart();
        g.initClusters();
        g.updateAncestorClusterCnt(1);

        MessagePassing mp = new MessagePassing(g);
        Map<RefV, List<MessagePassing.Candidate>> candidates = mp.V(V.Type.REFERENCE)
                .out(E.Type.REF_TKN).in(E.Type.REF_TKN).aggRefVsTerminal(2);

        // ------ rule-based approach of clustering ---------------------------
        mp.greedyClustering(candidates);

        FMeasure eval = new FMeasure(g);
        eval.evaluate(args[2].replace("XXX", dataset));
//        IO.writeSimilarityGraph(candidates, "matching/out");
        System.out.println(eval);
        Stats.calcStats(g);
    }

}
