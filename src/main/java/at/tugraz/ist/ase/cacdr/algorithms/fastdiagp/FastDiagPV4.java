package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp;

import at.tugraz.ist.ase.cacdr.algorithms.core.CCManager;
import at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.corev4.LookAheadWorkerV4;
import at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.corev4.LookupTableV4;
import at.tugraz.ist.ase.cdrmodel.CDRModel;
import at.tugraz.ist.ase.common.LoggerUtils;
import at.tugraz.ist.ase.kb.core.Constraint;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static at.tugraz.ist.ase.cacdr.algorithms.core.Utils.*;
import static at.tugraz.ist.ase.cacdr.eval.CAEvaluator.*;
import static at.tugraz.ist.ase.common.ConstraintUtils.split;

/**
 * Implementation of FastDiagP algorithm.
 *
 * Variant of V3, but doesn't use ConsistencyCheckResultV3
 */
@Slf4j
public class FastDiagPV4 extends AbstractFastDiagP {

    // for evaluation
    public static final String TIMER_FASTDIAGPV4 = "Timer for FastDiagP V4";
    public static final String COUNTER_FASTDIAGPV4_CALLS = "The number of FastDiagP V4 calls";

    protected CCManager ccManager;

    protected LookupTableV4 lookupTable = new LookupTableV4();

//    protected ConcurrentMap<Set<Constraint>, LookAheadNodeV3> lookupNode = new ConcurrentHashMap<>();

