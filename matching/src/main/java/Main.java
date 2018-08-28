import analysis.Stats;
import dao.edge.E;
import dao.G;
import dao.vertex.RefV;
import dao.vertex.V;
import evaluation.Pairwise;
import helper.IO;
import logic.MessagePassing;

import java.io.IOException;
import java.util.*;


public class Main {
    public static void main(String[] args) throws IOException {
        G g = new G();
        g.init(args[0], args[1]);
        g.initNamesPart();
        g.initClusters();
        g.updateAncestorClusterCnt(1);

        Pairwise eval = new Pairwise(g);
        eval.evaluate();

        MessagePassing mp = new MessagePassing(g);
        Map<V, List<MessagePassing.Candidate>> candidates = mp.V(V.Type.REFERENCE)
                .out(E.Type.REF_TKN).in(E.Type.REF_TKN).aggRefVsTerminal(2);

        // ------ rule-based approach of clustering ---------------------------
        mp.greedyClustering(candidates);
        // g.updateClusters(components);

        // ------ check upper bound of performance in this step ---------------
        // Collection<ImmutableValueGraph<Long, Double>> componentGraphs = mp.connectedCandidatesGuavaGraphs(candidates);
        // Map<V, Collection<V>> components = mp.graphsToClusters(componentGraphs);
        // g.updateClustersToRealClusters(components.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));

        eval.evaluate();
        //IO.writeSimilarityGraph(candidates, "matching/out");
        System.out.println(eval);
        Stats.calcStats(g);
    }

}
