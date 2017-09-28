package graph;

import java.io.IOException;

public class themain {
    public static void main(String[] args) throws IOException {
        Graph g = new Graph();
//        g.addInfos(g.loadInfos("infos-sample.json"));
//        g.write(System.out, "Turtle");
        System.out.println("size: "+g.tags("quantity", true));

    }
}
