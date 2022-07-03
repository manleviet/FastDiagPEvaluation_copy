package at.tugraz.ist.ase.cacdr.core.io;

import at.tugraz.ist.ase.cacdr.core.Combination;
import at.tugraz.ist.ase.cacdr.core.CombinationType;
import at.tugraz.ist.ase.cacdr.core.UserRequirement;
import at.tugraz.ist.ase.common.LoggerUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j
public class ScenarioCombinationBuilder extends CombinationBuilder {

    public ScenarioCombinationBuilder(CombinationType type, IUserRequirementBuildable userRequirementBuilder) {
        super(type, userRequirementBuilder);
    }

    @Override
    public Combination buildCombination(@NonNull File file, @NonNull String line) {
        log.trace("{}Building combination from [line={}] >>>", LoggerUtils.tab(), line);

        String[] tokens = line.split(" - ");

        String filename = tokens[0];
        int cardCombination = Integer.parseInt(tokens[1]);
        int indexCombination1 = Integer.parseInt(tokens[2]);

        String id = tokens[3];
        String combination = tokens[4]; // need to split
        List<UserRequirement> userRequirement = userRequirementBuilder.buildUserRequirement(combination);
        boolean isConsistent = false;

        int cardCD = Integer.parseInt(tokens[5]);
        double runtime = Double.parseDouble(tokens[6]);
        int numCC = Integer.parseInt(tokens[7]);
        String fileCD = tokens[8];

        Combination comb = Combination.builder()
                .filename(filename)
                .cardCombination(cardCombination)
                .indexCombination1(indexCombination1)
                .id(id)
                .combination(combination)
                .userRequirements(userRequirement)
                .isConsistent(isConsistent)
                .cardCD(cardCD)
                .runtime(runtime)
                .filenameCD(fileCD)
                .numCC(numCC)
                .build();

        log.debug("{}<<< Built combination [combination={}]", LoggerUtils.tab(), comb);
        return comb;
    }
}
