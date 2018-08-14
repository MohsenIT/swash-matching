package helper; /**
 * Created by Ghalam on 2017-09-04.
 */

import com.steelbridgelabs.oss.neo4j.structure.Neo4JElementIdProvider;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph;
import com.steelbridgelabs.oss.neo4j.structure.providers.Neo4JNativeElementIdProvider;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.process.traversal.Operator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import java.util.List;
import java.util.Map;

import static org.apache.tinkerpop.gremlin.process.traversal.Operator.assign;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;

public class ConnectToNeo4j {

    public static void testNeo4jGraph() {
        Neo4jGraph graph = (Neo4jGraph) connectToNeo4JGraph(Neo4jConnectionType.EMBEDDED);
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

    public enum Neo4jConnectionType{EMBEDDED, BOLT}

    public static Graph connectToNeo4JGraph() {
        return connectToNeo4JGraph(Neo4jConnectionType.EMBEDDED);
    }

    public static Graph connectToNeo4JGraph(Neo4jConnectionType connectionType) {
        switch (connectionType) {
            case EMBEDDED:
                Configuration conf = new BaseConfiguration();
                conf.setProperty("gremlin.neo4j.directory","D:/University/PHDResearch/Dev/r/name-matching-graph-gen/out/names.db");
                conf.setProperty("gremlin.neo4j.multiProperties",true);
                conf.setProperty("gremlin.neo4j.metaProperties",true);
                return Neo4jGraph.open(conf);
            case BOLT:
                Driver driver = GraphDatabase.driver("bolt://localhost", AuthTokens.basic("neo4j", "8531069"));
                Neo4JElementIdProvider<?> vertexIdProvider = new Neo4JNativeElementIdProvider();
                Neo4JElementIdProvider<?> edgeIdProvider = new Neo4JNativeElementIdProvider();
                Neo4JGraph graph = new Neo4JGraph(driver, vertexIdProvider, edgeIdProvider);
                //graph.setProfilerEnabled(true);
                //int cnt = graph.execute("MATCH (n) RETURN count(n)").next().get(0).asInt();
                //List records = graph.execute("MATCH (n1) WHERE n1.resolved_id = \"31586\" RETURN n1").list();
                return graph;
        }
        return null;
    }
}


