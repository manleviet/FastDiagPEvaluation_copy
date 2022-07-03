package at.tugraz.ist.ase.cacdr.core.io;

import at.tugraz.ist.ase.cacdr.core.Combination;
import at.tugraz.ist.ase.cacdr.core.CombinationType;
import at.tugraz.ist.ase.cacdr.core.UserRequirement;
import at.tugraz.ist.ase.common.LoggerUtils;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static at.tugraz.ist.ase.cacdr.core.CombinationType.CONFLICT;

@Slf4j
@Getter @Setter
public class CombinationBuilder implements ICombinationBuildable {

    protected CombinationType type;
    protected IUserRequirementBuildable userRequirementBuilder;

    public CombinationBuilder(CombinationType type, IUserRequirementBuildable userRequirementBuilder) {
        this.type = type;
        this.userRequirementBuilder = userRequirementBuilder;
    }

    @Override
    public List<Combination> buildCombination(@NonNull File file) throws IOException {
        log.trace("{}Building combination list from [file={}] >>>", LoggerUtils.tab(), file.getName());
        LoggerUtils.indent();

        @Cleanup InputStream is = new FileInputStream(file);
        @Cleanup BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        br.readLine(); // omit first line

        // Read all combinations
        List<Combination> combsList = br.lines().map(line -> {
                    try {
                        return buildCombination(file, line);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(comb -> !comb.isConsistent() && comb.getCardCD() > 0)
                .collect(Collectors.toCollection(LinkedList::new));

        LoggerUtils.outdent();
        log.debug("{}<<< Built combination list from [file={}, combsList={}]", LoggerUtils.tab(), file.getName(), combsList);
        return combsList;
    }

    @Override
    public Combination buildCombination(@NonNull File file, @NonNull String line) throws IOException {
        log.trace("{}Building combination from [line={}] >>>", LoggerUtils.tab(), line);

        String filename = file.getName();
        String path = file.getParent();
        int cardCombination = extractCardCombination(filename);
        int indexCombination1 = extractIndexOfFirstCombinationSelection(filename);

        String[] tokens = line.split(" - ");

        String id = tokens[0];
        String combination = tokens[1]; // need to split
        List<UserRequirement> userRequirement = userRequirementBuilder.buildUserRequirement(combination);
        boolean isConsistent = !tokens[2].equals("INCONSISTENT");

        int cardCD = 0;
        double runtime = 0;
        int numCC = 0;
        File fileCD = null;
        if (!isConsistent) {
            if (tokens.length > 3) {
                cardCD = Integer.parseInt(tokens[3]);
                runtime = Double.parseDouble(tokens[4]);
            }

            // create the filename which contains information of the conflict/diagnosis set
//            fileCD = getPathToCDFile(path, filename, id);
            fileCD = new File(tokens[7]);
            // open and read file which contains information of the conflict set
//            numCC = readNumConsistencyChecks(fileCD);
            numCC = Integer.parseInt(tokens[5]);
        }

        Combination comb = Combination.builder()
                .filename(filename)
                .path(path)
                .cardCombination(cardCombination)
                .indexCombination1(indexCombination1)
                .id(id)
                .combination(combination)
                .userRequirements(userRequirement)
                .isConsistent(isConsistent)
                .cardCD(cardCD)
                .runtime(runtime)
                .filenameCD(fileCD == null ? "" : fileCD.getName())
                .numCC(numCC)
                .build();

        log.debug("{}<<< Built combination [combination={}]", LoggerUtils.tab(), comb);
        return comb;
    }

    private int extractCardCombination(String filename) {
        int index1 = filename.indexOf("_");
        int index2 = filename.indexOf("_", index1 + 1);
        return Integer.parseInt(filename.substring(index1 + 1, index2));
    }

    private int extractIndexOfFirstCombinationSelection(String filename) {
        int index = filename.indexOf("_", filename.indexOf("_") + 1);
        return Integer.parseInt(filename.substring(index + 1, filename.length() - 3));
    }

//    private List<Assignment> parseUserRequirement(String ur) {
//        List<Assignment> userRequirement = new LinkedList<>();
//
//        String[] tokens = ur.split(",");
//
//        for (String token : tokens) {
//            String[] items = token.split("=");
//
//            String variable = items[0];
//            String value = items[1];
//
//            Assignment assignment = Assignment.builder().variable(variable).value(value).build();
//
//            userRequirement.add(assignment);
//        }
//        log.trace("{}Parsed a user requirement [ur={}]", LoggerUtils.tab(), userRequirement);
//        return userRequirement;
//    }

    private File getPathToCDFile(String path, String filename, String id) throws IOException {
        String filenameCD = (this.type == CONFLICT ? "conflict_" : "diagnosis_")
                + filename.substring(6, filename.length() - 3) + "_" + id + ".da";
        String folderCD = this.type == CONFLICT ? "conflict" : "diagnosis";

        File file = new File(path + "/" + folderCD + "/" + filenameCD);

        if (!file.exists()) {
            throw new IOException("File " + file.getAbsolutePath() + " does not exist");
        }

        return file;
    }

    private int readNumConsistencyChecks(File file) throws IOException {
        // Open file
        @Cleanup BufferedReader reader = new BufferedReader(new FileReader(file));

        for (int i = 0; i < 7; i++) {
            reader.readLine(); // read and pass six lines
        }

        // Read the seventh line
        String line = reader.readLine();
        int index = line.indexOf(":");

        return Integer.parseInt(line.substring(index + 1).trim());
    }
}
