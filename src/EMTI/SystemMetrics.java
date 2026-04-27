package EMTI;

public final class SystemMetrics {

    private SystemMetrics() {
    }

    public static String getMemoryInfo() {
        return nro.server.SystemMetrics.getMemoryInfo();
    }

    public static String getCpuInfo() {
        return nro.server.SystemMetrics.getCpuInfo();
    }

    public static String getSystemMetrics() {
        return nro.server.SystemMetrics.getSystemMetrics();
    }

    public static String ToString() {
        return getSystemMetrics();
    }
}
