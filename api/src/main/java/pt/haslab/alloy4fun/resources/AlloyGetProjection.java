package pt.haslab.alloy4fun.resources;


import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4viz.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import pt.haslab.alloy4fun.data.models.Session;
import pt.haslab.alloy4fun.data.transfer.InstanceTrace;
import pt.haslab.alloy4fun.repositories.SessionRepository;
import pt.haslab.alloy4fun.util.ParseUtil;
import pt.haslab.alloy4fun.util.UncheckedIOException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Path("/getProjection")
public class AlloyGetProjection {

    @Inject
    Logger log;
    @Inject
    SessionRepository sessionManager;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPost(Request request) {
        Session session = sessionManager.findById(request.uuid);

        if (session == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        try {
            AlloyInstance instance = ParseUtil.parseInstance(session.answers.get(request.index));

            VizState myState = new VizState(instance);
            VizState theme = new VizState(myState);
            theme.useOriginalName(true);

            Map<AlloyType, AlloyAtom> map = new LinkedHashMap<>();
            for (AlloyAtom alloy_atom : myState.getOriginalInstance().getAllAtoms()) {
                for (String projectingType : request.type) {
                    if (alloy_atom.getVizName(theme, true).replace("$", "").equals(projectingType))
                        map.put(alloy_atom.getType(), alloy_atom);
                }
            }

            AlloyProjection currentProjection = new AlloyProjection(map);
            AlloyInstance projected = StaticProjector.project(instance, currentProjection);

            log.debug(projected.toString());

            return Response.ok(List.of(InstanceTrace.from(projected))).build();
        } catch (Err | UncheckedIOException e) {
            log.error("Error during parsing.", e);
            return Response.ok("").build();
        }
    }

    static class Request {
        public String uuid;
        public List<String> type;
        public int index;
    }
}
