package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.corev6;

import at.tugraz.ist.ase.cacdr.algorithms.core.CCManager;
import at.tugraz.ist.ase.common.LoggerUtils;
import at.tugraz.ist.ase.kb.core.Constraint;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import static at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.AbstractFastDiagP.COUNTER_LOOKAHEAD;
import static at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.AbstractFastDiagP.COUNTER_LOOKUP;
import static at.tugraz.ist.ase.cacdr.eval.CAEvaluator.COUNTER_UNION_OPERATOR;
import static at.tugraz.ist.ase.common.ConstraintUtils.split;
import static at.tugraz.ist.ase.eval.PerformanceEvaluator.incrementCounter;

@Slf4j
public class LookAheadWorkerV6 extends RecursiveAction {
    private final Set<Constraint> C;
    private final Set<Constraint> B;
    private final List<Set<Constraint>> Δ;
    private final int level;
    private final int maxLevel;

    private final LookupTableV6 lookupTable;
    private final CCManager ccManager;
    private final ForkJoinPool pool;

    public LookAheadWorkerV6(Set<Constraint> C, Set<Constraint> B, List<Set<Constraint>> Δ, int level, int maxLevel,
                             @NonNull LookupTableV6 lookupTable,
                             @NonNull CCManager ccManager,
                             @NonNull ForkJoinPool pool) {
        this.C = C;
        this.B = B;
        this.Δ = Δ;
        this.level = level;
        this.maxLevel = maxLevel;

        this.lookupTable = lookupTable;
        this.ccManager = ccManager;
        this.pool = pool;

        log.debug("{}(LookAheadWorker) Created LookAhead for [C={}, B={}, Δ={}]", LoggerUtils.tab(), C, B, Δ);
    }

    @Override
    protected void compute() {
//        Set<Constraint> BwithC = Sets.union(B, C); incrementCounter(COUNTER_UNION_OPERATOR);
//        ConsistencyCheckResultV6 result = new ConsistencyCheckResultV6(BwithC);
//        lookupTable.put(BwithC.hashCode(), result);

//        try {
//            TimeUnit.NANOSECONDS.sleep(10);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

//        System.out.println("Root: " + BwithC);

        lookAhead(C, B, Δ, level, maxLevel);
    }

