package com.github.parker8283.bon2.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class IOUtils {

    public static byte[] readStreamFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(8192, is.available()));
        byte[] buffer = new byte[8192];
        int read;
        while((read = is.read(buffer)) >= 0) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    public static ClassNode readClassFromBytes(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);
        return classNode;
    }

    public static byte[] writeClassToBytes(ClassNode classNode) {
        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    public static int getSecondToLastIndexOf(String string, char character) {
        String temp = string.substring(0, string.lastIndexOf(character));
        return temp.lastIndexOf(character);
    }

    public static List<File> getJavaSrcFiles(Path path) {
        ArrayList<File> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(path)) {
            stream.filter(Files::isRegularFile)
                    .filter((f) -> f.toFile().getName().endsWith(".java"))
                    .forEach(f -> files.add(f.toFile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //for (File file : Objects.requireNonNull(path.toFile().listFiles())) {
        //    Files.it
        //    if (file.getName().endsWith(".java")) {
        //        files.add(file);
        //    }
        //}
        return files;
    }
}
