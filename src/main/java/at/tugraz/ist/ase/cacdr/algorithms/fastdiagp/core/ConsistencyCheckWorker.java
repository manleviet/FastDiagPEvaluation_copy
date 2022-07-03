package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.core;

import at.tugraz.ist.ase.cacdr.algorithms.core.CCManager;
import at.tugraz.ist.ase.cacdr.checker.ChocoConsistencyChecker;
import at.tugraz.ist.ase.common.LoggerUtils;
import at.tugraz.ist.ase.kb.core.Constraint;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.RecursiveAction;

import static at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.AbstractFastDiagP.COUNTER_CONSISTENCYCHECKWORKER_CREATION;
import static at.tugraz.ist.ase.eval.PerformanceEvaluator.incrementCounter;

@Slf4j
public class ConsistencyCheckWorker extends RecursiveAction {

    public static final String COUNTER_CONSISTENCY_CHECKS_IN_WORKER = "The number of consistency checks performed by workers";

    private final Set<Constraint> C;

    private final CCManager ccManager;
    private final LookupTable lookupTable;

    private final ConsistencyCheckResult ccResult;

    public ConsistencyCheckWorker(@NonNull Set<Constraint> C,
                                  @NonNull CCManager ccManager,
                                  @NonNull ConsistencyCheckResult result,
                                  @NonNull LookupTable lookupTable) {
        incrementCounter(COUNTER_CONSISTENCYCHECKWORKER_CREATION);

        this.ccManager = ccManager;
        this.lookupTable = lookupTable;
        this.ccResult = result;

        this.C = C;

        ccResult.setWorker(this);

        log.debug("{}(ConsistencyCheckWorker) Created ConsistencyCheck for [C={}]", LoggerUtils.tab(), C);
    }

    @Override
    protected void compute() {
        long threadID = Thread.currentThread().getId();

        if (ccResult.isInQueue()) {
            log.debug("{}(ConsistencyCheckWorker) Checking consistency of [CCResult={}]", LoggerUtils.tab(), ccResult);
            LoggerUtils.indent();

//            ConsistencyCheckResult alternative = lookupTable.getAlternativeSet(C);
//
            boolean consistent;
//            if (alternative != null) {
//                consistent = alternative.isConsistent(); incrementCounter(COUNTER_EXIST_ALTERNATIVE);
//                log.debug("{}(isConsistent) Using consistency form [alternative={}]", LoggerUtils.tab(), alternative.getC());
//            } else {
                try {
                    // get a free ChocoConsistencyChecker
                    ChocoConsistencyChecker checker = ccManager.getChecker();

                    if (checker == null) {
                        log.error("{}(ConsistencyCheckWorker) No free ChocoConsistencyChecker for [C={}]", LoggerUtils.tab(), C);
                        throw new RuntimeException("No free ChocoConsistencyChecker for " + C);
                    }

                    // identify the consistency of the constraint set
                    consistent = checker.isConsistent(C);
                    incrementCounter(COUNTER_CONSISTENCY_CHECKS_IN_WORKER);
                    ccManager.releaseChecker(checker); // release the checker
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
//            }

            ccResult.setConsistency(consistent, threadID);

            // cancel all unnecessary tasks
            lookupTable.cleanUpCC(ccResult.getC(), consistent);

            LoggerUtils.outdent();
            log.debug("{}(ConsistencyCheckWorker) Checked [CCResult={}]", LoggerUtils.tab(), ccResult);

//            printLookupTable("After checking the consistency", lookupTable);
        } else {
            ccResult.release();
            log.debug("{}(ConsistencyCheckWorker) ConsistencyCheckResult may be done [CCResult={}]", LoggerUtils.tab(), ccResult);
        }

        ccResult.setWorker(null);
    }
}
