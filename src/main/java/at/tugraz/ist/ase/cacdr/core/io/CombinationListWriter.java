package at.tugraz.ist.ase.cacdr.core.io;

import at.tugraz.ist.ase.cacdr.core.Combination;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CombinationListWriter extends BufferedWriter {

    public CombinationListWriter(String filename) throws IOException {
        super(new FileWriter(filename));
    }

    public void write(List<Combination> combsList, List<Integer> selectedList, int numSelectedCombs) throws IOException {
        int cc = 0;
        double running = 0;

        for (Integer index: selectedList) {
            Combination comb = combsList.get(index);

            cc += comb.getNumCC();
            running += comb.getRuntime();
        }

        this.write( ((double)cc / numSelectedCombs) + " " + (running / numSelectedCombs) + "\n");
        for (Integer index: selectedList) {
            Combination comb = combsList.get(index);

            this.write(comb.toString() + "\n");
        }
    }
}
