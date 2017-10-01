package webapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import graph.Graph;
import graph.Graph.TagWithQuantity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

//@JsonAutoDetect
public class InfosByTags {
//    public class Tags implements Serializable {

//    private String order;
//    private TagWithQuantity[] tags;
    private Map<String, Integer> keyPairs;

    public InfosByTags(List<String> tags, String aggr, String lan) throws IOException {

        Graph graph = new Graph();
//        graph.addInfos(graph.loadInfos("infos-sample.json"));
        this.keyPairs = graph.infosByTags(tags, aggr, lan);
//        graph.write(System.out, "turtle");
//        this.order = order;
    }

//    public TagWithQuantity[] getTags() {
        @JsonProperty
        public Map<String, Integer> getKeyPairs() {
//        System.out.println("tag list: "+tags[0]);
//        System.out.println("tag list: "+tags.get(0));
        return keyPairs;
    }
}
