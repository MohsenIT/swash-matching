import dao.edge.E;
import dao.G;
import dao.edge.TokenE;
import dao.vertex.ElementV;
import dao.vertex.RefV;
import dao.vertex.V;
import logic.MessagePassing;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static logic.matching.ClusterProfile.outElementVsAtLeast;


public class MainSimGraphGen {
    @SuppressWarnings("Duplicates")
    public static void main(String[] args) throws IOException {
        // TODO: 03/09/2018 Log4j
        String dataset = "arxiv";
        G g = new G();
        g.init(args[0].replace("XXX", dataset), args[1].replace("XXX", dataset));
        g.initNamesPart();
        g.initClusters();
        g.updateAncestorClusterCnt(3);

        MessagePassing mp = new MessagePassing(g);
        Map<RefV, List<MessagePassing.Candidate>> candidates = mp.V(V.Type.REFERENCE)
                .out(E.Type.REF_TKN).in(E.Type.REF_TKN).aggRefVsTerminal(1, 0.5f);

        // possible candidates
        List<MessagePassing.Candidate> candidateList = candidates.values().stream().flatMap(Collection::stream)
                .filter(e -> !e.getOriginRefV().equals(e.getDestRefV())).collect(Collectors.toList());

        //region bi-directional path finding
        for (MessagePassing.Candidate candidate : candidateList) {
            if(candidate.getOriginRefV().equals(candidate.getDestRefV())) continue;
            float sim = 0f;
            Map<ElementV, Set<TokenE>> orgRefMap = candidate.getOriginRefV().getTokenEs().stream().collect(groupingBy(TokenE::getOutV, mapping(Function.identity(), toSet())));
            Map<ElementV, Set<TokenE>> dstRefMap = candidate.getDestRefV().getTokenEs().stream().collect(groupingBy(TokenE::getOutV, mapping(Function.identity(), toSet())));

            for (int i = 1; i <= V.Type.maxLevel; i++) {
                orgRefMap = outElementVsAtLeast(orgRefMap, i);
                dstRefMap = outElementVsAtLeast(dstRefMap, i);

                Set<ElementV> elementToRemove = new HashSet<>(2);
                for (Map.Entry<ElementV, Set<TokenE>> r : orgRefMap.entrySet()) {
                    ElementV m = r.getKey();
                    if(dstRefMap.containsKey(m)) {
                        sim += (1 / Float.valueOf(m.getClusterCount())) * Math.min(orgRefMap.get(m).size(), dstRefMap.get(m).size());
                        elementToRemove.add(m);
                    }
                }
                // elementVs is removed out of above loops to does not change Maps during iteration
                for (ElementV m : elementToRemove) {
                    orgRefMap.remove(m);
                    dstRefMap.remove(m);
                }
            }
            candidate.setSumSimilarity(sim);
        }
        //endregion

        helper.IO.writeSimilarityGraph(candidates, "matching/out", true, g);


    }

}
