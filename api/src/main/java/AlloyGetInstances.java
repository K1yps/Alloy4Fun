import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
import edu.mit.csail.sdg.ast.Sig.SubsetSig;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4Tuple;
import edu.mit.csail.sdg.translator.A4TupleSet;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;
import kodkod.util.ints.IndexedEntry;
import utils.InstancesRequest;

@Path("/getInstances")
public class AlloyGetInstances {
	@POST
	@Produces("text/json")
	public Response doGet(String body) throws IOException {
		InstancesRequest req = parseJSON(body);
		List<ErrorWarning> warnings = new ArrayList<ErrorWarning>();
		A4Reporter rep = new A4Reporter() {
			public void warning (ErrorWarning msg) {
				warnings.add(msg);
   			}
		};
		File tempFile = File.createTempFile("a4f", "als");
		tempFile.deleteOnExit();
		CompModule world;

		try {
			world = CompUtil.parseEverything_fromString(rep, req.model);			
		} catch (Err e) {
			System.out.println(e.getMessage());
			JsonObjectBuilder instanceJSON = Json.createObjectBuilder();
			instanceJSON.add("alloy_error", true);
			instanceJSON.add("msg", e.msg);
			instanceJSON.add("line", e.pos.y);
			instanceJSON.add("column", e.pos.x);
			return Response.ok(instanceJSON.build().toString()).build();
		} catch (Exception e) {
			JsonObjectBuilder instanceJSON = Json.createObjectBuilder();
			System.out.println(e.getMessage());
			instanceJSON.add("alloy_error", true);
			instanceJSON.add("msg", e.getMessage());
			return Response.ok(instanceJSON.build().toString()).build();
		}

		JsonArrayBuilder solsArrayJSON = Json.createArrayBuilder();
		
		A4Options opt = new A4Options();
		opt.solver = A4Options.SatSolver.SAT4J;
		String res = "";
		Command command = world.getAllCommands().get(req.commandIndex);
		A4Solution ans;
		try {
			ans = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command, opt);

			if (ans.satisfiable()) {

				A4Solution aux = ans;
				try {
					for (int n = 0; n < req.numberOfInstances && aux.satisfiable(); n++) {
						UUID uuid = UUID.randomUUID();
						solsArrayJSON.add(answerToJson(uuid, aux, warnings));
						RestApplication.answers.put(uuid, aux);
						ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
						scheduler.schedule(new Runnable() {
							public void run() {
								RestApplication.answers.remove(uuid);
							}
						}, 7200, TimeUnit.SECONDS);
						aux = aux.next();
					}
				} catch (Exception e) {
					// this.iteration--;
					System.out.println("There probably aren't that many solutions!");
					res = e.getMessage();
				}
				res = solsArrayJSON.build().toString();
			} else {
				UUID uuid = UUID.randomUUID();
				res = solsArrayJSON.add(answerToJson(uuid, ans, warnings)).build().toString();

			}

		} catch (Err e) {
			System.out.println(e.getMessage());
			JsonObjectBuilder instanceJSON = Json.createObjectBuilder();
			instanceJSON.add("alloy_error", true);
			instanceJSON.add("msg", e.msg);
			instanceJSON.add("line", e.pos.y);
			instanceJSON.add("column", e.pos.x);
			return Response.ok(instanceJSON.build().toString()).build();
		}

