/* PomDaisy (iOS): app/src/main/cpp/nds/dsperate_android.h includes <sys/user.h> for bionic's PAGE_SIZE (to undefine it
 * before DSperate's headers). iOS has no such header, and glibc's defines PAGE_SHIFT too: nothing here. */
#pragma once