    public FastDiagPV4(@NonNull CDRModel diagModel, int lookAheadPoolSize, int checkerPoolSize, int maxLevel) {
        super(diagModel,  lookAheadPoolSize, checkerPoolSize, maxLevel);

        // create a list of ChocoConsistencyCheckers
        try {
            ccManager = new CCManager(checkerPoolSize, cdrModel);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void dispose() {
        super.dispose();
        this.ccManager = null;
    }

    /**
     * This function will activate FastDiag algorithm if there exists at least one constraint,
     * which induces an inconsistency in B. Otherwise, it returns an empty set.
     *
     * // Func FastDiag(C, B) : ??
     * // if isEmpty(C) or consistent(B U C) return ??
     * // else return C \ FD(C, B, ??)
     *
     * @param C a consideration set of constraints. Need to inverse the order of the possibly faulty constraint set.
     * @param B a background knowledge
     * @return a diagnosis or an empty set
     */
    public Set<Constraint> findDiagnosis(@NonNull Set<Constraint> C, @NonNull Set<Constraint> B) {
        log.debug("{}(findDiagnosis) Identifying diagnosis for [C={}, B={}] >>>", LoggerUtils.tab(), C, B);
        log.debug("{}(findDiagnosis) [poolSize={}, maxLevel={}]", LoggerUtils.tab(), checkerPoolSize, maxLevel);
        LoggerUtils.indent();

        Set<Constraint> diag = null;

        try {
            lookAheadPool = new ForkJoinPool(lookAheadPoolSize);
            checkerPool = new ForkJoinPool(checkerPoolSize);

            System.out.println("lookAheadPoolSize = " + lookAheadPoolSize);
            System.out.println("checkerPoolSize = " + checkerPoolSize);

//            Set<Constraint> BwithC = Sets.union(B, C);

            // if isEmpty(C) or consistent(B U C) return ??
            if (C.isEmpty() || isConsistent(C, B, Collections.emptyList())) {// checker.isConsistent(BwithC)) {

                LoggerUtils.outdent();
                log.debug("{}(findDiagnosis) <<< No diagnosis found", LoggerUtils.tab());

                diag = Collections.emptySet();
            } else { // else return C \ FD(C, B, ??)
                incrementCounter(COUNTER_FASTDIAGPV4_CALLS);
                start(TIMER_FASTDIAGPV4);
                Set<Constraint> mss = fd(Collections.emptySet(), C, B);
                stop(TIMER_FASTDIAGPV4);

                incrementCounter(COUNTER_DIFFERENT_OPERATOR);
                diag = Sets.difference(C, mss);

                LoggerUtils.outdent();
                log.debug("{}(findDiagnosis) <<< Found diagnosis [diag={}]", LoggerUtils.tab(), diag);
            }
        } catch (Exception e) {
            log.error("{}(findDiagnosis) <<< Exception occurred - {}", LoggerUtils.tab(), e.getMessage());
        } finally {

//            printLookupTable("DONE diagnosis identification", lookupTable);
            try {
                lookupTable.print("DONE diagnosis identification");
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            lookupTable.clear();
            shutdownAndAwaitTermination(lookAheadPool, "lookAheadPool");
            shutdownAndAwaitTermination(checkerPool, "checkerPool");
        }

        return diag;
    }

    /**
     * The implementation of MSS-based FastDiag algorithm.
     * The algorithm determines a maximal satisfiable subset MSS (??) of C U B.
     *
     * // Func FD(??, C = {c1..cn}, B) : MSS
     * // if ?? != ?? and consistent(B U C) return C;
     * // if singleton(C) return ??;
     * // k = n/2;
     * // C1 = {c1..ck}; C2 = {ck+1..cn};
     * // ??2 = FD(C2, C1, B);
     * // ??1 = FD(C1 - ??2, C2, B U ??2);
     * // return ??1 ??? ??2;
     *
     * @param ?? check to skip redundant consistency checks
     * @param C a consideration set of constraints
     * @param B a background knowledge
     * @return a maximal satisfiable subset MSS of C U B.
     */
    private Set<Constraint> fd(Set<Constraint> ??, Set<Constraint> C, Set<Constraint> B) {
        log.debug("{}(fd) FD [??={}, C={}, B={}] >>>", LoggerUtils.tab(), ??, C, B);
        LoggerUtils.indent();

        // if ?? != ?? and consistent(B U C) return C;
        if (!??.isEmpty()) {
            if (isConsistent(C, B, Collections.singletonList(??))) {
                LoggerUtils.outdent();
                log.debug("{}(fd) <<< return [{}]", LoggerUtils.tab(), C);

                return C;
            }
        }

        // if singleton(C) return ??;
        int n = C.size();
        if (n == 1) {
            LoggerUtils.outdent();
            log.debug("{}(fd) <<< return ??", LoggerUtils.tab());

            return Collections.emptySet();
        }

        // C1 = {c1..ck}; C2 = {ck+1..cn};
        Set<Constraint> C1 = new LinkedHashSet<>();
        Set<Constraint> C2 = new LinkedHashSet<>();
        split(C, C1, C2);
        log.trace("{}(fd) Split C into [C1={}, C2={}]", LoggerUtils.tab(), C1, C2);

        // ??1 = FD(C2, C1, B);
        incrementCounter(COUNTER_LEFT_BRANCH_CALLS);
        incrementCounter(COUNTER_FASTDIAGPV4_CALLS);
        Set<Constraint> ??1 = fd(C2, C1, B);

        // ??2 = FD(C1 - ??1, C2, B U ??1);
        Set<Constraint> Bwith??1 = Sets.union(B, ??1); incrementCounter(COUNTER_UNION_OPERATOR);
        Set<Constraint> C1without??1 = Sets.difference(C1, ??1); incrementCounter(COUNTER_DIFFERENT_OPERATOR);
        incrementCounter(COUNTER_RIGHT_BRANCH_CALLS);
        incrementCounter(COUNTER_FASTDIAGPV4_CALLS);
        Set<Constraint> ??2 = fd(C1without??1, C2, Bwith??1);

        LoggerUtils.outdent();
        log.debug("{}(fd) <<< return [??1={} ??? ??2={}]", LoggerUtils.tab(), ??1, ??2);

        // return ??1 ??? ??2;
        incrementCounter(COUNTER_UNION_OPERATOR);
        return Sets.union(??1, ??2);
    }

    private boolean firstTime = true;
    @SneakyThrows
    private boolean isConsistent(Set<Constraint> C, Set<Constraint> B, List<Set<Constraint>> ??) {
        log.debug("{}(isConsistent) Checking the consistency of [C={}, B={}, ??={}] >>>", LoggerUtils.tab(), C, B, ??);
        LoggerUtils.indent();

        Set<Constraint> BwithC = Sets.union(B, C); incrementCounter(COUNTER_UNION_OPERATOR);

        Boolean consistent;
        if (!lookupTable.contains(BwithC.hashCode())) {
            log.debug("{}(isConsistent) Not found ConsistencyCheckResultV3 for [BwithC={}]", LoggerUtils.tab(), BwithC);
            incrementCounter(COUNTER_NOT_EXISTCC);

            int maxLevel = this.maxLevel;
            if (this.maxLevel == 1 && firstTime) {
                maxLevel += 1;
                firstTime = false;
            }
            LookAheadWorkerV4 lookAheadWorker = new LookAheadWorkerV4(C, B, ??, maxLevel,
                    lookupTable, ccManager, checkerPool);
            lookAheadPool.execute(lookAheadWorker);
            incrementCounter(COUNTER_LOOKAHEAD);

            log.debug("{}(isConsistent) Checking consistency for [BwithC={}]", LoggerUtils.tab(), BwithC);
            consistent = checker.isConsistent(BwithC);
            incrementCounter(COUNTER_CONSISTENCY_CHECKS);

//            ConsistencyCheckResultV3 result = new ConsistencyCheckResultV3(BwithC);
//            result.setConsistency(consistent, Thread.currentThread().getId());
//            lookupTable.put(BwithC, result);
        } else {
            incrementCounter(COUNTER_EXISTCC);
            log.debug("{}(isConsistent) Found a ConsistencyCheckResultV3 for [BwithC={}]", LoggerUtils.tab(), BwithC);

            consistent = lookupTable.getConsistency(BwithC.hashCode());

            if (consistent == null) {
                log.debug("{}(isConsistent) Checking consistency for [BwithC={}]", LoggerUtils.tab(), BwithC);
                consistent = checker.isConsistent(BwithC);
                incrementCounter(COUNTER_CONSISTENCY_CHECKS);
            }
        }

//        try {
//            TimeUnit.MILLISECONDS.sleep(1);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        LoggerUtils.outdent();
        log.debug("{}<<< (isConsistent) Checked [result={}]", LoggerUtils.tab(), consistent);

        return consistent;
    }
}
