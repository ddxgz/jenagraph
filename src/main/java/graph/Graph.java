package graph;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import com.sun.org.apache.regexp.internal.RE;
import org.apache.jena.base.Sys;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import com.google.gson.*;

public class Graph {

    private Model graph;
    private static String tdbDir;
    private Dataset dataset;

    public static String nsLinkedInfo = "http://www.linkedinfo.co/vocab/";
    public static String prefixLinkedInfo = "http://www.linkedinfo.co/";
    public static String prefixInfo = prefixLinkedInfo + "infos/";
    public static String prefixCreator = prefixLinkedInfo + "creators/";
    public static String prefixTag = prefixLinkedInfo + "tags/";
    public static String prefixAggregation = prefixLinkedInfo + "aggregations/";

    Resource resInfo;
    Resource resTag;
    Resource resAggregation;
    Property propURL;
    Property propInfoKey;
    Property propCreatorID;
    Property propTagID;
    Property propHasTag;
    Property propAggregationID;
    Property propInAggregation;

    public class InfosJSON {
        /* "content": [],
        * "rel_self":"/infos?offset=0",
        * "rel_prev":"/infos?offset=-10",
        * "rel_next":"/infos?offset=10",
        * "per_page":10,
        * "quantity":1370,
        * "offset":0
        * */
        Info[] content;
        String rel_self;
        String rel_prev;
        String rel_next;
        int per_page;
        int quantity;
        int offset;

        public Info[] getContent() {
            return content;
        }
    }

    class Info {
        String key;
        String url;
        String title;
        String language;
        Creator[] creators;
        Tag[] tags;
        Aggregation[] inAggregations;
    }

    class Creator {
        String creatorID;
        String label;
    }

    class Tag {
        String tagID;
        String label;

        public String getTagID() {
            return tagID;
        }

        public String getLabel() {
            return label;
        }
    }

    class Aggregation {
        String aggregationID;
    }

    public Graph() {
        String value = System.getenv("USER");
        if (value != null && value.equals("pc")) {
            this.tdbDir = "/Users/pc/linkedinfo/TDBData";
        } else {
            this.tdbDir = "/root/linkedinfo/TDBData";
        }

        this.dataset = TDBFactory.createDataset(this.tdbDir);
        log("connected to TDB at: " + this.tdbDir);
//        this.graph = ModelFactory.createDefaultModel();
//        this.graph = this.tdbDataset.getDefaultModel();

        this.dataset.begin(ReadWrite.WRITE);
        try {
            this.graph = this.dataset.getDefaultModel();

            resInfo = this.graph.createResource(this.nsLinkedInfo + "info");
            resTag = this.graph.createResource(this.nsLinkedInfo + "tag");
            resAggregation = this.graph.createResource(this.nsLinkedInfo + "aggregation");

            propURL = this.graph.createProperty(this.nsLinkedInfo + "url");
            propInfoKey = this.graph.createProperty(this.nsLinkedInfo + "key");
            propCreatorID = this.graph.createProperty(this.nsLinkedInfo + "creatorID");
            propTagID = this.graph.createProperty(this.nsLinkedInfo + "tagID");
            propHasTag = this.graph.createProperty(this.nsLinkedInfo + "hasTag");
            propAggregationID = this.graph.createProperty(this.nsLinkedInfo + "aggregationID");
            propInAggregation = this.graph.createProperty(this.nsLinkedInfo + "inAggregation");

            this.dataset.commit();
        } finally {
            this.dataset.end();
        }
    }

