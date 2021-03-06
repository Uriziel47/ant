/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.TeeOutputStream;

/**
 * stress out java task
 * */
public class JavaTest extends BuildFileTest {

    private static final int TIME_TO_WAIT = 1;
    // wait 1 second extra to allow for java to start ...
    // this time was OK on a Win NT machine and on nagoya
    private static final int SECURITY_MARGIN = 2000;

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private boolean runFatalTests=false;

    public JavaTest(String name) {
        super(name);
    }

    /**
     * configure the project.
     * if the property junit.run.fatal.tests is set we run
     * the fatal tests
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/java.xml");

        //final String propname="tests-classpath.value";
        //String testClasspath=System.getProperty(propname);
        //System.out.println("Test cp="+testClasspath);
        String runFatal=System.getProperty("junit.run.fatal.tests");
        if(runFatal!=null)
            runFatalTests=true;
    }

    public void tearDown() {
        // remove log file from testSpawn
        project.executeTarget("cleanup");
    }

    public void testNoJarNoClassname(){
        expectBuildExceptionContaining("testNoJarNoClassname",
            "parameter validation",
            "Classname must not be null.");
    }

    public void testJarNoFork() {
        expectBuildExceptionContaining("testJarNoFork",
            "parameter validation",
            "Cannot execute a jar in non-forked mode. "
                + "Please set fork='true'. ");
    }

    public void testJarAndClassName() {
        expectBuildException("testJarAndClassName",
            "Should not be able to set both classname AND jar");
    }


    public void testClassnameAndJar() {
        expectBuildException("testClassnameAndJar",
            "Should not be able to set both classname AND jar");
    }

    public void testRun() {
        executeTarget("testRun");
    }



    /** this test fails but we ignore the return value;
     *  we verify that failure only matters when failonerror is set
     */
    public void testRunFail() {
        if(runFatalTests) {
            executeTarget("testRunFail");
        }
    }

    public void testRunFailFoe() {
        if(runFatalTests) {
            expectBuildExceptionContaining("testRunFailFoe",
                "java failures being propagated",
                "Java returned:");
        }
}

    public void testRunFailFoeFork() {
        expectBuildExceptionContaining("testRunFailFoeFork",
            "java failures being propagated",
            "Java returned:");
    }

    public void testExcepting() {
        expectLogContaining("testExcepting",
                            "Exception raised inside called program");
    }

    public void testExceptingFork() {
        expectLogContaining("testExceptingFork",
                            "Java Result:");
    }

    public void testExceptingFoe() {
        expectBuildExceptionContaining("testExceptingFoe",
            "passes exception through",
            "Exception raised inside called program");
    }

    public void testExceptingFoeFork() {
        expectBuildExceptionContaining("testExceptingFoeFork",
            "exceptions turned into error codes",
            "Java returned:");
    }

    public void testResultPropertyZero() {
        executeTarget("testResultPropertyZero");
        assertEquals("0",project.getProperty("exitcode"));
    }

    public void testResultPropertyNonZero() {
        executeTarget("testResultPropertyNonZero");
        assertEquals("2",project.getProperty("exitcode"));
    }

    public void testResultPropertyZeroNoFork() {
        executeTarget("testResultPropertyZeroNoFork");
        assertEquals("0",project.getProperty("exitcode"));
    }

    public void testResultPropertyNonZeroNoFork() {
        executeTarget("testResultPropertyNonZeroNoFork");
         assertEquals("-1",project.getProperty("exitcode"));
     }

    public void testRunFailWithFailOnError() {
        expectBuildExceptionContaining("testRunFailWithFailOnError",
            "non zero return code",
            "Java returned:");
    }

    public void testRunSuccessWithFailOnError() {
        executeTarget("testRunSuccessWithFailOnError");
    }

    public void testSpawn() {
        File logFile = FILE_UTILS.createTempFile("spawn","log", project.getBaseDir(), false, false);
        // this is guaranteed by FileUtils#createTempFile
        assertTrue("log file not existing", !logFile.exists());
        project.setProperty("logFile", logFile.getAbsolutePath());
        project.setProperty("timeToWait", Long.toString(TIME_TO_WAIT));
        project.executeTarget("testSpawn");
        try {
            Thread.sleep(TIME_TO_WAIT * 1000 + SECURITY_MARGIN);
        } catch (Exception ex) {
            System.out.println("my sleep was interrupted");
        }
        // let's be nice with the next generation of developers
        if (!logFile.exists()) {
            System.out.println("suggestion: increase the constant"
            + " SECURITY_MARGIN to give more time for java to start.");
        }
        assertTrue("log file exists", logFile.exists());
    }

    public void testRedirect1() {
        executeTarget("redirect1");
    }

    public void testRedirect2() {
        executeTarget("redirect2");
    }

    public void testRedirect3() {
        executeTarget("redirect3");
    }

    public void testRedirector1() {
        executeTarget("redirector1");
    }

