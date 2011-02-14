/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.BitSet;
import org.ala.spatial.analysis.index.SpeciesIndex;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.Tile;

/**
 *
 * @author Adam
 */
public class ShapeIntersectionService {
    //use OccurrenceIndex points gridding

    static ArrayList<ShapeGridContainer> lookupData = new ArrayList<ShapeGridContainer>();

    public static void init() {
        String intersectionsConfigFile = TabulationSettings.shape_intersection_files;

        try {
            BufferedReader r = new BufferedReader(new FileReader(intersectionsConfigFile));

            String s;
            while ((s = r.readLine()) != null) {
                if (s.length() > 0) {
                    String[] words = s.split(",");

                    String append = "";
                    if (words.length > 3) {
                        append = s.substring(words[0].length() + words[1].length() + words[2].length() + 3);
                    }

                    try {
                        if ((new File(words[0] + ".shp")).exists()
                                || (new File(words[0])).exists()) {
                            ShapeGridContainer sgc = new ShapeGridContainer(words[0], words[1], words[2], append);
                            lookupData.add(sgc);
                            System.out.println("loaded: " + words[0]);
                        } else {
                            System.out.println("no shape file at: " + words[0]);
                        }
                    } catch (Exception e) {
                        if (words != null && words.length > 0) {
                            System.out.println("failed to load: " + words[0]);
                        }
                        e.printStackTrace();
                    }
                }
            }

            r.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int[] getIntersections(SimpleRegion region) {
        ShapeGridContainer sgc = new ShapeGridContainer(region);

        //if any cells overlap, include
        int[] intersections = new int[lookupData.size()];
        int pos = 0;
        for (int i = 0; i < lookupData.size(); i++) {
            if (lookupData.get(i).getGrid().intersects(sgc.getGrid())) {
                intersections[pos] = i;
                pos++;
            }
        }

        if (pos > 0) {
            return java.util.Arrays.copyOf(intersections, pos);
        }

        return null;
    }

    public static String[] convertToLsids(int[] intersections) {
        String[] lsids = new String[intersections.length];
        for (int i = 0; i < intersections.length; i++) {
            lsids[i] = lookupData.get(intersections[i]).getLsid();
        }
        return lsids;
    }

    public static String convertToString(int[] intersections) {
        if (intersections == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < intersections.length; i++) {
            sb.append(lookupData.get(intersections[i]).getLsid());
            sb.append(",");
            sb.append(lookupData.get(intersections[i]).getData());
            if (i + 1 < intersections.length) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * list LSID + WMS Get info for each layer
     *
     * WMS may be '/' delimited for multiple
     * @return
     */
    public static String list() {
        ArrayList<String> a = new ArrayList<String>();
        for (int i = 0; i < lookupData.size(); i++) {
            String lsid = lookupData.get(i).getLsid();
            if (lsid != null && lsid.length() > 0) {
                //append if it is already in the list
                int j = 0;
                for (j = 0; j < a.size(); j++) {
                    if (a.get(j).startsWith(lsid)) {
                        a.set(j, a.get(j) + "\t" + lookupData.get(i).getWMSGet());
                        break;
                    }
                }
                //add it if it was not in the list already
                if (j == a.size()) {
                    a.add(lsid + "," + lookupData.get(i).getWMSGet());
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.size(); i++) {
            sb.append(a.get(i));
            if (i + 1 < a.size()) {
                sb.append("\n");
            }

        }

        return sb.toString();
    }
    
    
    public static String getHeader() {
        String header = null;
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(TabulationSettings.shape_intersection_files));
            String s = br.readLine();
            //remove first 2 columns (path to shape file, species name, wms get url)
            //attached 'lsid' as first column
            int p = s.indexOf(','); //first ','
            p = s.indexOf(',',p+1); //2nd ','
            p = s.indexOf(',',p+1); //3rd ','
            header = "LSID," + s.substring(p+1);
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return header;
    }
}

class ShapeGridContainer {

    BitSet grid;
    String speciesName;
    String lsid;
    String data;
    String wmsGet;

    public ShapeGridContainer(SimpleRegion region) {
        byte[][] mask = new byte[720][720];
        int[][] cells = region.getOverlapGridCells(-180, -90, 180, 90, 720, 720, mask);

        lsid = null;
        speciesName = null;
        data = null;

        grid = maskToBitSet(mask);
    }

    public String getLsid() {
        if ((lsid == null || lsid.length() == 0) && speciesName != null) {
            lsid = SpeciesIndex.getLSID(speciesName);
        }
        return lsid;
    }

    public String getWMSGet() {
        return wmsGet;
    }

    public String getData() {
        return data;
    }

    public ShapeGridContainer(String shapeFileName, String speciesName, String wmsGet, String data) {
        this.speciesName = speciesName;
        this.data = data;
        this.wmsGet = wmsGet;

        SimpleShapeFile ssf = new SimpleShapeFile(shapeFileName);

        Tile[] tiles = ssf.getTileList(0, -180, -90, 180, 90, 720, 720);

        grid = tilesToBitSet(tiles);
    }

    BitSet maskToBitSet(byte[][] mask) {
        int len1 = mask.length;
        int len2 = mask[0].length;

        BitSet bs = new BitSet(len1 * len2);

        for (int i = 0; i < len1; i++) {
            for (int j = 0; j < len2; j++) {
                if (mask[i][j] == SimpleRegion.GI_FULLY_PRESENT
                        || mask[i][j] == SimpleRegion.GI_PARTIALLY_PRESENT) {
                    //to align with Tile: (height - 1 - i) * width + j)
                    bs.set((len1 - 1 - i) * len2 + j);
                }
            }
        }

        return bs;
    }

    private BitSet tilesToBitSet(Tile[] tiles) {
        int len1 = 720;
        int len2 = 720;

        BitSet bs = new BitSet(len1 * len2);

        for (int i = 0; i < tiles.length; i++) {
            bs.set(tiles[i].pos_);
        }

        return bs;
    }

    public BitSet getGrid() {
        return grid;
    }
}
