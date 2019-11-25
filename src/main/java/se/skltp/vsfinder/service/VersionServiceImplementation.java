package se.skltp.vsfinder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.skltp.vsfinder.utilities.OperatingSystem;
import se.skltp.vsfinder.utilities.RunEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class VersionServiceImplementation implements VersionService {
    private static final Logger log = LoggerFactory.getLogger(VersionServiceImplementation.class);

    private String[] directoriesWithApps;   //= new String[]{"war", "apps", "muleapp"};
    private String
            outfile,
            pathToDirectories,
            pathToScript,
            script,
            targetOutput;

    public VersionServiceImplementation(@Value("${versionfinder.resulting.outfile}") String outfile,
                                        @Value("${versionfinder.directories.path}") String pathToDirectories,
                                        @Value("${versionfinder.directories.names}") String[] directoriesWithApps,
                                        @Value("${versionfinder.script.path}") String pathToScript,
                                        @Value("${versionfinder.script.name}") String script,
                                        @Value("${versionfinder.target.dir.out.name}") String targetOutput) {
        this.outfile = outfile;
        this.pathToDirectories = pathToDirectories;
        this.directoriesWithApps = directoriesWithApps;
        this.pathToScript = pathToScript;
        this.script = script;
        this.targetOutput = targetOutput;

        log();
    }

    /**
     * Formats a string to be used as a valid path
     *
     * @param parameter string representing a path
     * @return a path-friendly string with a forward slash in the end if one is not present
     */
    private String formatParameterToPath(String parameter) {
        if (parameter.charAt(parameter.length() - 1) != '/') {
            return parameter + "/";
        }
        return parameter;
    }

    /**
     * Calls on the script to be run in a given directory and outputs in another given directory.
     * Makes sure that all resulting files are compiled into one.
     *
     * @param environmentPath path to directory to run script in
     * @param targetOutput    path to directory in which to put resulting files from a script
     * @return message on whether operation was executed successfully or not
     */
    public String compile(String environmentPath, String targetOutput) {

        environmentPath = formatParameterToPath(environmentPath);

        //If path doesn't exist, return
        if (!(new File(pathToDirectories + environmentPath).exists())) {
            return "INVALID PATH: [" + pathToDirectories + environmentPath + "]";
        }

        //No specific output was given, use default
        if (targetOutput == null) {
            targetOutput = this.targetOutput;
        } else {
            targetOutput = this.targetOutput + formatParameterToPath(targetOutput);
        }

        //Create thread pool
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = new ForkJoinPool(availableProcessors);

        //Run script in every directory in parallel
        for (String dir : directoriesWithApps) {
            String finalTargetOutput = targetOutput;
            String finalEnvironmentPath = environmentPath;

            executorService.submit(() ->
                    runScript(new String[]{
                            pathToScript + script,
                            pathToDirectories + finalEnvironmentPath + dir,
                            finalTargetOutput + dir
                    }));
        }

        executorService.shutdown();

        try {
            //Wait 10 seconds to finish script executions
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return "Timeout Error running script";
        }

        //Merge resulting files to one and return message on whether merge was ok or not
        return mergeFiles(outfile, targetOutput) ? "Compile successful" : "Error merging files";
    }

    /**
     * Merges files specified in application properties into one
     *
     * @param outfileName  name of the resulting file
     * @param targetOutput location of the resulting file
     * @return whether or not the merge was successful
     * @throws IOException if files or paths doesn't exist
     */
    private boolean mergeFiles(String outfileName, String targetOutput) {
        BufferedReader br = null;

        //Output stream to file
        try (PrintWriter pw = new PrintWriter(targetOutput + outfileName)) {

            for (String file : directoriesWithApps) {
                File f = new File(targetOutput + file);
                br = new BufferedReader(new FileReader(f));

                //First line is date, remove it
                br.readLine();

                String line;

                //Print each read line to output file
                while ((line = br.readLine()) != null) {
                    pw.println(line);
                }

                pw.flush();
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;

        } finally {
            //Close input stream
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Runs a shell script.
     *
     * @param scriptArgs the array including the shell script to run and its parameters
     * @throws IOException          if the script fails to run or if the scripts path parameters are invalid paths
     * @throws InterruptedException if the process running the script is interrupted while it's waited for
     */
    public void runScript(String[] scriptArgs) {

        ProcessBuilder processBuilder;

        String[] processArgs = {};

        //If OS is Windows
        if (RunEnvironment.getOS() == OperatingSystem.WINDOWS) {
            log.debug("System: WINDOWS");

            processArgs = new String[scriptArgs.length + 2];
            processArgs[0] = "CMD";
            processArgs[1] = "/C";

            System.arraycopy(scriptArgs, 0, processArgs, 2, scriptArgs.length);

            // If OS is Linux
        } else if (RunEnvironment.getOS() == OperatingSystem.LINUX) {
            log.debug("System: LINUX");

            processArgs = new String[scriptArgs.length + 5];
            processArgs[0] = "sudo";
            processArgs[1] = "-u";
            processArgs[2] = "ine-app";
            processArgs[3] = "bash";
            processArgs[4] = "-c";

            System.arraycopy(scriptArgs, 0, processArgs, 5, scriptArgs.length);
//sudo -H -u ine-app bash -c 'bash /www/inera/home/ine-app/finder/script/versions.sh'
        }

        processBuilder = new ProcessBuilder(processArgs).inheritIO();

        log.debug("Command for run sh script: " + processBuilder.command());

        try {
            //Executing script starts a new native process
            Process process = processBuilder.start();

            //Wait for native process to finish before continuing
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetches the version of an app in specified directory.
     * Returns an error message if no such app exist.
     *
     * @param app         name of the app whose version to find
     * @param environment the environment on which the app is running
     * @return the version of the app
     */
    public String getAppVersion(String app, String environment) {
        String path = targetOutput + formatParameterToPath(environment) + outfile;
        String version = parse(path, false).get(app);
        return version != null ?
                version :
                "App: [" + app + "] or path [" + path + "] does not exit";
    }


    /**
     * Parses file and returns a map with its content.
     *
     * @param absolutePathToFile path to file to parse
     * @param removeHeader       if true, first line of the file is removed
     * @return a map representing the parsed file
     */
    public Map<String, String> parse(String absolutePathToFile, boolean removeHeader) {
        log.debug("File path: " + absolutePathToFile);

        HashMap<String, String> nameVersionMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(absolutePathToFile))) {
            String line;

            //First line is date
            if (removeHeader) {
                reader.readLine();
            }

            while ((line = reader.readLine()) != null) {
                //Index: 0=name, 1=version, 2=path
                String[] tokens = line.split("=");
                log.debug("[" + tokens[0] + " " + tokens[1] + "]");
                nameVersionMap.put(tokens[0], tokens[1]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return nameVersionMap;
    }


    private void log() {
        log.debug("outfile:" + outfile);
        log.debug("pathToDirectories: " + pathToDirectories);
        log.debug("directoriesWithApps: " + Arrays.toString(directoriesWithApps));
        log.debug("pathToScript: " + pathToScript);
        log.debug("script: " + script);
        log.debug("targetOutput: " + targetOutput);
    }


}
