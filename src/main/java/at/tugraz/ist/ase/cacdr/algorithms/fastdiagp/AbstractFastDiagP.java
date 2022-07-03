package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp;

import at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.core.LookupTable;
import at.tugraz.ist.ase.cacdr.checker.ChocoConsistencyChecker;
import at.tugraz.ist.ase.cdrmodel.CDRModel;
import at.tugraz.ist.ase.kb.core.Constraint;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

@Slf4j
public abstract class AbstractFastDiagP {
    public static final String COUNTER_LOOKUP = "The number of lookups";
    public static final String COUNTER_LOOKUP_ALTERNATIVE = "The number of alternative lookups";
    public static final String COUNTER_EXISTCC = "The number of existing CC";
    public static final String COUNTER_EXIST_ALTERNATIVE = "The number of existing alternative";
    public static final String COUNTER_EXISTCC_NOTSTARTED = "The number of existing CC but not started yet";
    public static final String COUNTER_EXISTCC_NOTDONE = "The number of existing CC but not done yet";
    public static final String COUNTER_NOT_EXISTCC = "The number of not existing CC";
    public static final String COUNTER_LOOKAHEAD = "The number of lookahead";
    public static final String COUNTER_CONSISTENCYCHECKWORKER_CREATION = "The number of consistency check worker creation";
    public static final String COUNTER_MATCH_LOOKAHEAD = "The number of match lookahead";

    protected ChocoConsistencyChecker checker;
    protected CDRModel cdrModel;

    protected int maxLevel = 1;

    protected LookupTable lookupTable = new LookupTable();
    // shouldn't use hashcode here, because it is not unique for this Collection<Constraint>
    protected final ConcurrentLinkedQueue<Collection<Constraint>> lookAheadTable = new ConcurrentLinkedQueue<>();

    protected ForkJoinPool lookAheadPool;
    protected ForkJoinPool checkerPool;
    protected int lookAheadPoolSize = 1;
    protected int checkerPoolSize = 4;

    public AbstractFastDiagP(@NonNull CDRModel diagModel, int lookAheadPoolSize, int checkerPoolSize, int maxLevel) {
        cdrModel = diagModel;
        this.checker = new ChocoConsistencyChecker(cdrModel);
        this.lookAheadPoolSize = Math.max(lookAheadPoolSize, this.lookAheadPoolSize);
        this.checkerPoolSize = Math.max(checkerPoolSize, this.checkerPoolSize);
        this.maxLevel = Math.max(maxLevel, this.maxLevel);
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
    abstract Set<Constraint> findDiagnosis(@NonNull Set<Constraint> C, @NonNull Set<Constraint> B);

    protected void dispose() {
        if (!lookAheadPool.isShutdown()) {
            lookAheadPool.shutdown();
            checkerPool.shutdown();
        }
        lookupTable.clear();

        this.checker.dispose();
        this.checker = null;

        this.cdrModel = null;
    }
}
