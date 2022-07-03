package at.tugraz.ist.ase.cacdr.algorithms.fastdiagp;

import at.tugraz.ist.ase.cacdr.algorithms.FastDiagV3;
import at.tugraz.ist.ase.cacdr.checker.ChocoConsistencyChecker;
import at.tugraz.ist.ase.cacdr.eval.CAEvaluator;
import at.tugraz.ist.ase.cdrmodel.test.model.*;
import at.tugraz.ist.ase.kb.core.Constraint;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static at.tugraz.ist.ase.cacdr.algorithms.FastDiagV3.TIMER_FASTDIAGV3;
import static at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.FastDiagPV4.TIMER_FASTDIAGPV4;
import static at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.core.ConsistencyCheckWorker.COUNTER_CONSISTENCY_CHECKS_IN_WORKER;
import static at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.core.LookupTable.*;
import static at.tugraz.ist.ase.cacdr.checker.ChocoConsistencyChecker.TIMER_SOLVER;
import static at.tugraz.ist.ase.cacdr.eval.CAEvaluator.COUNTER_CONSISTENCY_CHECKS;
import static at.tugraz.ist.ase.cacdr.eval.CAEvaluator.printPerformance;
import static at.tugraz.ist.ase.common.ConstraintUtils.convertToString;
import static at.tugraz.ist.ase.eval.PerformanceEvaluator.getCounter;
import static at.tugraz.ist.ase.eval.PerformanceEvaluator.setCommonTimer;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FastDiagPV4Test {
    private final int lookAheadPoolSize = 1;
    private final int checkerPoolSize = 6;
    private final int maxLevel = 3;

    @Test
    void testFindDiagnosis1() throws Exception {
        TestModel1 testModel = new TestModel1();
        testModel.initialize();

        System.out.println("=========================================");
        System.out.println("Choco's commands translated from the text file:");
        System.out.println(convertToString(testModel.getPossiblyFaultyConstraints()));
        System.out.println("=========================================");

        Set<Constraint> C = testModel.getPossiblyFaultyConstraints();
        Set<Constraint> B = testModel.getCorrectConstraints();

        // run the fastDiag to find a diagnosis
        ChocoConsistencyChecker checker = new ChocoConsistencyChecker(testModel);
        FastDiagV3 fd = new FastDiagV3(checker);

        CAEvaluator.reset();
        setCommonTimer(TIMER_SOLVER);
        setCommonTimer(TIMER_FASTDIAGV3);
        Set<Constraint> firstDiagFD = fd.findDiagnosis(C, B);

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiag:");
        System.out.println(firstDiagFD);
        printPerformance();
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));

        // run the FastDiagPV4 to find a diagnosis
        FastDiagPV4 fdpV5 = new FastDiagPV4(testModel, lookAheadPoolSize, checkerPoolSize, maxLevel);

        CAEvaluator.reset();
        setCommonTimer(TIMER_SOLVER);
        setCommonTimer(TIMER_FASTDIAGPV4);
        setCommonTimer(TIMER_LOOKUP_ALTERNATIVE);
        setCommonTimer(TIMER_LOOKUP_GET);
        setCommonTimer(TIMER_CLEANUP);
        Set<Constraint> firstDiagV3 = fdpV5.findDiagnosis(C, B);

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiagPV4:");
        System.out.println(firstDiagV3);
        printPerformance();
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS_IN_WORKER));

        assertEquals(testModel.getExpectedFirstDiagnosis(), firstDiagV3);
    }

    @Test
    void testFindDiagnosis2() throws Exception {
        TestModel2 testModel = new TestModel2();
        testModel.initialize();

        System.out.println("=========================================");
        System.out.println("Choco's commands translated from the text file:");
        System.out.println(convertToString(testModel.getPossiblyFaultyConstraints()));
        System.out.println("=========================================");

        Set<Constraint> C = testModel.getPossiblyFaultyConstraints();
        Set<Constraint> B = testModel.getCorrectConstraints();

        // run the fastDiag to find a diagnosis
        ChocoConsistencyChecker checker = new ChocoConsistencyChecker(testModel);
        FastDiagV3 fd = new FastDiagV3(checker);

        CAEvaluator.reset();
        setCommonTimer(TIMER_SOLVER);
        setCommonTimer(TIMER_FASTDIAGV3);
        Set<Constraint> firstDiagFD = fd.findDiagnosis(C, B);

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiag:");
        System.out.println(firstDiagFD);
        printPerformance();
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));

        // run the FastDiagPV4 to find a diagnosis
        FastDiagPV4 fdpV5 = new FastDiagPV4(testModel, lookAheadPoolSize, checkerPoolSize, maxLevel);

        CAEvaluator.reset();
        setCommonTimer(TIMER_SOLVER);
        setCommonTimer(TIMER_FASTDIAGPV4);
        setCommonTimer(TIMER_LOOKUP_ALTERNATIVE);
        setCommonTimer(TIMER_LOOKUP_GET);
        setCommonTimer(TIMER_CLEANUP);
        Set<Constraint> firstDiagV3 = fdpV5.findDiagnosis(C, B);

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiagPV4:");
        System.out.println(firstDiagV3);
        printPerformance();
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS_IN_WORKER));

        assertEquals(testModel.getExpectedFirstDiagnosis(), firstDiagV3);
    }

    @Test
    void testFindDiagnosis3() throws Exception {
        TestModel3 testModel = new TestModel3();
        testModel.initialize();

        System.out.println("=========================================");
        System.out.println("Choco's commands translated from the text file:");
        System.out.println(convertToString(testModel.getPossiblyFaultyConstraints()));
        System.out.println("=========================================");

        Set<Constraint> C = testModel.getPossiblyFaultyConstraints();
        Set<Constraint> B = testModel.getCorrectConstraints();

        // run the fastDiag to find a diagnosis
        ChocoConsistencyChecker checker = new ChocoConsistencyChecker(testModel);
        FastDiagV3 fd = new FastDiagV3(checker);

        CAEvaluator.reset();
        setCommonTimer(TIMER_SOLVER);
        setCommonTimer(TIMER_FASTDIAGV3);
        Set<Constraint> firstDiagFD = fd.findDiagnosis(C, B);

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiag:");
        System.out.println(firstDiagFD);
        printPerformance();
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));

        // run the FastDiagPV4 to find a diagnosis
        FastDiagPV4 fdpV5 = new FastDiagPV4(testModel, lookAheadPoolSize, checkerPoolSize, maxLevel);

        CAEvaluator.reset();
        setCommonTimer(TIMER_SOLVER);
        setCommonTimer(TIMER_FASTDIAGPV4);
        setCommonTimer(TIMER_LOOKUP_ALTERNATIVE);
        setCommonTimer(TIMER_LOOKUP_GET);
        setCommonTimer(TIMER_CLEANUP);
        Set<Constraint> firstDiagV3 = fdpV5.findDiagnosis(C, B);

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiagPV4:");
        System.out.println(firstDiagV3);
        printPerformance();
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS_IN_WORKER));

        assertEquals(testModel.getExpectedFirstDiagnosis(), firstDiagV3);
    }

    @Test
    void testFindDiagnosis4() throws Exception {
        TestModel4 testModel = new TestModel4();
        testModel.initialize();

        System.out.println("=========================================");
        System.out.println("Choco's commands translated from the text file:");
        System.out.println(convertToString(testModel.getPossiblyFaultyConstraints()));
        System.out.println("=========================================");

        Set<Constraint> C = testModel.getPossiblyFaultyConstraints();
        Set<Constraint> B = testModel.getCorrectConstraints();

        // run the fastDiag to find a diagnosis
        ChocoConsistencyChecker checker = new ChocoConsistencyChecker(testModel);
        FastDiagV3 fd = new FastDiagV3(checker);

        CAEvaluator.reset();
        setCommonTimer(TIMER_SOLVER);
        setCommonTimer(TIMER_FASTDIAGV3);
        Set<Constraint> firstDiagFD = fd.findDiagnosis(C, B);

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiag:");
        System.out.println(firstDiagFD);
        printPerformance();
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));

        // run the FastDiagPV4 to find a diagnosis
        FastDiagPV4 fdpV5 = new FastDiagPV4(testModel, lookAheadPoolSize, checkerPoolSize, maxLevel);

        CAEvaluator.reset();
        setCommonTimer(TIMER_SOLVER);
        setCommonTimer(TIMER_FASTDIAGPV4);
        setCommonTimer(TIMER_LOOKUP_ALTERNATIVE);
        setCommonTimer(TIMER_LOOKUP_GET);
        setCommonTimer(TIMER_CLEANUP);
        Set<Constraint> firstDiagV3 = fdpV5.findDiagnosis(C, B);

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiagPV4:");
        System.out.println(firstDiagV3);
        printPerformance();
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS_IN_WORKER));

        assertEquals(testModel.getExpectedFirstDiagnosis(), firstDiagV3);
    }

    @Test
    void testFindDiagnosis5() throws Exception {
        TestModel5 testModel = new TestModel5();
        testModel.initialize();

        System.out.println("=========================================");
        System.out.println("Choco's commands translated from the text file:");
        System.out.println(convertToString(testModel.getPossiblyFaultyConstraints()));
        System.out.println("=========================================");

        Set<Constraint> C = testModel.getPossiblyFaultyConstraints();
        Set<Constraint> B = testModel.getCorrectConstraints();

        // run the fastDiag to find a diagnosis
        ChocoConsistencyChecker checker = new ChocoConsistencyChecker(testModel);
        FastDiagV3 fd = new FastDiagV3(checker);

        CAEvaluator.reset();
        setCommonTimer(TIMER_SOLVER);
        setCommonTimer(TIMER_FASTDIAGV3);
        Set<Constraint> firstDiagFD = fd.findDiagnosis(C, B);

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiag:");
        System.out.println(firstDiagFD);
        printPerformance();
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));

        // run the FastDiagPV4 to find a diagnosis
        FastDiagPV4 fdpV5 = new FastDiagPV4(testModel, lookAheadPoolSize, checkerPoolSize, maxLevel);

        CAEvaluator.reset();
        setCommonTimer(TIMER_SOLVER);
        setCommonTimer(TIMER_FASTDIAGPV4);
        setCommonTimer(TIMER_LOOKUP_ALTERNATIVE);
        setCommonTimer(TIMER_LOOKUP_GET);
        setCommonTimer(TIMER_CLEANUP);
        Set<Constraint> firstDiagV3 = fdpV5.findDiagnosis(C, B);

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiagPV4:");
        System.out.println(firstDiagV3);
        printPerformance();
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));
        System.out.println("CC: " + getCounter(COUNTER_CONSISTENCY_CHECKS_IN_WORKER));

        assertEquals(testModel.getExpectedFirstDiagnosis(), firstDiagV3);
    }
}