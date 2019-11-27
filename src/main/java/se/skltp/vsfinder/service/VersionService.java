package se.skltp.vsfinder.service;

import java.util.Map;

public interface VersionService {
    String compile(String environmentPath, String output);
    void runScript(String[] args, StringBuilder output);
    Map<String, String> parse(String path, boolean removeHeader);
    String getAppVersion(String appName, String environment);
}
