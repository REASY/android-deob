package com.example.obfuscate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import dalvik.system.InMemoryDexClassLoader;
import android.content.Context;
import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;

public final class DynamicLoaderOriginal {
    private static volatile ClassLoader dynamicClassLoader;

    public static synchronized ClassLoader init(Context ctx, String path) throws IOException {
        if (dynamicClassLoader != null) return dynamicClassLoader;

        byte[] dexBytesEncrypted = readAssetFully(ctx.getAssets(), path);
        byte[] dexBytes = uniffi.obfuscate.ObfuscateKt.decryptBytes(dexBytesEncrypted);
        ByteBuffer buf = ByteBuffer.wrap(dexBytes);

        ClassLoader parent = ctx.getClassLoader();
        dynamicClassLoader = new InMemoryDexClassLoader(buf, parent);
        return dynamicClassLoader;
    }

    public static Class<?> loadClass(String fqcn) throws ClassNotFoundException {
        if (dynamicClassLoader == null) throw new ClassNotFoundException("Dynamic CL not initialized");
        return dynamicClassLoader.loadClass(fqcn);
    }

    private static byte[] readAssetFully(AssetManager am, String name) throws IOException {
        try (InputStream is = am.open(name); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = is.read(buf)) != -1) bos.write(buf, 0, r);
            return bos.toByteArray();
        }
    }
}

