package pt.haslab.alloy4fun.resources;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import pt.haslab.alloy4fun.data.models.Session;
import pt.haslab.alloy4fun.data.request.InstancesRequest;
import pt.haslab.alloy4fun.data.transfer.InstanceMsg;
import pt.haslab.alloy4fun.data.transfer.InstanceResponse;
import pt.haslab.alloy4fun.data.transfer.InstanceTrace;
import pt.haslab.alloy4fun.repositories.SessionRepository;
import pt.haslab.alloy4fun.util.ParseUtil;
import pt.haslab.alloy4fun.util.UncheckedIOException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Path("/getInstances")
@RequestScoped
public class AlloyGetInstances {

    @Inject
    Logger log;

    @Inject
    SessionRepository sessionManager;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPost(InstancesRequest request) {

        log.info("Received request for session: " + request.sessionId + "with parent (" + request.parentId + ")");

        if (sessionManager.deleteById(request.parentId))
            log.debug("Deleted parent session (" + request.parentId + ").");

        try {

            List<ErrorWarning> warnings = new ArrayList<>();
            A4Reporter rep = new A4Reporter() {
                public void warning(ErrorWarning msg) {
                    warnings.add(msg);
                }
            };

            Session session = ensureSession(request, rep);

            return Response.ok(batchAdd(request.numberOfInstances, session, warnings)).build();
        } catch (Err e) {
            log.info("Responding with an alloy error.");
            return Response.ok(InstanceMsg.from(e)).build();
        } catch (IOException e) {
            log.info("Responding with an error message.");
            return Response.ok(InstanceMsg.error(e.getMessage())).build();
        }
    }

    public Session ensureSession(InstancesRequest request, A4Reporter rep) throws Err, IOException {
        Session result = sessionManager.findById(request.sessionId);

        if (result == null) {
            CompModule world = ParseUtil.parseModel(request.model, rep);
            Command command = world.getAllCommands().get(request.commandIndex);

            A4Options options = new A4Options();
            options.originalFilename = java.nio.file.Path.of(world.path()).getFileName().toString();
            options.solver = A4Options.SatSolver.SAT4J;

            A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command, options);

            result = Session.create(request.sessionId, ans, command, world.getAllFunc().makeConstList());

            sessionManager.update(result);
        }

        return result;
    }

    public List<InstanceResponse> batchAdd(Integer numberOfInstances, Session session, List<ErrorWarning> warnings) throws IOException {
        List<InstanceResponse> result = new ArrayList<>();

        for (int i = 0; i < numberOfInstances && session.getSolution().satisfiable(); i++) {
            result.add(assembleInstanceResponse(session, warnings));
            session.next();
        }
        if (!session.getSolution().satisfiable()) {
            result.add(assembleInstanceResponse(session, warnings));
        }

        return result;
    }


    public InstanceResponse assembleInstanceResponse(Session session, List<ErrorWarning> warnings) {
        A4Solution answer = session.getSolution();
        int cnt = session.getCount();

        InstanceResponse result = new InstanceResponse();

        if (!warnings.isEmpty())
            result.warning = InstanceMsg.from(warnings.get(0));

        result.sessionId = session.id;
        result.unsat = !answer.satisfiable();
        result.check = session.cmd.check;
        result.cmd_n = session.cmd.label;
        result.cnt = cnt;
        result.is_static = answer.getMaxTrace() < 0;

        if (answer.satisfiable()) {
            result.loop = answer.getLoopState();
            try {
                result.instance = ParseUtil.parseInstances(answer, answer.getTraceLength())
                        .stream()
                        .map(InstanceTrace::from)
                        .toList();
            } catch (Err e) {
                log.error("Alloy errored during solution parsing.", e);
                return InstanceResponse.err(e);
            } catch (UncheckedIOException e) {
                log.error("IO error during solution parsing.", e);
            }
        }

        return result;
    }

}
