package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp;

import at.tugraz.ist.ase.cacdr.algorithms.core.CCManager;
import at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.corev6.ConsistencyCheckResultV6;
import at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.corev6.LookAheadWorkerV6;
import at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.corev6.LookupTableV6;
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
import java.util.concurrent.ForkJoinPool;

import static at.tugraz.ist.ase.cacdr.algorithms.core.Utils.*;
import static at.tugraz.ist.ase.cacdr.eval.CAEvaluator.*;
import static at.tugraz.ist.ase.common.ConstraintUtils.split;

/**
 * Implementation of FastDiagP algorithm.
 *
 * A Variant of FastDiagPV2
 * BFS-like algorithm
 *
 */
@Slf4j
public class FastDiagPV6 extends AbstractFastDiagP {

    // for evaluation
    public static final String TIMER_FASTDIAGPV6 = "Timer for FastDiagP V6";
    public static final String COUNTER_FASTDIAGPV6_CALLS = "The number of FastDiagP V6 calls";

    protected CCManager ccManager;

    protected LookupTableV6 lookupTable = new LookupTableV6();

    public FastDiagPV6(@NonNull CDRModel diagModel, int lookAheadPoolSize, int checkerPoolSize, int maxLevel) {
        super(diagModel, lookAheadPoolSize, checkerPoolSize, maxLevel);

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
     * // Func FastDiag(C, B) : Δ
     * // if isEmpty(C) or consistent(B U C) return Φ
     * // else return C \ FD(C, B, Φ)
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

            // if isEmpty(C) or consistent(B U C) return Φ
            if (C.isEmpty() || isConsistent(C, B, Collections.emptyList())) {// checker.isConsistent(BwithC)) {

                LoggerUtils.outdent();
                log.debug("{}(findDiagnosis) <<< No diagnosis found", LoggerUtils.tab());

                diag = Collections.emptySet();
            } else { // else return C \ FD(C, B, Φ)
                incrementCounter(COUNTER_FASTDIAGPV6_CALLS);
                start(TIMER_FASTDIAGPV6);
                Set<Constraint> mss = fd(Collections.emptySet(), C, B);
                stop(TIMER_FASTDIAGPV6);

                incrementCounter(COUNTER_DIFFERENT_OPERATOR);
                diag = Sets.difference(C, mss);

                LoggerUtils.outdent();
                log.debug("{}(findDiagnosis) <<< Found diagnosis [diag={}]", LoggerUtils.tab(), diag);
            }
        } catch (Exception e) {
            log.error("{}(findDiagnosis) <<< Exception occurred - {}", LoggerUtils.tab(), e.getMessage());
        } finally {

//            printLookupTable("DONE diagnosis identification", lookupTable);
            lookupTable.print("DONE diagnosis identification");

            lookupTable.clear();
            shutdownAndAwaitTermination(lookAheadPool, "lookAheadPool");
            shutdownAndAwaitTermination(checkerPool, "checkerPool");
        }

