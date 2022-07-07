package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.corev6;

import at.tugraz.ist.ase.common.LoggerUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

import static at.tugraz.ist.ase.eval.PerformanceEvaluator.start;
import static at.tugraz.ist.ase.eval.PerformanceEvaluator.stop;

@Slf4j
public class LookupTableV6 {
    public static final String COUNTER_PRUNED_CC = "The number of pruned consistency checks";
    public static final String COUNTER_SUPERSET = "The number of superset";
    public static final String COUNTER_SUBSET = "The number of subset";
    public static final String TIMER_LOOKUP_ALTERNATIVE = "The time spent in lookup alternative";
    public static final String TIMER_LOOKUP_GET = "The time spent in lookup get";
    public static final String TIMER_CLEANUP = "The time spent in cleanup";

    /**
     * LookupTable holds the consistency of consistency checks
     * using a Map of <hashcode of a constraint set, the consistency of the constraint set>
     */
    protected final ConcurrentMap<Integer, ConsistencyCheckResultV6> lookupTable = new ConcurrentHashMap<>();

    private final Semaphore semaphore = new Semaphore(1); // already try fair=true

    public boolean contains(int hashCode) {
        return lookupTable.containsKey(hashCode);
    }

    public ConsistencyCheckResultV6 get(int hashCode) {
        start(TIMER_LOOKUP_GET);
        ConsistencyCheckResultV6 result = lookupTable.get(hashCode);
        stop(TIMER_LOOKUP_GET);
        return result;
    }

    /**
     * Returns the result of the consistency check for the given constraint set.
     * Usually called after checking the existence of the consistency check using the contains function.
     * @param hashCode - the hashcode of the constraint set
     */
    public Boolean getConsistency(int hashCode) throws ExecutionException, InterruptedException {
        start(TIMER_LOOKUP_GET);
        ConsistencyCheckResultV6 result = lookupTable.get(hashCode); // find the ConsistencyCheckResult for the given constraint set
        Boolean consistency = null;
        // if the worker for the ConsistencyCheck is done, return the consistency
        if (result.getWorker() == null || result.getWorker().isDone()) {
            consistency = result.isConsistent();
        } else { // if the worker for the ConsistencyCheck is not done, wait for it to finish
            try {
                consistency = result.getWorker().get(5, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.trace("{}(LookupTable-getConsistency) [hashCode={}, consistency={}]", LoggerUtils.tab(), hashCode, consistency);
            }
        }
        stop(TIMER_LOOKUP_GET);
        return consistency;
    }

    public void put(int hashCode, ConsistencyCheckResultV6 result) {
        lookupTable.put(hashCode, result);
        log.debug("{}(LookupTable-put) Put to LookupTable for [C={}]", LoggerUtils.tab(), result.getC());
    }

    public void putIfAbsent(int hashCode, ConsistencyCheckResultV6 result) {
        lookupTable.putIfAbsent(hashCode, result);
        log.debug("{}(LookupTable-putIfAbsent) Put to LookupTable for [C={}]", LoggerUtils.tab(), result.getC());
    }

    public void clear() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        lookupTable.clear();
        semaphore.release();
    }

    /**
     *
     * @param message
     */
    public void print(String message) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (log.isTraceEnabled()) {
            final StringBuilder sb = new StringBuilder(message);
            sb.append("\n size: ").append(lookupTable.size());
            for (Map.Entry<Integer, ConsistencyCheckResultV6> entry : lookupTable.entrySet()) {
                String key = entry.getKey().toString();
                ConsistencyCheckResultV6 result = entry.getValue();
                sb.append('\n').append("key: ").append(key).append("; ").append(result);
            }
            log.trace("{}(printLookupTable) LookupTable: {}", LoggerUtils.tab(), sb);
        }

        semaphore.release();
    }
}