    //TODO: to use model transaction, and graph set method instead of add
    public int addInfosNoUpdate(Info[] infos) {
//        System.out.println("infos: " + infos[0].title);
        this.dataset.begin(ReadWrite.WRITE);
        try {
            this.graph = this.dataset.getDefaultModel();
            for (Info info : infos) {
//                System.out.println("info title: " + info.title);
                Resource infoNode = this.graph.createResource(this.prefixInfo + info.key);
                this.graph.add(infoNode, RDF.type, this.resInfo);
                this.graph.add(infoNode, this.propInfoKey, info.key);
                this.graph.add(infoNode, this.propURL, info.url);
                this.graph.add(infoNode, DC.title, info.title);
                this.graph.add(infoNode, DC.language, info.language);

                for (Creator creator : info.creators) {
//                    System.out.println("info creator: " + creator.creatorID);
                    Resource creatorNode = this.graph.createResource(this.prefixCreator + creator.creatorID);
                    this.graph.add(creatorNode, RDF.type, DC.creator);
                    this.graph.add(creatorNode, RDFS.label, creator.label);
                    this.graph.add(creatorNode, this.propCreatorID, creator.creatorID);
                    this.graph.add(infoNode, DC.creator, creatorNode);
                }
/*            Resource creatorNode = this.graph.createResource(this.prefixCreator + "defaultCreator");
            this.graph.add(creatorNode, RDF.type, DC.creator);
            this.graph.add(creatorNode, RDFS.label, "default creator");
            this.graph.add(creatorNode, this.propCreatorID, "defaultCreator");
            this.graph.add(infoNode, DC.creator, creatorNode);*/

                for (Tag tag : info.tags) {
//                    System.out.println("info tag: " + tag.tagID);
                    Resource tagNode = this.graph.createResource(this.prefixTag + tag.tagID);
//                    tagNode.addProperty(RDFS.label, tag.label);
                    this.graph.add(tagNode, RDF.type, this.resTag);
                    this.graph.add(tagNode, RDFS.label, tag.label);
                    this.graph.add(tagNode, this.propTagID, tag.tagID);
                    this.graph.add(infoNode, this.propHasTag, tagNode);
                }

                if (info.inAggregations.length > 0) {
//                System.out.println("info 0: "+info.inAggregations[0].aggregationID);
                    for (Aggregation aggr : info.inAggregations) {
//                        System.out.println("info aggr: " + aggr.aggregationID);
                        Resource aggrNode = this.graph.createResource(this.prefixTag + aggr.aggregationID);
                        this.graph.add(aggrNode, RDF.type, this.resAggregation);
                        this.graph.add(aggrNode, this.propAggregationID, aggr.aggregationID);
                        this.graph.add(infoNode, this.propInAggregation, aggrNode);
                    }
                }
            }
            this.dataset.commit();
        } finally {
            this.dataset.end();
        }
//        System.out.println("size: " + this.size());
        return 0;
    }

    public int addInfos(Info[] infos) {
//        System.out.println("infos: " + infos[0].title);
        this.dataset.begin(ReadWrite.WRITE);
        try {
            this.graph = this.dataset.getDefaultModel();
            for (Info info : infos) {
//                System.out.println("info title: " + info.title);
                Resource infoNode = this.graph.createResource(this.prefixInfo + info.key);

                // remove all statements under resource, then add new
                this.graph.removeAll(infoNode, null, null);

                this.graph.add(infoNode, RDF.type, this.resInfo);
                this.graph.add(infoNode, this.propInfoKey, info.key);
                this.graph.add(infoNode, this.propURL, info.url);
                this.graph.add(infoNode, DC.title, info.title);
                this.graph.add(infoNode, DC.language, info.language);

                for (Creator creator : info.creators) {
//                    System.out.println("info creator: " + creator.creatorID);
                    Resource creatorNode = this.graph.createResource(this.prefixCreator + creator.creatorID);
                    // remove all statements under resource, then add new
                    this.graph.removeAll(creatorNode, null, null);
                    this.graph.add(creatorNode, RDF.type, DC.creator);
                    this.graph.add(creatorNode, RDFS.label, creator.label);
                    this.graph.add(creatorNode, this.propCreatorID, creator.creatorID);
                    this.graph.add(infoNode, DC.creator, creatorNode);
                }
/*            Resource creatorNode = this.graph.createResource(this.prefixCreator + "defaultCreator");
            this.graph.add(creatorNode, RDF.type, DC.creator);
            this.graph.add(creatorNode, RDFS.label, "default creator");
            this.graph.add(creatorNode, this.propCreatorID, "defaultCreator");
            this.graph.add(infoNode, DC.creator, creatorNode);*/

                for (Tag tag : info.tags) {
//                    System.out.println("info tag: " + tag.tagID);
//                    Resource tagNode = this.addOrChangeTag(tag);
                    Resource tagNode = this.graph.createResource(this.prefixTag + tag.tagID);

                    // remove all statements under resource, then add new
                    this.graph.removeAll(tagNode, null, null);

                    tagNode.addProperty(this.propTagID, tag.tagID);
                    tagNode.addProperty(RDF.type, this.resTag);
                    tagNode.addProperty(RDFS.label, tag.label);
//                    this.graph.add(tagNode, RDF.type, this.resTag);
//                    this.graph.add(tagNode, RDFS.label, tag.label);
//                    this.graph.add(tagNode, this.propTagID, tag.tagID);
                    this.graph.add(infoNode, this.propHasTag, tagNode);
                }

                if (info.inAggregations.length > 0) {
//                System.out.println("info 0: "+info.inAggregations[0].aggregationID);
                    for (Aggregation aggr : info.inAggregations) {
//                        System.out.println("info aggr: " + aggr.aggregationID);
                        Resource aggrNode = this.graph.createResource(this.prefixTag + aggr.aggregationID);
                        // remove all statements under resource, then add new
                        this.graph.removeAll(aggrNode, null, null);

                        this.graph.add(aggrNode, RDF.type, this.resAggregation);
                        this.graph.add(aggrNode, this.propAggregationID, aggr.aggregationID);
                        this.graph.add(infoNode, this.propInAggregation, aggrNode);
                    }
                }
            }
            this.dataset.commit();
        } finally {
            this.dataset.end();
        }
//        System.out.println("size: " + this.size());
        return 0;
    }

