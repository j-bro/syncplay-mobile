#pragma once

#include <android/log.h>

#define ALOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "mpv", __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, "mpv", __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, "mpv", __VA_ARGS__)

void die(const char *msg);