    public void testRedirector2() {
        executeTarget("redirector2");
    }

    public void testReleasedInput() throws Exception {
        PipedOutputStream out = new PipedOutputStream();
        final PipedInputStream in = new PipedInputStream(out);
        project.setInputHandler(new DefaultInputHandler() {
            protected InputStream getInputStream() {
                return in;
            }
        });
        project.setDefaultInputStream(in);

        Java java = new Java();
        java.setProject(project);
        java.setClassname("org.apache.tools.ant.Main");
        java.setArgs("-version");
        java.setFork(true);
        // note: due to the missing classpath it will fail, but the input stream
        // reader will be read
        java.execute();

        Thread inputThread = new Thread(new Runnable() {
            public void run() {
                Input input = new Input();
                input.setProject(project);
                input.setAddproperty("input.value");
                input.execute();
            }
        });
        inputThread.start();

        // wait a little bit for the task to wait for input
        Thread.sleep(100);

        // write some stuff in the input stream to be catched by the input task
        out.write("foo\n".getBytes());
        out.flush();
        out.write("bar\n".getBytes());
        out.flush();

        inputThread.join(2000);

        assertEquals("foo", project.getProperty("input.value"));
    }

    public void testFlushedInput() throws Exception {
        final PipedOutputStream out = new PipedOutputStream();
        final PipedInputStream in = new PipedInputStream(out);
        project.setInputHandler(new DefaultInputHandler() {
            protected InputStream getInputStream() {
                return in;
            }
        });
        project.setDefaultInputStream(in);

        final boolean[] timeout = new boolean[1];
        timeout[0] = false;

        Thread writingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    // wait a little bit to have the target executed
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // don't care
                }
                try {
                    out.write("foo-FlushedInput\n".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        writingThread.setDaemon(true);

        writingThread.start();
        executeTarget("flushedInput");
    }

    /**
     * entry point class with no dependencies other
     * than normal JRE runtime
     */
    public static class EntryPoint {

    /**
     * this entry point is used by the java.xml tests to
     * generate failure strings to handle
     * argv[0] = exit code (optional)
     * argv[1] = string to print to System.out (optional)
     * argv[1] = string to print to System.err (optional)
     */
        public static void main(String[] argv) {
            int exitCode=0;
            if(argv.length>0) {
                try {
                    exitCode=Integer.parseInt(argv[0]);
                } catch(NumberFormatException nfe) {
                    exitCode=-1;
                }
            }
            if(argv.length>1) {
                System.out.println(argv[1]);
            }
            if(argv.length>2) {
                System.err.println(argv[2]);
            }
            if(exitCode!=0) {
                System.exit(exitCode);
            }
        }
    }

    /**
     * entry point class with no dependencies other
     * than normal JRE runtime
     */
    public static class ExceptingEntryPoint {

        /**
         * throw a run time exception which does not need
         * to be in the signature of the entry point
         */
        public static void main(String[] argv) {
            throw new NullPointerException("Exception raised inside called program");
        }
    }
    /**
     * test class for spawn
     */
    public static class SpawnEntryPoint {
        public static void main(String [] argv) {
            int sleepTime = 10;
            String logFile = "spawn.log";
            if (argv.length >= 1) {
                sleepTime = Integer.parseInt(argv[0]);
            }
            if (argv.length >= 2)
            {
                logFile = argv[1];
            }
            OutputStreamWriter out = null;
            try {
                Thread.sleep(sleepTime * 1000);
            } catch (InterruptedException ex) {
                System.out.println("my sleep was interrupted");
            }

            try {
                File dest = new File(logFile);
                FileOutputStream fos = new FileOutputStream(dest);
                out = new OutputStreamWriter(fos);
                out.write("bye bye\n");
            } catch (Exception ex) {}
            finally {
                try {out.close();} catch (IOException ioe) {}}

        }
    }

    /**
     * entry point class to pipe System.in to the specified stream:
     * "out", "err", or "both".  If none specified, swallow the input.
     */
    public static class PipeEntryPoint {

        /**
         * pipe input to specified output
         */
        public static void main(String[] args) {
            OutputStream os = null;
            if (args.length > 0) {
                if ("out".equalsIgnoreCase(args[0])) {
                    os = System.out;
                } else if ("err".equalsIgnoreCase(args[0])) {
                    os = System.err;
                } else if ("both".equalsIgnoreCase(args[0])) {
                    os = new TeeOutputStream(System.out, System.err);
                }
            }
            if (os != null) {
                Thread t = new Thread(new StreamPumper(System.in, os, true));
                t.setName("PipeEntryPoint " + args[0]);
                t.start();
                try {
                    t.join();
                } catch (InterruptedException eyeEx) {
                }
            }
        }
    }

    public static class ReadPoint {
        public static void main(String[] args) throws IOException {
            String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
            System.out.println(line);
        }
    }

}
