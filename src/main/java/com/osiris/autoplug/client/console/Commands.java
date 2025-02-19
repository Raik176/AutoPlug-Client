/*
 * Copyright (c) 2021-2023 Osiris-Team.
 * All rights reserved.
 *
 * This software is copyrighted work, licensed under the terms
 * of the MIT-License. Consult the "LICENSE" file for details.
 */

package com.osiris.autoplug.client.console;

import com.osiris.autoplug.client.Main;
import com.osiris.autoplug.client.Server;
import com.osiris.autoplug.client.configs.UpdaterConfig;
import com.osiris.autoplug.client.managers.FileManager;
import com.osiris.autoplug.client.network.online.connections.ConSendPrivateDetails;
import com.osiris.autoplug.client.network.online.connections.ConSendPublicDetails;
import com.osiris.autoplug.client.tasks.BeforeServerStartupTasks;
import com.osiris.autoplug.client.tasks.backup.TaskBackup;
import com.osiris.autoplug.client.tasks.updater.java.TaskJavaUpdater;
import com.osiris.autoplug.client.tasks.updater.mods.InstalledModLoader;
import com.osiris.autoplug.client.tasks.updater.mods.MinecraftMod;
import com.osiris.autoplug.client.tasks.updater.mods.TaskModDownload;
import com.osiris.autoplug.client.tasks.updater.mods.TaskModsUpdater;
import com.osiris.autoplug.client.tasks.updater.plugins.MinecraftPlugin;
import com.osiris.autoplug.client.tasks.updater.plugins.ResourceFinder;
import com.osiris.autoplug.client.tasks.updater.plugins.TaskPluginDownload;
import com.osiris.autoplug.client.tasks.updater.plugins.TaskPluginsUpdater;
import com.osiris.autoplug.client.tasks.updater.search.SearchResult;
import com.osiris.autoplug.client.tasks.updater.self.TaskSelfUpdater;
import com.osiris.autoplug.client.tasks.updater.server.TaskServerUpdater;
import com.osiris.autoplug.client.utils.GD;
import com.osiris.autoplug.client.utils.UtilsFile;
import com.osiris.autoplug.client.utils.UtilsMinecraft;
import com.osiris.autoplug.client.utils.tasks.MyBThreadManager;
import com.osiris.autoplug.client.utils.tasks.UtilsTasks;
import com.osiris.jlib.logger.AL;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens for input started with .
 * List the server with .help
 */
public final class Commands {

