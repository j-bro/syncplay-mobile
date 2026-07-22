#pragma once

#include <jni.h>

// Checks whether mpv has been initialized. Must be called after the JNI function
// has fetched the JNIEnv pointer via jni_func.  Use CHECK_MPV_INIT() in void-returning
// functions and CHECK_MPV_INIT_RET(val) in value-returning ones.
#define CHECK_MPV_INIT() \
    do { if (!g_mpv) { ALOGE("mpv not initialized"); return; } } while(0)
#define CHECK_MPV_INIT_RET(val) \
    do { if (!g_mpv) { ALOGE("mpv not initialized"); return (val); } } while(0)

// Convenience macro around a JNI function declaration and its name resolution.
// Usage: jni_func(ReturnType, functionName, ...args)
#define jni_func(rettype, name, ...) \
    JNIEXPORT rettype JNICALL Java_is_xyz_mpv_MPVLib_ ## name (JNIEnv *env, jclass clazz, ##__VA_ARGS__)

// Acquire a JNIEnv pointer for the current thread. Attaches the thread if needed.
bool acquire_jni_env(JavaVM *vm, JNIEnv **env);

// Cache JNI class references and method IDs. Must be called once during init.
void init_methods_cache(JNIEnv *env);

// Utility macro: when UTIL_EXTERN is defined, define variables; otherwise declare extern.
#ifdef UTIL_EXTERN
#define JNI_CACHE_DEFN
#else
#define JNI_CACHE_DEFN extern
#endif

// Global JNI class references (cached).
JNI_CACHE_DEFN jclass java_Integer;
JNI_CACHE_DEFN jmethodID java_Integer_init;
JNI_CACHE_DEFN jclass java_Double;
JNI_CACHE_DEFN jmethodID java_Double_init;
JNI_CACHE_DEFN jclass java_Boolean;
JNI_CACHE_DEFN jmethodID java_Boolean_init;

JNI_CACHE_DEFN jclass android_graphics_Bitmap;
JNI_CACHE_DEFN jmethodID android_graphics_Bitmap_createBitmap;
JNI_CACHE_DEFN jclass android_graphics_Bitmap_Config;
JNI_CACHE_DEFN jfieldID android_graphics_Bitmap_Config_ARGB_8888;

JNI_CACHE_DEFN jclass mpv_MPVLib;
JNI_CACHE_DEFN jmethodID mpv_MPVLib_eventProperty_S;
JNI_CACHE_DEFN jmethodID mpv_MPVLib_eventProperty_Sb;
JNI_CACHE_DEFN jmethodID mpv_MPVLib_eventProperty_Sl;
JNI_CACHE_DEFN jmethodID mpv_MPVLib_eventProperty_Sd;
JNI_CACHE_DEFN jmethodID mpv_MPVLib_eventProperty_SS;
JNI_CACHE_DEFN jmethodID mpv_MPVLib_event;
JNI_CACHE_DEFN jmethodID mpv_MPVLib_logMessage_SiS;

#undef JNI_CACHE_DEFN
