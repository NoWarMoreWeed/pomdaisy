// PomDaisy (iOS): DS RESOLUTION's GPU path (pk_nds_gl.cpp) isn't built - it needs OpenGL ES 3.2 or 3.0 with
// draw_buffers_indexed, which iOS doesn't have. Its calls answer "the software renderer, at the DS's own size".
#include "pk_nds_gl.h"

int pk_nds_gl_set_scale(melonDS::NDS*, int) { return 1; }
int pk_nds_gl_scale() { return 1; }
int pk_nds_gl_supported() { return 0; }
void pk_nds_gl_end(melonDS::NDS*) {}
void pk_nds_gl_begin_frame() {}
void pk_nds_gl_set_window(ANativeWindow*) {}
bool pk_nds_gl_frame(melonDS::NDS*, int, int, int, int, uint32_t*, uint32_t*) { return false; }
void pk_nds_gl_capture(bool) {}
const uint32_t* pk_nds_gl_captured(int* w, int* h)
{
    if (w) *w = 0;
    if (h) *h = 0;
    return nullptr;
}
bool pk_nds_gl_frame_info(int64_t, int32_t*) { return false; }
