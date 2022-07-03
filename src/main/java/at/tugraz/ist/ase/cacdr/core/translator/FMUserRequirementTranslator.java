package at.tugraz.ist.ase.cacdr.core.translator;

import at.tugraz.ist.ase.cacdr.core.UserRequirement;
import at.tugraz.ist.ase.common.LoggerUtils;
import at.tugraz.ist.ase.kb.core.Constraint;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.nary.cnf.LogOp;
import org.chocosolver.solver.variables.BoolVar;

import static at.tugraz.ist.ase.common.ChocoSolverUtils.getVariable;

@Slf4j
public class FMUserRequirementTranslator implements IUserRequirementTranslatable {
    /**
     * Translates user requirement to Choco constraints.
     */
    @Override
    public void translate(@NonNull UserRequirement userRequirement, @NonNull Model model) {

//        if (userRequirement instanceof UserRequirement tc) {
            log.trace("{}Translating user requirement [ur={}] >>>", LoggerUtils.tab(), userRequirement);
            createUserRequirement(userRequirement, model);
//        }
//        else if (testCase instanceof AggregatedTestCase atc) {
//            log.trace("{}Translating aggregated test case [testcase={}] >>>", LoggerUtils.tab(), testCase);
//            for (ITestCase tc : atc.getTestcases()) {
//                createTestCase((TestCase) tc, model);
//            }
//        }
    }

    /**
     * Translates a user requirement to Choco constraints.
     */
    private void createUserRequirement(UserRequirement ur, Model model) {
        int startIdx = model.getNbCstrs();

        LogOp logOp = LogOp.and(); // creates a AND LogOp
        BoolVar v = (BoolVar) getVariable(model, ur.getVariable()); // get the corresponding variable
        if (ur.getValue().equals("true")) { // true
            logOp.addChild(v);
        } else { // false
            logOp.addChild(v.not());
        }
        model.addClauses(logOp); // add the translated constraints to the Choco model
        int lastCstrIdx = model.getNbCstrs();

        Constraint c = new Constraint(ur.toString());
        c.addChocoConstraints(model, startIdx, lastCstrIdx - 1, false);

        // Negative user requirement
//        LogOp negLogOp = LogOp.nand(logOp);
//        startIdx = model.getNbCstrs();
//        model.addClauses(negLogOp);
//        lastCstrIdx = model.getNbCstrs();
//        c.addChocoConstraints(model, startIdx, lastCstrIdx - 1, true);

        ur.setConstraint(c);

        log.debug("{}Translated test case [testcase={}] >>>", LoggerUtils.tab(), ur);
    }
}
