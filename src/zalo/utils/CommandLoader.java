package zalo.utils;

/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */

import zalo.interfaces.Command;
import java.io.File;
import java.net.URL;
import java.util.*;

public class CommandLoader {

    private static final String COMMANDS_PACKAGE = "zalo.commands";

    public static Map<String, Command> loadCommands() {
        return loadCommandsFromPackage(COMMANDS_PACKAGE);
    }

    public static Map<String, Command> loadCommandsFromPackage(String packageName) {
        Map<String, Command> commands = new HashMap<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            URL resource = classLoader.getResource(path);

            if (resource == null) {
                System.err.println("Package not found: " + packageName);
                return commands;
            }

            File packageDir = new File(resource.getFile());
            if (!packageDir.exists() || !packageDir.isDirectory()) {
                System.err.println("Package directory not found: " + packageName);
                return commands;
            }

            File[] files = packageDir.listFiles();
            if (files == null) {
                return commands;
            }

            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = file.getName().replace(".class", "");
                    String fullClassName = packageName + "." + className;

                    try {
                        Class<?> clazz = Class.forName(fullClassName);
                        if (Command.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                            Command cmd = (Command) clazz.getDeclaredConstructor().newInstance();
                            String key = cmd.getName().toLowerCase();
                            commands.put(key, cmd);
                            System.out.println(className.toUpperCase() + " | LOADED");
                        }
                    } catch (ClassNotFoundException e) {
                        System.err.println(className.toUpperCase() + " | CLASS NOT FOUND: " + fullClassName);
                    } catch (Exception e) {
                        System.err.println(className.toUpperCase() + " | ERROR: " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("LOAD COMMANDS FROM PACKAGE | ERROR: " + e.getMessage());
        }

        return commands;
    }
}
