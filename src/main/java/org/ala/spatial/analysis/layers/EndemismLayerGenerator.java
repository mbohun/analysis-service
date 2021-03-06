package org.ala.spatial.analysis.layers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.apache.commons.math3.util.Pair;

public class EndemismLayerGenerator extends CalculatedLayerGenerator {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("args[0]=Resolution in degrees, e.g. 0.1 for 0.1 by 0.1 degree cells\n"
                    + "args[1]=Path to species cell count file (should be generated by biocache store via jenkins - file resolution must match the resolution provided to this tool)\n"
                    + "args[2]=Path to cell species list file (should be generated by biocache store via jenkins - file resolution must match the resolution provided to this tool)\n"
                    + "args[3]=Path of directory in which to write output files\n"
                    + "args[4]=Prefix to use for names of output files.\n");

            return;
        }

        BigDecimal resolution = new BigDecimal(args[0]).setScale(2);
        File speciesCellCountFile = new File(args[1]);
        File cellSpeciesFile = new File(args[2]);
        File outputFileDirectory = new File(args[3]);
        String outputFileNamePrefix = args[4];

        new EndemismLayerGenerator(resolution, speciesCellCountFile, cellSpeciesFile).writeGrid(outputFileDirectory, outputFileNamePrefix);
    }

    public EndemismLayerGenerator(BigDecimal resolution, File speciesCellCountFile, File cellSpeciesFile) throws IOException {
        super(resolution);
        readSpeciesCellCounts(speciesCellCountFile);
        readCellSpeciesLists(cellSpeciesFile);
    }

    @Override
    protected float handleCell(Pair<BigDecimal, BigDecimal> coordPair, float maxValue, PrintWriter ascPrintWriter, BufferedOutputStream divaOutputStream) throws IOException {
        if (_cellSpecies.containsKey(coordPair)) {
            // Calculate endemism value for the cell. Sum (1 / total
            // species cell count) for each species that occurs in
            // the cell. Then divide by the number of species that
            // occur in the cell.

            float endemicityValue = 0;
            List<String> speciesLsids = _cellSpecies.get(coordPair);
            for (String lsid : speciesLsids) {
                int speciesCellCount = _speciesCellCounts.get(lsid);
                endemicityValue += 1.0 / speciesCellCount;
            }
            endemicityValue = endemicityValue / speciesLsids.size();

            float newMaxValue = 0;
            if (maxValue < endemicityValue) {
                newMaxValue = endemicityValue;
            } else {
                newMaxValue = maxValue;
            }

            ascPrintWriter.print(endemicityValue);

            ByteBuffer bb = ByteBuffer.wrap(new byte[Float.SIZE / Byte.SIZE]);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putFloat(endemicityValue);
            divaOutputStream.write(bb.array());

            return newMaxValue;
        } else {
            // No species occurrences in this cell. Endemism value
            // is zero.
            ascPrintWriter.print("0");

            ByteBuffer bb = ByteBuffer.wrap(new byte[Float.SIZE / Byte.SIZE]);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putFloat(0);
            divaOutputStream.write(bb.array());
            return maxValue;
        }
    }
}
