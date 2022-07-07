package at.tugraz.ist.ase.cacdr.algorithms.core;

import at.tugraz.ist.ase.cacdr.checker.ChocoConsistencyChecker;
import at.tugraz.ist.ase.cdrmodel.CDRModel;
import at.tugraz.ist.ase.common.LoggerUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import static at.tugraz.ist.ase.eval.PerformanceEvaluator.incrementCounter;

/**
 * Manages a list of pre-generated {@link ChocoConsistencyChecker}s.
 * In the parallel scheme, when a worker need to check the consistency of a constraint set,
 * it will ask CCManager to get a free ChocoConsistencyChecker that is not being used by other workers.
 *
 * Each checker is an instance of {@link ChocoConsistencyChecker}, which provides a set of methods
 * to check the consistency of a constraint set. Since Choco Solver is not thread-safe, we need to
 * create a new ChocoConsistencyChecker for each thread.
 */
@Slf4j
public class CCManager {

    private static final String COUNTER_GET_CHECKER = "The number of get checkers";

//    private final CDRModel model;

    private final ConcurrentLinkedQueue<ChocoConsistencyChecker> checkers; // list of ChocoConsistencyCheckers
    private final ConcurrentMap<Integer, Boolean> usingCheckers; // control the usage of checkers, false - free, true - used

    private final Semaphore manager_semaphore;
    private final Semaphore checker_semaphore;

    public CCManager(int numCheckers, @NonNull CDRModel diagModel) throws CloneNotSupportedException {
//        this.model = diagModel;

        // create checkers
        checkers = new ConcurrentLinkedQueue<>();
        usingCheckers = new ConcurrentHashMap<>();

        List<Thread> threads = new LinkedList<>();
        for (int i = 0; i < numCheckers * 2; i++) {
            Thread t = new Thread(() -> {
                try {
                    CDRModel copy = (CDRModel) diagModel.clone(); // clone the model

                    ChocoConsistencyChecker checker = new ChocoConsistencyChecker(copy); // create a new checker

                    checkers.add(checker);
                    usingCheckers.put(checker.hashCode(), false); // initially, all checkers are not used
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            });

            t.start();
            threads.add(t);
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // limit the number of workers operating at the same time
        // numCheckers == size of checkerPool
        manager_semaphore = new Semaphore(numCheckers);

        checker_semaphore = new Semaphore(1);
    }

//    public long getNumCheckers() {
//        return checkers.size();
//    }
//
//    public long getNumFreeCheckers() {
//        return usingCheckers.values().parallelStream().filter(b -> !b).count();
//    }
//
//    public void addCheckers(long numCheckers) throws CloneNotSupportedException {
//        if (numCheckers > getNumCheckers()) {
//            long numNewCheckers = numCheckers - getNumCheckers();
//            for (int i = 0; i < numNewCheckers; i++) {
//                CDRModel copy = (CDRModel) model.clone();
//
//                ChocoConsistencyChecker checker = new ChocoConsistencyChecker(copy);
//
//                checkers.add(checker);
//                usingCheckers.put(checker.hashCode(), false); // initially, all checkers are not used
//            }
//        }
//    }

    /**
     * Returns a free {@link ChocoConsistencyChecker}
     * @throws InterruptedException if interrupted
     */
    public ChocoConsistencyChecker getChecker() throws InterruptedException {
        manager_semaphore.acquire();
        checker_semaphore.acquire();
        ChocoConsistencyChecker freeChecker = checkers.stream().filter(checker -> !usingCheckers.get(checker.hashCode())).findFirst().orElse(null);

        if (freeChecker != null) {
            usingCheckers.replace(freeChecker.hashCode(), true);
            log.debug("{}(CCManager) found a free [checker={}] ", LoggerUtils.tab(), freeChecker.hashCode());

            incrementCounter(COUNTER_GET_CHECKER);
        } else {
            // TODO - maybe create one more checker?
            log.error("{}(CCManager) no free checker found", LoggerUtils.tab());
        }

        checker_semaphore.release();
        return freeChecker;
    }

    /**
     * Release a {@link ChocoConsistencyChecker} which is done in use
     * @param checker the {@link ChocoConsistencyChecker} to release
     * @throws InterruptedException if interrupted
     */
    public void releaseChecker(ChocoConsistencyChecker checker) throws InterruptedException {
//        checker_semaphore.acquire();
        usingCheckers.replace(checker.hashCode(), false);
        log.debug("{}(CCManager) released [checker={}] ", LoggerUtils.tab(), checker.hashCode());
        manager_semaphore.release();
//        checker_semaphore.release();
    }
}
