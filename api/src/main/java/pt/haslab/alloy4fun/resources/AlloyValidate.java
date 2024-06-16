package pt.haslab.alloy4fun.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.mit.csail.sdg.alloy4.Err;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.haslab.alloy4fun.util.ParseUtil;
import pt.haslab.alloy4fun.util.UncheckedIOException;

import java.util.Map;


@Path("/validate")
public class AlloyValidate {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPost(Request request) {
        try {
            ParseUtil.parseModel(request.model);

            return Response.ok(Map.of("success", true)).build();
        } catch (Err e) {
            return Response.ok(makeErrorJson(e)).build();
        } catch (UncheckedIOException e) {
            return Response.serverError().build();
        }
    }

    private Map<String, Object> makeErrorJson(Err e) {
        return Map.of(
                "success", false,
                "errorLocation", Map.of("line", e.pos.x, "column", e.pos.y),
                "errorMessage", e.getMessage().replace("\n", " ").trim()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Request {
        public String model;
    }
}
