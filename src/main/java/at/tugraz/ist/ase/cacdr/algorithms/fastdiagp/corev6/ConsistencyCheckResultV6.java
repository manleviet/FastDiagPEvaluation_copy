package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.corev6;

import at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.core.CCSTATE;
import at.tugraz.ist.ase.common.LoggerUtils;
import at.tugraz.ist.ase.kb.core.Constraint;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.Semaphore;

@Slf4j
public class ConsistencyCheckResultV6 {
    @Getter
    private final Set<Constraint> C;
    @Getter
    private final Integer hashCode;

    @Getter
    private boolean consistent;
    @Getter @Setter
    private CCSTATE status;
    private final Semaphore semaphore;

    @Getter
    private long threadId = -1;

    @Getter @Setter
    private ConsistencyCheckWorkerV6 worker = null;

    public ConsistencyCheckResultV6(@NonNull Set<Constraint> C) {
        this.C = C;
        this.hashCode = C.hashCode();
        this.consistent = false;
        this.status = CCSTATE.IN_QUEUE;

        semaphore = new Semaphore(1); // already try fair=true
        log.debug("{}(ConsistencyCheckResult) created A ConsistencyCheckResult for [C={}]", LoggerUtils.tab(), C);
    }

    public void setConsistency(Boolean isConsistent, long threadId) {
        this.consistent = isConsistent;
        this.threadId = threadId;
        status = CCSTATE.DONE;

        log.debug("{}(ConsistencyCheckResult-setConsistency) [C={}, consistency={}, status={}]", LoggerUtils.tab(), C, consistent, status);
    }

    public boolean isInQueue() {
        return status == CCSTATE.IN_QUEUE;
    }

    public boolean isDone() {
        return status == CCSTATE.DONE;
    }

    public void putbackInQueue() {
        status = CCSTATE.IN_QUEUE;
    }

    public void acquire() throws InterruptedException {
        semaphore.acquire();
        log.debug("{}(ConsistencyCheckResult-acquire) acquired for [C={}]", LoggerUtils.tab(), C);
    }

    public void release() {
        semaphore.release();
        log.debug("{}(ConsistencyCheckResult-release) released for [C={}]", LoggerUtils.tab(), C);
    }

    @Override
    public String toString() {
        return "ConsistencyCheckResult{" +
                "C='" + C + '\'' +
                ", isConsistent=" + consistent +
                ", status=" + status +
                ", threadId=" + threadId +
                '}';
    }
}
