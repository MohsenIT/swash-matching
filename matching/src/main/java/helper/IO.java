package helper;

import com.koloboke.collect.set.hash.HashObjSets;
import dao.G;
import dao.edge.E;
import dao.vertex.RefV;
import dao.vertex.V;
import logic.MessagePassing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IO {
    private static final String CSV_DELIMITER = "\t";
//    private static final String CSV_DELIMITER = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";

    public static List<String[]> readCSVLines(String csvFilePath) {
        try (Stream<String> stream = Files.lines(Paths.get(csvFilePath))) {
            return stream.map(line -> line.split(CSV_DELIMITER)).collect(Collectors.toList());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    public static void writeSimilarityGraph(Map<RefV, List<MessagePassing.Candidate>> candidates, String outPath, Boolean hasAllVs, G g) throws IOException {
        List<MessagePassing.Candidate> candidateList = candidates.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toList());
        Set<V> Vertices = HashObjSets.newMutableSet();
        if(hasAllVs){
            Vertices.addAll(g.getVs(V.Type.REFERENCE));
        } else {
            Vertices.addAll(candidateList.stream().map(MessagePassing.Candidate::getOriginRefV).collect(Collectors.toList()));
            Vertices.addAll(candidateList.stream().map(MessagePassing.Candidate::getDestRefV).collect(Collectors.toList()));
        }

        StringBuilder vCsvRows = new StringBuilder();
        vCsvRows.append(String.format("Id%1$s Label%1$s Weight%1$s Res_Id\r\n", CSV_DELIMITER));
        for (V v : Vertices) {
            String firstResId = v.getInV(E.Type.RID_REF).iterator().next().getVal();
            vCsvRows.append(String.format("%d%5$s %s%5$s %d%5$s %s\r\n", v.getId(), v.getVal(), v.getWeight(), firstResId, CSV_DELIMITER));
        }
        Files.write(Paths.get(outPath + "/vertices.tsv").toAbsolutePath(), vCsvRows.toString().getBytes(), StandardOpenOption.CREATE);

        Set<String> rows = HashObjSets.newMutableSet(candidateList.size()/2);
        for (MessagePassing.Candidate c : candidateList) {
            Long o = c.getOriginRefV().getId();
            Long d = c.getDestRefV().getId();
            rows.add(String.format("%d%5$s %d%5$s %.6f%5$s %d\r\n", o<d?o:d, o>=d?o:d, c.getSumSimilarity(), c.getCntMessage(), CSV_DELIMITER));
        }
        String edgesColumnHeader = String.format("Source%2$s Target%2$s Weight%2$s Common_Token_Cnt\r\n%s", String.join("", rows), CSV_DELIMITER);
        byte[] edgesBytes = edgesColumnHeader.getBytes();
        Files.write(Paths.get(outPath + "/edges.tsv").toAbsolutePath(), edgesBytes, StandardOpenOption.CREATE);
    }
}
