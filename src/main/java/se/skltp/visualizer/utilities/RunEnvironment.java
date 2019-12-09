package se.skltp.visualizer.utilities;

public class RunEnvironment {

    private static OperatingSystem currentOS = null;

    public static OperatingSystem getOS() {
        if (currentOS == null) {
            currentOS = runningOS();
        }
        return currentOS;
    }

    private static OperatingSystem runningOS() {
        String os = System.getProperty("os.name").toUpperCase();

        if (os.contains("WIN")) {
            return OperatingSystem.WINDOWS;
        } else if (os.contains("MAC")) {
            return OperatingSystem.MAC;
        } else if (os.contains("NIX") || os.contains("NUX") || os.contains("AIX")) {
            return OperatingSystem.LINUX;
        } else {
            throw new RuntimeException("Unknown hosting OS");
        }
    }
}
