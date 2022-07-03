package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.core;

import at.tugraz.ist.ase.common.LoggerUtils;
import at.tugraz.ist.ase.kb.core.Constraint;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import static at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.AbstractFastDiagP.COUNTER_EXIST_ALTERNATIVE;
import static at.tugraz.ist.ase.eval.PerformanceEvaluator.*;

@Slf4j
public class LookupTable {
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
    protected final ConcurrentMap<Integer, ConsistencyCheckResult> lookupTable = new ConcurrentHashMap<>();

    private final Semaphore semaphore = new Semaphore(1); // already try fair=true

    public boolean contains(int hashCode) {
        return lookupTable.containsKey(hashCode);
    }

    public ConsistencyCheckResult get(int hashCode) {
        start(TIMER_LOOKUP_GET);
        ConsistencyCheckResult result = lookupTable.get(hashCode);
        stop(TIMER_LOOKUP_GET);
        return result;
    }

    public void put(int hashCode, ConsistencyCheckResult result, boolean updateWithAlternative) {
        lookupTable.put(hashCode, result);
        log.debug("{}(LookupTable-put) Put to LookupTable for [C={}]", LoggerUtils.tab(), result.getC());

        // update the consistency if there exists an alternative
        if (updateWithAlternative && result.isInQueue()) {
            ConsistencyCheckResult alternative = getAlternativeSet(result.getC());

            if (alternative != null) {
                result.setConsistency(alternative.isConsistent(), Thread.currentThread().getId());
//                System.out.println("Update with alternative: " + result.getC());

                incrementCounter(COUNTER_EXIST_ALTERNATIVE);
                log.debug("{}(LookupTable-put) Update consistency for [C={}] to [{}]", LoggerUtils.tab(), result.getC(), result.isConsistent());
            }
        }
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

    public void print(String message) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (log.isTraceEnabled()) {
            final StringBuilder sb = new StringBuilder(message);
            sb.append("\n size: ").append(lookupTable.size());
            for (Map.Entry<Integer, ConsistencyCheckResult> entry : lookupTable.entrySet()) {
                String key = entry.getKey().toString();
                ConsistencyCheckResult result = entry.getValue();
                sb.append('\n').append("key: ").append(key).append("; ").append(result);
            }
            log.trace("{}(printLookupTable) LookupTable: {}", LoggerUtils.tab(), sb);
        }

        semaphore.release();
    }

    public ConsistencyCheckResult getAlternativeSet(Set<Constraint> C) {
        start(TIMER_LOOKUP_ALTERNATIVE);

        for (ConsistencyCheckResult result : lookupTable.values()) {
            if (result.isDone()) {
                if (result.isConsistent() && result.getC().containsAll(C)) {
                    incrementCounter(COUNTER_SUPERSET);
                    log.debug("{}(LookupTable-prune) return superset [superset={}]", LoggerUtils.tab(), result.getC());
                    stop(TIMER_LOOKUP_ALTERNATIVE);
                    return result;
                }
                else if (!result.isConsistent() && C.containsAll(result.getC())) {
                    incrementCounter(COUNTER_SUBSET);
                    log.debug("{}(LookupTable-prune) return subset [subset={}]", LoggerUtils.tab(), result.getC());
                    stop(TIMER_LOOKUP_ALTERNATIVE);
                    return result;
                }
            }
        }
        stop(TIMER_LOOKUP_ALTERNATIVE);
        return null;
    }

    public void cleanUpCC(Set<Constraint> C, boolean consistent) {
        log.debug("{}(LookupTable-prune) pruning supersets/subsets of [C={}]", LoggerUtils.tab(), C);

        start(TIMER_CLEANUP);

        for (Map.Entry<Integer, ConsistencyCheckResult> entry : lookupTable.entrySet()) {
            int hashCode = entry.getKey();

            if (hashCode == C.hashCode()) {
                continue;
            }

            ConsistencyCheckResult result = entry.getValue();

            if (result.isInQueue() && ((consistent && C.containsAll(result.getC())) || (!consistent && result.getC().containsAll(C)))) {

                if ((result.getWorker() != null) && result.getWorker().cancel(false)) {
                    result.setConsistency(consistent, Thread.currentThread().getId());

                    incrementCounter(COUNTER_PRUNED_CC);
                    log.debug("{}(LookupTable-prune) cancelled [C={}]", LoggerUtils.tab(), result.getC());
                }
            }
        }
        stop(TIMER_CLEANUP);
    }
}
