package pt.haslab.specassistant.util;

import org.jboss.logging.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public interface FutureUtil {

    static <R> R inline(CompletableFuture<R> future) throws Throwable {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    static <R> Optional<R> inlineOptional(CompletableFuture<R> future) {
        try {
            return Optional.of(inline(future));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    static <R> R inlineRuntime(CompletableFuture<R> future) {
        try {
            return inline(future);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    static <V> CompletableFuture<Void> allFutures(Stream<CompletableFuture<V>> futureStream) {
        return CompletableFuture.allOf(futureStream.toArray(CompletableFuture[]::new));
    }

    static <V> CompletableFuture<Void> allFutures(Collection<CompletableFuture<V>> futureStream) {
        return CompletableFuture.allOf(futureStream.toArray(CompletableFuture[]::new));
    }

    static <V, R> CompletableFuture<R> forEachOrderedAsync(Collection<V> elems, Function<V, CompletableFuture<R>> function) {
        if (elems.isEmpty())
            return CompletableFuture.completedFuture(null);
        List<V> targets = List.copyOf(elems);
        CompletableFuture<R> job = function.apply(targets.get(0));
        for (int i = 1; i < targets.size(); i++) {
            int finalI = i;
            job = job.thenCompose(n -> function.apply(targets.get(finalI)));
        }
        return job;
    }

    static <V, R> CompletableFuture<R> forEachOrderedAsync(Collection<V> elems, Function<V, CompletableFuture<R>> function, Function<Throwable, R> exceptions) {
        if (elems.isEmpty())
            return CompletableFuture.completedFuture(null);
        List<V> targets = List.copyOf(elems);
        CompletableFuture<R> job = function.apply(targets.get(0));
        for (int i = 1; i < targets.size(); i++) {
            int finalI = i;
            job = job.thenCompose(n -> function.apply(targets.get(finalI))).exceptionally(exceptions);
        }
        return job;
    }

    static <V> CompletableFuture<Void> forEachAsync(Stream<V> stream, Consumer<V> consumer) {
        return CompletableFuture.allOf(stream.map(t -> CompletableFuture.runAsync(() -> consumer.accept(t))).toArray(CompletableFuture[]::new));
    }

    static <V, R> CompletableFuture<Void> runEachAsync(Stream<V> stream, Function<V, CompletableFuture<R>> function) {
        return CompletableFuture.allOf(stream.map(function).toArray(CompletableFuture[]::new));
    }

    static <V, R> CompletableFuture<Void> runEachAsync(Stream<V> stream, Function<V, CompletableFuture<R>> function, Function<Throwable, R> exception) {
        return CompletableFuture.allOf(stream.map(function).map(x -> x.exceptionally(exception)).toArray(CompletableFuture[]::new));
    }

    static <V> Function<Throwable, V> errorLog(Logger logger, String error_msg) {
        return (error) -> {
            logger.error(error_msg + ": " + error.getClass().getSimpleName() + ":" + error.getMessage());
            return null;
        };
    }

    static <V> BiConsumer<V, Throwable> log(Logger logger) {
        return (nil, error) -> {
            if (error != null)
                logger.error(error);
        };
    }

    static BiConsumer<? super Void, ? super Throwable> logTrace(Logger logger, String msg) {
        return (nil, error) -> {
            if (error != null)
                logger.error(error);
            else
                logger.trace(msg);
        };
    }
}