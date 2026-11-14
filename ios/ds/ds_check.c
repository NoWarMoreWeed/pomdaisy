/* Links the app's bridge (pd_bridge.c) with both cores, as the app does: a duplicate or missing symbol shows here
 * before Xcode links the app. On a host that can run it, it also plays games through the bridge:
 *   [PD_DS_CORE=1] pomdaisy_ds_check <dir for the core's files> <game> [frames] [<game> [frames]] ...
 * (a .nds goes to a DS core - DSperate, or melonDS with PD_DS_CORE=1 - anything else to the Game Boy core), printing
 * each game's speed. */
#include "pd_bridge.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

static double now(void) {
  struct timespec t;
  clock_gettime(CLOCK_MONOTONIC, &t);
  return t.tv_sec + t.tv_nsec / 1e9;
}

static int play(const char *path, int frames) {
  void *rom = NULL;
  size_t size = 0;
  if (!pd_is_ds_path(path)) {
    FILE *f = fopen(path, "rb");
    if (!f)
      return 1;
    fseek(f, 0, SEEK_END);
    size = (size_t)ftell(f);
    rewind(f);
    rom = malloc(size);
    if (!rom || fread(rom, 1, size, f) != size)
      return 1;
    fclose(f);
  }
  if (!pd_load(path, rom, size)) {
    printf("%s: did not load\n", path);
    return 1;
  }
  free(rom);
  static uint32_t frame[1280 * 960];
  int w = 0, h = 0;
  uint64_t serial = 0;
  double t0 = now();
  for (int i = 0; i < frames; i++)
    pd_run();
  double t = now() - t0;
  serial = pd_frame_copy(0, frame, 1280, 960, &w, &h);
  uint32_t sum = 0;
  for (int i = 0; i < w * h; i++)
    sum = sum * 31 + frame[i];
  size_t stateSize = pd_state_size();
  void *state = malloc(stateSize ? stateSize : 1);
  bool stateOk = stateSize && pd_state_save(state, stateSize) && pd_state_load(state, stateSize);
  free(state);
  static uint32_t touch[256 * 192];
  uint64_t touchSerial = pd_touch_copy(0, touch); // a DS game: the touch screen comes beside the frame
  uint32_t touchSum = 0;
  for (int i = 0; i < 256 * 192; i++)
    touchSum = touchSum * 31 + touch[i];
  bool touchOk = pd_is_ds_path(path) ? touchSerial != 0 : touchSerial == 0;
  printf("%s: %d frames in %.2f s = %.0f fps (%.1fx of %.2f), %dx%d frame %llu sum %08x, touch screen %llu sum %08x, "
         "save %zu, state %zu %s\n",
         path, frames, t, frames / t, frames / t / pd_fps(), pd_fps(), w, h, (unsigned long long)serial, sum,
         (unsigned long long)touchSerial, touchSum, pd_save_image(NULL), stateSize, stateOk && touchOk ? "ok" : "FAILED");
  pd_unload();
  return serial && stateOk && touchOk ? 0 : 1;
}

int main(int argc, char **argv) {
  if (argc < 3)
    return 0; // the link was the check
  pd_set_dirs(argv[1], argv[1]);
  if (getenv("PD_DS_CORE")) // 1: DS games on melonDS
    pd_set_ds_core(atoi(getenv("PD_DS_CORE")));
  int bad = 0;
  for (int i = 2; i < argc; i++) {
    const char *path = argv[i];
    int frames = 600;
    if (i + 1 < argc && atoi(argv[i + 1]) > 0)
      frames = atoi(argv[++i]);
    bad |= play(path, frames);
  }
  return bad;
}
