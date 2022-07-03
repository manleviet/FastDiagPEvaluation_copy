package at.tugraz.ist.ase.cacdr.core.translator;

import at.tugraz.ist.ase.cacdr.core.UserRequirement;
import org.chocosolver.solver.Model;

public interface IUserRequirementTranslatable {
    void translate(UserRequirement userRequirement, Model model);
}
