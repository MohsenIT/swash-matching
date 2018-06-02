package helper; /**
 * Created by Ghalam on 2017-09-04.
 */

import com.steelbridgelabs.oss.neo4j.structure.Neo4JElementIdProvider;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph;
import com.steelbridgelabs.oss.neo4j.structure.providers.Neo4JNativeElementIdProvider;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

public class ConnectToNeo4j {

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


