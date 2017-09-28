package webapp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import graph.Graph;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping("/greeting")
    public Greeting greeting(@RequestParam(value="name", defaultValue="World") String name) {
        return new Greeting(counter.incrementAndGet(),
                            String.format(template, name));
    }

    @RequestMapping("/tags")
//    public Map<String, Object> tags(@RequestParam(value="order", defaultValue = "alphabet") String order) throws IOException {
        public Tags tags(@RequestParam(value="order", defaultValue = "alphabet") String order) throws IOException {
//        System.out.println("got order parameter: "+order);
//        Graph graph = new Graph();
//        graph.addInfos(graph.loadInfos("infos-sample.json"));
//        this.tagList = graph.tags(order);
//        Map<String, Object> res = new HashMap<>();
//        res.put("tags", graph.tags(order));
        return new Tags(order);
//        return res;
    }
}
