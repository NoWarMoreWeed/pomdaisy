#pragma once

/// Catches fatal signals from here on and appends what it can to [path] (see pd_crash.c).
void pd_crash_install(const char *path);
/// Appends [text] to the crash file (e.g. an Objective-C exception's reason, before it aborts).
void pd_crash_note(const char *text);
