package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.corev4;

import at.tugraz.ist.ase.common.LoggerUtils;
import at.tugraz.ist.ase.kb.core.Constraint;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static at.tugraz.ist.ase.eval.PerformanceEvaluator.start;
import static at.tugraz.ist.ase.eval.PerformanceEvaluator.stop;

@Slf4j
public class LookupTableV4 {
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
    protected final ConcurrentMap<Integer, LookAheadNodeV4> lookupTable = new ConcurrentHashMap<>();

    private final Semaphore semaphore = new Semaphore(1); // already try fair=true

    public boolean contains(int hashCode) {
        return lookupTable.containsKey(hashCode);
    }

    public LookAheadNodeV4 get(int hashCode) {
        return lookupTable.get(hashCode);
    }

    public Boolean getConsistency(int hashCode) throws ExecutionException, InterruptedException {
        start(TIMER_LOOKUP_GET);
        LookAheadNodeV4 node = lookupTable.get(hashCode);
        Boolean consistency = null;
        if (node.getWorker() == null || node.getWorker().isDone()) {
            consistency = node.getConsistency();
        } else {
            try {
                consistency = node.getWorker().get(5, TimeUnit.MILLISECONDS);
            } catch (RuntimeException e) {
            } catch (TimeoutException e) {
                log.trace("{}(LookupTable-get) Timeout when waiting for get()", LoggerUtils.tab());
            }
        }
        stop(TIMER_LOOKUP_GET);
        return consistency;
    }

    public void put(Set<Constraint> C, LookAheadNodeV4 result) {
        lookupTable.put(C.hashCode(), result);
        log.debug("{}(LookupTable-put) Put to LookupTable for [C={}]", LoggerUtils.tab(), C);
    }

    public void putIfAbsent(Set<Constraint> C, LookAheadNodeV4 result) {
        lookupTable.putIfAbsent(C.hashCode(), result);
        log.debug("{}(LookupTable-putIfAbsent) Put to LookupTable for [C={}]", LoggerUtils.tab(), C);
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

    public void print(String message) throws ExecutionException, InterruptedException {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (log.isTraceEnabled()) {
            final StringBuilder sb = new StringBuilder(message);
            sb.append("\n size: ").append(lookupTable.size());
            for (Map.Entry<Integer, LookAheadNodeV4> entry : lookupTable.entrySet()) {
                String key = entry.getKey().toString();
                LookAheadNodeV4 result = entry.getValue();
                sb.append('\n').append("key: ").append(key).append("; ").append(result.getC())
                        .append(" - ").append(result.getConsistency());
            }
            log.trace("{}(printLookupTable) LookupTable: {}", LoggerUtils.tab(), sb);
        }

        semaphore.release();
    }
}
