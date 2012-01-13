/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.web.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Iterator;
import javax.servlet.http.HttpServletResponse;
import org.ala.spatial.util.TabulationSettings;
import org.ala.spatial.util.Zipper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/download/")
public class DownloadController {

//    @RequestMapping(value = "/{pid}", method = RequestMethod.GET)
//    @ResponseBody
//    public String download(@PathVariable String pid, HttpServletResponse response) {
//        try {
//
//            File dir = findFile(pid);
//
//            if (dir != null) {
//                //System.out.println("Found session data: " + dir.getAbsolutePath());
//                //return "Found session data: " + dir.getAbsolutePath();
//
//                String parentName = "ALA_";
//                String parentPath = dir.getParent().substring(dir.getParent().lastIndexOf("/") + 1);
//
//                String zipfile = dir.getParent() + "/" + pid + ".zip";
//                Zipper.zipDirectory(dir.getAbsolutePath(), zipfile);
//
//                System.out.println("Found " + dir.getName() + " in " + dir.getParent() + " and zipped at: " + zipfile);
//                //return "Found " + dir.getName() + " in " + dir.getParent() + " and zipped at: " + zipfile;
//
//                if ("maxent".equals(parentPath)) {
//                    parentName = "ALA_Prediction_";
//                } else if ("sampling".equals(parentPath)) {
//                    parentName = "ALA_Species_Samples_";
//                } else if ("layers".equals(parentPath) || "aloc".equals(parentPath)) {
//                    parentName = "ALA_Classification_";
//                } else if ("gdm".equals(parentPath)) {
//                    parentName = "ALA_GDM_";
//                } else if ("filtering".equals(parentPath)) {
//                    parentName = "ALA_EnvFilter_";
//                } else if ("sitesbyspecies".equals(parentPath)) {
//                    parentName = "ALA_SitesBySpecies_";
//                }
//
//                File file = new File(zipfile);
//                response.setContentType("application/zip");
//                response.setContentLength(safeLongToInt(file.length()));
//                response.setHeader("Content-Disposition", "attachment; filename=\"" + parentName + pid + ".zip\"");
//
//                FileCopyUtils.copy(new FileInputStream(file), response.getOutputStream());
//
//                return null;
//
//            } else {
//                System.out.println("Could not find session data");
//                return "Could not find session data";
//            }
//
//        } catch (Exception e) {
//            System.out.println("Unable to download:");
//            e.printStackTrace(System.out);
//        }
//
//        return "";
//    }