    public Resource addOrChangeTag(Tag tag) {
        if (tagChanged(tag)) {
            return this.changeTag(tag);
        } else {
            return this.addTag(tag);
        }
    }

    private boolean tagChanged(Tag tag) {
        Resource tagNode = this.graph.getResource(this.prefixTag+tag.tagID);
        if (!tagNode.getProperty(RDFS.label).equals(tag.label)){
            return true;
        }
        return false;
    }

    public Resource changeTag(Tag tag){
        this.removeTag(tag.tagID);
        return this.addTag(tag);
    }

    public void removeTag(String tagID){
        Resource tagNode = this.graph.createResource(this.prefixTag + tagID);
//        this.graph.remove(this.graph.getProperty(tagNode, RDFS.label));
//        this.graph.remove(this.graph.getProperty(tagNode, RDF.type));
//        this.graph.remove(this.graph.getProperty(tagNode, this.propTagID));
        this.graph.removeAll(tagNode, null, null);
    }

    private Resource addTag(Tag tag){
        Resource tagNode = this.graph.createResource(this.prefixTag + tag.tagID);
        tagNode.addProperty(this.propTagID, tag.tagID);
        tagNode.addProperty(RDF.type, this.resTag);
        tagNode.addProperty(RDFS.label, tag.label);
        return tagNode;
    }

    public class TagWithQuantity extends Tag {
        int quantity;

        public int getQuantity() {
            return quantity;
        }

        public String toString() {
            return this.tagID + " - " + this.label + " - " + this.quantity;
        }
    }


    private void tagMapToList(Map<String, Integer> tagCount, List<TagWithQuantity> tags){
        for (Map.Entry<String, Integer> tagC : tagCount.entrySet()) {
            TagWithQuantity tag = new TagWithQuantity();
            tag.tagID = tagC.getKey();
            tag.quantity = tagC.getValue();
//            log("tag.quantity: " + tag.quantity);
            tag.label = this.graph.getResource(this.prefixTag + tag.tagID).getProperty(RDFS.label).getString();
            tags.add(tag);
        }
    }

    public Map<String, Integer> infosByTags(List<String> tagNames, String aggr, String lan) {
        Map<String, Integer> keyCount = new HashMap<String, Integer>();

        this.dataset.begin(ReadWrite.READ);
        try {
            this.graph = this.dataset.getDefaultModel();
            keyCount = traveTags(tagNames,aggr,lan);
            if(keyCount.size() == 0) {
                tagNames = tagNames.subList(0,tagNames.size()-1);
                this.dataset.end();
                return infosByTags(tagNames,aggr,lan);
            }

        } finally {
            this.dataset.end();
        }
        return keyCount;
    }

    public Map<String, Integer> traveTags(List<String> tagNames, String aggr, String lan) {
        Map<String, Integer> keyCount = new HashMap<String, Integer>();
        String conditions = "";
        for (String tagName : tagNames) {
//                Resource tag = this.graph.getResource(this.prefixTag+tagName);
            String tagCond = " ?info <" + this.propHasTag + "> <" + this.prefixTag + tagName + "> .\n";
            conditions += tagCond;
        }

//            Resource aggrRes = this.graph.getResource(this.prefixAggregation + aggr);
//            StmtIterator infosIter = this.graph.listStatements(null,this.propInAggregation,aggrRes);


        String queryString = "SELECT ?info \n" +
                "WHERE { " + conditions + " }";
        //                log("query string: "+queryString);
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, this.graph)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext(); ) {
                QuerySolution soln = results.nextSolution();
                RDFNode info = soln.get("info");       // Get a result variable by name.
                String infoKey = info.toString();
                keyCount.put(infoKey, tagNames.size());
//                    Resource tag = soln.getResource("tag") ; // Get a result variable - must be a resource
//                        Literal l = soln.getLiteral("VarL") ;   // Get a result variable - must be a literal
//                        log("x: "+x +"  r:"+r);
//                    String tagID = info.getProperty(this.propTagID).getString();
//                    Integer i = tagCount.get(tagID);
//                    if (i == null) {
//                        tagCount.put(tagID, 1);
//                    } else {
//                            log("i: " + i);
//                        tagCount.put(tagID, i + 1);
//                    }
            }
        }
        return keyCount;
    }
    //TODO: different order
