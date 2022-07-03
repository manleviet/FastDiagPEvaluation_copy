package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.corev4;

import at.tugraz.ist.ase.cacdr.algorithms.core.CCManager;
import at.tugraz.ist.ase.common.LoggerUtils;
import at.tugraz.ist.ase.kb.core.Constraint;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import static at.tugraz.ist.ase.cacdr.eval.CAEvaluator.COUNTER_UNION_OPERATOR;
import static at.tugraz.ist.ase.common.ConstraintUtils.split;
import static at.tugraz.ist.ase.eval.PerformanceEvaluator.incrementCounter;

@Slf4j
public class LookAheadWorkerV4 extends RecursiveAction {
    private final Set<Constraint> CC;

    private final Set<Constraint> C;
    private final Set<Constraint> B;
    private final List<Set<Constraint>> Δ;
    private final int maxLevel;

    private final LookupTableV4 lookupTable;
    private final CCManager ccManager;
    private final ForkJoinPool pool;

    @Getter
    protected LookAheadNodeV4 root = null;
    protected ConcurrentLinkedQueue<LookAheadNodeV4> openNodes = new ConcurrentLinkedQueue<>();

    public LookAheadWorkerV4(Set<Constraint> C, Set<Constraint> B, List<Set<Constraint>> Δ, int maxLevel,
                             @NonNull LookupTableV4 lookupTable,
                             @NonNull CCManager ccManager,
                             @NonNull ForkJoinPool pool) {
        this.C = C;
        this.B = B;
        this.Δ = Δ;
        this.maxLevel = maxLevel;

        this.CC = Sets.union(B, C);

        this.lookupTable = lookupTable;
        this.ccManager = ccManager;
        this.pool = pool;

        log.debug("{}(LookAheadWorker) Created LookAhead for [C={}, B={}, Δ={}]", LoggerUtils.tab(), C, B, Δ);
    }

