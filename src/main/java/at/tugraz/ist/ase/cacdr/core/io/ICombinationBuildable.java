package at.tugraz.ist.ase.cacdr.core.io;

import at.tugraz.ist.ase.cacdr.core.Combination;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ICombinationBuildable {
    List<Combination> buildCombination(@NonNull File file) throws IOException;

    Combination buildCombination(@NonNull File file, @NonNull String line) throws IOException;
}
