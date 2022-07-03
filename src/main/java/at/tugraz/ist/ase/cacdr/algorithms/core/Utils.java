package at.tugraz.ist.ase.cacdr.algorithms.core;

import at.tugraz.ist.ase.common.LoggerUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@UtilityClass
@Slf4j
public class Utils {
    public void shutdownAndAwaitTermination(ForkJoinPool pool, String poolName) {
        log.debug("{}(shutdownAndAwaitTermination) Trying to shut down the pool {}", LoggerUtils.tab(), pool);
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                log.debug("{}(shutdownAndAwaitTermination) Trying to shut down NOW the pool {}", LoggerUtils.tab(), pool);
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(1, TimeUnit.SECONDS))
                    log.debug("{}(shutdownAndAwaitTermination) Pool {} did not terminate", LoggerUtils.tab(), pool);
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            log.debug("{}(shutdownAndAwaitTermination) Forging to shut down NOW the pool {}", LoggerUtils.tab(), pool);
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

//    public void printLookupTable(String message, @NonNull ConcurrentMap<Integer, ConsistencyCheckResult> lookupTable) {
//        if (log.isTraceEnabled()) {
//            final StringBuilder sb = new StringBuilder(message);
//            sb.append("\n size: ").append(lookupTable.size());
//            for (Map.Entry<Integer, ConsistencyCheckResult> entry : lookupTable.entrySet()) {
//                String key = entry.getKey().toString();
//                ConsistencyCheckResult result = entry.getValue();
//                sb.append('\n').append("key: ").append(key).append("; ").append(result);
//            }
//            log.trace("{}(printLookupTable) LookupTable: {}", LoggerUtils.tab(), sb);
//        }
//    }
}
