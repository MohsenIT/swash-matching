import analysis.Stats;
import dao.G;
import dao.edge.E;
import dao.vertex.RefV;
import dao.vertex.V;
import evaluation.collective.PairwiseFMeasure;
import logic.MessagePassing;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public class MainSortSensitivity {
    public static void main(String[] args) throws IOException {
        for (int i = 0; i < 30; i++) {
            Main.main(args);
        }
    }
}
