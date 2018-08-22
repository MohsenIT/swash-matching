package helper;

import com.koloboke.collect.set.hash.HashObjSets;
import dao.edge.E;
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
    private static final String CSV_DELIMITER = ",";

    public static List<String[]> readCSVLines(String csvFilePath) {
        try (Stream<String> stream = Files.lines(Paths.get(csvFilePath))) {
            return stream.map(line -> line.split(CSV_DELIMITER)).collect(Collectors.toList());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    public static void writeSimilarityGraph(Map<V, List<MessagePassing.Candidate>> candidates, String outPath) throws IOException {
        Set<V> Vertices = HashObjSets.newMutableSet();
        List<MessagePassing.Candidate> candidateList = candidates.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toList());
        Vertices.addAll(candidateList.stream().map(MessagePassing.Candidate::getOriginRefV).collect(Collectors.toList()));
        Vertices.addAll(candidateList.stream().map(MessagePassing.Candidate::getDestRefV).collect(Collectors.toList()));

        StringBuilder vCsvRows = new StringBuilder();
        vCsvRows.append("Id, Label, Weight, Res_Id\r\n");
        for (V v : Vertices) {
            String firstResId = ((V) v.getInV(E.Type.RID_REF).iterator().next()).getVal().toString();
            vCsvRows.append(String.format("%d, %s, %d, %s\r\n", v.getId(), v.getVal(), v.getWeight(), firstResId));
        }
        Files.write(Paths.get(outPath + "/vertices.csv").toAbsolutePath(), vCsvRows.toString().getBytes(), StandardOpenOption.CREATE);

        Set<String> rows = HashObjSets.newMutableSet(candidateList.size()/2);
        for (MessagePassing.Candidate c : candidateList) {
            Long o = c.getOriginRefV().getId();
            Long d = c.getDestRefV().getId();
            rows.add(String.format("%d, %d, %.6f, %d\r\n", o<d?o:d, o>=d?o:d, c.getSumSimilarity(), c.getCntMessage()));
        }
        byte[] edgesBytes = ("Source, Target, Weight, Common_Token_Cnt\r\n" + String.join("", rows)).getBytes();
        Files.write(Paths.get(outPath + "/edges.csv").toAbsolutePath(), edgesBytes, StandardOpenOption.CREATE);
        int a = 1;
    }
}
