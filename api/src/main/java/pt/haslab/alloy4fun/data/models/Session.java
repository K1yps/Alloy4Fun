package pt.haslab.alloy4fun.data.models;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.translator.A4Solution;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class Session {

    public String id;

    public Instant lastAccess;

    public List<A4Solution> answers;

    public Command cmd;

    public Collection<Func> skolem;

    public static Session create(String sessionId, A4Solution ans, Command cmd, Collection<Func> skolem) {
        Session result = new Session();

        result.id = sessionId;
        result.registerAccess();
        result.answers = new ArrayList<>(List.of(ans));
        result.cmd = cmd;
        result.skolem = skolem;

        return result;
    }

    public synchronized void next() throws Err {
        answers.add(answers.get(answers.size() - 1).next());
    }

    public synchronized Integer getCount() {
        return answers.size() - 1;
    }

    public void registerAccess() {
        lastAccess = Instant.now();
    }

    public synchronized boolean isIdleForXSec(Long seconds) {
        return lastAccess.plusSeconds(seconds).isBefore(Instant.now());
    }

    public synchronized A4Solution getSolution() {
        if (!answers.isEmpty())
            return answers.get(answers.size() - 1);
        return null;
    }
}