    private File findFile(String pid) {
        try {
            System.out.println("Looking for: " + pid + " to downloadd");

            // the 'pid's are unique, so lets figure out
            // which directory they live under.

            String basedir = TabulationSettings.base_output_dir + File.separator + "output" + File.separator;
            File baseDir = new File(basedir);
            FilenameFilter ff = DirectoryFileFilter.DIRECTORY;
            File[] files = baseDir.listFiles(ff);
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                if (f.isDirectory() && !f.getName().equalsIgnoreCase("layers")) {
                    if (f.getName().equalsIgnoreCase(pid)) {
                        return f;
                    } else {
                        File[] files2 = f.listFiles(ff);
                        for (int j = 0; j < files2.length; j++) {
                            File f2 = files2[j];
                            if (f2.getName().equalsIgnoreCase(pid)) {
                                return f2;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error finding session data:");
            e.printStackTrace(System.out);
        }

        return null;
    }

    private static void fixMaxentFiles(String pid, File dir) {
        try {
            File tmpdir = new File(dir.getParent() + "/temp/" + pid);
            //FileCopyUtils.copy(new FileInputStream(dir), new FileOutputStream(tmpdir));
            FileUtils.copyDirectory(dir, tmpdir);

            File grd = new File(tmpdir.getAbsolutePath() + "/" + pid + ".grd");
            File grdnew = new File(tmpdir.getAbsolutePath() + "/prediction.grd");

            File gri = new File(tmpdir.getAbsolutePath() + "/" + pid + ".gri");
            File grinew = new File(tmpdir.getAbsolutePath() + "/prediction.gri");

            File rsp = new File(tmpdir.getAbsolutePath() + "/removedSpecies.txt");
            File rspnew = new File(tmpdir.getAbsolutePath() + "/prediction_removedSpecies.txt");

            File msp = new File(tmpdir.getAbsolutePath() + "/maskedOutSensitiveSpecies.txt");
            File mspnew = new File(tmpdir.getAbsolutePath() + "/prediction_maskedOutSensitiveSpecies.txt");

            grd.renameTo(grdnew);
            gri.renameTo(grinew);
            rsp.renameTo(rspnew);
            msp.renameTo(mspnew);

            (new File(tmpdir.getAbsolutePath() + "/species.zip")).delete();

        } catch (Exception e) {
            System.out.println("Unable to fix Prediction files");
            e.printStackTrace(System.out);
        }
    }

    private static void fixAlocFiles(String pid, File dir) {
        try {
            File tmpdir = new File(dir.getParent() + "/temp/" + pid);
            //FileCopyUtils.copy(new FileInputStream(dir), new FileOutputStream(tmpdir));
            FileUtils.copyDirectory(dir, tmpdir);

            File grd = new File(tmpdir.getAbsolutePath() + "/" + pid + ".grd");
            File grdnew = new File(tmpdir.getAbsolutePath() + "/classfication.grd");

            File gri = new File(tmpdir.getAbsolutePath() + "/" + pid + ".gri");
            File grinew = new File(tmpdir.getAbsolutePath() + "/classfication.gri");

            File asc = new File(tmpdir.getAbsolutePath() + "/" + pid + ".asc");
            File ascnew = new File(tmpdir.getAbsolutePath() + "/classfication.asc");

            File prj = new File(tmpdir.getAbsolutePath() + "/" + pid + ".prj");
            File prjnew = new File(tmpdir.getAbsolutePath() + "/classfication.prj");

            File png = new File(tmpdir.getAbsolutePath() + "/aloc.png");
            File pngnew = new File(tmpdir.getAbsolutePath() + "/classfication.png");

            File pgw = new File(tmpdir.getAbsolutePath() + "/aloc.pgw");
            File pgwnew = new File(tmpdir.getAbsolutePath() + "/classfication.pgw");

            grd.renameTo(grdnew);
            gri.renameTo(grinew);
            asc.renameTo(ascnew);
            prj.renameTo(prjnew);
            png.renameTo(pngnew);
            pgw.renameTo(pgwnew);

            (new File(tmpdir.getAbsolutePath() + "/" + pid + ".asc.zip")).delete();
            (new File(tmpdir.getAbsolutePath() + "/" + pid + ".sld")).delete();
            (new File(tmpdir.getAbsolutePath() + "/aloc.pngextents.txt")).delete();
            (new File(tmpdir.getAbsolutePath() + "/aloc.prj")).delete();
            (new File(tmpdir.getAbsolutePath() + "/t_aloc.tif")).delete();
            (new File(tmpdir.getAbsolutePath() + "/t_aloc.png")).delete();

        } catch (Exception e) {
            System.out.println("Unable to fix Classification files");
            e.printStackTrace(System.out);
        }
    }

    private static File sfindFile(String pid) {
        try {
            System.out.println("Looking for: " + pid + " to download");

            // the 'pid's are unique, so lets figure out
            // which directory they live under.

            String basedir = "/data/ala/runtime/output" + File.separator;
            File baseDir = new File(basedir);
            FilenameFilter ff = DirectoryFileFilter.DIRECTORY;
            File[] files = baseDir.listFiles(ff);
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                if (f.isDirectory() && !f.getName().equalsIgnoreCase("layers")) {
                    if (f.getName().equalsIgnoreCase(pid)) {
                        return f;
                    } else {
                        File[] files2 = f.listFiles(ff);
                        for (int j = 0; j < files2.length; j++) {
                            File f2 = files2[j];
                            if (f2.getName().equalsIgnoreCase(pid)) {
                                return f2;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error finding session data:");
            e.printStackTrace(System.out);
        }

        return null;
    }

    private int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public static void main(String[] args) {

        // maxent - 1323641423144
        // aloc   - 1323844048457

        String pid = "1323844048457";

        try {
            File dir = sfindFile(pid);

            if (dir != null) {
                //System.out.println("Found session data: " + dir.getAbsolutePath());
                //return "Found session data: " + dir.getAbsolutePath();

                String parentName = "ALA_";
                String parentPath = dir.getParent().substring(dir.getParent().lastIndexOf("/") + 1);

                String zipfile = dir.getParent() + "/" + pid + ".zip";

                if ("maxent".equals(parentPath)) {
                    fixMaxentFiles(pid, dir);
                    Zipper.zipDirectory(dir.getParent()+"/temp/"+pid, zipfile);
                } else if ("layers".equals(parentPath) || "aloc".equals(parentPath)) {
                    fixAlocFiles(pid, dir);
                    Zipper.zipDirectory(dir.getParent()+"/temp/"+pid, zipfile);
                } else {
                    Zipper.zipDirectory(dir.getAbsolutePath(), zipfile);
                }

                

                System.out.println("Found " + dir.getName() + " in " + dir.getParent() + " and zipped at: " + zipfile);
                //return "Found " + dir.getName() + " in " + dir.getParent() + " and zipped at: " + zipfile;

                if ("maxent".equals(parentPath)) {
                    parentName = "ALA_Prediction_";
                } else if ("sampling".equals(parentPath)) {
                    parentName = "ALA_Species_Samples_";
                } else if ("layers".equals(parentPath) || "aloc".equals(parentPath)) {
                    parentName = "ALA_Classification_";
                } else if ("gdm".equals(parentPath)) {
                    parentName = "ALA_GDM_";
                } else if ("filtering".equals(parentPath)) {
                    parentName = "ALA_EnvFilter_";
                } else if ("sitesbyspecies".equals(parentPath)) {
                    parentName = "ALA_SitesBySpecies_";
                }

                File file = new File(zipfile);
//                response.setContentType("application/zip");
//                response.setContentLength(safeLongToInt(file.length()));
//                response.setHeader("Content-Disposition", "attachment; filename=\"" + parentName + pid + ".zip\"");

//                FileCopyUtils.copy(new FileInputStream(file), response.getOutputStream());

//                return null;

                System.out.println("File generated: " + file.getAbsolutePath());

            } else {
                System.out.println("Could not find session data");
                //return "Could not find session data";
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
