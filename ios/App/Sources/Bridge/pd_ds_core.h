/* The Nintendo DS core's entry points (ios/ds: DSperate + the DS companion, prelinked into pomdaisy_ds.o). libretro's
 * own functions under other names, so this core links beside the Game Boy one. */
#ifndef PD_DS_CORE_H
#define PD_DS_CORE_H
#include "libretro.h"

#ifdef __cplusplus
extern "C" {
#endif
void pdds_set_environment(retro_environment_t cb);
void pdds_set_video_refresh(retro_video_refresh_t cb);
void pdds_set_audio_sample(retro_audio_sample_t cb);
void pdds_set_audio_sample_batch(retro_audio_sample_batch_t cb);
void pdds_set_input_poll(retro_input_poll_t cb);
void pdds_set_input_state(retro_input_state_t cb);
void pdds_init(void);
void pdds_deinit(void);
bool pdds_load_game(const struct retro_game_info *info);
void pdds_unload_game(void);
void pdds_get_system_av_info(struct retro_system_av_info *info);
void pdds_run(void);
void pdds_reset(void);
size_t pdds_serialize_size(void);
bool pdds_serialize(void *data, size_t size);
bool pdds_unserialize(const void *data, size_t size);
void *pdds_get_memory_data(unsigned id);
size_t pdds_get_memory_size(unsigned id);
/* The host shows the DS's touch screen itself: the core's frame is then the top screen alone. Before pdds_load_game. */
void pdds_host_screens(bool on);
/* The pen on the touch screen, in its pixels (0..255, 0..191). Before a frame, on the core's thread. */
void pdds_set_pen(bool down, int x, int y);
/* The touch screen's picture, 256 x 192 XRGB8888, as the last frame left it (NULL with no game). */
const uint32_t *pdds_touch_screen(void);
#ifdef __cplusplus
}
#endif
#endif
