package pt.haslab.specassistant.repositories;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;
import pt.haslab.specassistant.data.models.HintNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class HintNodeRepository implements PanacheMongoRepository<HintNode> {

    private static Document appendFormulaToDocument(Document graph_id, Map<String, String> formula) {
        Document query = graph_id;
        for (Map.Entry<String, String> entry : formula.entrySet()) {
            query = query.append("formula." + entry.getKey(), entry.getValue());
        }
        return query;
    }

    public Optional<HintNode> findByGraphIdAndFormula(ObjectId graph_id, Map<String, String> formula) {
        Document query = appendFormulaToDocument(new Document("graph_id", graph_id), formula);
        return find(query).firstResultOptional();
    }

    public synchronized HintNode incrementOrCreate(Map<String, String> formula, Boolean valid, ObjectId graph_id, String witness) {
        HintNode res = findByGraphIdAndFormula(graph_id, formula).orElseGet(() -> HintNode.create(graph_id, formula, valid, witness)).visit();
        res.persistOrUpdate();
        return res;
    }

    public void deleteByGraphId(ObjectId graph_id) {
        delete(new Document("graph_id", graph_id));
    }

    public Optional<HintNode> findBestByGraphIdAndFormulaIn(ObjectId graph_id, List<Map<String, String>> formulas) {
        return find(new Document("$or", formulas.stream().map(x -> appendFormulaToDocument(new Document("graph_id", graph_id), x)).toList()), new Document("score", -1).append("visits", -1)).firstResultOptional();
    }

    public void incrementLeaveById(ObjectId oldNodeId) {
        update(new Document("$inc", new Document("leaves", 1))).where(new Document("_id", oldNodeId));
    }

    public Stream<HintNode> streamByGraphIdAndValidTrue(ObjectId graphId) {
        return find(new Document("graph_id", graphId).append("valid", true)).stream();
    }

    public void deleteByScoreNull(ObjectId graph_id) {
        delete(new Document("score", null).append("graph_id", graph_id));
    }

    public Long getTotalVisitsFromScoredGraph(ObjectId graph_id) {
        return find(new Document("graph_id", graph_id).append("score", new Document("$ne", null))).stream().map(x -> x.visits).map(Integer::longValue).reduce(0L, Long::sum);
    }
    public Long getTotalVisitsFromGraph(ObjectId graph_id) {
        return find(new Document("graph_id", graph_id)).stream().map(x -> x.visits).map(Integer::longValue).reduce(0L, Long::sum);
    }

    public void unsetAllScoresFrom(ObjectId graph_id) {
        update(new Document("$unset", new Document("score", null))).where("graph_id", graph_id);
    }
}