    // Cl, B, newΔ, level + 1, maxLevel,
    protected void lookAhead(Set<Constraint> C, Set<Constraint> B, List<Set<Constraint>> Δ, int level, int maxLevel) {
        log.debug("{}(LookAheadWorker) LookAhead for [C={}, B={}, Δ={}]", LoggerUtils.tab(), C, B, Δ);
        LoggerUtils.indent();

        // if l < lmax
        if (level < maxLevel) {

            // AddCC(B U C)
            Set<Constraint> BwithC = SetUtils.union(B, C); incrementCounter(COUNTER_UNION_OPERATOR);

            if (!lookupTable.contains(BwithC.hashCode())) {
                ConsistencyCheckResultV6 result = new ConsistencyCheckResultV6(BwithC);
                lookupTable.putIfAbsent(BwithC.hashCode(), result);

                result = lookupTable.get(BwithC.hashCode()); incrementCounter(COUNTER_LOOKUP);

                try {
                    result.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (result.getWorker() == null) {
                    ConsistencyCheckWorkerV6 cc = new ConsistencyCheckWorkerV6(result.getC(), ccManager, result);
                    pool.execute(cc);

                    log.debug("{}(LookAheadWorker) AddCC [C={}]", LoggerUtils.tab(), result.getC());
//                    System.out.println("AddCC:" + BwithC);
                }

                result.release();
            }

            int sizeC = C.size();

            // B U C assumed inconsistent
//            if (result.isInQueue() || (result.isDone() && !result.isConsistent())) {
            if (sizeC > 1) {
                log.debug("{}(LookAheadWorker) B U C assumed inconsistent - C1.1", LoggerUtils.tab());
                // Split(C, Cl, Cr);
                Set<Constraint> Cl = new LinkedHashSet<>();
                Set<Constraint> Cr = new LinkedHashSet<>();
                split(C, Cl, Cr);

                // LookAhead(Cl, B, Cr U Δ, l + 1);
                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
                newΔ.add(0, Cr);

                lookAhead(Cl, B, newΔ, level + 1, maxLevel); incrementCounter(COUNTER_LOOKAHEAD);

            } else if (sizeC == 1 && !Δ.isEmpty() && Δ.get(0).size() == 1) {
                log.debug("{}(LookAheadWorker) B U C assumed inconsistent - C1.2", LoggerUtils.tab());

                Set<Constraint> Δ1 = Δ.get(0);
                // LookAhead(Δ1, B, Δ \ {Δ1}, l + 1);
                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
                newΔ.remove(0);

                lookAhead(Δ1, B, newΔ, level + 1, maxLevel); incrementCounter(COUNTER_LOOKAHEAD);

            } else if (sizeC == 1 && !Δ.isEmpty() && Δ.get(0).size() > 1) {
                log.debug("{}(LookAheadWorker) B U C assumed inconsistent - C1.3", LoggerUtils.tab());

                Set<Constraint> Δ1 = Δ.get(0);
                // Split(Δ1, Δ1l, Δ1r);
                Set<Constraint> Δ1l = new LinkedHashSet<>();
                Set<Constraint> Δ1r = new LinkedHashSet<>();
                split(Δ1, Δ1l, Δ1r);

                // LookAhead(Δ1l, B, Δ1r U (Δ \ {Δ1})), l + 1);
                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
                newΔ.remove(0);
                newΔ.add(0, Δ1r);

                lookAhead(Δ1l, B, newΔ, level + 1, maxLevel); incrementCounter(COUNTER_LOOKAHEAD);
            }

            // B U C assumed consistent
//            if (result.isInQueue() || (result.isDone() && result.isConsistent())) {
            if (!Δ.isEmpty() && Δ.size() > 1 && Δ.get(0).size() == 1 && lookupTable.contains(SetUtils.union(BwithC, Δ.get(0)).hashCode())) {
                log.debug("{}(LookAheadWorker) B U C assumed consistent - C2.3", LoggerUtils.tab());

                Set<Constraint> Δ2 = Δ.get(1);
                // Split(Δ2, Δ2l, Δ2r);
                Set<Constraint> Δ2l = new LinkedHashSet<>();
                Set<Constraint> Δ2r = new LinkedHashSet<>();
                split(Δ2, Δ2l, Δ2r); // split uses addAll

                // LookAhead(Δ2l, B U C, Δ2r U (Δ \ {Δ1, Δ2})), l + 1);
                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
                newΔ.remove(0);
                newΔ.remove(0);
                newΔ.add(0, Δ2r);

                lookAhead(Δ2l, BwithC, newΔ, level + 1, maxLevel); incrementCounter(COUNTER_LOOKAHEAD);
            } else if (!Δ.isEmpty() && Δ.get(0).size() == 1) {
                log.debug("{}(LookAheadWorker) B U C assumed consistent - C2.2", LoggerUtils.tab());

                Set<Constraint> Δ1 = Δ.get(0);
                // LookAhead(Δ1, B U C, Φ, l + 1);
                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
                newΔ.remove(0);

                lookAhead(Δ1, BwithC, newΔ, level + 1, maxLevel); incrementCounter(COUNTER_LOOKAHEAD);

            } else if (!Δ.isEmpty() && Δ.get(0).size() > 1) {
                log.debug("{}(LookAheadWorker) B U C assumed consistent - C2.1", LoggerUtils.tab());

                Set<Constraint> Δ1 = Δ.get(0);
                // Split(Δ1, Δ1l, Δ1r);
                Set<Constraint> Δ1l = new LinkedHashSet<>();
                Set<Constraint> Δ1r = new LinkedHashSet<>();
                split(Δ1, Δ1l, Δ1r);

                // LookAhead(Δ1l, B U C, Δ1r U (Δ \ {Δ1})), l + 1);
                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
                newΔ.remove(0);
                newΔ.add(0, Δ1r);

                lookAhead(Δ1l, BwithC, newΔ, level + 1, maxLevel); incrementCounter(COUNTER_LOOKAHEAD);
            }

        } else {
            log.debug("{}(LookAheadWorker) maxLevel [C={}, B={}, Δ={}]", LoggerUtils.tab(), C, B, Δ);
        }

        LoggerUtils.outdent();
    }
}
