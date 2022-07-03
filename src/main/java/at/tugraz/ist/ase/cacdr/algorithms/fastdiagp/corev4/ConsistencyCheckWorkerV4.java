package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.corev4;

import at.tugraz.ist.ase.cacdr.algorithms.core.CCManager;
import at.tugraz.ist.ase.cacdr.checker.ChocoConsistencyChecker;
import at.tugraz.ist.ase.common.LoggerUtils;
import at.tugraz.ist.ase.kb.core.Constraint;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.RecursiveTask;

import static at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.AbstractFastDiagP.COUNTER_CONSISTENCYCHECKWORKER_CREATION;
import static at.tugraz.ist.ase.eval.PerformanceEvaluator.incrementCounter;

@Slf4j
public class ConsistencyCheckWorkerV4 extends RecursiveTask<Boolean> {

    public static final String COUNTER_CONSISTENCY_CHECKS_IN_WORKER = "The number of consistency checks performed by workers";

    @Getter
    private Set<Constraint> C;

    private CCManager ccManager;

    @Getter @Setter
    private LookAheadNodeV4 node;

    public ConsistencyCheckWorkerV4(@NonNull Set<Constraint> C,
                                    @NonNull CCManager ccManager,
                                    @NonNull LookAheadNodeV4 node) {
        incrementCounter(COUNTER_CONSISTENCYCHECKWORKER_CREATION);

        this.ccManager = ccManager;
        this.node = node;
        this.C = C;

        node.setWorker(this);

        log.debug("{}(ConsistencyCheckWorker) Created ConsistencyCheck for [C={}]", LoggerUtils.tab(), C);
    }

    @Override
    protected Boolean compute() {
        log.debug("{}(ConsistencyCheckWorker) Checking consistency of [C={}]", LoggerUtils.tab(), C);
        LoggerUtils.indent();

        Boolean consistent = null;
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

            node.setConsistency(consistent);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            node.setWorker(null);
            this.dispose();
        }

        LoggerUtils.outdent();
        log.debug("{}(ConsistencyCheckWorker) Checked [consistency={}]", LoggerUtils.tab(), consistent);

        return consistent;
    }

    public void dispose() {
        C = null;
        ccManager = null;
        node = null;
    }
}