    /**
     * Returns true if the provided String is a AutoPlug command.
     *
     * @param command An AutoPlug command like .help for example.
     */
    public static boolean execute(@NotNull String command) {

        String first = "";
        try {
            Objects.requireNonNull(command);
            command = command.trim();
            first = Character.toString(command.charAt(0));
        } catch (Exception e) {
            AL.warn("Failed to read command '" + command + "'! Enter .help for all available commands!", e);
            return false;
        }

        if (first.equals(".")) {
            try {
                if (command.equals(".help") || command.equals(".h")) {
                    AL.info("");
                    AL.info(".help | Prints out this (Shortcut: .h)");
                    AL.info(".run tasks | Runs the 'before server startup tasks' without starting the server (.rt)");
                    AL.info(".con info | Shows details about AutoPlugs network connections (.ci)");
                    AL.info(".con reload | Closes and reconnects all connections (.cr)");
                    AL.info(".backup | Ignores cool-down and does an backup (.b)");
                    AL.info(".env info | Shows environment details (.ei)");
                    AL.info(".find java | Finds all Java installations and lists current Javas binaries (.fj)");
                    AL.info("");
                    AL.info("Server related commands:");
                    AL.info(".start | Starts the server (.s)");
                    AL.info(".restart | Restarts the server (.r)");
                    AL.info(".stop | Stops and saves the server (.st)");
                    AL.info(".stop both | Stops, saves your server and closes AutoPlug safely (.stb)");
                    AL.info(".kill | Kills the server without saving (.k)");
                    AL.info(".kill both | Kills the server without saving and closes AutoPlug (.kb)");
                    AL.info(".server info | Shows details about this server (.si)");
                    AL.info("");
                    AL.info("Direct install commands:");
                    AL.info(".install plugin spigot|bukkit|github|modrinth|search <id> | Installs a new plugin (.ip)");
                    AL.info(".install mod modrinth|curseforge | Installs a new mod (.im)");
                    AL.info("");
                    AL.info("Update checking commands: (note that all the checks below");
                    AL.info("ignore the cool-down and behave according to the selected profile)");
                    AL.info(".check | Checks for AutoPlug updates (.c)");
                    AL.info(".check java | Checks for Java updates (.cj)");
                    AL.info(".check server | Checks for server updates (.cs)");
                    AL.info(".check plugins | Checks for plugins updates (.cp)");
                    AL.info(".check mods | Checks for mods updates (.cm)");

                    AL.info("");
                    return true;
                } else if (command.equals(".start") || command.equals(".s")) {
                    Server.start();
                    return true;
                } else if (command.equals(".restart") || command.equals(".r")) {
                    Server.restart();
                    return true;
                } else if (command.equals(".stop") || command.equals(".st")) {
                    Server.stop();
                    return true;
                } else if (command.equals(".stop both") || command.equals(".stb")) {
                    // All the stuff that needs to be done before shutdown is done by the ShutdownHook.
                    // See SystemChecker.addShutdownHook() for details.
                    System.exit(0);
                    return true;
                } else if (command.equals(".kill") || command.equals(".k")) {
                    Server.kill();
                    return true;
                } else if (command.equals(".kill both") || command.equals(".kb")) {
                    Server.kill();
                    AL.info("Killing AutoPlug-Client and Server! Ahhhh!");
                    AL.info("Achievement unlocked: Double kill!");
                    Thread.sleep(3000);
                    System.exit(0);
                    return true;
                } else if (command.equals(".run tasks") || command.equals(".rt")) {
                    new BeforeServerStartupTasks();
                    return true;
                } else if (command.equals(".con info") || command.equals(".ci")) {
                    AL.info("Main connection: connected=" + Main.CON.isAlive() + " interrupted=" + Main.CON.isInterrupted() + " user/staff active=" + Main.CON.isUserActive.get());
                    AL.info(Main.CON.CON_PUBLIC_DETAILS.toString());
                    AL.info(Main.CON.CON_PRIVATE_DETAILS.toString());
                    AL.info(Main.CON.CON_CONSOLE_SEND.toString());
                    AL.info(Main.CON.CON_CONSOLE_RECEIVE.toString());
                    AL.info(Main.CON.CON_FILE_MANAGER.toString());
                    return true;
                } else if (command.equals(".con reload") || command.equals(".cr")) {
                    Main.CON.close();
                    AL.info("Closed connections, reconnecting in 10 seconds...");
                    Thread.sleep(10000);
                    Main.CON.open();
                    return true;
                } else if (command.equals(".server info") || command.equals(".si")) {
                    AL.info("AutoPlug-Version: " + GD.VERSION);
                    ConSendPublicDetails conPublic = Main.CON.CON_PUBLIC_DETAILS;
                    ConSendPrivateDetails conPrivate = Main.CON.CON_PRIVATE_DETAILS;
                    AL.info("Running: " + Server.isRunning());
                    String ip;
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()))) {
                        ip = in.readLine();
                    } catch (Exception e) {
                        ip = "- (failed to reach http://checkip.amazonaws.com)";
                    }
                    AL.info("Public-IP: " + ip);
                    AL.info("Device-/Local-IP: " + InetAddress.getLocalHost().getHostAddress());
                    if (!conPublic.isAlive()) {
                        AL.info(conPublic.getClass().getSimpleName() + " is not active, thus more information cannot be retrieved!");
                    } else {
                        AL.info("Details from " + conPublic.getClass().getSimpleName() + ":");
                        AL.info("Host: " + conPublic.host + ":" + conPublic.port);
                        AL.info("Running: " + conPublic.isRunning);
                        AL.info("Version: " + conPublic.version);
                        AL.info("Players: " + conPublic.currentPlayers);
                        if (conPublic.mineStat != null) {
                            AL.info("Ping result: " + conPublic.mineStat.pingResult.name());
                        } else
                            AL.info("Ping result: -");
                        AL.info("Details from " + conPrivate.getClass().getSimpleName() + ":");
                        AL.info("CPU usage: " + conPrivate.cpuUsage + "%");
                        AL.info("CPU current: " + conPrivate.cpuSpeed + " GHz");
                        AL.info("CPU max: " + conPrivate.cpuMaxSpeed + " GHz");
                        AL.info("MEM free: " + conPrivate.memAvailable + " Gb");
                        AL.info("MEM used: " + conPrivate.memUsed + " Gb");
                        AL.info("MEM total: " + conPrivate.memTotal + " Gb");
                    }
                    return true;
                } else if (command.equals(".env info") || command.equals(".ei")) {

                    AL.info("###################################################");
                    AL.info("ALL PROPERTIES:");
                    AL.info("###################################################");
                    Properties props = System.getProperties();
                    props.forEach((key, val) -> {
                        AL.info(key + ": " + val);
                    });

                    AL.info("###################################################");
                    AL.info("ALL ENVIRONMENT VARIABLES:");
                    AL.info("###################################################");
                    // Get all environment variables
                    Map<String, String> env = System.getenv();
                    // Loop through and print each environment variable
                    for (Map.Entry<String, String> entry : env.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        AL.info(key + ": " + value);
                    }

                    return true;
                } else if (command.equals(".find java") || command.equals(".fj")) {
                    Path directoryPath = new File(System.getProperty("java.home")).toPath();

                    AL.info("###################################################");
                    AL.info("ALL BINARY FILES IN " + directoryPath + ": ");
                    AL.info("###################################################");
                    try {
                        Files.walkFileTree(directoryPath, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                // Check if the file name contains "java"
                                if (file.toFile().isFile() && (!file.getFileName().toString().contains(".") || file.getFileName().toString().contains(".exe"))) {
                                    AL.info(file.toString());
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        AL.warn(e);
                    }

                    AL.info("###################################################");
                    AL.info("JAVA INSTALLATIONS: ");
                    AL.info("###################################################");
                    List<String> javaInstallations = findJavaInstallations();

                    if (javaInstallations.isEmpty()) {
                        AL.info("No Java installations found.");
                    } else {
                        AL.info("Possible Java installations found:");
                        for (String installation : javaInstallations) {
                            AL.info(installation);
                        }
                    }
                    return true;
                } else if (command.equals(".check") || command.equals(".c")) {
                    MyBThreadManager myManager = new UtilsTasks().createManagerAndPrinter();
                    new TaskSelfUpdater("SelfUpdater", myManager.manager).start();
                    new UtilsTasks().printResultsWhenDone(myManager.manager);
                    return true;
                } else if (command.equals(".check java") || command.equals(".cj")) {
                    MyBThreadManager myManager = new UtilsTasks().createManagerAndPrinter();
                    new TaskJavaUpdater("JavaUpdater", myManager.manager).start();
                    new UtilsTasks().printResultsWhenDone(myManager.manager);
                    return true;
                } else if (command.equals(".check server") || command.equals(".cs")) {
                    MyBThreadManager myManager = new UtilsTasks().createManagerAndPrinter();
                    new TaskServerUpdater("ServerUpdater", myManager.manager).start();
                    new UtilsTasks().printResultsWhenDone(myManager.manager);
                    return true;
                } else if (command.equals(".check plugins") || command.equals(".cp")) {
                    MyBThreadManager myManager = new UtilsTasks().createManagerAndPrinter();
                    new TaskPluginsUpdater("PluginsUpdater", myManager.manager).start();
                    new UtilsTasks().printResultsWhenDone(myManager.manager);
                    return true;
                } else if (command.equals(".check mods") || command.equals(".cm")) {
                    MyBThreadManager myManager = new UtilsTasks().createManagerAndPrinter();
                    new TaskModsUpdater("ModsUpdater", myManager.manager).start();
                    new UtilsTasks().printResultsWhenDone(myManager.manager);
                    return true;
                } else if (command.startsWith(".install plugin") || command.startsWith(".ip")) {
                    installPlugin(command);
                    return true;
                } else if (command.startsWith(".install mod") || command.startsWith(".im")) {
                    installMod(command);
                    return true;
                } else if (command.equals(".backup") || command.equals(".b")) {
                    MyBThreadManager myManager = new UtilsTasks().createManagerAndPrinter();
                    TaskBackup backupTask = new TaskBackup("BackupTask", myManager.manager);
                    backupTask.ignoreCooldown = true;
                    backupTask.start();
                    new UtilsTasks().printResultsWhenDone(myManager.manager);
                    return true;
                } else {
                    AL.info("Command '" + command + "' not found! Enter .help or .h for all available commands!");
                    return true;
                }
            } catch (Exception e) {
                AL.warn("Error at execution of '" + command + "' command!", e);
                return true;
            }
        } else
            return false;
    }

    public static List<String> findJavaInstallations() {
        List<String> installations = new ArrayList<>();

        // Get all drives on the system
        File[] drives;
        try {
            drives = File.listRoots();
        } catch (Exception e) {
            AL.warn("Failed to fetch drives, using fallback hardcoded drives.", e);
            drives = new File[]{Paths.get("/").toFile(), Paths.get("C:\\").toFile(), Paths.get("D:\\").toFile()};
        }

        for (File drive : drives) {
            try {
                findJavaInstallationsInDrive(drive, installations);
            } catch (Exception e) {
                AL.warn("Failed for drive: " + drive, e);
            }
        }

        return installations;
    }

    private static void findJavaInstallationsInDrive(File directory, List<String> installations) {
        // Check if the directory is named "bin"
        File binDirectory = new File(directory, "bin");

        if (binDirectory.exists() && binDirectory.isDirectory()) {
            // Check if the "java" file exists in the "bin" directory
            for (File f : binDirectory.listFiles()) {
                if (f.getName().startsWith("java")) {
                    installations.add(binDirectory.getAbsolutePath());
                    break;
                }
            }
        }

        // Recursively search subdirectories
        File[] subDirectories = directory.listFiles(File::isDirectory);
        if (subDirectories != null) {
            for (File subDirectory : subDirectories) {
                findJavaInstallationsInDrive(subDirectory, installations);
            }
        }
    }

    static final Pattern pluginPattern = Pattern.compile("(spigot|bukkit|github|modrinth|search)([\\w.!?_,\\-]+)");
    public static boolean installPlugin(String command) throws Exception {
        String input = command.replaceFirst("\\.install plugin", "").replaceFirst("\\.ip", "").trim();
        SearchResult result = null;
        String tempName = "NEW_PLUGIN";
        File pluginsDir = FileManager.convertRelativeToAbsolutePath(new UpdaterConfig().plugins_updater_path.asString());
        String[] vars = input.split(" ");
        MinecraftPlugin plugin = new MinecraftPlugin(new File(pluginsDir + "/" + tempName).getAbsolutePath(),
                tempName, "0", "", 0, 0, "");
        String mcVersion = new UpdaterConfig().mods_updater_version.asString();

        if (mcVersion == null) mcVersion = new UtilsMinecraft().getInstalledVersion();
        if (mcVersion == null) throw new NullPointerException("Failed to determine Minecraft version.");
        if (vars.length < 2) { //Try the regex support thing (Probably would be better to just remove whitespaces from the input)
            Matcher m = pluginPattern.matcher(input);
            if (!m.find()) return false;
            vars = new String[] {m.group(1), m.group(2)};
        }
        switch (vars[0]) {
            case "spigot":
                int spigotId = Integer.parseInt(vars[1]);
                result = new ResourceFinder().findPluginBySpigotId(new MinecraftPlugin(new File(pluginsDir + "/" + tempName).getAbsolutePath(), tempName, "0", "", spigotId, 0, ""));
                break;
            case "bukkit":
                int bukkitId = Integer.parseInt(vars[1]);
                result = new ResourceFinder().findPluginByBukkitId(new MinecraftPlugin(new File(pluginsDir + "/" + tempName).getAbsolutePath(),
                        tempName, "0", "", 0, bukkitId, ""));
                break;
            case "github":
                String[] split = vars[1].split("/");
                if (split.length < 2) {
                    AL.warn("The github format must be githubrepo/asset");
                    return false;
                }
                plugin.setGithubRepoName(split[0]);
                plugin.setGithubAssetName(split[1]);
                result = new ResourceFinder().findByGithubUrl(plugin);
                break;
            case "modrinth":
                MinecraftPlugin plugin2 = new MinecraftPlugin(new File(pluginsDir + "/" + tempName).getAbsolutePath(),
                        tempName, "0", "", 0, 0, "");
                plugin2.setModrinthId(vars[1]);
                result = new ResourceFinder().findPluginByModrinthId(plugin2, mcVersion);
                break;
            case "search":
                //Try every single way to download a plugin and stop if it finds something
                try { // SPIGOT
                    int id = Integer.parseInt(vars[1]);
                    plugin.setSpigotId(id);
                    result = new ResourceFinder().findPluginBySpigotId(plugin);
                } catch (Exception e) {
                    plugin.setSpigotId(0);
                }
                if (result == null) {
                    try { // BUKKIT
                        int id = Integer.parseInt(vars[1]);
                        plugin.setBukkitId(id);
                        result = new ResourceFinder().findPluginByBukkitId(plugin);
                    } catch (Exception e) {
                        plugin.setBukkitId(0);
                    }
                }
                if (result == null) {
                    try { // GITHUB
                        split = vars[1].split("/");
                        if (split.length > 1) {
                            plugin.setGithubRepoName(split[0]);
                            plugin.setGithubAssetName(split[1]);
                            result = new ResourceFinder().findByGithubUrl(plugin);
                        }
                    } catch (Exception e) {
                        plugin.setGithubAssetName(null);
                        plugin.setGithubRepoName(null);
                    }
                }
                if (result == null) {
                    try { //MODRINTH
                        plugin.setModrinthId(vars[1]);
                        result = new ResourceFinder().findPluginByModrinthId(plugin, mcVersion);
                    } catch (Exception e) {
                        plugin.setModrinthId(null);
                    }
                }
                if (result == null || result.isError() || result.plugin == null) {
                    AL.warn("No plugin with that id found.");
                    return false;
                }
                break;
            default:
                AL.warn("Invalid syntax, use '.help' for more info."); //too lazy to add every argument here
                return false;
        }
        if (result.isError()) {
            AL.warn(result.exception);
            return false;
        }
        MyBThreadManager myManager = new UtilsTasks().createManagerAndPrinter();
        File finalDest = new File(pluginsDir + "/" + result.plugin.getName() + "-LATEST-[" + result.latestVersion + "].jar");
        TaskPluginDownload task = new TaskPluginDownload("PluginDownloader", myManager.manager, tempName, result.latestVersion,
                result.downloadUrl, result.plugin.getIgnoreContentType(), "AUTOMATIC", finalDest);
        task.start();
        new UtilsTasks().printResultsWhenDone(myManager.manager);
        List<MinecraftPlugin> plugins = new UtilsMinecraft().getPlugins(pluginsDir);
        for (MinecraftPlugin pl : plugins) {
            if (pl.getInstallationPath().equals(finalDest.getAbsolutePath())) {
                // Replace tempName with actual plugin name
                finalDest = new UtilsFile().renameFile(task.getFinalDest(),
                        new File(pl.getInstallationPath()).getName().replace(tempName, pl.getName()));
            }
        }
        AL.info("Installed to: " + finalDest);
        return true;
    }

    static final Pattern modPattern = Pattern.compile("(curse|forge|curseforge|modrinth|github|search)([\\w.!?_,\\-]+)");
    public static boolean installMod(String command) throws Exception {
        String input = command.replaceFirst("\\.install mod", "").replaceFirst("\\.im", "").trim();
        String[] vars = input.split(" ");
        if (vars.length < 2) { //Try the regex support thing (Probably would be better to just remove whitespaces from the input)
            Matcher m = modPattern.matcher(input);
            if (!m.find()) return false;
            vars = new String[] {m.group(1), m.group(2)};
        }
        SearchResult result = null;
        String tempName = "NEW_MOD";
        UpdaterConfig updaterConfig = new UpdaterConfig();
        File modsDir = FileManager.convertRelativeToAbsolutePath(updaterConfig.mods_updater_path.asString());
        String mcVersion = updaterConfig.mods_updater_version.asString();
        if (mcVersion == null) mcVersion = new UtilsMinecraft().getInstalledVersion();
        if (mcVersion == null) throw new NullPointerException("Failed to determine Minecraft version.");
        MinecraftMod mod = new MinecraftMod(new File(modsDir + "/" + tempName).getAbsolutePath(), tempName, "0",
                "", "0", "0", "");

        switch (vars[0]) {
            case "modrinth":
                mod.modrinthId = vars[1];
                result = new ResourceFinder().findModByModrinthId(new InstalledModLoader(),
                        mod, mcVersion);
                break;
            case "curse":
            case "forge":
            case "curseforge":
                mod.curseforgeId = vars[1];
                result = new ResourceFinder().findModByCurseforgeId(new InstalledModLoader(), mod,
                        mcVersion, updaterConfig.mods_update_check_name_for_mod_loader.asBoolean());
                break;
            case "github":
                String[] split = vars[1].split("/");
                if (split.length  < 2) {
                    AL.warn("The github format must be githubrepo/asset");
                    return false;
                }
                mod.githubRepoName = split[0];
                mod.githubAssetName = split[1];
                result = new ResourceFinder().findByGithubUrl(mod);
                break;
            case "search":
                try { //MODRINTH
                    mod.modrinthId = vars[1];
                    result = new ResourceFinder().findModByModrinthId(new InstalledModLoader(), mod, mcVersion);
                } catch (Exception e) {
                    mod.modrinthId = null;
                }
                if (result == null) {
                    try { // GITHUB
                        split = vars[1].split("/");
                        if (split.length > 1) {
                            mod.githubRepoName = split[0];
                            mod.githubAssetName = split[1];
                            result = new ResourceFinder().findByGithubUrl(mod);
                        }
                    } catch (Exception e) {
                        mod.githubRepoName = null;
                        mod.githubAssetName = null;
                    }
                }
                if (result == null) {
                    try { //CURSEFORGE
                        mod.curseforgeId = vars[1];
                        result = new ResourceFinder().findModByCurseforgeId(new InstalledModLoader(), mod,
                                mcVersion, updaterConfig.mods_update_check_name_for_mod_loader.asBoolean());
                    } catch (Exception e) {
                        mod.curseforgeId = null;
                    }
                }
                if (result == null || result.isError() || result.plugin == null) {
                    AL.warn("No mod with that id found.");
                    return false;
                }
                break;
            default:
                AL.warn("Invalid syntax, use '.help' for more info."); //too lazy to add every argument here
                return false;
        }
        if (result.isError()) {
            AL.warn(result.exception);
            return false;
        }
        MyBThreadManager myManager = new UtilsTasks().createManagerAndPrinter();
        File finalDest = new File(modsDir + "/" + result.mod.getName() + "-LATEST-[" + result.latestVersion + "].jar");
        TaskModDownload task = new TaskModDownload("ModDownloader", myManager.manager, tempName, result.latestVersion,
                result.downloadUrl, result.mod.ignoreContentType, "AUTOMATIC", finalDest);
        task.start();
        new UtilsTasks().printResultsWhenDone(myManager.manager);
        List<MinecraftMod> plugins = new UtilsMinecraft().getMods(modsDir);
        for (MinecraftMod mod2 : plugins) {
            if (mod2.installationPath.equals(finalDest.getAbsolutePath())) {
                // Replace tempName with actual plugin name
                finalDest = new UtilsFile().renameFile(task.getFinalDest(),
                        new File(mod2.installationPath).getName().replace(tempName, mod2.getName()));
            }
        }
        AL.info("Installed to: " + finalDest);
        return true;
    }


}
