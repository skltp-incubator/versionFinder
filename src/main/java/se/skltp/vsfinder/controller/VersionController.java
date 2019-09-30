package se.skltp.vsfinder.controller;

import se.skltp.vsfinder.service.VersionService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/vs/")
public class VersionController {

    private VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    /**
     * Runs script on each directory containing applications defined in properties.
     * Then compiles all output files into one
     * @return result message on whether it was a successful run or not
     */
    @RequestMapping(value = "applications/run_script", method = RequestMethod.GET)
    public String compileAllVersions(
            @RequestParam(value = "where", required = true) String where,
            @RequestParam(value = "output", required = false) String output) {
        return versionService.compile(where, output);
    }


    //****************************vettig*************************************************
    @RequestMapping(value = "applications", method = RequestMethod.GET)
    public String getAppOnEnv(
            @RequestParam(value = "env", required = true) String environment,
            @RequestParam(value = "app", required = true) String app) {

        return environment + " + " + app + " : " + versionService.getAppVersion(app, environment);
    }

    //********************************ej vettig******************************************
//    static int counter = 0;
//
//    @RequestMapping(value = "applications/test", method = RequestMethod.GET)
//    public String testRunSh(
//            @RequestParam(value = "script", required = true) String script,
//            @RequestParam(value = "path", required = true) String path) {
//
//        versionService.runScript(new String[]{path+script, "" + ++counter});
//        return script + " + " + path + " : ";
//    }

    //@Param whatArg should be 'allversions' for all app names and versions
    @RequestMapping(value = "applications/alljson/{whatFile}", method = RequestMethod.GET)
    public Map<String, String> getAllAppsAndVersionsForEnvironment(@PathVariable String whatFile) {
        return versionService.parse(whatFile, false);
    }

//    //Asso ... venne vad denna ska användas för riktigt?
//    @RequestMapping(value = "applications/{dirname}/{appName}", method = RequestMethod.GET)
//    public String getVersionOfApp(@PathVariable String dirname, @PathVariable String appName) {
//        return versionService.getAppVersion(dirname, appName);
//    }

    @RequestMapping(value = "applications/all", method = RequestMethod.GET)
    public HashMap<String, String> getAll() {
        return (HashMap<String, String>) versionService.testAll();
    }
}
