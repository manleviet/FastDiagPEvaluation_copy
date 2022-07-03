package at.tugraz.ist.ase.cacdr.algorithms.linux;

import at.tugraz.ist.ase.cacdr.algorithms.FastDiagV3;
import at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.FastDiagPV4;
import at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.FastDiagPV6;
import at.tugraz.ist.ase.cacdr.checker.ChocoConsistencyChecker;
import at.tugraz.ist.ase.cacdr.core.Combination;
import at.tugraz.ist.ase.cacdr.core.UserRequirement;
import at.tugraz.ist.ase.cacdr.core.io.UserRequirementBuilder;
import at.tugraz.ist.ase.cacdr.core.translator.FMUserRequirementTranslator;
import at.tugraz.ist.ase.cacdr.eval.CAEvaluator;
import at.tugraz.ist.ase.cacdr.model.FMModel;
import at.tugraz.ist.ase.fm.core.FeatureModel;
import at.tugraz.ist.ase.fm.parser.FMFormat;
import at.tugraz.ist.ase.fm.parser.FeatureModelParser;
import at.tugraz.ist.ase.fm.parser.FeatureModelParserException;
import at.tugraz.ist.ase.fm.parser.factory.FMParserFactory;
import at.tugraz.ist.ase.kb.core.Constraint;
import com.google.common.io.Files;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;
import java.util.Set;

