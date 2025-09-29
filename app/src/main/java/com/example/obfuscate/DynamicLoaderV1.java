package com.example.obfuscate;

import uniffi.obfuscate.ObfuscateKt;

public final class DynamicLoaderV1 {
    // keep as Object (not ClassLoader) so the concrete type is hidden
    private static volatile Object dynamicClassLoader;

    /**
     * Initialize the dynamic class loader using reflection-only APIs.
     *
     * @param ctx  Android Context as Object
     * @param path asset path to the dex/jar file inside assets
     * @return the created classloader (as Object)
     */
    public static synchronized Object init(Object ctx, String path) throws Exception {
        if (dynamicClassLoader != null) return dynamicClassLoader;

        // ctx.getAssets()
        Object assetManager = ctx.getClass().getMethod(getStr(new byte[]{0x06, 0x48, 0x02, 0x24, 0x01, 0x0A, 0x48, 0x07, 0x16})).invoke(ctx);

        // read asset into byte[]
        byte[] dexBytesEncrypted = (byte[]) readAssetFully(assetManager, path);
        // Get the instance of the class
        Class<?> obfuscateClass = Class.forName(getStr(new byte[]{0x14, 0x43, 0x1F, 0x03, 0x14, 0x10, 0x03, 0x1C, 0x07, 0x05, 0x07, 0x16, 0x17, 0x4C, 0x1F, 0x00, 0x57, 0x62, 0x04, 0x09, 0x07, 0x5E, 0x17, 0x09, 0x1D, 0x16, 0x66, 0x55}));
        // Decrypt the bytes
        byte[] dexBytes =  (byte[]) obfuscateClass.getMethod(getStr(new byte[]{0x05, 0x48, 0x15, 0x17, 0x0B, 0x09, 0x59, 0x31, 0x1C, 0x17, 0x17, 0x16}), byte[].class).invoke(null, dexBytesEncrypted);

        // java.nio.ByteBuffer.wrap(byte[])
        Class<?> byteBufferClass = Class.forName(getStr(new byte[]{0x0B, 0x4C, 0x00, 0x04, 0x5C, 0x17, 0x44, 0x1C, 0x4B, 0x21, 0x0B, 0x11, 0x11, 0x6F, 0x1E, 0x03, 0x1F, 0x48, 0x14}));
        Object byteBuffer = byteBufferClass.getMethod(getStr(new byte[]{0x16, 0x5F, 0x17, 0x15}), byte[].class).invoke(null, (Object) dexBytes);

        // ctx.getClassLoader() -> parent
        Object parentCL = ctx.getClass().getMethod(getStr(new byte[]{0x06, 0x48, 0x02, 0x26, 0x1E, 0x18, 0x5E, 0x00, 0x29, 0x0C, 0x13, 0x01, 0x11, 0x5F})).invoke(ctx);

        // Construct dalvik.system.InMemoryDexClassLoader(ByteBuffer, ClassLoader)
        Class<?> imdclClass = Class.forName(getStr(new byte[]{0x05, 0x4C, 0x1A, 0x13, 0x1B, 0x12, 0x03, 0x00, 0x1C, 0x10, 0x06, 0x00, 0x19, 0x03, 0x22, 0x0B, 0x34, 0x48, 0x0B, 0x00, 0x00, 0x54, 0x30, 0x0D, 0x11, 0x30, 0x41, 0x40, 0x33, 0x50, 0x68, 0x0E, 0x4C, 0x12, 0x00, 0x00}));
        java.lang.reflect.Constructor<?> ctor =
                imdclClass.getConstructor(Class.forName(getStr(new byte[]{0x0B, 0x4C, 0x00, 0x04, 0x5C, 0x17, 0x44, 0x1C, 0x4B, 0x21, 0x0B, 0x11, 0x11, 0x6F, 0x1E, 0x03, 0x1F, 0x48, 0x14})), Class.forName(getStr(new byte[]{0x0B, 0x4C, 0x00, 0x04, 0x5C, 0x15, 0x4C, 0x1D, 0x02, 0x4D, 0x31, 0x09, 0x15, 0x5E, 0x18, 0x29, 0x16, 0x4C, 0x02, 0x0A, 0x00})));
        dynamicClassLoader = ctor.newInstance(byteBuffer, parentCL);

        return dynamicClassLoader;
    }


