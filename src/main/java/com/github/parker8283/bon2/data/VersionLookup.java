package com.github.parker8283.bon2.data;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Map;

import com.github.parker8283.bon2.data.VersionJson.MappingsJson;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.net.ssl.HttpsURLConnection;

public enum VersionLookup {

    INSTANCE;

    //private static final String VERSION_JSON = "https://mcpbot.unascribed.com/versions.json";
    //private static final String VERSION_JSON = "http://export.mcpbot.bspk.rs/versions.json";
    private static final String VERSION_JSON = "https://192.99.194.128/versions.json";
    private static final Gson GSON = new GsonBuilder().create();

    private VersionJson jsoncache;

    public String getVersionFor(String version) {
        if (jsoncache != null) {
            for (String s : jsoncache.getVersions()) {
                MappingsJson mappings = jsoncache.getMappings(s);
                if (mappings.hasSnapshot(version) || mappings.hasStable(version)) {
                    return s;
                }
            }
        }
        return null;
    }
    
    public VersionJson getVersions() {
        return jsoncache;
    }

    public void refresh() throws IOException {
        refresh(VERSION_JSON);
    }

    public void refresh(String mcp_url) throws IOException {
        // To:
        File mappingFile = new File(mcp_url);
        InputStream request = Files.newInputStream(mappingFile.toPath());
// Then pass this 'request' stream to your InputStreamReader

        //URL url = new URL(mcp_url);
        //URLConnection request = url.openConnection();

        try (Reader in = new InputStreamReader(request)) {
            INSTANCE.jsoncache = new VersionJson(GSON.fromJson(in,
                    new TypeToken<Map<String, MappingsJson>>() {
            }.getType()));
        }
    }
}
