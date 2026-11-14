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
#ifdef __cplusplus
}
#endif
#endif
