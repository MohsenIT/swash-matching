import helper.ConnectToNeo4j;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.process.traversal.Operator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.apache.tinkerpop.gremlin.process.traversal.Operator.assign;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;


public class Main {
    public static void main(String[] args) throws Exception {
        Neo4jGraph graph = (Neo4jGraph) ConnectToNeo4j.connectToNeo4JGraph(ConnectToNeo4j.Neo4jConnectionType.EMBEDDED);
        assert graph != null;
        GraphTraversalSource g = graph.traversal();

        List<Object> sources = g.V().hasLabel("REF").limit(1).values("val").toList();
        List<Object> targets = g.withBulk(false).withSack(1.0f, Operator.sum).V().hasLabel("REF").limit(1)
                .out("REF_TKN").in("REF_TKN").sack().toList();

        List<Map<String, Object>> maps = g.V().hasLabel("REF")
                .has("val", "valeri frolov").as("source")
                .sack(assign).by(
                        project("name", "sim")
                                .by(values("val"))
                                .by(constant(1.0f))
                )
                .out("REF_TKN")
                .sack(assign).by(
                        project("name", "sim")
                                .by(sack().select("name"))
                                .by(union(sack().select("sim"), constant(2.0f)).sum())
                )
                .in("REF_TKN")
                .sack(assign).map(e -> sack(Operator.div).select("sim").by(constant(2)))
                .by(
                        project("name", "sim")
                                .by(sack().select("name"))
                                .by()
                )
                .as("target")
                .project("target", "source", "sim")
                .by(values("val"))
                .by(sack().select("name"))
                .by(sack().select("sim"))
                .toList();

        Long cnt = g.V().hasLabel("REF").out("REF_TKN").in("REF_TKN").count().next();
        System.out.println(cnt);

    }

}
