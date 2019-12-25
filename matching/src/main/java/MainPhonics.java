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


public class MainPhonics {
    public static void main(String[] args) throws IOException {
        String[] phonicMethods = new String[]{"rgrt", "mtph", "nys", "onca", "statcan"};

        for (String m: phonicMethods) {
            System.out.println("=================="+m.toUpperCase()+"==================");
            String[] arguments = args.clone();
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = args[i].replace(".csv", String.format("_%s.csv", m));
            }
            Main.main(arguments);
        }
    }
}
