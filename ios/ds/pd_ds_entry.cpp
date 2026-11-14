// PomDaisy (iOS): the DS core's libretro entry points under names of their own (pd_ds_core.h). The app links this core
// beside the Game Boy one, whose entry points are retro_* too; after the prelink (CMakeLists.txt) only these are visible.
//
// The core itself is included here, not compiled beside this file: the host functions at the end reach into it (the
// console is in an unnamed namespace there).
#include "../../linux/libretro-ds/pd_ds_libretro.cpp"
#include "../App/Sources/Bridge/pd_ds_core.h"

extern "C" {
void pdds_set_environment(retro_environment_t cb) { retro_set_environment(cb); }
void pdds_set_video_refresh(retro_video_refresh_t cb) { retro_set_video_refresh(cb); }
void pdds_set_audio_sample(retro_audio_sample_t cb) { retro_set_audio_sample(cb); }
void pdds_set_audio_sample_batch(retro_audio_sample_batch_t cb) { retro_set_audio_sample_batch(cb); }
void pdds_set_input_poll(retro_input_poll_t cb) { retro_set_input_poll(cb); }
void pdds_set_input_state(retro_input_state_t cb) { retro_set_input_state(cb); }
void pdds_init(void) { retro_init(); }
void pdds_deinit(void) { retro_deinit(); }
bool pdds_load_game(const struct retro_game_info* info) { return retro_load_game(info); }
void pdds_unload_game(void) { retro_unload_game(); }
void pdds_get_system_av_info(struct retro_system_av_info* info) { retro_get_system_av_info(info); }
void pdds_run(void) { retro_run(); }
void pdds_reset(void) { retro_reset(); }
size_t pdds_serialize_size(void) { return retro_serialize_size(); }
bool pdds_serialize(void* data, size_t size) { return retro_serialize(data, size); }
bool pdds_unserialize(const void* data, size_t size) { return retro_unserialize(data, size); }
void* pdds_get_memory_data(unsigned id) { return retro_get_memory_data(id); }
size_t pdds_get_memory_size(unsigned id) { return retro_get_memory_size(id); }

// The host shows the touch screen itself: the core's frame is the top screen (with the HUD, companion and quick menu),
// the touch screen's picture is read here after a frame, and the pen is given in its pixels (256 x 192) before one.
void pdds_host_screens(bool on) { pd_ds::hostScreens = on; }
void pdds_set_pen(bool down, int x, int y) {
  pd_ds::hostPenDown = down;
  pd_ds::hostPenX = x < 0 ? 0 : x > 255 ? 255 : x;
  pd_ds::hostPenY = y < 0 ? 0 : y > 191 ? 191 : y;
}
const uint32_t* pdds_touch_screen(void) { return nds ? nds->gpu.framebuffer(1) : nullptr; }
}