//    public TagWithQuantity[] tags(String order) {
    public List<TagWithQuantity> tags(String order, Boolean sparql) {
//        Tag[] tags = new Tag[]{};
        List<TagWithQuantity> tags = new ArrayList<TagWithQuantity>();
        Map<String, Integer> tagCount = new HashMap<String, Integer>();
//        Selector selector = new SimpleSelector(null, this.propTagID, (RDFNode) null);
//        StmtIterator iter = this.graph.listStatements(
//                new SimpleSelector(null, this.propTagID, (RDFNode) null) { });

//        log("got order parameter: "+order);
        this.dataset.begin(ReadWrite.READ);
        try {
            this.graph = this.dataset.getDefaultModel();
            if (order.endsWith("quantity")) {
//                log("got order parameter: "+order);
                String queryString =  "SELECT ?info ?tag \n" +
                        "WHERE { ?info <" + this.propHasTag + "> ?tag }";
//                log("query string: "+queryString);
                Query query = QueryFactory.create(queryString) ;
                try (QueryExecution qexec = QueryExecutionFactory.create(query, this.graph)) {
                    ResultSet results = qexec.execSelect() ;
                    for ( ; results.hasNext() ; )
                    {
                        QuerySolution soln = results.nextSolution() ;
                        RDFNode info = soln.get("info") ;       // Get a result variable by name.
                        Resource tag = soln.getResource("tag") ; // Get a result variable - must be a resource
//                        Literal l = soln.getLiteral("VarL") ;   // Get a result variable - must be a literal
//                        log("x: "+x +"  r:"+r);
                        String tagID = tag.getProperty(this.propTagID).getString();
                        Integer i = tagCount.get(tagID);
                        if (i == null) {
                            tagCount.put(tagID, 1);
                        } else {
//                            log("i: " + i);
                            tagCount.put(tagID, i + 1);
                        }
                    }
                }

                tagMapToList(tagCount, tags);

                Collections.sort(tags, new Comparator<TagWithQuantity>() {
                    @Override
                    public int compare(TagWithQuantity z1, TagWithQuantity z2) {
                        if (z1.quantity < z2.quantity)
                            return 1;
                        if (z1.quantity > z2.quantity)
                            return -1;
                        return 0;
                    }
                });

            } else {
//                log("got order parameter: "+order);
                String queryString =  "SELECT ?tag \n" +
                        "WHERE { ?tag a "+ "<" + this.resTag + "> }";
//                log("query string: "+queryString);
                Query query = QueryFactory.create(queryString) ;
                try (QueryExecution qexec = QueryExecutionFactory.create(query, this.graph)) {
                    ResultSet results = qexec.execSelect() ;
                    for ( ; results.hasNext() ; )
                    {
                        QuerySolution soln = results.nextSolution() ;
//                        RDFNode info = soln.get("info") ;       // Get a result variable by name.
                        Resource tag = soln.getResource("tag") ; // Get a result variable - must be a resource
//                        Literal l = soln.getLiteral("VarL") ;   // Get a result variable - must be a literal
//                        log("x: "+x +"  r:"+r);
                        String tagID = tag.getProperty(this.propTagID).getString();
                        Integer i = tagCount.get(tagID);
                        if (i == null) {
                            tagCount.put(tagID, 1);
                        } else {
//                            log("i: " + i);
                            tagCount.put(tagID, i + 1);
                        }
                    }
                }

                // TODO:change this part to sort by alphabeta
                tagMapToList(tagCount, tags);

                Collections.sort(tags, new Comparator<TagWithQuantity>() {
                    @Override
                    public int compare(TagWithQuantity z1, TagWithQuantity z2) {
                        return z1.label.compareToIgnoreCase(z2.label);
                    }
                });
            }

        } finally {
            this.dataset.end();
        }
//        LinkedHashMap<String, Integer> sortedTag = this.sortByValue(tagCount);
//        tags = tagCount.keySet();
//        TagWithQuantity[] tagsArray = new TagWithQuantity[tags.size()];
//        tagsArray = tags.toArray(tagsArray);
//        return tagsArray;
        return tags;
    }

    //TODO: not correctly implemented
    public List<TagWithQuantity> tags(String order) {
//        Tag[] tags = new Tag[]{};
        List<TagWithQuantity> tags = new ArrayList<TagWithQuantity>();
        Map<String, Integer> tagCount = new HashMap<String, Integer>();
//        Selector selector = new SimpleSelector(null, this.propTagID, (RDFNode) null);
//        StmtIterator iter = this.graph.listStatements(
//                new SimpleSelector(null, this.propTagID, (RDFNode) null) { });

//        log("got order parameter: "+order);
        this.dataset.begin(ReadWrite.READ);
        try {
            this.graph = this.dataset.getDefaultModel();
            if (order.endsWith("quantity")) {
                log("got order parameter: "+order);
                String queryString =  "SELECT ?info ?tag" +
                        "WHERE { ?info " + this.propHasTag + " ?tag }";
                log("query string: "+queryString);
                Query query = QueryFactory.create(queryString) ;
                try (QueryExecution qexec = QueryExecutionFactory.create(query, this.graph)) {
                    ResultSet results = qexec.execSelect() ;
                    for ( ; results.hasNext() ; )
                    {
                        QuerySolution soln = results.nextSolution() ;
                        RDFNode x = soln.get("varName") ;       // Get a result variable by name.
                        Resource r = soln.getResource("VarR") ; // Get a result variable - must be a resource
                        Literal l = soln.getLiteral("VarL") ;   // Get a result variable - must be a literal
                    }
                }

                for (Map.Entry<String, Integer> tagC : tagCount.entrySet()) {
                    TagWithQuantity tag = new TagWithQuantity();
                    tag.tagID = tagC.getKey();
                    tag.quantity = tagC.getValue();
                    log("tag.quantity: " + tag.quantity);
                    tag.label = this.graph.getResource(this.prefixTag + tag.tagID).getProperty(RDFS.label).getString();
                    tags.add(tag);
                }
            } else {
                ResIterator iter = this.graph.listSubjectsWithProperty(this.propTagID);
                while (iter.hasNext()) {
                    String tagID = iter.nextResource().getProperty(this.propTagID).getString();
                    Integer i = tagCount.get(tagID);
//                log("i: "+i);
                    if (i == null) {
                        tagCount.put(tagID, 1);
                    } else {
                        log("i: " + i);
                        tagCount.put(tagID, i + 1);
                    }
                }

                // TODO:change this part to sort by alphabeta
                for (Map.Entry<String, Integer> tagC : tagCount.entrySet()) {
                    TagWithQuantity tag = new TagWithQuantity();
                    tag.tagID = tagC.getKey();
                    tag.quantity = tagC.getValue();
                    log("tag.quantity: " + tag.quantity);
                    tag.label = this.graph.getResource(this.prefixTag + tag.tagID).getProperty(RDFS.label).getString();
                    tags.add(tag);
                }
            }

        } finally {
            this.dataset.end();
        }
//        LinkedHashMap<String, Integer> sortedTag = this.sortByValue(tagCount);
//        tags = tagCount.keySet();
//        TagWithQuantity[] tagsArray = new TagWithQuantity[tags.size()];
//        tagsArray = tags.toArray(tagsArray);
//        return tagsArray;
        return tags;
    }

