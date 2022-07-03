package at.tugraz.ist.ase.cacdr.core.io;

import at.tugraz.ist.ase.cacdr.core.Combination;
import at.tugraz.ist.ase.common.LoggerUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Getter
public class ScenarioBuilder implements ICombinationListBuildable {

    @Setter
    protected ICombinationBuildable combinationBuilder;

    public ScenarioBuilder(ICombinationBuildable combinationBuilder) {
        this.combinationBuilder = combinationBuilder;
    }

    @Override
    public List<Combination> buildCombinationList(@NonNull String path) throws IOException {
        log.trace("{}Building combination list from scenario file [file={}] >>>", LoggerUtils.tab(), path);
        LoggerUtils.indent();

        File file = new File(path);

        List<Combination> combsList = null;
        if (file.exists()) { // exists
            log.trace("{}Reading file {}", LoggerUtils.tab(), file.getName());
            combsList = combinationBuilder.buildCombination(file);
            log.trace("{}Added {} combinations to combination list", LoggerUtils.tab(), combsList.size());
        } else {
            log.error("{}File {} does not exist!", LoggerUtils.tab(), path);
        }

        LoggerUtils.outdent();
        log.debug("{}<<< Built combination list [combList={}]", LoggerUtils.tab(), combsList);
        return combsList;
    }
}
