import org.apache.tinkerpop.gremlin.structure.Vertex;

public class Message {
    public Vertex srcRef;
    public Float similarity;
    public Vertex dstRef;
    public Vertex startTkn;
    public Integer maxLevel;
}
