package pt.haslab.alloy4fun.data.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4.Pos;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public class InstanceMsg {

    @JsonInclude(NON_DEFAULT)
    public Boolean warning_error = false;
    @JsonInclude(NON_DEFAULT)
    public Boolean alloy_error = false;
    @JsonInclude(NON_NULL)
    public String msg;
    @JsonInclude(NON_NULL)
    public Integer line;
    @JsonInclude(NON_NULL)
    public Integer column;
    @JsonInclude(NON_NULL)
    public Integer line2;
    @JsonInclude(NON_NULL)
    public Integer column2;

    public InstanceMsg() {
    }

    public static InstanceMsg error(String msg) {
        InstanceMsg response = new InstanceMsg();
        response.alloy_error = true;
        response.msg = msg;
        return response;
    }

    public static InstanceMsg from(Err err) {
        InstanceMsg response = new InstanceMsg();

        if (err instanceof ErrorWarning) {
            response.warning_error = true;
        } else {
            response.alloy_error = true;
        }
        response.msg = err.getMessage();
        response.mapPos(err.pos);

        return response;
    }

    public static InstanceMsg from(Err err, String msg) {
        InstanceMsg response = from(err);
        response.msg = msg;

        return response;
    }

    private void mapPos(Pos pos) {
        if (pos != null) {
            line = pos.y;
            column = pos.x;
            line2 = pos.y2;
            column2 = pos.x2;
        }
    }
}
