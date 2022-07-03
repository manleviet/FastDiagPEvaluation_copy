package at.tugraz.ist.ase.cacdr.core;

import at.tugraz.ist.ase.kb.core.Constraint;
import at.tugraz.ist.ase.test.Assignment;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter @Setter
public class UserRequirement extends Assignment {
    private Constraint constraint;

    @Builder(builderMethodName = "userRequirementBuilder")
    public UserRequirement(@NonNull String variable, @NonNull String value, Constraint constraint) {
        super(variable, value);
        this.constraint = constraint;
    }

    public void dispose() {
        constraint = null;
    }
}
