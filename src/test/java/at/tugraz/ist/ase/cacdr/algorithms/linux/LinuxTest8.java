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
public class LinuxTest8 {
    private static FeatureModel featureModel;
    private static Combination combination;

    private final int poolSize = 7;
    private final int lookAheadPoolSize = 1;
    private final int checkerPoolSize = 6;

    private final int maxLevel = 2;
    private final int iterations = 1;
    private static final String user_requirement = "REED_SOLOMON_ENC16=true,DS1620=true,FTRACE_NMI_ENTER=true,ARCH_INLINE_SPIN_UNLOCK_IRQ=true,QFMT_V1=true,SERIAL_SH_SCI=true,LOCK_KERNEL=true,OF_MDIO=true,PCCARD=true,KGDB=true,TRACE_IRQFLAGS_SUPPORT=true,HAVE_DMA_API_DEBUG=true,X86_HT=true,GENERIC_CMOS_UPDATE=true,INLINE_SPIN_TRYLOCK=true,PCI_DEBUG=true,INFINIBAND=true,HT_IRQ=true,MCP_UCB1200=true,FW_LOADER=true,BACKTRACE_SELF_TEST=true,DEBUG_PER_CPU_MAPS=true,SERIAL_ATMEL=true,GENERIC_TIME=true,HZ_300_alt=true,X86_ELAN=true,SECURITY_TOMOYO=true,HAVE_PERF_EVENTS=true,SELECT_MEMORY_MODEL=true,BTRFS_FS=true,NLS=true,PHYS_ADDR_T_64BIT=true,PRINTER=true,FIREWIRE=true,SERIAL_SUNSU=true,X86_PLATFORM_DEVICES=true,SERIAL_ICOM=true,STRICT_DEVMEM=true,DLM=true,UIO=true,CHR_DEV_OSST=true,PDC_CONSOLE=true,GENERIC_HARDIRQS_NO__DO_IRQ=true,INLINE_WRITE_LOCK=true,PERF_COUNTERS=true,SYSFS_DEPRECATED_V2=true,ASYNC_PQ=true,FRAMEBUFFER_CONSOLE=true,RCU_TRACE=false,TELCLOCK=true,ARCH_INLINE_READ_TRYLOCK=true,MGEODEGX1_alt=false,SYSVIPC=true,SERIAL_S3C2412=true,DEBUG_PAGEALLOC=true,IOMMU_DEBUG=true,INIT_ALL_POSSIBLE=true,LOCALVERSION=true,HAVE_ARCH_EARLY_PFN_TO_NID=true,SERIAL_SAMSUNG_UARTS=false,SERIAL_SUNZILOG=false,SPLIT_PTLOCK_CPUS=false,ARCH_MAY_HAVE_PC_FDC=true,EFI_VARS=false,GENERIC_ATOMIC64=true";

    @BeforeAll
    public static void setUp() throws FeatureModelParserException {
        // loads feature model
        File file = new File("./data/kb/linux-2.6.33.3.xml");
        FMFormat fmFormat = FMFormat.getFMFormat(Files.getFileExtension(file.getName()));
        FeatureModelParser parser = FMParserFactory.getInstance().getParser(fmFormat);
        featureModel = parser.parse(file);

        UserRequirementBuilder builder = new UserRequirementBuilder();
        List<UserRequirement> userRequirement = builder.buildUserRequirement(user_requirement);
        combination = Combination.builder()
                .combination(user_requirement)
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

        assertEquals(8, firstDiagFD.size());
        assertEquals("[REED_SOLOMON_ENC16=true, ARCH_INLINE_SPIN_UNLOCK_IRQ=true, ARCH_INLINE_READ_TRYLOCK=true, MGEODEGX1_alt=false, INIT_ALL_POSSIBLE=true, SERIAL_SAMSUNG_UARTS=false, SPLIT_PTLOCK_CPUS=false, GENERIC_ATOMIC64=true]",
                firstDiagFD.toString());
    }

    @Test
    @Order(1)
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

        assertEquals(8, firstDiag.size());
        assertEquals("[REED_SOLOMON_ENC16=true, ARCH_INLINE_SPIN_UNLOCK_IRQ=true, ARCH_INLINE_READ_TRYLOCK=true, MGEODEGX1_alt=false, INIT_ALL_POSSIBLE=true, SERIAL_SAMSUNG_UARTS=false, SPLIT_PTLOCK_CPUS=false, GENERIC_ATOMIC64=true]",
                firstDiag.toString());
    }

    @Test
    @Order(2)
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

        assertEquals(8, firstDiag.size());
        assertEquals("[REED_SOLOMON_ENC16=true, ARCH_INLINE_SPIN_UNLOCK_IRQ=true, ARCH_INLINE_READ_TRYLOCK=true, MGEODEGX1_alt=false, INIT_ALL_POSSIBLE=true, SERIAL_SAMSUNG_UARTS=false, SPLIT_PTLOCK_CPUS=false, GENERIC_ATOMIC64=true]",
                firstDiag.toString());
    }
}
