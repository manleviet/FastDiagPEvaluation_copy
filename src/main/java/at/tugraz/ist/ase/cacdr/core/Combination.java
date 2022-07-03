package at.tugraz.ist.ase.cacdr.core;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Builder
public class Combination implements Comparable<Combination> {
    private String filename;
    private String path;

    private int cardCombination; // card of the combination
    private int indexCombination1; // index of combination
    private int indexCombination2; // index of value combination

    private String id;
    @Getter
    private boolean isConsistent;

    @Getter
    private int cardCD; // cardinality of the conflict/diagnosis
    @Getter
    private double runtime; // runtime of the conflict/diagnosis identification
    @Getter
    private int numCC; // number of consistency checks

    private String filenameCD;
    @Getter
    private String combination;
    @Getter
    private List<UserRequirement> userRequirements;

    /***
     * @return a string: [filename] - [#card] - [indexCombination1] - [id] - [combs] - [#conflictset] - [#time] - [#cc]
     *
     * #card: the cardinality of combination
     * #conflictset: the cardinality of conflict set
     * #time: the running time
     * #cc: number of consistency check
     */
    @Override
    public String toString() {
        return filename + " - " + cardCombination + " - " + indexCombination1 + " - " + id + " - "
                + combination + " - " + cardCD + " - " + runtime + " - " + numCC
                + " - " + filenameCD;
    }

    @Override
    public int compareTo(Combination o) {
        if (this.cardCD > o.cardCD) {
            return 1;
        } else if (this.cardCD < o.cardCD) {
            return -1;
        } else {
            if (this.numCC > o.numCC) { // number of consistency checks
                return 1;
            } else if (this.numCC < o.numCC) {
                return -1;
            } else {
                if (this.runtime > o.runtime) { // runtime
                    return 1;
                } else if (this.runtime < o.runtime) {
                    return -1;
                }
            }
        }
        return 0;

//        if (this.runningtime > o.runningtime) {
//            return 1;
//        } else if (this.runningtime < o.runningtime) {
//            return -1;
//        } else {
//            if (this.cardCS > o.cardCS) {
//                return 1;
//            } else if (this.cardCS < o.cardCS) {
//                return -1;
//            } else {
//                if (this.numCC > o.numCC) {
//                    return 1;
//                } else if (this.numCC < o.numCC) {
//                    return -1;
//                }
//            }
//        }
//        return 0;
    }

    public void dispose() {
        userRequirements = null;
    }
}
