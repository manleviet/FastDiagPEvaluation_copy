package at.tugraz.ist.ase.cacdr.core.io;

import at.tugraz.ist.ase.cacdr.core.UserRequirement;
import lombok.NonNull;

import java.util.List;

public interface IUserRequirementBuildable {
    List<UserRequirement> buildUserRequirement(@NonNull String stringUR);
}
