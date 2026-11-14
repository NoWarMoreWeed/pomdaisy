/* The melonDS core's entry points (ios/melonds: the Android app's melonDS core with EXPAND, prelinked into
 * pomdaisy_md.o). libretro's functions under names of their own, so it links beside the other two cores. */
#ifndef PD_MD_CORE_H
#define PD_MD_CORE_H
#include "libretro.h"

#ifdef __cplusplus
extern "C" {
#endif
void pdmd_set_environment(retro_environment_t cb);
void pdmd_set_video_refresh(retro_video_refresh_t cb);
void pdmd_set_audio_sample(retro_audio_sample_t cb);
void pdmd_set_audio_sample_batch(retro_audio_sample_batch_t cb);
void pdmd_set_input_poll(retro_input_poll_t cb);
void pdmd_set_input_state(retro_input_state_t cb);
void pdmd_init(void);
void pdmd_deinit(void);
bool pdmd_load_game(const struct retro_game_info *info);
void pdmd_unload_game(void);
void pdmd_get_system_av_info(struct retro_system_av_info *info);
void pdmd_run(void);
void pdmd_reset(void);
size_t pdmd_serialize_size(void);
bool pdmd_serialize(void *data, size_t size);
bool pdmd_unserialize(const void *data, size_t size);
void *pdmd_get_memory_data(unsigned id);
size_t pdmd_get_memory_size(unsigned id);
/* The pen on the touch screen, in its pixels (0..255, 0..191). Before a frame, on the core's thread. */
void pdmd_set_pen(bool down, int x, int y);
/* The touch screen's picture, 256 x 192 XRGB8888, as the last frame left it (NULL with no game). */
const uint32_t *pdmd_touch_screen(void);
/* EXPAND on or off (on by default): the overworld drawn past the top screen's edges to the view's shape. */
void pdmd_set_expand(bool on);
/* The field read's PK_NDS_FIELD_* bits after the last frame (app/src/main/cpp/nds/pk_nds_field.h); 0 for a cart it has
 * no table for. From any thread. */
int pdmd_field_bits(void);
#ifdef __cplusplus
}
#endif
#endif
