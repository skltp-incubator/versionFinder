package se.skltp.vsfinder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.skltp.vsfinder.utilities.OperatingSystem;
import se.skltp.vsfinder.utilities.RunEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VersionServiceImplementation implements VersionService {
    private static final Logger log = LoggerFactory.getLogger(VersionServiceImplementation.class);

    private String[] directoriesWithApps;   //= new String[]{"war", "apps", "muleapp"};
    private String[] terminalArguments;
    private String[] environments;          // = new String[]{"dev", "test", "verifiering", "prod"};
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
                                        @Value("${versionfinder.target.dir.out.name}") String targetOutput,
                                        @Value("${versionfinder.terminal.arguments}") String[] terminalArguments,
                                        @Value("${versionfinder.directories.environments}") String[] environments) {
        this.outfile = outfile;
        this.pathToDirectories = pathToDirectories;
        this.directoriesWithApps = directoriesWithApps;
        this.pathToScript = pathToScript;
        this.script = script;
        this.targetOutput = targetOutput;
        this.terminalArguments = terminalArguments;
        this.environments = environments;

        log();
    }

    /**
     * Formats a string to be used as a valid path
     *
     * @param parameter string representing a path
     * @return a path-friendly string with a forward slash in the end if one is not present
     */
    private String formatParameterToPath(String parameter) {
        if (parameter.length() > 0 && parameter.charAt(parameter.length() - 1) != '/') {
            return parameter + "/";
        }
        return parameter;
    }

    /**
     * Calls on the script to be run in a given directory and outputs in another given directory.
     * Makes sure that all resulting files are compiled into one.
     *
     * @return message on whether operation was executed successfully or not
     */
    public String compile() {

//        environmentPath = formatParameterToPath(environmentPath);

        //If path doesn't exist, return


        //No specific output was given, use default
//        if (targetOutput == null) {
//            targetOutput = this.targetOutput;
//        } else {
//            targetOutput = this.targetOutput + formatParameterToPath(targetOutput);
//        }

        boolean isSuccessful = false;

        StringBuilder output; // = new StringBuilder();

        for (String env : environments) {
            output = new StringBuilder();

            for (String dir : directoriesWithApps) {
                runScript(new String[]{
                        pathToScript + script,
                        pathToDirectories + formatParameterToPath(env) + dir,
                        targetOutput + dir
                }, output);
            }

            isSuccessful = writeToFile(targetOutput + env, output);
            if (!isSuccessful) {
                return "Error merging files. See error log file for more info.";
            }
        }

        return "Compile successful";
    }

    private boolean pathExist(String path) {
        if (!(new File(path).exists())) {
            log.error("Error invalid path: " + path);
            return false;
        }
        return true;
    }

    /**
     * @param outfile file to be written to
     * @param output  StringBuilder containing content to be written to a file
     * @return true if everything is written to the file
     */
    private boolean writeToFile(String outfile, StringBuilder output) {
//        if (!pathExist(outfile)) {
//            return false;
//        }

        File file = new File(outfile);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.append(output);
            return true;
        } catch (IOException e) {
            log.error("Error writing to file. Caught Exception: ", e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Runs a shell script.
     *
     * @param scriptArgs the array including the shell script to run and its parameters
     * @throws IOException          if the script fails to run or if the scripts path parameters are invalid paths
     * @throws InterruptedException if the process running the script is interrupted while it's waited for
     */
    public void runScript(String[] scriptArgs, StringBuilder output) {

        ProcessBuilder processBuilder;

        //If OS is Windows
        if (RunEnvironment.getOS() == OperatingSystem.WINDOWS) {
            log.debug("Running script on operating system: WINDOWS");

            // If OS is Linux
        } else if (RunEnvironment.getOS() == OperatingSystem.LINUX) {
            log.debug("Running script on operating system: LINUX");
        }

        String scriptPaths = String.join(" ", scriptArgs);

        ArrayList<String> processArgs = new ArrayList<String>(Arrays.asList(terminalArguments));
        processArgs.add(scriptPaths);

        logList(processArgs);

        String[] args = processArgs.toArray(new String[processArgs.size()]);

        processBuilder = new ProcessBuilder(args);

        log.debug("Command for run sh script: " + processBuilder.command());

        try {
            //Executing script starts a new native process
            Process process = processBuilder.start();

            //Wait for native process to finish before continuing
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.debug("Result running script successful with exit code: " + exitCode);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    log.debug("Read output: " + line);
                    output.append(line);
                    output.append(System.getProperty("line.separator"));
                }

            } else {
                log.debug("Bad exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error running script. Caught exception: ", e);
            e.printStackTrace();
        }
    }

    private void logList(List<String> processArgs) {
        StringBuilder builder = new StringBuilder();
        processArgs.forEach(builder::append);
        log.debug("PROCESS ARGUMENTS: " + builder.toString());
    }
    //-------

    /**
     * __________ NOT USED __________
     * <p>
     * Merges files specified in application properties into one
     *
     * @param outfileName  name of the resulting file
     * @param targetOutput location of the resulting file
     * @return whether or not the merge was successful
     * @throws IOException if files or paths doesn't exist
     */
    @SuppressWarnings("unused")
    private boolean mergeFiles(String outfileName, String targetOutput) {
        BufferedReader br = null;

        boolean result;

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

            result = true;

        } catch (IOException e) {
            log.error("Error merging files. Caught exception: ", e);
            e.printStackTrace();
            result = false;

        } finally {
            //Close input stream
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.error("Error closing Buffered Reader. Caught exception: ", e);
                    e.printStackTrace();
                }
            }
        }
        return result;
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

    @Override
    public Map<String, Map<String, String>> getAppOnEnv(String app, String environment) {
        Map<String, String> appsAndVersions = parse(targetOutput + environment, false);
        appsAndVersions = filterMap(app, appsAndVersions);

        Map<String, Map<String, String>> envAppVsMap = new HashMap<>();
        envAppVsMap.put(environment, appsAndVersions);

        return envAppVsMap;
    }

    @Override
    public Map<String, Map<String, String>> getAppOnAllEnvs(String app) {
        Map<String, Map<String, String>> envAppVersionMap = new HashMap<>();
        for (String environment : environments) {
            Map<String, String> appsAndVersions = parse(targetOutput + environment, false);

            Map<String, String> map = filterMap(app, appsAndVersions);

            envAppVersionMap.put(environment, map);
        }
        return envAppVersionMap;
    }

    @Override
    public Map<String, Map<String, String>> getAllAppsOnAllEnvs() {
        Map<String, Map<String, String>> allAppsAllEnvs = new HashMap<>();

        Arrays.stream(environments).forEach(environment ->
                allAppsAllEnvs.put(environment, parse(targetOutput + environment, false))
        );

        return allAppsAllEnvs;
    }

    /**
     * Filter a map and leave only relevant entry with the relevant key
     *
     * @param app             the key to be kept when filtered
     * @param appsAndVersions the map to be filtered
     * @return a Map with only one (or zero) entry left
     */
    private Map<String, String> filterMap(String app, Map<String, String> appsAndVersions) {
        return appsAndVersions.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals(app))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

                //Makes sure to ignore possible dates
                if (tokens.length >= 2) {
                    log.debug("[" + tokens[0] + " " + tokens[1] + "]");
                    nameVersionMap.put(tokens[0], tokens[1]);
                } else {
                    log.debug("Date: " + Arrays.toString(tokens));
                }
            }

        } catch (IOException e) {
            log.error("Error parsing file. Caught exception: ", e);
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
        log.debug("terminalArguments" + Arrays.toString(terminalArguments));
    }
}
