/* Links every libretro entry point the iOS app calls, so CI catches a missing symbol in the static core. Never run. */
#include <stddef.h>
#include "libretro.h"

static bool env(unsigned cmd, void *data) { (void)cmd; (void)data; return false; }
static void video(const void *d, unsigned w, unsigned h, size_t p) { (void)d; (void)w; (void)h; (void)p; }
static void sample(int16_t l, int16_t r) { (void)l; (void)r; }
static size_t batch(const int16_t *d, size_t n) { (void)d; return n; }
static void poll(void) {}
static int16_t state(unsigned p, unsigned d, unsigned i, unsigned id) { (void)p; (void)d; (void)i; (void)id; return 0; }

int main(int argc, char **argv) {
  (void)argv;
  if (argc > 100) { // never true: only the references matter
    struct retro_system_info si;
    struct retro_system_av_info av;
    struct retro_game_info gi = {0};
    retro_set_environment(env);
    retro_set_video_refresh(video);
    retro_set_audio_sample(sample);
    retro_set_audio_sample_batch(batch);
    retro_set_input_poll(poll);
    retro_set_input_state(state);
    retro_get_system_info(&si);
    retro_init();
    retro_load_game(&gi);
    retro_get_system_av_info(&av);
    retro_run();
    size_t n = retro_serialize_size();
    retro_serialize(NULL, n);
    retro_unserialize(NULL, n);
    retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
    retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
    retro_reset();
    retro_unload_game();
    retro_deinit();
  }
  return 0;
}