import static at.tugraz.ist.ase.cacdr.algorithms.FastDiagV3.TIMER_FASTDIAGV3;
import static at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.FastDiagPV4.TIMER_FASTDIAGPV4;
import static at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.FastDiagPV6.TIMER_FASTDIAGPV6;
import static at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.core.ConsistencyCheckWorker.COUNTER_CONSISTENCY_CHECKS_IN_WORKER;
import static at.tugraz.ist.ase.cacdr.algorithms.fastdiagp.core.LookupTable.*;
import static at.tugraz.ist.ase.cacdr.checker.ChocoConsistencyChecker.TIMER_SOLVER;
import static at.tugraz.ist.ase.cacdr.eval.CAEvaluator.COUNTER_CONSISTENCY_CHECKS;
import static at.tugraz.ist.ase.cacdr.eval.CAEvaluator.printPerformance;
import static at.tugraz.ist.ase.eval.PerformanceEvaluator.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LinuxTest2 {
    private static FeatureModel featureModel;
    private static Combination combination;

    private final int poolSize = 7;
    private final int lookAheadPoolSize = 1;
    private final int checkerPoolSize = 6;

    private final int maxLevel = 2;
    private final int iterations = 1;
    private static final String var_value_combination = "MCP_UCB1200=true,ARCH_INLINE_SPIN_UNLOCK_IRQ=true,FRAMEBUFFER_CONSOLE=true,SERIAL_SUNZILOG=true,FIREWIRE=true,PCI_DEBUG=true,HAVE_DMA_API_DEBUG=true,BACKTRACE_SELF_TEST=true,PHYS_ADDR_T_64BIT=true,INFINIBAND=true,SYSVIPC=true,ASYNC_PQ=true,STRICT_DEVMEM=true,X86_DS=true,HAVE_PERF_EVENTS=true,LOCK_KERNEL=true,SERIAL_SUNSU=true,UIO=true,NLS=true,PCCARD=true,GENERIC_PENDING_IRQ=true,SECURITY_TOMOYO=true,LOCALVERSION=true,X86_ELAN=true,SELECT_MEMORY_MODEL=true,KGDB=true,EZX_PCAP=true,QFMT_V1=true,INIT_ALL_POSSIBLE=true,HT_IRQ=true,DEBUG_PER_CPU_MAPS=true,GENERIC_HARDIRQS_NO__DO_IRQ=true,HAVE_ARCH_EARLY_PFN_TO_NID=true,FW_LOADER=true,FTRACE_NMI_ENTER=false,TWL4030_CORE=false,IOMMU_DEBUG=false,CHR_DEV_OSST=false,X86_HT=false,SERIAL_SAMSUNG_UARTS=true,PDC_CONSOLE=false,SERIAL_SH_SCI=false,MGEODEGX1_alt=true,EFI_VARS=true,ARCH_INLINE_READ_TRYLOCK=false,MAXSMP=false,GENERIC_CMOS_UPDATE=true,GENERIC_TIME=false,DEBUG_PAGEALLOC=false,SERIAL_ICOM=true,SERIAL_S3C2412=true,TRACE_IRQFLAGS_SUPPORT=true,PRINTER=false,INLINE_WRITE_LOCK=false,DLM=true";

    @BeforeAll
    public static void setUp() throws FeatureModelParserException {
        // loads feature model
        File file = new File("./data/kb/linux-2.6.33.3.xml");
        FMFormat fmFormat = FMFormat.getFMFormat(Files.getFileExtension(file.getName()));
        FeatureModelParser parser = FMParserFactory.getInstance().getParser(fmFormat);
        featureModel = parser.parse(file);

        UserRequirementBuilder builder = new UserRequirementBuilder();
        List<UserRequirement> userRequirement = builder.buildUserRequirement(var_value_combination);
        combination = Combination.builder()
                .combination(var_value_combination)
                .userRequirements(userRequirement)
                .build();
    }

    @Test
    @Order(3)
    void fastDiag() {
        double time = 0.0;
        Set<Constraint> firstDiagFD = null;
        for (int i = 0; i < iterations; i++) {
            System.gc();

            FMModel diagModel = new FMModel(featureModel, combination, new FMUserRequirementTranslator(), true, false);
            diagModel.initialize();

            ChocoConsistencyChecker checker = new ChocoConsistencyChecker(diagModel);

            Set<Constraint> C = diagModel.getPossiblyFaultyConstraints();
            Set<Constraint> B = diagModel.getCorrectConstraints();

            // run the fastDiag to find diagnoses
            FastDiagV3 fd = new FastDiagV3(checker);

            CAEvaluator.reset();
            setCommonTimer(TIMER_SOLVER);
            setCommonTimer(TIMER_FASTDIAGV3);
            firstDiagFD = fd.findDiagnosis(C, B);

            time += totalCommonTimer(TIMER_FASTDIAGV3) / 1000000000.0;
        }
        time /= iterations;

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiag:");
        System.out.println(firstDiagFD);
        System.out.println("\t\tCardinality: " + firstDiagFD.size());
        System.out.println("\t\tRuntime: " + time + " seconds");
        System.out.println("\t\tCC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));
        printPerformance();

        assertEquals(2, firstDiagFD.size());
        assertEquals("[ARCH_INLINE_SPIN_UNLOCK_IRQ=true, INIT_ALL_POSSIBLE=true]", firstDiagFD.toString());
    }

    @Test
    @Order(2)
    void fastDiagPV4() {
        double time = 0.0;
        Set<Constraint> firstDiag = null;
        for (int i = 0; i < iterations; i++) {
            System.gc();

            FMModel diagModel = new FMModel(featureModel, combination, new FMUserRequirementTranslator(), true, false);
            diagModel.initialize();

            Set<Constraint> C = diagModel.getPossiblyFaultyConstraints();
            Set<Constraint> B = diagModel.getCorrectConstraints();

            // run fastDiagP
            FastDiagPV4 fdp = new FastDiagPV4(diagModel, lookAheadPoolSize, checkerPoolSize, maxLevel);

            CAEvaluator.reset();
            setCommonTimer(TIMER_SOLVER);
            setCommonTimer(TIMER_FASTDIAGPV4);
            setCommonTimer(TIMER_LOOKUP_ALTERNATIVE);
            setCommonTimer(TIMER_LOOKUP_GET);
            setCommonTimer(TIMER_CLEANUP);
            firstDiag = fdp.findDiagnosis(C, B);

            time += totalCommonTimer(TIMER_FASTDIAGPV4) / 1000000000.0;
        }
        time /= iterations;

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiagPV4:");
        System.out.println(firstDiag);
        System.out.println("\t\tCardinality: " + firstDiag.size());
        System.out.println("\t\tRuntime: " + time + " seconds");
        System.out.println("\t\tCC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));
        System.out.println("\t\tCC: " + getCounter(COUNTER_CONSISTENCY_CHECKS_IN_WORKER));
        printPerformance();

        assertEquals(2, firstDiag.size());
        assertEquals("[ARCH_INLINE_SPIN_UNLOCK_IRQ=true, INIT_ALL_POSSIBLE=true]", firstDiag.toString());
    }

    @Test
    @Order(1)
    void fastDiagPV6() {
        double time = 0.0;
        Set<Constraint> firstDiag = null;
        for (int i = 0; i < iterations; i++) {
            System.gc();

            FMModel diagModel = new FMModel(featureModel, combination, new FMUserRequirementTranslator(), true, false);
            diagModel.initialize();

            Set<Constraint> C = diagModel.getPossiblyFaultyConstraints();
            Set<Constraint> B = diagModel.getCorrectConstraints();

            // run fastDiagP
            FastDiagPV6 fdp = new FastDiagPV6(diagModel, lookAheadPoolSize, checkerPoolSize, maxLevel);

            CAEvaluator.reset();
            setCommonTimer(TIMER_SOLVER);
            setCommonTimer(TIMER_FASTDIAGPV6);
            setCommonTimer(TIMER_LOOKUP_ALTERNATIVE);
            setCommonTimer(TIMER_LOOKUP_GET);
            setCommonTimer(TIMER_CLEANUP);
            firstDiag = fdp.findDiagnosis(C, B);

            time += totalCommonTimer(TIMER_FASTDIAGPV6) / 1000000000.0;
        }
        time /= iterations;

        System.out.println("=========================================");
        System.out.println("Preferred diagnosis found by FastDiagPV4:");
        System.out.println(firstDiag);
        System.out.println("\t\tCardinality: " + firstDiag.size());
        System.out.println("\t\tRuntime: " + time + " seconds");
        System.out.println("\t\tCC: " + getCounter(COUNTER_CONSISTENCY_CHECKS));
        System.out.println("\t\tCC: " + getCounter(COUNTER_CONSISTENCY_CHECKS_IN_WORKER));
        printPerformance();

        assertEquals(2, firstDiag.size());
        assertEquals("[ARCH_INLINE_SPIN_UNLOCK_IRQ=true, INIT_ALL_POSSIBLE=true]", firstDiag.toString());
    }
}
