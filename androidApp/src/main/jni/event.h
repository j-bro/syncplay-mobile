#pragma once

// Entry point for the event thread that processes mpv events
// and dispatches them to Java via JNI callbacks.
void *event_thread(void *arg);