//    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<String, Integer> map) {
//        return map.entrySet()
//                .stream()
//                .sorted(Map.Entry.comparingByValue(/*Collections.reverseOrder()*/))
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        Map.Entry::getValue,
//                        (e1, e2) -> e1,
//                        LinkedHashMap::new
//                ));
//    }

//    public listInfos() {
//        StmtIterator iter = this.graph.listStatements(
//                new SimpleSelector(null, null, null) );
//
//    }

    public Info[] loadInfos(String filename) throws IOException {
        String infosString = new String(Files.readAllBytes(Paths.get(filename)));

        Gson gson = new Gson();
        InfosJSON infosJSON = gson.fromJson(infosString, InfosJSON.class);

//        System.out.println("info 0: "+infosJSON.content[0].inAggregations[0]);

        return infosJSON.content;
    }

    public Model write(OutputStream writer, String lang) {
        this.dataset.begin(ReadWrite.READ);
        try {
            this.graph = this.dataset.getDefaultModel();
            return this.graph.write(writer, lang);
        } finally {
            this.dataset.end();
        }
    }

    public long size() {
        this.dataset.begin(ReadWrite.READ);
        try {
            this.graph = this.dataset.getDefaultModel();
            return this.graph.size();
        } finally {
            this.dataset.end();
        }
    }

    static final long startMillis = System.currentTimeMillis();

    private static void log(String string, Object... args) {
        long millisSinceStart = System.currentTimeMillis() - startMillis;
        System.out.printf("%20s %6d %s\n", Thread.currentThread().getName(), millisSinceStart,
                String.format(string, args));
    }
}