		return Response.ok(res).build();
	}

	private InstancesRequest parseJSON(String body) {
		JSONObject jo = new JSONObject(body);
		InstancesRequest req = new InstancesRequest();

		req.model = jo.getString("model");
		req.numberOfInstances = jo.getInt("numberOfInstances");
		req.commandIndex = jo.getInt("commandIndex");

		return req;
	}

	public JsonObject answerToJson(UUID uuid, A4Solution answer, List<ErrorWarning> warns) {
		JsonObjectBuilder instanceJSON = Json.createObjectBuilder();

		if (!answer.satisfiable()) {
			instanceJSON.add("unsat", "true");

			if (warns.size() > 0) {
				instanceJSON.add("warning_error", true);
				instanceJSON.add("msg", warns.get(0).msg);
				instanceJSON.add("line", warns.get(0).pos.y);
				instanceJSON.add("column", warns.get(0).pos.x);
			}

			return instanceJSON.build();
		}

		try {
			instanceJSON.add("uuid", uuid.toString());
			Instance sol = answer.debugExtractKInstance();
			instanceJSON.add("unsat", false);

			JsonArrayBuilder integersArrayJSON = Json.createArrayBuilder();
			for (IndexedEntry<TupleSet> e : sol.intTuples()) {
				Object atom = e.value().iterator().next().atom(0);
				integersArrayJSON.add(atom.toString());
			}
			instanceJSON.add("integers", integersArrayJSON);

			JsonArrayBuilder atomsJSON = Json.createArrayBuilder();
			JsonArrayBuilder fieldsJSON = Json.createArrayBuilder();

			for (Sig signature : answer.getAllReachableSigs()) {
				atomsJSON.add(sigToJSON(answer, signature));

				for (Field field : signature.getFields()) {
					fieldsJSON.add(fieldToJSON(answer, signature, field));
				}
			}
			instanceJSON.add("atoms", atomsJSON);
			instanceJSON.add("fields", fieldsJSON);

			instanceJSON.add("skolem", skolemsToJSON(answer));

			if (warns.size() > 0) {
				instanceJSON.add("warning_error", true);
				instanceJSON.add("msg", warns.get(0).msg);
				instanceJSON.add("line", warns.get(0).pos.y);
				instanceJSON.add("column", warns.get(0).pos.x);
			}

			return instanceJSON.build();
		} catch (Err er) {
			JsonObjectBuilder errorJSON = Json.createObjectBuilder();
			errorJSON.add("err", String.format("Evaluator error occurred: %s", er));
			return errorJSON.build();
		}
	}

	JsonObjectBuilder skolemsToJSON(A4Solution answer) throws Err {
		JsonObjectBuilder skolemJSON = Json.createObjectBuilder();
		for (ExprVar var : answer.getAllSkolems()) {
			A4TupleSet tupleSet = (A4TupleSet) answer.eval(var);
			JsonArrayBuilder varTuplesJSON = Json.createArrayBuilder();
			for (A4Tuple tuple : tupleSet) {
				varTuplesJSON.add(tupleToJSONArray(tuple));
			}
			skolemJSON.add(var.label, varTuplesJSON);
		}
		return skolemJSON;
	}

	JsonArrayBuilder tupleToJSONArray(A4Tuple tuple) {
		JsonArrayBuilder tupleJSON = Json.createArrayBuilder();
		for (int i = 0; i < tuple.arity(); i++)
			tupleJSON.add(tuple.atom(i));
		return tupleJSON;
	}

	JsonObjectBuilder fieldToJSON(A4Solution answer, Sig signature, Field field) {
		JsonObjectBuilder fieldJSON = Json.createObjectBuilder();
		fieldJSON.add("type", signature.toString());
		fieldJSON.add("label", field.label);

		Iterator<A4Tuple> tupleIt = answer.eval(field).iterator();
		if (tupleIt.hasNext()) {
			A4Tuple tuple = tupleIt.next();
			fieldJSON.add("arity", tuple.arity());

			JsonArrayBuilder tupleValuesJSON = Json.createArrayBuilder();
			tupleValuesJSON.add(tupleToJSONArray(tuple));
			while (tupleIt.hasNext())
				tupleValuesJSON.add(tupleToJSONArray(tupleIt.next()));
			fieldJSON.add("values", tupleValuesJSON);
		} else {
			fieldJSON.add("values", Json.createArrayBuilder());
		}

		return fieldJSON;
	}

	JsonObjectBuilder sigToJSON(A4Solution answer, Sig signature) {
		JsonObjectBuilder atomJSON = Json.createObjectBuilder();
		atomJSON.add("type", signature.toString());
		atomJSON.add("isSubsetSig", signature instanceof SubsetSig);

		String parent = "";
		if (signature instanceof PrimSig) {
			PrimSig primSignature = (PrimSig) signature;
			if (primSignature.parent != null) {
				parent = primSignature.parent.label;
			} else
				parent = "null";
		}
		atomJSON.add("parent", parent);

		JsonArrayBuilder atomParentsJSON = Json.createArrayBuilder();
		if (signature instanceof SubsetSig) {
			SubsetSig subsetSignature = (SubsetSig) signature;
			for (Sig subsetSigParent : subsetSignature.parents) {
				atomParentsJSON.add(subsetSigParent.label);
			}
		}
		atomJSON.add("parents", atomParentsJSON);
		atomJSON.add("isPrimSig", signature instanceof PrimSig);

		JsonArrayBuilder instancesJSON = Json.createArrayBuilder();
		for (A4Tuple tuple : answer.eval(signature)) {
			instancesJSON.add(tuple.atom(0));
		}
		atomJSON.add("values", instancesJSON);

		return atomJSON;
	}
}