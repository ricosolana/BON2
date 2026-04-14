package com.github.parker8283.bon2;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import javax.swing.UIManager;

import com.github.parker8283.bon2.cli.CLIErrorHandler;
import com.github.parker8283.bon2.cli.CLIProgressListener;
import com.github.parker8283.bon2.data.BONFiles;
import com.github.parker8283.bon2.data.IErrorHandler;
import com.github.parker8283.bon2.data.MappingVersion;
import com.github.parker8283.bon2.data.VersionLookup;
import com.github.parker8283.bon2.exception.InvalidMappingsVersionException;
import com.github.parker8283.bon2.util.BONUtils;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class BON2 {
    public static final String VERSION = "Bearded Octo Nemesis v${DEV} by Parker8283. BON v1 by immibis.";

    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                parseArgs(args);
            } else {
                launchGui();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseArgs(String[] args) throws Exception {
        OptionParser parser = new OptionParser();
        parser.accepts("help", "Shows this help menu").forHelp();
        parser.accepts("version", "Shows the version").forHelp();
        parser.accepts("inputJar", "The jar file to deobfuscate").withRequiredArg().required();
        parser.accepts("outputJar", "The location and name of the output jar. Defaults to same dir and appends \"-deobf\"").withRequiredArg();
        parser.accepts("mappingsVer", "The version of the mappings to use. Must exist in Gradle cache. Format is \"mcVer-forgeVer-mappingVer\". For use with FG2, use \"1.8(.8)-mappingVer\". This is a temporary solution until BON 2.3.").withRequiredArg().required();
        parser.accepts("mappings", "Lists detected mappings").forHelp();
        parser.accepts("versionsJson", "versions.json path, see https://mcpbot.unascribed.com/").withRequiredArg().required();

        try {
            OptionSet options = parser.parse(args);

            boolean successVer = false;
            try {
                if (options.has("versionsJson")) {
                    VersionLookup.INSTANCE.refresh((String) options.valueOf("versionsJson"));
                    successVer = true;
                }
            } catch (Exception e1) {
                System.err.println("Failed to find MCP versions.json");
                System.err.println("Must be a valid path to versions.json");
                e1.printStackTrace();
            } finally {
                // parse
                if (!successVer) {
                    VersionLookup.INSTANCE.refresh();
                }
            }
            List<MappingVersion> mappings = BONUtils.buildValidMappings();

            if(options.has("help")) {
                System.out.println(VERSION);
                parser.printHelpOn(System.out);
                System.out.println("Example usage:");
                System.out.println("--versionsJson \"/home/bob/IdeaProjects/BON2/mcp-versions.json\" --mappingsVer 1.12-stable_39 --inputJar \"/home/bob/Documents/TinkersConstruct.jar\"");
                return;
            }
            else if(options.has("version")) {
                System.out.println(VERSION);
                return;
            }
            else if (options.has("mappings")) {
                printMappings(mappings);
                return;
            }

            String inputJar = (String)options.valueOf("inputJar");
            String outputJar = options.has("outputJar") ? (String)options.valueOf("outputJar") : inputJar.replace(".jar", "-deobf.jar");
            String mappingsVer = (String)options.valueOf("mappingsVer");

            if(!new File(inputJar).exists()) {
                System.err.println("The provided inputJar does not exist");
                throw new FileNotFoundException(inputJar);
            }

            MappingVersion mapping = null;
            for (MappingVersion m : mappings) {
                if (m.getVersion().contains(mappingsVer)) {
                    mapping = m;
                    break;
                }
            }
            
            if (mapping == null) {
                System.err.println("Invalid mappingsVer. The mappings must exist in your Gradle cache.");
                printMappings(mappings);
                throw new InvalidMappingsVersionException(mappingsVer);
            }
            
            IErrorHandler errorHandler = new CLIErrorHandler();

            log(VERSION);
            log("Input JAR:       " + inputJar);
            log("Output JAR:      " + outputJar);
            log("Mappings:        " + mappingsVer);
            log("Gradle User Dir: " + BONFiles.USER_GRADLE_FOLDER);

            try {
                BON2Impl.remap(new File(inputJar), new File(outputJar), mapping, errorHandler, new CLIProgressListener());
            } catch(Exception e) {
                logErr(e.getMessage(), e);
                System.exit(1);
            }
        } catch(OptionException e) {
            e.printStackTrace();
            parser.printHelpOn(System.err);
        }
    }

    private static void printMappings(List<MappingVersion> mappings) {
        if (mappings.isEmpty()) {
            System.err.println("No mappings detected (does your ~/.gradle exist?");
        } else {
            System.out.println("Valid mappings: ");
            for (MappingVersion m : mappings) {
                System.out.println(m.getVersion());
            }
        }
    }

    private static void log(String message) {
        System.out.println(message);
    }

    private static void logErr(String message, Throwable t) {
        System.err.println(message);
        t.printStackTrace();
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
