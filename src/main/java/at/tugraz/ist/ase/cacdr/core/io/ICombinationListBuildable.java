package at.tugraz.ist.ase.cacdr.core.io;

import at.tugraz.ist.ase.cacdr.core.Combination;
import lombok.NonNull;

import java.io.IOException;
import java.util.List;

public interface ICombinationListBuildable {
    List<Combination> buildCombinationList(@NonNull String path) throws IOException;
}
