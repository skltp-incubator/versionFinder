package se.skltp.visualizer.controller;

import se.skltp.visualizer.service.VersionService;
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
    public String compileAllVersions() {
        return versionService.compile();
    }

    @RequestMapping(value = "applications", method = RequestMethod.GET)
    public Map<String, Map<String, String>> getAllAppsOnAllEnvironments(
            @RequestParam(value = "env", required = false) String environment,
            @RequestParam(value = "app", required = false) String app) {

//        if (environment == null) {
//            environment = "/";
//        }

        if (app != null && environment != null) {
            return versionService.getAppOnEnv(app, environment);

        } else if (app != null && environment == null) {
            return versionService.getAppOnAllEnvs(app);

        } else {
            return versionService.getAllAppsOnAllEnvs();
            //return environment + " + " + app + " : " + versionService.getAppVersion(app, environment);
        }
    }
}
