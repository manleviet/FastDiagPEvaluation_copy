package at.tugraz.ist.ase.cacdr.core.io;

import at.tugraz.ist.ase.cacdr.core.Combination;
import at.tugraz.ist.ase.cacdr.core.CombinationType;
import at.tugraz.ist.ase.common.LoggerUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Getter
public class CombinationListBuilder implements ICombinationListBuildable {

    protected int numFiles = 0;
    @Setter
    protected CombinationType type;
    @Setter
    protected ICombinationBuildable combinationBuilder;

    public CombinationListBuilder(CombinationType type, ICombinationBuildable combinationBuilder) {
        this.type = type;
        this.combinationBuilder = combinationBuilder;
    }

    @Override
    public List<Combination> buildCombinationList(@NonNull String path) throws IOException {
        log.trace("{}Building combination list from combination files in the folder [path={}] >>>", LoggerUtils.tab(), path);
        LoggerUtils.indent();

        File folder = new File(path);

        List<Combination> combsList = new LinkedList<>();
        if (folder.exists()) { // exists
            for (final File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.getName().contains(".da")) {
                    numFiles++;

                    log.trace("{}Reading file {}", LoggerUtils.tab(), file.getName());
                    List<Combination> comb = combinationBuilder.buildCombination(file);

                    combsList.addAll(comb);
                    log.trace("{}Added {} combinations to combination list", LoggerUtils.tab(), comb.size());
                }
            }
        } else {
            log.error("{}Folder {} does not exist!", LoggerUtils.tab(), path);
        }

        LoggerUtils.outdent();
        log.debug("{}<<< Built combination list [combList={}]", LoggerUtils.tab(), combsList);
        return combsList;
    }
}
