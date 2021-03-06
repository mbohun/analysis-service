/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.spatial.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the analysis queue. Maximum number of analysis jobs running at the
 * same time is set by limit.jobs in alaspatial.properties.
 * <p/>
 * Has two threads, a job consumer and a finished job monitor.
 * <p/>
 * All analysis jobs are serialised into workingdir set in alaspatial.properties
 * as files with the name "JOB" + analysisId.
 * <p/>
 * All analysis jobs extend AnalysisJob.
 * <p/>
 * Use:
 * <code>AlaysisQueue.addJob(analysisJob);</code>
 *
 * @author Adam
 */
public class AnalysisQueue {

    static LinkedBlockingQueue<AnalysisJob> jobs = new LinkedBlockingQueue<AnalysisJob>();
    static ConcurrentHashMap<String, AnalysisJob> runningJobs = new ConcurrentHashMap<String, AnalysisJob>();
    static ConcurrentHashMap<String, AnalysisJob> finishedJobs = new ConcurrentHashMap<String, AnalysisJob>();
    static AnalysisJobConsumer analysisJobConsumer = null;
    static AnalysisJobFinishedConsumer analysisJobFinishedConsumer = null;
    static boolean initDone = false;

    public static void init() {
        if (initDone) {
            return;
        }
        initDone = true;

        //search for saved jobs to load
        File f = new File(AlaspatialProperties.getAnalysisWorkingDir());
        String[] js = f.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith("JOB");
            }
        });

        for (String j : js) {
            try {
                String name = j.substring(j.lastIndexOf("JOB") + 3);
                AnalysisJob jb = getSavedJob(name);
                jb.setName(name);

                //addJob(jb);
                finishedJobs.put(jb.getName(), jb);
            } catch (Exception e) {
                System.out.println("failed to load " + j);
            }
        }
    }

    public static void cancelJob(String pid) {
        AnalysisJob job = getJob(pid);
        if (job != null && !job.isFinished()) {
            job.setCurrentState(AnalysisJob.CANCELLED);
        }
        //don't wait for it to finish.
    }

    public static void addJob(AnalysisJob job) {
        init();

        try {
            if (job.isFinished()) {
                finishedJobs.put(job.getName(), job);
            } else {
                jobs.put(job);
            }
        } catch (Exception ex) {
            Logger.getLogger(AnalysisQueue.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (analysisJobConsumer == null) {
            analysisJobConsumer = new AnalysisJobConsumer(jobs, runningJobs, finishedJobs);
            analysisJobConsumer.start();
            analysisJobFinishedConsumer = new AnalysisJobFinishedConsumer(jobs, runningJobs, finishedJobs);
            analysisJobFinishedConsumer.start();
        }
        saveJob(job);
    }

    public static boolean removeJob(String pid) {
        AnalysisJob j = getJob(pid);
        if (j != null) {
            if (!jobs.remove(j)) {
                j.cancel();
                runningJobs.remove(j);
            }
            return true;
        }
        return false;
    }

    public static long getEstimate(String pid) {
        AnalysisJob j = getJob(pid);
        if (j != null) {
            return j.getEstimate();
        }
        return -1;
    }

    public static String getStatus(String pid) {
        init();

        String msg = "";

        long queuePosition = getQueuePosition(pid);
        if (queuePosition >= 0) {
            msg += "In Queue: " + (queuePosition + 1) + " of " + jobs.size() + "\r\n";

            long queueEstimate = getQueueEstimate(pid);
            if (queueEstimate > 0) {
                msg += "Est to start: " + (queueEstimate / 1000) + " s\r\n";
            }
        }

        String jobMsg = getJobStatus(pid);
        if (jobMsg != null) {
            msg += jobMsg;
        }

        return msg;
    }

    static AnalysisJob getJob(String pid) {
        //check in waiting jobs
        for (AnalysisJob j : jobs) {
            if (j.getName().equals(pid)) {
                return j;
            }
        }
        //check in running jobs
        for (Entry<String, AnalysisJob> e : runningJobs.entrySet()) {
            if (e.getValue().getName().equals(pid)) {
                return e.getValue();
            }
        }
        //check in finished jobs
        for (Entry<String, AnalysisJob> e : finishedJobs.entrySet()) {
            if (e.getValue().getName().equals(pid)) {
                return e.getValue();
            }
        }
        //check in saved jobs, move to finished jobs here
        AnalysisJob j = getSavedJob(pid);
        if (j != null) {
            finishedJobs.put(j.getName(), j);
        }
        return j;
    }

    public static String getJobStatus(String pid) {
        init();

        AnalysisJob j = getJob(pid);
        if (j != null) {
            String msg = "";
            int qp = getQueuePosition(pid);
            if (qp >= 0) {
                msg = "in queue: " + (qp + 1) + " of " + jobs.size() + ", ";
            }
            return msg + j.getStatus();
        }
        return null;
    }

    public static int getQueuePosition(String pid) {
        int position = 0;
        for (AnalysisJob j : jobs) {
            if (j.getName().equals(pid)) {
                return position;
            }
            position++;
        }
        return -1;
    }

    public static String getLog(String pid) {
        init();

        AnalysisJob j = getJob(pid);
        if (j != null) {
            return j.getLog();
        }
        return null;
    }

    public static String getProgress(String pid) {
        init();

        AnalysisJob j = getJob(pid);
        if (j != null) {
            return String.valueOf(j.getProgress());
        }
        return null;
    }

    public static String getState(String pid) {
        init();

        AnalysisJob j = getJob(pid);
        if (j != null) {
            return j.getCurrentState();
        }
        return null;
    }

    public static String getMessage(String pid) {
        init();

        AnalysisJob j = getJob(pid);
        if (j != null) {
            return j.getMessage();
        }
        return null;
    }

    private static long getQueueEstimate(String pid) {
        long estimate = 0;
        for (AnalysisJob j : jobs) {
            if (j.getName().equals(pid)) {
                break;
            }
            estimate += j.getEstimate();
        }
        for (Entry<String, AnalysisJob> e : runningJobs.entrySet()) {
            estimate += e.getValue().getEstimate();
        }
        return estimate;
    }

    public static void saveJob(AnalysisJob j) {
        try {
            FileOutputStream fos = new FileOutputStream(
                    AlaspatialProperties.getAnalysisWorkingDir()
                            + "JOB" + j.getName());
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(j);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static AnalysisJob getSavedJob(String pid) {
        AnalysisJob j = null;
        try {
            FileInputStream fis = new FileInputStream(
                    AlaspatialProperties.getAnalysisWorkingDir()
                            + "JOB" + pid);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            try {
                j = (AnalysisJob) ois.readObject();
            } finally {
                ois.close();
            }
        } catch (Exception e) {
            //  e.printStackTrace();
        }
        return j;
    }

    public static String listFinished() {
        init();

        StringBuffer sb = new StringBuffer();

        for (Entry<String, AnalysisJob> e : finishedJobs.entrySet()) {
            sb.append(e.getValue().toString()).append("\n");
        }

        return sb.toString();
    }

    public static String listRunning() {
        init();

        StringBuffer sb = new StringBuffer();

        for (Entry<String, AnalysisJob> e : runningJobs.entrySet()) {
            sb.append(e.getValue().toString()).append("\n");
        }

        return sb.toString();
    }

    public static String listWaiting() {
        init();

        StringBuffer sb = new StringBuffer();

        for (AnalysisJob j : jobs) {
            sb.append(j.toString()).append("\n");
        }

        return sb.toString();
    }

    public static String getInputs(String pid) {
        init();

        AnalysisJob j = getJob(pid);
        if (j != null) {
            return j.getInputs();
        }
        return null;
    }

    public static String getImage(String pid) {
        init();

        AnalysisJob j = getJob(pid);
        if (j != null) {
            return j.getImage();
        }
        return null;
    }

    public static String copy(String pid) {
        AnalysisJob j = getJob(pid);
        if (j != null) {
            //  AnalysisJob jcopy = j.copy();
            //  if(jcopy != null){
            //      addJob(jcopy);
            //      return jcopy.getName();
            //  }
        }
        return null;
    }
}

class AnalysisJobConsumer extends Thread {

    LinkedBlockingQueue<AnalysisJob> jobs;
    ConcurrentHashMap<String, AnalysisJob> runningJobs;
    ConcurrentHashMap<String, AnalysisJob> finishedJobs;

    public AnalysisJobConsumer(LinkedBlockingQueue<AnalysisJob> jobs_, ConcurrentHashMap<String, AnalysisJob> runningJobs_, ConcurrentHashMap<String, AnalysisJob> finishedJobs_) {
        jobs = jobs_;
        runningJobs = runningJobs_;
        finishedJobs = finishedJobs_;

        setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        while (true) {
            try {
                while (runningJobs.size() >= AlaspatialProperties.GetAnalysisMaxJobs()) {
                    Thread.sleep(1000);
                }
                AnalysisJob j = jobs.take();
                runningJobs.put(j.getName(), j);
                j.start();
            } catch (InterruptedException ex) {
                Logger.getLogger(AnalysisJobConsumer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    void removeFinishedJobs() {
        for (Entry<String, AnalysisJob> e : runningJobs.entrySet()) {
            if (!e.getValue().isAlive()) {
                runningJobs.remove(e.getValue());
                finishedJobs.put(e.getValue().getName(), e.getValue());

                //save finished job
                AnalysisQueue.saveJob(e.getValue());
            }
        }
    }
}

class AnalysisJobFinishedConsumer extends Thread {

    LinkedBlockingQueue<AnalysisJob> jobs;
    ConcurrentHashMap<String, AnalysisJob> runningJobs;
    ConcurrentHashMap<String, AnalysisJob> finishedJobs;

    public AnalysisJobFinishedConsumer(LinkedBlockingQueue<AnalysisJob> jobs_, ConcurrentHashMap<String, AnalysisJob> runningJobs_, ConcurrentHashMap<String, AnalysisJob> finishedJobs_) {
        jobs = jobs_;
        runningJobs = runningJobs_;
        finishedJobs = finishedJobs_;

        setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (runningJobs.size() >= 0) {
                    removeFinishedJobs();
                }
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(AnalysisJobConsumer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    void removeFinishedJobs() {
        for (Entry<String, AnalysisJob> e : runningJobs.entrySet()) {
            if (!e.getValue().isAlive()) {
                if (!((AnalysisJob) e.getValue()).isFinished()) {
                    ((AnalysisJob) e.getValue()).setCurrentState(AnalysisJob.FAILED);
                }
                runningJobs.remove(e.getValue().getName());
                finishedJobs.put(e.getValue().getName(), e.getValue());

                //save finished job
                AnalysisQueue.saveJob(e.getValue());
            }
        }
    }
}
