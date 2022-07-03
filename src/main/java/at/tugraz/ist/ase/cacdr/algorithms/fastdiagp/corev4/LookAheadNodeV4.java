package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.corev4;

import at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.lookaheadtree.LookAheadNodeStatus;
import at.tugraz.ist.ase.common.LoggerUtils;
import at.tugraz.ist.ase.kb.core.Constraint;
import com.google.common.collect.Sets;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Getter
@NoArgsConstructor
@Slf4j
public class LookAheadNodeV4 {
    private static long generatingNodeId = -1;
    private final long id = ++generatingNodeId;

    /**
     * The tree level
     */
    private int level = 0;

    /**
     * The node status
     */
    @Setter
    private LookAheadNodeStatus status = LookAheadNodeStatus.Open;

//    /**
//     * A label of this node - the consistency (true/false)
//     */
//    @Setter
//    private boolean label;

    /**
     * This is the assumption associated to the arch which comes to this node.
     * Can be null for the root node.
     */
    private Boolean arcLabel = null;

    /**
     * The node's children
     */
    private final ConcurrentHashMap<Boolean, LookAheadNodeV4> children = new ConcurrentHashMap<>();

    /**
     * The node's parent. Can be null for the root node.
     */
    private LookAheadNodeV4 parent = null;

//    /**
//     * The labelers' parameters
//     */
//    @Setter
//    private AbstractHSParameters parameters;

    @Getter
    private Set<Constraint> CC;

    @Getter
    private Set<Constraint> C;
    @Getter
    private Set<Constraint> B;
    @Getter
    private List<Set<Constraint>> Δ;

    @Getter @Setter
    Boolean consistency = null;

    @Getter @Setter
    ConsistencyCheckWorkerV4 worker = null;

    private final Semaphore semaphore = new Semaphore(1);

    /**
     * Constructor for the root node.
     */
    public static LookAheadNodeV4 createRoot(Set<Constraint> C, Set<Constraint> B, List<Set<Constraint>> Δ, Boolean consistency) {
        generatingNodeId = -1;

        LookAheadNodeV4 root = new LookAheadNodeV4();
//        root.label = label;
//        root.parameters = parameters;
        root.C = C;
        root.B = B;
        root.CC = Sets.union(B, C);

        root.Δ = Δ;
        root.consistency = consistency;

        log.trace("{}Created root node with [C={}, B={}, cc={}]", LoggerUtils.tab(), C, B, consistency);
        return root;
    }

    public static LookAheadNodeV4 createRoot(Set<Constraint> C, Set<Constraint> B, List<Set<Constraint>> Δ) {
        generatingNodeId = -1;

        LookAheadNodeV4 root = new LookAheadNodeV4();
//        root.label = label;
//        root.parameters = parameters;
        root.C = C;
        root.B = B;
        root.CC = Sets.union(B, C);

        root.Δ = Δ;

        log.trace("{}Created root node with [C={}, B={}]", LoggerUtils.tab(), C, B);
        return root;
    }

    /**
     * Constructor for child nodes.
     */
    @Builder
    public LookAheadNodeV4(@NonNull LookAheadNodeV4 parent,
                           Set<Constraint> C, Set<Constraint> B, List<Set<Constraint>> Δ, boolean arcLabel) {
        this.parent = parent;
//        this.parents.add(parent);
        this.level = parent.level + 1;
        this.arcLabel = arcLabel;

        this.C = C;
        this.B = B;
        this.CC = Sets.union(B, C);
        this.Δ = Δ;

        parent.children.put(arcLabel, this);

        log.trace("{}Created child node with [parent={}, arcLabel={}]", LoggerUtils.tab(), parent, arcLabel);
    }

    protected void acquire() throws InterruptedException {
        semaphore.acquire();
        log.trace("{}(Node) acquired for [node={}]", LoggerUtils.tab(), this);
    }

    public void release() {
        semaphore.release();
        log.trace("{}(Node) released for [node={}]", LoggerUtils.tab(), this);
    }

    /**
     * Adds a parent to this node.
     */
    public void addParent(LookAheadNodeV4 parent) {
        if (isRoot()) {
            throw new IllegalArgumentException("The root node cannot have parents.");
        } else {
            this.parent = parent;

            log.trace("{}Added parent node with [parent={}, child={}]", LoggerUtils.tab(), parent, this);
        }
    }

    /**
     * Adds a child node to this node.
     */
    public void addChild(@NonNull Boolean arcLabel, @NonNull LookAheadNodeV4 child) {
        this.children.put(arcLabel, child);
        child.addParent(this);

        log.trace("{}Added child node with [parent={}, arcLabel={}, child={}]", LoggerUtils.tab(), this, arcLabel, child);
    }

    /**
     * Returns isRoot value
     *
     * @return true if this node is the root node, otherwise false.
     */
    public boolean isRoot() {
        return this.parent == null;
    }

    @Override
    public String toString() {
        return "LookAheadNode{" +
                "id=" + id +
                ", level=" + level +
                ", status=" + status +
                ", arcLabel=" + arcLabel +
                ", children=" + children +
                ", C=" + C +
                ", B=" + B +
                ", Δ=" + Δ +
                ", consistency=" + consistency +
                '}';
    }
}