        return diag;
    }

    /**
     * The implementation of MSS-based FastDiag algorithm.
     * The algorithm determines a maximal satisfiable subset MSS (Γ) of C U B.
     *
     * // Func FD(Δ, C = {c1..cn}, B) : MSS
     * // if Δ != Φ and consistent(B U C) return C;
     * // if singleton(C) return Φ;
     * // k = n/2;
     * // C1 = {c1..ck}; C2 = {ck+1..cn};
     * // Δ2 = FD(C2, C1, B);
     * // Δ1 = FD(C1 - Δ2, C2, B U Δ2);
     * // return Δ1 ∪ Δ2;
     *
     * @param Δ check to skip redundant consistency checks
     * @param C a consideration set of constraints
     * @param B a background knowledge
     * @return a maximal satisfiable subset MSS of C U B.
     */
    private Set<Constraint> fd(Set<Constraint> Δ, Set<Constraint> C, Set<Constraint> B) {
        log.debug("{}(fd) FD [Δ={}, C={}, B={}] >>>", LoggerUtils.tab(), Δ, C, B);
        LoggerUtils.indent();

        // if Δ != Φ and consistent(B U C) return C;
        if (!Δ.isEmpty()) {
            if (isConsistent(C, B, Collections.singletonList(Δ))) {
                LoggerUtils.outdent();
                log.debug("{}(fd) <<< return [{}]", LoggerUtils.tab(), C);

                return C;
            }
        }

        // if singleton(C) return Φ;
        int n = C.size();
        if (n == 1) {
            LoggerUtils.outdent();
            log.debug("{}(fd) <<< return Φ", LoggerUtils.tab());

            return Collections.emptySet();
        }

        // C1 = {c1..ck}; C2 = {ck+1..cn};
        Set<Constraint> C1 = new LinkedHashSet<>();
        Set<Constraint> C2 = new LinkedHashSet<>();
        split(C, C1, C2);
        log.trace("{}(fd) Split C into [C1={}, C2={}]", LoggerUtils.tab(), C1, C2);

        // Δ1 = FD(C2, C1, B);
        incrementCounter(COUNTER_LEFT_BRANCH_CALLS);
        incrementCounter(COUNTER_FASTDIAGPV6_CALLS);
        Set<Constraint> Δ1 = fd(C2, C1, B);

        // Δ2 = FD(C1 - Δ1, C2, B U Δ1);
        Set<Constraint> BwithΔ1 = Sets.union(B, Δ1); incrementCounter(COUNTER_UNION_OPERATOR);
        Set<Constraint> C1withoutΔ1 = Sets.difference(C1, Δ1); incrementCounter(COUNTER_DIFFERENT_OPERATOR);
        incrementCounter(COUNTER_RIGHT_BRANCH_CALLS);
        incrementCounter(COUNTER_FASTDIAGPV6_CALLS);
        Set<Constraint> Δ2 = fd(C1withoutΔ1, C2, BwithΔ1);

        LoggerUtils.outdent();
        log.debug("{}(fd) <<< return [Δ1={} ∪ Δ2={}]", LoggerUtils.tab(), Δ1, Δ2);

        // return Δ1 ∪ Δ2;
        incrementCounter(COUNTER_UNION_OPERATOR);
        return Sets.union(Δ1, Δ2);
    }

    boolean firstTime = true;
    @SneakyThrows
    private boolean isConsistent(Set<Constraint> C, Set<Constraint> B, List<Set<Constraint>> Δ) {
        log.debug("{}(isConsistent) Checking the consistency of [C={}, B={}, Δ={}] >>>", LoggerUtils.tab(), C, B, Δ);
        LoggerUtils.indent();

        Set<Constraint> BwithC = Sets.union(B, C); incrementCounter(COUNTER_UNION_OPERATOR);

        Boolean consistent;
        // check the existence of the consistency check for B U C
        if (!lookupTable.contains(BwithC.hashCode())) {
            log.debug("{}(isConsistent) Not found ConsistencyCheckResultV6 for [BwithC={}]", LoggerUtils.tab(), BwithC);
            incrementCounter(COUNTER_NOT_EXISTCC);

            int maxLevel = this.maxLevel;
            if (this.maxLevel == 1 && firstTime) {
                maxLevel += 1; // to generate more consistency checks
                firstTime = false;
            }

            // Add to LookupTable before running the lookAhead
            ConsistencyCheckResultV6 result = new ConsistencyCheckResultV6(BwithC);
            lookupTable.put(BwithC.hashCode(), result);
//            System.out.println("Root: " + BwithC);

            // Run the lookAhead
            LookAheadWorkerV6 lookAheadWorker = new LookAheadWorkerV6(C, B, Δ, 0, maxLevel,
                    lookupTable, ccManager, checkerPool);
            lookAheadPool.execute(lookAheadWorker); // run the lookAhead in the lookAheadPool
            incrementCounter(COUNTER_LOOKAHEAD);

            log.debug("{}(isConsistent) Checking consistency for [BwithC={}]", LoggerUtils.tab(), BwithC);
            consistent = checker.isConsistent(BwithC); // run the checker in the main thread
            incrementCounter(COUNTER_CONSISTENCY_CHECKS);

//            result.setConsistency(consistent, Thread.currentThread().getId());
        } else {
            incrementCounter(COUNTER_EXISTCC);
            log.debug("{}(isConsistent) Found a ConsistencyCheckResultV3 for [BwithC={}]", LoggerUtils.tab(), BwithC);

            // Get the result of the consistency check from the lookupTable
            consistent = lookupTable.getConsistency(BwithC.hashCode());

            if (consistent == null) { // if the result is not yet available
                log.debug("{}(isConsistent) Checking consistency for [BwithC={}]", LoggerUtils.tab(), BwithC);
                consistent = checker.isConsistent(BwithC); // run the checker in the main thread
                incrementCounter(COUNTER_CONSISTENCY_CHECKS);
            }
        }

        LoggerUtils.outdent();
        log.debug("{}<<< (isConsistent) Checked [result={}]", LoggerUtils.tab(), consistent);

        return consistent;
    }
}
