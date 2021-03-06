/*
 * Copyright 2015 Stuart Scott
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import frontend.Category;
import frontend.Lexem;
import frontend.Lexer;
import frontend.Parser;

public class Match implements IMatch {

    public static final String MATCH = "match";
    public static final List<Lexem> LEXEMS = new ArrayList<Lexem>();
    static {
        LEXEMS.add(new Lexem(Category.NEWLINE, "\n"));
        LEXEMS.add(new Lexem(Category.WHITESPACE, "\\s"));
        LEXEMS.add(new Lexem(Category.ASSIGN, "="));
        LEXEMS.add(new Lexem(Category.COMMENT, "#.*\n"));
        LEXEMS.add(new Lexem(Category.ORB, "\\("));
        LEXEMS.add(new Lexem(Category.CRB, "\\)"));
        LEXEMS.add(new Lexem(Category.OSB, "\\["));
        LEXEMS.add(new Lexem(Category.CSB, "\\]"));
        LEXEMS.add(new Lexem(Category.STRING_LITERAL, "\".*\""));
        LEXEMS.add(new Lexem(Category.UPPER_CASE, "[A-Z][_a-zA-Z0-9]*"));
        LEXEMS.add(new Lexem(Category.LOWER_CASE, "[a-z][_a-zA-Z0-9]*"));
    }

    private File mRoot;
    private Map<String, String> mProperties = new ConcurrentHashMap<String, String>();
    private Map<String, CountDownLatch> mFiles = new ConcurrentHashMap<String, CountDownLatch>();
    private final List<File> mMatchFiles = new ArrayList<File>();
    private final List<File> mAllFiles = new ArrayList<File>();
    public boolean mQuiet = false;

    public Match(File root) {
        mRoot = root;
    }

    List<File> getAllFiles() {
        return mAllFiles;
    }

    /**
     * {inheritDoc}
     */
    @Override
    public String getProperty(String key) {
        String property = mProperties.get(key);
        if (property == null) {
            error(String.format("no targets set property %s", key));
        }
        return property;
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void setProperty(String key, String value) {
        mProperties.put(key, value);
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void addFile(String file) {
        mFiles.put(file, new CountDownLatch(1));
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void provideFile(String file) {
        CountDownLatch latch = mFiles.get(file);
        if (latch == null) {
            error(String.format("provideFile called before addFile for %s", file));
        }
        latch.countDown();
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void awaitFile(String file) {
        CountDownLatch latch = mFiles.get(file);
        if (latch == null) {
            error(String.format("no targets provided %s", file));
        }
        try {
            latch.await();
        } catch(InterruptedException e) {
            error("target interrupted");
        }
    }

    private void scanRoot(File root) {
        for (File child : root.listFiles()) {
            String name = child.getName();
            if (!name.matches("\\..*") && !name.equals("out")) {
                if (child.isDirectory()) {
                    loadFiles(child);
                } else {
                    addScannedFile(child);
                }
            }
        }
    }

    private void loadFiles(File directory) {
        for (File child : directory.listFiles()) {
            if (child.isDirectory()) {
                loadFiles(child);
            } else {
                addScannedFile(child);
            }
        }
    }

    private void addScannedFile(File file) {
        mAllFiles.add(file);
        if (file.getName().equals(MATCH)) {
            mMatchFiles.add(file);
        }
    }

    void light() {
        long start = System.currentTimeMillis();
        println("Scanning");
        scanRoot(mRoot);
        println("Parsing");
        List<ITarget> targets = new ArrayList<ITarget>();
        for (File match : mMatchFiles) {
            Lexer lexer = new Lexer(this, LEXEMS, match);
            Parser parser = new Parser(this, lexer);
            targets.addAll(parser.parse());
        }
        println("Configuring");
        // Create a thread for each target, but only start a thread if the number of targets that
        // aren't blocked is under MAX_THREADS. If all targets are blocked there is a deadlock.
        for (File file : mAllFiles) {
            String full = file.toString();
            addFile(full);
            provideFile(full);
        }
        for (ITarget target : targets) {
            target.configure();
        }
        println("Building");
        CountDownLatch latch = new CountDownLatch(targets.size());
        for (ITarget target : targets) {
            new BuildThread(target, latch).start();
        }
        try {
            latch.await();
        } catch(InterruptedException e) {
            error("build interrupted");
        }
        long delta = (System.currentTimeMillis() - start) / 1000;
        long hours = delta / 3600;
        long minutes = (delta % 3600) / 60;
        long seconds = (delta % 60);
        String message = null;
        if (hours > 0) {
            message = String.format("Done %dh:%02dm:%02ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            message = String.format("Done %dm:%02ds", minutes, seconds);
        } else {
            message = String.format("Done %ds", seconds);
        }
        println(message);
        // Create a thread for each target, but only start a thread if the number of targets that
        // aren't blocked is under MAX_THREADS. If all targets are blocked there is a deadlock.
        // Look at the output files of a target and all the files under the output directory,
        // delete files that were created in the last build but is no longer made by any targets.
        // This means all targets have to know their output files even if they dont need to build.
        // This is difficult for java compiles because you cannot know beforehand, given source
        // files, which classes will get generated because of inner/anonymous classes.
        // Could maybe be done by a target - it just gets built last.
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void runCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"/bin/bash", "-c", command});
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            int result = process.waitFor();
            String line = "";
            boolean loop = true;
            while (loop) {
                loop = false;
                if ((line = input.readLine()) != null) {
                    println(line);
                    loop = true;
                }
                if ((line = error.readLine()) != null) {
                    if (result != 0) {
                        println(String.format("error: %s", line));
                    }
                    loop = true;
                }
            }
            if (result != 0) {
                error("error: " + command);
            }
        } catch (Exception e) {
            error(e);
        }
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void warn(String message) {
        println(String.format("warning: %s", message));
    }

    /**
     * {inheritDoc}
     */
    @Override
    public synchronized void println(String message) {
        if (!mQuiet) {
            System.out.println(message);
        }
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void error(String message) {
        throw new RuntimeException(message);
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void error(Exception exception) {
        throw new RuntimeException(exception);
    }

    public static void main(String args[]) {
        File root = new File(args[0]);
        Match match = new Match(root);
        match.light();
    }

    public static class BuildThread extends Thread {

        private final ITarget mTarget;
        private final CountDownLatch mLatch;

        private BuildThread(ITarget target, CountDownLatch latch) {
            mTarget = target;
            mLatch = latch;
        }

        @Override
        public void run() {
            try {
                mTarget.build();
            } finally {
                mLatch.countDown();
            }
        }
    }
}
