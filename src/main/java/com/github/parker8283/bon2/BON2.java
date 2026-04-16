package com.github.parker8283.bon2;

import com.github.parker8283.bon2.cli.CLIErrorHandler;
import com.github.parker8283.bon2.cli.CLIProgressListener;
import com.github.parker8283.bon2.data.BONFiles;
import com.github.parker8283.bon2.data.IErrorHandler;
import com.github.parker8283.bon2.data.MappingVersion;
import com.github.parker8283.bon2.data.VersionLookup;
import com.github.parker8283.bon2.exception.InvalidMappingsVersionException;
import com.github.parker8283.bon2.util.BONUtils;
import com.google.common.base.MoreObjects;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BON2 {
    public static final String VERSION = "Bearded Octo Nemesis v${DEV} by Parker8283. BON v1 by immibis.";
    public static File USER_GRADLE_FOLDER; // = new File(MoreObjects.firstNonNull(System.getenv("GRADLE_USER_HOME"), System.getProperty("user.home") + File.separator + ".gradle"));

    public static Logger LOGGER;

    public static void main(String[] args) {
        try {
            LOGGER = LogManager.getLogger("BON2");
            if (args.length > 0) {
                parseArgs(args);
            } else {
                launchGui();
            }
        } catch (Exception e) {
            logErr("Error", e);
            //e.printStackTrace();
        }
    }

    private static void parseArgs(String[] args) throws Exception {
        log(VERSION);
        
        OptionParser parser = new OptionParser();
        parser.accepts("help", "Shows this help menu").forHelp();
        parser.accepts("input", "The jar file or path to .java files to deobfuscate").withRequiredArg().required();
        parser.accepts("output", "The path for the converted jar or converted .java files. Otherwise appends \"-deobf\"").withRequiredArg();
        parser.accepts("mappingsVer", "The version of the mappings to use. Must exist in Gradle cache. Format is \"mcVer-forgeVer-mappingVer\". For use with FG2, use \"1.8(.8)-mappingVer\". This is a temporary solution until BON 2.3.").withRequiredArg().required();
        parser.accepts("mappings", "Lists detected mappings").forHelp();
        parser.accepts("versionsJson", "versions.json path, see https://mcpbot.unascribed.com/").withRequiredArg().required();
        parser.accepts("gradlePath", "The direct path to your global .gradle").withRequiredArg();

        try {
            OptionSet options = parser.parse(args);

            if(options.has("help")) {
                parser.printHelpOn(System.out);
                log("Example usage:");
                log("--versionsJson \"/home/bob/IdeaProjects/BON2/mcp-versions.json\" --mappingsVer 1.12-stable_39 --inputJar \"/home/bob/Documents/TinkersConstruct.jar\"");
                return;
            }

            // find .gradle
            
            if (options.has("gradlePath")) {
                USER_GRADLE_FOLDER = new File((String)options.valueOf("gradlePath"));
            } else {
                USER_GRADLE_FOLDER = new File(
                        MoreObjects.firstNonNull(
                                System.getenv("GRADLE_USER_HOME"), System.getProperty("user.home") + File.separator + ".gradle"));
            }

            if (!USER_GRADLE_FOLDER.exists() || !USER_GRADLE_FOLDER.isDirectory()) {
                logErr(".gradle/ not found in '" + USER_GRADLE_FOLDER.getPath() + "'");
                return;
            }

            // find versions
            
            boolean successVer = false;
            try {
                if (options.has("versionsJson")) {
                    VersionLookup.INSTANCE.refresh((String) options.valueOf("versionsJson"));
                    successVer = true;
                }
            } catch (Exception e1) {
                logErr("Failed to find MCP versions.json");
                logErr("Must be a valid path to versions.json");
                e1.printStackTrace();
            } finally {
                if (!successVer) {
                    VersionLookup.INSTANCE.refresh();
                }
            }
            
            List<MappingVersion> mappings = BONUtils.buildValidMappings();

            
            
            if (options.has("mappings")) {
                printMappings(mappings);
                return;
            }



            //String inputJar = (String)options.valueOf("inputJar");
            //String outputJar = options.has("outputJar") ? (String)options.valueOf("outputJar") : inputJar.replace(".jar", "-deobf.jar");
            String mappingsVer = (String)options.valueOf("mappingsVer");
//
            //if(!new File(inputJar).exists()) {
            //    logErr("The provided inputJar does not exist");
            //    throw new FileNotFoundException(inputJar);
            //}

            MappingVersion mapping = null;
            for (MappingVersion m : mappings) {
                if (m.getVersion().contains(mappingsVer)) {
                    mapping = m;
                    break;
                }
            }
            
            if (mapping == null) {
                logErr("Invalid mappingsVer. The mappings must exist in your Gradle cache.");
                printMappings(mappings);
                throw new InvalidMappingsVersionException(mappingsVer);
            }

            log("Mappings:        " + mappingsVer);
            log("Gradle User Dir: " + BONFiles.USER_GRADLE_FOLDER);
            
            IErrorHandler errorHandler = new CLIErrorHandler();

            // test if input is a jar or path to .java files
            String inputPathString = (String)options.valueOf("input");
            String outputPathString = options.has("output") ? (String)options.valueOf("output") : inputPathString + ".deobf";

            //File inputPath = new File(inputPathString);
            Path inputPath = Path.of(inputPathString);
            //File outputPath = new File(outputPathString);
            Path outputPath = Path.of(outputPathString);
            //File outputPath = options.has("outputJar") ? (String)options.valueOf("outputJar") : inputJar.replace(".jar", "-deobf.jar");

            if (inputPath.equals(outputPath)) {
                logErr("Input and output path must not overlap");
                throw new RuntimeException();
            }
            
            if (!Files.exists(inputPath)) {
                logErr("The provided input path does not exist");
                throw new FileNotFoundException(inputPathString);
            } else {
                if (Files.isRegularFile(inputPath) && inputPath.getFileName().toString().endsWith(".jar")) {
                    //File inputJar = inputPath;
                    //File outputJar = outputPath; //new File(outputPathString.replace(".jar", "-deobf.jar"));
                                        
                    // good
                    log("Input JAR:       " + inputPath);
                    log("Output JAR:      " + outputPath);

                    try {
                        BON2Impl.remap(inputPath.toFile(), outputPath.toFile(), mapping, errorHandler, new CLIProgressListener());
                    } catch(Exception e) {
                        logErr(e.getMessage(), e);
                        System.exit(1);
                    }                    
                } else if (Files.isDirectory(inputPath)) { // && Arrays.stream(Objects.requireNonNull(inputPath.listFiles())).anyMatch(file -> file.getName().endsWith(".java"))){
                    // good too
                    //outputPath = new File(outputPathString); // + "-deobf");

                    // good
                    log("Input path:       " + inputPath);
                    log("Output path:      " + outputPath);

                    try {
                        BON2Impl.remapPath(inputPath, outputPath, mapping, errorHandler, new CLIProgressListener());
                    } catch(Exception e) {
                        logErr(e.getMessage(), e);
                        System.exit(1);
                    }
                } else {
                    logErr("The provided input is not a jar or has no .java files");
                    throw new RuntimeException();
                }
            }
        } catch(OptionException e) {
            e.printStackTrace();
            parser.printHelpOn(System.err);
        }
    }

    private static void printMappings(List<MappingVersion> mappings) {
        if (mappings.isEmpty()) {
            logErr("No mappings detected (does your ~/.gradle exist?");
        } else {
            log("Valid mappings: ");
            for (MappingVersion m : mappings) {
                log(m.getVersion());
            }
        }
    }

    public static void log(String message) {
        //System.out.println(message);
        LOGGER.info(message);
    }

    public static void logErr(String message) {
        //System.err.println(message);
        LOGGER.error(message);
    }

    public static void logErr(String message, Throwable t) {
        //System.err.println(message);
        //t.printStackTrace();
        LOGGER.error(message, t);
    }

    private static void launchGui() {
        log(VERSION);
        log("No arguments passed. Launching gui...");
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    BON2Gui frame = new BON2Gui();
                    frame.setVisible(true);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
