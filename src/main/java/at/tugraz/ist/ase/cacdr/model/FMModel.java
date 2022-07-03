/*
 * WipeOut: Automated Redundancy Detection in Feature Models
 *
 * Copyright (c) 2022-2022
 *
 * @author: Viet-Man Le (vietman.le@ist.tugraz.at)
 */

package at.tugraz.ist.ase.cacdr.model;

import at.tugraz.ist.ase.cacdr.core.Combination;
import at.tugraz.ist.ase.cacdr.core.UserRequirement;
import at.tugraz.ist.ase.cacdr.core.translator.IUserRequirementTranslatable;
import at.tugraz.ist.ase.cdrmodel.CDRModel;
import at.tugraz.ist.ase.cdrmodel.IChocoModel;
import at.tugraz.ist.ase.common.LoggerUtils;
import at.tugraz.ist.ase.fm.core.FeatureModel;
import at.tugraz.ist.ase.kb.core.Constraint;
import at.tugraz.ist.ase.kb.fm.FMKB;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static at.tugraz.ist.ase.common.ChocoSolverUtils.getVariable;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class FMModel extends CDRModel implements IChocoModel {

    @Getter
    private Model model;
    private FeatureModel featureModel;
    @Getter
    private FMKB fmkb;

    private Combination combination;
    private IUserRequirementTranslatable translator;

    @Getter
    private final boolean rootConstraints;

    @Getter
    private final boolean reversedConstraintsOrder;

    /**
     * A constructor
     * On the basic of a given {@link FeatureModel}, it creates
     * corresponding variables and constraints for the model.
     *
     * @param featureModel a {@link FeatureModel}
     */
    public FMModel(@NonNull FeatureModel featureModel, Combination combination,
                   IUserRequirementTranslatable translator,
                   boolean rootConstraints, boolean reversedConstraintsOrder) {
        super(featureModel.getName());
        checkArgument((combination == null && translator == null) || (combination != null && translator != null),
                "Combination and translator must not be null as the same time");

        this.featureModel = featureModel;
        this.fmkb = new FMKB(featureModel, false);
        this.model = fmkb.getModelKB();

        this.combination = combination;

        this.translator = translator;

        this.rootConstraints = rootConstraints;
        this.reversedConstraintsOrder = reversedConstraintsOrder;
    }

    /**
     * This function creates a Choco models, variables, constraints
     * for a corresponding feature models. Besides, test cases are
     * also translated to Choco constraints.
     */
    @Override
    public void initialize() {
        log.debug("{}Initializing FMModel for {} >>>", LoggerUtils.tab(), getName());
        LoggerUtils.indent();

        // sets possibly faulty constraints to super class
        log.trace("{}Adding possibly faulty constraints", LoggerUtils.tab());
        List<Constraint> C = new LinkedList<>(fmkb.getConstraintList());
        // translates user requirements to Choco constraints
        log.trace("{}Translating user requirements to Choco constraints", LoggerUtils.tab());
        if (combination != null) {
            createUserRequirement();
            // add user requirements to C
            combination.getUserRequirements().stream().map(UserRequirement::getConstraint).forEachOrdered(C::add);
        }
        if (isReversedConstraintsOrder()) {
            Collections.reverse(C); // in default, this shouldn't happen
        }
        this.setPossiblyFaultyConstraints(C);

        // sets correct constraints to super class
        if (isRootConstraints()) {
            log.trace("{}Adding correct constraints", LoggerUtils.tab());
            // {f0 = true}
            int startIdx = model.getNbCstrs();
            String f0 = fmkb.getVariable(0).getName();
            BoolVar f0Var = (BoolVar) getVariable(model, f0);
            model.addClauseTrue(f0Var);

            Constraint constraint = new Constraint(f0 + " = true");
            constraint.addChocoConstraints(model, startIdx, model.getNbCstrs() - 1, false);

            this.setCorrectConstraints(Collections.singletonList(constraint));
        }

        // remove all Choco constraints
        model.unpost(model.getCstrs());

        LoggerUtils.outdent();
        log.debug("{}<<< Model {} initialized", LoggerUtils.tab(), getName());
    }

    /**
     * Translates user requirements to Choco constraints.
     */
    private void createUserRequirement() {
        for (UserRequirement userRequirement : combination.getUserRequirements()) {
            translator.translate(userRequirement, model);
        }
    }

    public Object clone() throws CloneNotSupportedException {
        FMModel clone = (FMModel) super.clone();

        clone.fmkb = new FMKB(featureModel, true);
        clone.model = clone.fmkb.getModelKB();

        clone.initialize();

        return clone;
    }

    @Override
    public void dispose() {
        super.dispose();
        model = null;
        featureModel = null;
        fmkb.dispose();
        fmkb = null;
        combination = null;
        translator = null;
    }
}