    @Override
    protected void compute() {
//        lookAhead(C, B, Δ, level, maxLevel);
        // create the root node
        root = LookAheadNodeV4.createRoot(C, B, Δ); //incrementCounter(COUNTER_CONSTRUCTED_NODES);
        lookupTable.put(root.getCC(), root);
//        try {
//            TimeUnit.NANOSECONDS.sleep(10);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println(Thread.currentThread().getId() + " - root: " + root.getCC());

        openNodes.add(root);

        int count = 0;
        while (hasNodesToExpand()) {
            LookAheadNodeV4 node = getNextNode();

            // AddCC(B U C)
//            Set<Constraint> BwithC = Sets.union(node.getB(), node.getC()); incrementCounter(COUNTER_UNION_OPERATOR);

            if (!node.isRoot() && node.getArcLabel() // only take into account assumption of B U C consistent
                    && (node.getCC().size() >= (CC.size() - 2))
                    && !lookupTable.contains(node.getCC().hashCode())
                    && node.getLevel() >= maxLevel
                    && count < pool.getParallelism()) { //
                lookupTable.putIfAbsent(node.getCC(), node);

                node = lookupTable.get(node.getCC().hashCode());

                try {
                    node.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (node.getWorker() == null) {
                    ConsistencyCheckWorkerV4 cc = new ConsistencyCheckWorkerV4(node.getCC(), ccManager, node);
                    count++;

                    pool.execute(cc);

//                ConsistencyCheckResultV3 result = new ConsistencyCheckResultV3(BwithC);
//                result.setWorker(cc);
//                node.setWorker(cc);

//                    System.out.println(Thread.currentThread().getId() + " - AddCC: " + node.getCC());

                    log.debug("{}(LookAheadWorker) AddCC [C={}]", LoggerUtils.tab(), node.getCC());
                }

                node.release();
            }

            if (node.getLevel() <= maxLevel) {
//                System.out.println(Thread.currentThread().getId() + " - expand: " + node.getLevel());
                // Expand(node)
                log.debug("{}(LookAheadTree-createNodes) Generating the children nodes of [node={}]", LoggerUtils.tab(), node);
                LoggerUtils.indent();

                // create the children nodes
                List<LookAheadNodeV4> children = lookAhead(node, node.getC(), node.getB(), node.getΔ());

                children.forEach(child -> {
                    openNodes.add(child);
                    log.debug("{}(LookAheadTree-createNodes) Added to openNodes [node={}]", LoggerUtils.tab(), child);
                });

//                System.out.println(Thread.currentThread().getId() + " - children: " + children.size());

                LoggerUtils.outdent();
            }
        }
//        System.out.println("Created " + count + " CC workers");
    }

    // Cl, B, newΔ, level + 1, maxLevel,
    protected List<LookAheadNodeV4> lookAhead(LookAheadNodeV4 parent, Set<Constraint> C, Set<Constraint> B, List<Set<Constraint>> Δ) {
        log.debug("{}(LookAheadWorker) LookAhead for [C={}, B={}, Δ={}]", LoggerUtils.tab(), C, B, Δ);
        LoggerUtils.indent();

        List<LookAheadNodeV4> children = new LinkedList<>();

//        Set<Constraint> BwithC = Sets.union(B, C); incrementCounter(COUNTER_UNION_OPERATOR);
        int sizeC = C.size();



        // B U C assumed consistent
//            if (result.isInQueue() || (result.isDone() && result.isConsistent())) {
        if (!Δ.isEmpty() && Δ.size() > 1 && Δ.get(0).size() == 1 && lookupTable.contains(Sets.union(parent.getCC(), Δ.get(0)).hashCode())) {
            log.debug("{}(LookAheadWorker) B U C assumed consistent - C2.3", LoggerUtils.tab());

            Set<Constraint> Δ2 = Δ.get(1);
            // Split(Δ2, Δ2l, Δ2r);
            Set<Constraint> Δ2l = new LinkedHashSet<>();
            Set<Constraint> Δ2r = new LinkedHashSet<>();
            split(Δ2, Δ2l, Δ2r); // split uses addAll

            Set<Constraint> CC = Sets.union(parent.getCC(), Δ2l); incrementCounter(COUNTER_UNION_OPERATOR);
            if (CC.size() != root.getCC().size() || !CC.equals(root.getCC())) {
                // LookAhead(Δ2l, B U C, Δ2r U (Δ \ {Δ1, Δ2})), l + 1);
                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
                newΔ.remove(0);
                newΔ.remove(0);
                newΔ.add(0, Δ2r);

                LookAheadNodeV4 node = new LookAheadNodeV4(parent, Δ2l, parent.getCC(), newΔ, true);
                children.add(node);
//                lookupNode.put(CC, node);
//                incrementCounter(COUNTER_CONSTRUCTED_NODES);

//                    System.out.println("C2.3:" + node.getCC());
            }
        } else if (!Δ.isEmpty() && Δ.get(0).size() == 1) {
            log.debug("{}(LookAheadWorker) B U C assumed consistent - C2.2", LoggerUtils.tab());

            Set<Constraint> Δ1 = Δ.get(0);
            Set<Constraint> CC = Sets.union(parent.getCC(), Δ1); incrementCounter(COUNTER_UNION_OPERATOR);
            if (CC.size() != root.getCC().size() || !CC.equals(root.getCC())) {
                // LookAhead(Δ1, B U C, Φ, l + 1);
                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
                newΔ.remove(0);

                LookAheadNodeV4 node = new LookAheadNodeV4(parent, Δ1, parent.getCC(), newΔ, true);
                children.add(node);
//                lookupNode.put(CC, node);
//                incrementCounter(COUNTER_CONSTRUCTED_NODES);

//                    System.out.println("C2.2:" + node.getCC());
            }

        } else if (!Δ.isEmpty() && Δ.get(0).size() > 1) {
            log.debug("{}(LookAheadWorker) B U C assumed consistent - C2.1", LoggerUtils.tab());

            Set<Constraint> Δ1 = Δ.get(0);
            // Split(Δ1, Δ1l, Δ1r);
            Set<Constraint> Δ1l = new LinkedHashSet<>();
            Set<Constraint> Δ1r = new LinkedHashSet<>();
            split(Δ1, Δ1l, Δ1r);

            Set<Constraint> CC = Sets.union(parent.getCC(), Δ1l); incrementCounter(COUNTER_UNION_OPERATOR);
            if (CC.size() != root.getCC().size() || !CC.equals(root.getCC())) {
                // LookAhead(Δ1l, B U C, Δ1r U (Δ \ {Δ1})), l + 1);
                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
                newΔ.remove(0);
                newΔ.add(0, Δ1r);

                LookAheadNodeV4 node = new LookAheadNodeV4(parent, Δ1l, parent.getCC(), newΔ, true);
                children.add(node);
//                lookupNode.put(CC, node);
//                incrementCounter(COUNTER_CONSTRUCTED_NODES);

//                    System.out.println("C2.1:" + node.getCC());
            }
        }

        // B U C assumed inconsistent
        if (sizeC > 1) {
            log.debug("{}(LookAheadWorker) B U C assumed inconsistent - C1.1", LoggerUtils.tab());
            // Split(C, Cl, Cr);
            Set<Constraint> Cl = new LinkedHashSet<>();
            Set<Constraint> Cr = new LinkedHashSet<>();
            split(C, Cl, Cr);

            Set<Constraint> CC = Sets.union(B, Cl); incrementCounter(COUNTER_UNION_OPERATOR);
            if (CC.size() != root.getCC().size() || !CC.equals(root.getCC())) {
                // LookAhead(Cl, B, Cr U Δ, l + 1);
                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
                newΔ.add(0, Cr);

                LookAheadNodeV4 node = new LookAheadNodeV4(parent, Cl, B, newΔ, false);
                children.add(node);
//                lookupNode.put(CC, node);
//                incrementCounter(COUNTER_CONSTRUCTED_NODES);

//                    System.out.println("C1.1:" + node.getCC());
            }

        } else if (sizeC == 1 && !Δ.isEmpty() && Δ.get(0).size() == 1) {
            log.debug("{}(LookAheadWorker) B U C assumed inconsistent - C1.2", LoggerUtils.tab());

            Set<Constraint> Δ1 = Δ.get(0);
            Set<Constraint> CC = Sets.union(B, Δ1); incrementCounter(COUNTER_UNION_OPERATOR);
            if (CC.size() != root.getCC().size() || !CC.equals(root.getCC())) {
                // LookAhead(Δ1, B, Δ \ {Δ1}, l + 1);
                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
                newΔ.remove(0);

                LookAheadNodeV4 node = new LookAheadNodeV4(parent, Δ1, B, newΔ, false);
                children.add(node);
//                lookupNode.put(CC, node);
//                incrementCounter(COUNTER_CONSTRUCTED_NODES);

//                    System.out.println("C1.2:" + node.getCC());
            }

        } else if (sizeC == 1 && !Δ.isEmpty() && Δ.get(0).size() > 1) {
            log.debug("{}(LookAheadWorker) B U C assumed inconsistent - C1.3", LoggerUtils.tab());

            Set<Constraint> Δ1 = Δ.get(0);
            // Split(Δ1, Δ1l, Δ1r);
            Set<Constraint> Δ1l = new LinkedHashSet<>();
            Set<Constraint> Δ1r = new LinkedHashSet<>();
            split(Δ1, Δ1l, Δ1r);

            Set<Constraint> CC = Sets.union(B, Δ1l); incrementCounter(COUNTER_UNION_OPERATOR);
            if (CC.size() != root.getCC().size() || !CC.equals(root.getCC())) {
                // LookAhead(Δ1l, B, Δ1r U (Δ \ {Δ1})), l + 1);
                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
                newΔ.remove(0);
                newΔ.add(0, Δ1r);

                LookAheadNodeV4 node = new LookAheadNodeV4(parent, Δ1l, B, newΔ, false);
                children.add(node);
//                lookupNode.put(CC, node);
//                incrementCounter(COUNTER_CONSTRUCTED_NODES);

//                    System.out.println("C1.3:" + node.getCC());
            }
        }

        LoggerUtils.outdent();
        return children;
    }

//    protected List<LookAheadNodeV4> lookAhead(LookAheadNodeV4 parent, Set<Constraint> C, Set<Constraint> B, List<Set<Constraint>> Δ) {
//        log.debug("{}(LookAheadWorker) LookAhead for [C={}, B={}, Δ={}]", LoggerUtils.tab(), C, B, Δ);
//        LoggerUtils.indent();
//
//        List<LookAheadNodeV4> children = new LinkedList<>();
//
////        Set<Constraint> BwithC = Sets.union(B, C); incrementCounter(COUNTER_UNION_OPERATOR);
//        int sizeC = C.size();
//
//
//
//        // B U C assumed consistent
////            if (result.isInQueue() || (result.isDone() && result.isConsistent())) {
//        if (!Δ.isEmpty() && Δ.size() > 1 && Δ.get(0).size() == 1 && lookupTable.contains(Sets.union(parent.getCC(), Δ.get(0)).hashCode())) {
//            log.debug("{}(LookAheadWorker) B U C assumed consistent - C2.3", LoggerUtils.tab());
//
//            Set<Constraint> Δ2 = Δ.get(1);
//            // Split(Δ2, Δ2l, Δ2r);
//            Set<Constraint> Δ2l = new LinkedHashSet<>();
//            Set<Constraint> Δ2r = new LinkedHashSet<>();
//            split(Δ2, Δ2l, Δ2r); // split uses addAll
//
//            Set<Constraint> CC = Sets.union(parent.getCC(), Δ2l); incrementCounter(COUNTER_UNION_OPERATOR);
//            if (CC.size() != root.getCC().size() || !CC.equals(root.getCC())) {
//                // LookAhead(Δ2l, B U C, Δ2r U (Δ \ {Δ1, Δ2})), l + 1);
//                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
//                newΔ.remove(0);
//                newΔ.remove(0);
//                newΔ.add(0, Δ2r);
//
//                LookAheadNodeV4 node = new LookAheadNodeV4(parent, Δ2l, parent.getCC(), newΔ, true);
//                children.add(node);
////                lookupNode.put(CC, node);
////                incrementCounter(COUNTER_CONSTRUCTED_NODES);
//
////                    System.out.println("C2.3:" + node.getCC());
//            }
//        } else if (!Δ.isEmpty() && Δ.get(0).size() == 1) {
//            log.debug("{}(LookAheadWorker) B U C assumed consistent - C2.2", LoggerUtils.tab());
//
//            Set<Constraint> Δ1 = Δ.get(0);
//            Set<Constraint> CC = Sets.union(parent.getCC(), Δ1); incrementCounter(COUNTER_UNION_OPERATOR);
//            if (CC.size() != root.getCC().size() || !CC.equals(root.getCC())) {
//                // LookAhead(Δ1, B U C, Φ, l + 1);
//                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
//                newΔ.remove(0);
//
//                LookAheadNodeV4 node = new LookAheadNodeV4(parent, Δ1, parent.getCC(), newΔ, true);
//                children.add(node);
////                lookupNode.put(CC, node);
////                incrementCounter(COUNTER_CONSTRUCTED_NODES);
//
////                    System.out.println("C2.2:" + node.getCC());
//            }
//
//        } else if (!Δ.isEmpty() && Δ.get(0).size() > 1) {
//            log.debug("{}(LookAheadWorker) B U C assumed consistent - C2.1", LoggerUtils.tab());
//
//            Set<Constraint> Δ1 = Δ.get(0);
//            // Split(Δ1, Δ1l, Δ1r);
//            Set<Constraint> Δ1l = new LinkedHashSet<>();
//            Set<Constraint> Δ1r = new LinkedHashSet<>();
//            split(Δ1, Δ1l, Δ1r);
//
//            Set<Constraint> CC = Sets.union(parent.getCC(), Δ1l); incrementCounter(COUNTER_UNION_OPERATOR);
//            if (CC.size() != root.getCC().size() || !CC.equals(root.getCC())) {
//                // LookAhead(Δ1l, B U C, Δ1r U (Δ \ {Δ1})), l + 1);
//                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
//                newΔ.remove(0);
//                newΔ.add(0, Δ1r);
//
//                LookAheadNodeV4 node = new LookAheadNodeV4(parent, Δ1l, parent.getCC(), newΔ, true);
//                children.add(node);
////                lookupNode.put(CC, node);
////                incrementCounter(COUNTER_CONSTRUCTED_NODES);
//
////                    System.out.println("C2.1:" + node.getCC());
//            }
//        }
//
//        // B U C assumed inconsistent
//        if (sizeC > 1) {
//            log.debug("{}(LookAheadWorker) B U C assumed inconsistent - C1.1", LoggerUtils.tab());
//            // Split(C, Cl, Cr);
//            Set<Constraint> Cl = new LinkedHashSet<>();
//            Set<Constraint> Cr = new LinkedHashSet<>();
//            split(C, Cl, Cr);
//
//            Set<Constraint> CC = Sets.union(B, Cl); incrementCounter(COUNTER_UNION_OPERATOR);
//            if (CC.size() != root.getCC().size() || !CC.equals(root.getCC())) {
//                // LookAhead(Cl, B, Cr U Δ, l + 1);
//                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
//                newΔ.add(0, Cr);
//
//                LookAheadNodeV4 node = new LookAheadNodeV4(parent, Cl, B, newΔ, false);
//                children.add(node);
////                lookupNode.put(CC, node);
////                incrementCounter(COUNTER_CONSTRUCTED_NODES);
//
////                    System.out.println("C1.1:" + node.getCC());
//            }
//
//        } else if (sizeC == 1 && !Δ.isEmpty() && Δ.get(0).size() == 1) {
//            log.debug("{}(LookAheadWorker) B U C assumed inconsistent - C1.2", LoggerUtils.tab());
//
//            Set<Constraint> Δ1 = Δ.get(0);
//            Set<Constraint> CC = Sets.union(B, Δ1); incrementCounter(COUNTER_UNION_OPERATOR);
//            if (CC.size() != root.getCC().size() || !CC.equals(root.getCC())) {
//                // LookAhead(Δ1, B, Δ \ {Δ1}, l + 1);
//                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
//                newΔ.remove(0);
//
//                LookAheadNodeV4 node = new LookAheadNodeV4(parent, Δ1, B, newΔ, false);
//                children.add(node);
////                lookupNode.put(CC, node);
////                incrementCounter(COUNTER_CONSTRUCTED_NODES);
//
////                    System.out.println("C1.2:" + node.getCC());
//            }
//
//        } else if (sizeC == 1 && !Δ.isEmpty() && Δ.get(0).size() > 1) {
//            log.debug("{}(LookAheadWorker) B U C assumed inconsistent - C1.3", LoggerUtils.tab());
//
//            Set<Constraint> Δ1 = Δ.get(0);
//            // Split(Δ1, Δ1l, Δ1r);
//            Set<Constraint> Δ1l = new LinkedHashSet<>();
//            Set<Constraint> Δ1r = new LinkedHashSet<>();
//            split(Δ1, Δ1l, Δ1r);
//
//            Set<Constraint> CC = Sets.union(B, Δ1l); incrementCounter(COUNTER_UNION_OPERATOR);
//            if (CC.size() != root.getCC().size() || !CC.equals(root.getCC())) {
//                // LookAhead(Δ1l, B, Δ1r U (Δ \ {Δ1})), l + 1);
//                List<Set<Constraint>> newΔ = new ArrayList<>(Δ);
//                newΔ.remove(0);
//                newΔ.add(0, Δ1r);
//
//                LookAheadNodeV4 node = new LookAheadNodeV4(parent, Δ1l, B, newΔ, false);
//                children.add(node);
////                lookupNode.put(CC, node);
////                incrementCounter(COUNTER_CONSTRUCTED_NODES);
//
////                    System.out.println("C1.3:" + node.getCC());
//            }
//        }
//
//        LoggerUtils.outdent();
//        return children;
//    }

    protected boolean hasNodesToExpand() {
        return !openNodes.isEmpty();
    }

    protected LookAheadNodeV4 getNextNode() {
        return openNodes.remove();
    }
}
