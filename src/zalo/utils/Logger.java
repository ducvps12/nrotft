/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

public class Logger {

    private Context ctx;

    public Logger(Context ctx) {
        this.ctx = ctx;
    }

    public void verbose(String... args) {
        if (ctx.getOptions() != null && ctx.getOptions().isLogging()) {
            System.out.print("\u001B[35m VERBOSE\u001B[0m ");
            for (String arg : args) {
                System.out.print(arg + " ");
            }
            System.out.println();
        }
    }

    public void info(String... args) {
        if (ctx.getOptions() != null && ctx.getOptions().isLogging()) {
            System.out.print("\u001B[34mINFO\u001B[0m ");
            for (String arg : args) {
                System.out.print(arg + " ");
            }
            System.out.println();
        }
    }

    public void warn(String... args) {
        if (ctx.getOptions() != null && ctx.getOptions().isLogging()) {
            System.out.print("\u001B[33mWARN\u001B[0m ");
            for (String arg : args) {
                System.out.print(arg + " ");
            }
            System.out.println();
        }
    }

    public void error(String... args) {
        if (ctx.getOptions() != null && ctx.getOptions().isLogging()) {
            System.out.print("\u001B[31mERROR\u001B[0m ");
            for (String arg : args) {
                System.out.print(arg + " ");
            }
            System.out.println();
        }
    }

    public void error(String message, Throwable error) {
        if (ctx.getOptions() != null && ctx.getOptions().isLogging()) {
            System.out.print("\u001B[31mERROR\u001B[0m " + message + ": ");
            error.printStackTrace();
        }
    }

    public void success(String... args) {
        if (ctx.getOptions() != null && ctx.getOptions().isLogging()) {
            System.out.print("\u001B[32mSUCCESS\u001B[0m ");
            for (String arg : args) {
                System.out.print(arg + " ");
            }
            System.out.println();
        }
    }
}
