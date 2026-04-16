package com.github.parker8283.bon2.util;

import com.github.parker8283.bon2.BON2;
import com.github.parker8283.bon2.data.IProgressListener;
import com.github.parker8283.bon2.srg.ClassCollection;
import com.github.parker8283.bon2.srg.Mapping;
import com.github.parker8283.bon2.srg.Repo;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Remapper {

    public static ClassCollection remap(ClassCollection cc, IProgressListener progress) {
        progress.start(cc.getClasses().size(), "Remapping");
        int classesRemapped = 0;
        progress.setMax(cc.getClasses().size());
        for(ClassNode classNode : cc.getClasses()) {
            for(MethodNode method : classNode.methods) {
                if(hasRemap(method.name)) {
                    Mapping mapping = getRemap(method.name);
                    method.name = mapping.getMcpName();
                }
                if(method.instructions != null && method.instructions.size() > 0) {
                    for(AbstractInsnNode node : method.instructions.toArray()) {
                        if(node instanceof FieldInsnNode field) {
                            if(hasRemap(field.name)) {
                                Mapping mapping = getRemap(field.name);
                                field.name = mapping.getMcpName();
                            }
                        } else if(node instanceof MethodInsnNode methodInsn) {
                            if(hasRemap(methodInsn.name)) {
                                Mapping mapping = getRemap(methodInsn.name);
                                methodInsn.name = mapping.getMcpName();
                            }
                        }
                    }
                }
            }
            for(FieldNode field : classNode.fields) {
                if(hasRemap(field.name)) {
                    Mapping mapping = getRemap(field.name);
                    field.name = mapping.getMcpName();
                }
            }
            progress.setProgress(++classesRemapped);
        }
        return cc;
    }

    public static List<String> remap(Stream<String> fileLines) {
        //Pattern pattern = Pattern.compile("(?:func_|field_)[a-zA-Z_\\d]+");
        Pattern pattern = getPrefixRegex();

        return fileLines.map(line -> pattern.matcher(line).replaceAll(matchResult -> {
            String obfuscated = matchResult.group();
            Mapping mapping = Repo.repo.get(obfuscated);

            if (mapping != null) {
                return mapping.getMcpName();
            } else {
                BON2.logErr("mapping not found for " + obfuscated);
                return obfuscated;
            }
        })).toList();
    }

    private static Pattern getPrefixRegex() {
        List<String> prefix_list = Repo.repo.keySet().stream()
                .map(key -> {
                    int index = key.indexOf('_');
                    return index != -1 ? key.substring(0, index + 1) : null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList(); // or collect(Collectors.toList()) if using Java < 16

        String sub_regex = String.join("|", prefix_list);
        return Pattern.compile("(" + sub_regex + ")[a-zA-Z_\\d]+");
    }

    private static boolean hasRemap(String key) {
        return Repo.repo.containsKey(key);
    }

    private static Mapping getRemap(String key) {
        return Repo.repo.get(key);
    }
}