    /**
     * Load a class name using the dynamically created classloader.
     * Returns a java.lang.Class object as Object.
     */
    public static Object loadClass(String fqcn) throws Exception {
        if (dynamicClassLoader == null) throw new ClassNotFoundException("Dynamic CL not initialized");
        return dynamicClassLoader.getClass().getMethod(getStr(new byte[]{0x0D, 0x42, 0x17, 0x01, 0x31, 0x15, 0x4C, 0x00, 0x16}), String.class).invoke(dynamicClassLoader, fqcn);
    }

    /**
     * Read asset fully using reflection: assetManager.open(name) -> InputStream -> ByteArrayOutputStream
     * Returns byte[] as Object.
     */
    private static Object readAssetFully(Object assetManager, String name) throws Exception {
        // InputStream is = assetManager.open(name);
        Object is = assetManager.getClass().getMethod(getStr(new byte[]{0x0E, 0x5D, 0x13, 0x0B}), String.class).invoke(assetManager, name);

        Class<?> baosClass = Class.forName(getStr(new byte[]{0x0B, 0x4C, 0x00, 0x04, 0x5C, 0x10, 0x42, 0x5D, 0x27, 0x1A, 0x06, 0x00, 0x35, 0x5F, 0x19, 0x04, 0x00, 0x62, 0x13, 0x1B, 0x02, 0x58, 0x00, 0x3B, 0x1D, 0x01, 0x48, 0x40, 0x2D}));
        Object bos = baosClass.getConstructor().newInstance();

        Class<?> inputStreamClass = Class.forName(getStr(new byte[]{0x0B, 0x4C, 0x00, 0x04, 0x5C, 0x10, 0x42, 0x5D, 0x2C, 0x0D, 0x02, 0x10, 0x00, 0x7E, 0x1F, 0x17, 0x1C, 0x4C, 0x0B}));

        java.lang.reflect.Method readMethod = inputStreamClass.getMethod(getStr(new byte[]{0x13, 0x48, 0x17, 0x01}), byte[].class);
        java.lang.reflect.Method writeMethod = baosClass.getMethod(getStr(new byte[]{0x16, 0x5F, 0x1F, 0x11, 0x17}), byte[].class, int.class, int.class);
        java.lang.reflect.Method isClose = inputStreamClass.getMethod(getStr(new byte[]{0x02, 0x41, 0x19, 0x16, 0x17}));
        java.lang.reflect.Method bosClose = baosClass.getMethod(getStr(new byte[]{0x02, 0x41, 0x19, 0x16, 0x17}));
        java.lang.reflect.Method toByteArray = baosClass.getMethod(getStr(new byte[]{0x15, 0x42, 0x34, 0x1C, 0x06, 0x1C, 0x6C, 0x01, 0x17, 0x02, 0x0B}));

        byte[] buffer = new byte[64 * 1024];

        try {
            while (true) {
                Object rObj = readMethod.invoke(is, (Object) buffer);
                int r = ((Number) rObj).intValue();
                if (r == -1) break;
                writeMethod.invoke(bos, (Object) buffer, 0, r);
            }
            // returns byte[] (as Object)
            return toByteArray.invoke(bos);
        } finally {
            try {
                if (is != null) isClose.invoke(is);
            } catch (Throwable ignored) {
            }
            try {
                if (bos != null) bosClose.invoke(bos);
            } catch (Throwable ignored) {
            }
        }
    }

    public static String getStr(byte[] arr) {
        return ObfuscateKt.decrypt(arr);
    }
}

