package com.github.parker8283.bon2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.parker8283.bon2.data.IErrorHandler;
import com.github.parker8283.bon2.data.IProgressListener;
import com.github.parker8283.bon2.data.MappingVersion;
import com.github.parker8283.bon2.srg.ClassCollection;
import com.github.parker8283.bon2.srg.Repo;
import com.github.parker8283.bon2.util.IOUtils;
import com.github.parker8283.bon2.util.JarUtils;
import com.github.parker8283.bon2.util.Remapper;
import com.sun.tools.javac.Main;

public class BON2Impl {

    /**
     * Deobfuscates the inputJar to MCP names using the passed-in mappings.
     * @param inputJar Jar mapped to SRG names to be deobfuscated.
     * @param outputJar The file that will be the remapped jar.
     * @param mappings The mappings to use. In form "minecraftVer-forgeVer-mappingVer".
     * @param errorHandler An IErrorHandler impl to handle when an error is encountered in the remapping process.
     * @param progressListener An IProgressListener impl to handle listening to the progress of the remapping.
     */
    public static void remap(File inputJar, File outputJar, MappingVersion mappings, IErrorHandler errorHandler, IProgressListener progressListener) throws IOException {
        File srgsFolder = mappings.getSrgs();
        Repo.loadMappings(srgsFolder, progressListener);
        ClassCollection inputCC = JarUtils.readFromJar(inputJar, errorHandler, progressListener);
        ClassCollection outputCC = Remapper.remap(inputCC, progressListener);
        JarUtils.writeToJar(outputCC, outputJar, progressListener);
        progressListener.start(1, "Done!");
        progressListener.setProgress(1);
    }

    public static void remapPath(Path inputPath, Path outputPath, MappingVersion mappings, IErrorHandler errorHandler, IProgressListener progressListener) throws IOException {
        if (Files.exists(outputPath)) {
            throw new RuntimeException("Output path must not exist: " + outputPath);
        }

        File srgsFolder = mappings.getSrgs();
        Repo.loadMappings(srgsFolder, progressListener);

        //ClassCollection inputCC = JarUtils.readFromJar(inputJar, errorHandler, progressListener);
        List<File> files = IOUtils.getJavaSrcFiles(inputPath);
        //ClassCollection outputCC = Remapper.remap(inputCC, progressListener);

        progressListener.start(files.size(), "Remapping");
        int classesRemapped = 0;
        for (File file : files) {
            Path currentPath = file.toPath();

            // 1. Calculate the relative path (the 'package' structure)
            // Result: items/SpecialItem.java
            Path relativePath = inputPath.relativize(currentPath);

            // 2. Determine the full target path
            // Result: ./src/main/com/me/mymod-remapped/items/SpecialItem.java
            Path targetPath = outputPath.resolve(relativePath);

            try (Stream<String> lines = Files.lines(currentPath)) {
                List<String> converted_lines = Remapper.remap(lines);

                // 3. Ensure the subdirectories (like /items/) exist before writing
                Files.createDirectories(targetPath.getParent());

                // 4. Write the file to the new location
                Files.write(targetPath, converted_lines);
            } catch (IOException e) {
                BON2.logErr("Failed to process " + currentPath);
                e.printStackTrace();
            }
            progressListener.setProgress(++classesRemapped);
        }

        progressListener.start(1, "Done!");
        progressListener.setProgress(1);

        //progressListener.start(1, "Processed: " + relativePath);
        //progressListener.setProgress(1);
    }

    /**
     * Utility to help build a valid mapping version to pass into the remap process.
     * @param mcVer The Minecraft Version to use.
     * @param forgeVer The Forge Version to use.
     * @param useShippedMappings Use the FML-shipped mappings? (1.7.10 or earlier only)
     * @param mappingVer The mappings version to use. Can safely be null if useShippedMappings is true.
     * @return The formatted mappings version.
     */
    public static String buildMappingVer(String mcVer, String forgeVer, boolean useShippedMappings, String mappingVer) {
        return String.format("%s-%s-%s", mcVer, forgeVer, useShippedMappings ? "shipped" : mappingVer);
    }
}
