#pragma once

#include <atomic>
#include <mpv/client.h>

// The JVM reference, set during mpv_create().
extern JavaVM *g_vm;

// The mpv handle, created once and held for the process lifetime.
extern mpv_handle *g_mpv;

// Flag to signal the event thread it should exit.
extern std::atomic<bool> g_event_thread_request_exit;
