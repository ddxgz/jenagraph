package webapp;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import graph.Graph.TagWithQuantity;
import graph.Graph;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

//@JsonAutoDetect
public class Tags {
//    public class Tags implements Serializable {

//    private String order;
//    private TagWithQuantity[] tags;
    private List<TagWithQuantity> tags;

    public Tags(String order) throws IOException {

        Graph graph = new Graph();
        graph.addInfos(graph.loadInfos("infos-sample.json"));
        this.tags = graph.tags(order, true);
//        graph.write(System.out, "turtle");
//        this.order = order;
    }

//    @JsonProperty
//    public TagWithQuantity[] getTags() {
        public List<TagWithQuantity> getTags() {
//        System.out.println("tag list: "+tags[0]);
        System.out.println("tag list: "+tags.get(0));
        return tags;
    }
}
