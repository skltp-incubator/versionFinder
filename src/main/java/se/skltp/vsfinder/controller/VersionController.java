package se.skltp.vsfinder.controller;

import se.skltp.vsfinder.service.VersionService;
import org.springframework.web.bind.annotation.*;

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
     *
     * @return result message on whether it was a successful run or not
     */
    @RequestMapping(value = "applications/run_script", method = RequestMethod.GET)
    public String compileAllVersions(
            @RequestParam(value = "where", required = false) String where,
            @RequestParam(value = "output", required = false) String output) {

        if (where == null) {
            where = "inera/";
        }
        return versionService.compile(where, output);
    }


    //****************************vettig*************************************************
    @RequestMapping(value = "applications", method = RequestMethod.GET)
    public String getAppOnEnv(
            @RequestParam(value = "env", required = false) String environment,
            @RequestParam(value = "app", required = true) String app) {

        if (environment == null) {
            environment = "/";
        }
        return environment + " + " + app + " : " + versionService.getAppVersion(app, environment);
    }


    //@Param whatArg should be 'allversions' for all app names and versions
    @RequestMapping(value = "applications/alljson/{whatFile}", method = RequestMethod.GET)
    public Map<String, String> getAllAppsAndVersionsForEnvironment(@PathVariable String whatFile) {
        return versionService.parse(whatFile, false);
    }

}
