/* PomDaisy: the libretro frontend's C side (see pd_bridge.h). Modelled on linux/main.c's host, which owns the same
 * things: the core's options (pokedaisy_panel among them), the frame, the sound and the joypad. */
#include "pd_bridge.h"
#include "libretro.h"
#include "pd_ds_core.h"

#include <pthread.h>
#include <stdarg.h>
#include <stdatomic.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <time.h>

/* ---- the two cores: the Game Boy one (mGBA + the companion, ios/core: retro_*) and the Nintendo DS one (DSperate +
 * the DS companion, ios/ds: pdds_*). Both are linked in; a game's file name picks one. ---- */
struct pd_core {
  void (*set_environment)(retro_environment_t);
  void (*set_video_refresh)(retro_video_refresh_t);
  void (*set_audio_sample)(retro_audio_sample_t);
  void (*set_audio_sample_batch)(retro_audio_sample_batch_t);
  void (*set_input_poll)(retro_input_poll_t);
  void (*set_input_state)(retro_input_state_t);
  void (*init)(void);
  void (*deinit)(void);
  bool (*load_game)(const struct retro_game_info *);
  void (*unload_game)(void);
  void (*get_system_av_info)(struct retro_system_av_info *);
  void (*run)(void);
  void (*reset)(void);
  size_t (*serialize_size)(void);
  bool (*serialize)(void *, size_t);
  bool (*unserialize)(const void *, size_t);
  void *(*get_memory_data)(unsigned);
  size_t (*get_memory_size)(unsigned);
  bool path_only; // the core reads the game from its path itself: no copy of it in memory
};
static const struct pd_core gbCore = {
  retro_set_environment, retro_set_video_refresh, retro_set_audio_sample, retro_set_audio_sample_batch,
  retro_set_input_poll, retro_set_input_state, retro_init, retro_deinit, retro_load_game, retro_unload_game,
  retro_get_system_av_info, retro_run, retro_reset, retro_serialize_size, retro_serialize, retro_unserialize,
  retro_get_memory_data, retro_get_memory_size, false};
static const struct pd_core dsCore = {
  pdds_set_environment, pdds_set_video_refresh, pdds_set_audio_sample, pdds_set_audio_sample_batch,
  pdds_set_input_poll, pdds_set_input_state, pdds_init, pdds_deinit, pdds_load_game, pdds_unload_game,
  pdds_get_system_av_info, pdds_run, pdds_reset, pdds_serialize_size, pdds_serialize, pdds_unserialize,
  pdds_get_memory_data, pdds_get_memory_size, true};
static const struct pd_core *core = &gbCore;

bool pd_is_ds_path(const char *path) {
  const char *dot = path ? strrchr(path, '.') : NULL;
  return dot && !strcasecmp(dot, ".nds");
}

/* ---- the core's options: defaults from the core, pokedaisy_panel from us ---- */
#define OPTIONS_MAX 160
static struct {
  char key[64];
  char value[96];
} options[OPTIONS_MAX];
static int optionN;
static bool optionsChanged;
static char panelValue[32] = "auto";
static pthread_mutex_t optionLock = PTHREAD_MUTEX_INITIALIZER;

static void option_set(const char *key, const char *value) {
  if (!key || !value)
    return;
  for (int i = 0; i < optionN; i++)
    if (!strcmp(options[i].key, key)) {
      snprintf(options[i].value, sizeof options[i].value, "%s", value);
      return;
    }
  if (optionN < OPTIONS_MAX) {
    snprintf(options[optionN].key, sizeof options[optionN].key, "%s", key);
    snprintf(options[optionN].value, sizeof options[optionN].value, "%s", value);
    optionN++;
  }
}
static void options_v2(const struct retro_core_option_v2_definition *d) {
  for (; d && d->key; d++)
    option_set(d->key, d->default_value ? d->default_value : d->values[0].value);
}
static void options_v1(const struct retro_core_option_definition *d) {
  for (; d && d->key; d++)
    option_set(d->key, d->default_value ? d->default_value : d->values[0].value);
}
static void options_variables(const struct retro_variable *v) {
  for (; v && v->key; v++) {
    // "Description; first|second|..." - the first is the default
    const char *semi = v->value ? strchr(v->value, ';') : NULL;
    if (!semi)
      continue;
    semi++;
    while (*semi == ' ')
      semi++;
    char first[96];
    size_t n = strcspn(semi, "|");
    if (n >= sizeof first)
      n = sizeof first - 1;
    memcpy(first, semi, n);
    first[n] = 0;
    option_set(v->key, first);
  }
}

/* ---- state the callbacks share ---- */
static char systemDir[1024] = ".", saveDir[1024] = ".";
static struct retro_system_av_info av;
static enum retro_pixel_format pixelFormat = RETRO_PIXEL_FORMAT_0RGB1555;
static bool loaded;
static void *romCopy;
static _Atomic uint32_t keys;
static _Atomic int pointerX, pointerY;
static _Atomic bool pointerDown;
static _Atomic bool pointerPressed; // a press since the last frame: held for the core at least one frame (a quick tap)
static bool pointerFrame;           // what the core sees this frame
static _Atomic int penX, penY;      // the DS pen, in the touch screen's pixels
static _Atomic bool penDown, penPressed;
static _Atomic bool ffOn;
static _Atomic int ffSpeed = 1;
static _Atomic double frameMs; // the core's time for a frame, averaged
static _Atomic double frameMsPeak; // the slowest since pd_frame_ms_peak
static _Atomic uint64_t framesRun;

void pd_set_dirs(const char *s, const char *d) {
  snprintf(systemDir, sizeof systemDir, "%s", s ? s : ".");
  snprintf(saveDir, sizeof saveDir, "%s", d ? d : ".");
}
void pd_set_keys(uint32_t mask) { atomic_store(&keys, mask); }
void pd_set_pointer(int x, int y, bool down) {
  atomic_store(&pointerX, x < -0x7FFF ? -0x7FFF : x > 0x7FFF ? 0x7FFF : x);
  atomic_store(&pointerY, y < -0x7FFF ? -0x7FFF : y > 0x7FFF ? 0x7FFF : y);
  atomic_store(&pointerDown, down);
  if (down)
    atomic_store(&pointerPressed, true);
}
void pd_set_pen(int x, int y, bool down) {
  atomic_store(&penX, x);
  atomic_store(&penY, y);
  atomic_store(&penDown, down);
  if (down)
    atomic_store(&penPressed, true);
}
void pd_set_panel(int w, int h) {
  char v[32];
  if (w > 0 && h > 0)
    snprintf(v, sizeof v, "%dx%d", w, h);
  else
    snprintf(v, sizeof v, "auto");
  pthread_mutex_lock(&optionLock);
  if (strcmp(v, panelValue)) {
    snprintf(panelValue, sizeof panelValue, "%s", v);
    optionsChanged = true;
  }
  pthread_mutex_unlock(&optionLock);
}
void pd_set_fast_forward(bool on, int speed) {
  atomic_store(&ffOn, on);
  atomic_store(&ffSpeed, speed < 1 ? 1 : speed);
}

static void core_log(enum retro_log_level level, const char *fmt, ...) {
  if (level < RETRO_LOG_WARN)
    return;
  va_list ap;
  va_start(ap, fmt);
  vfprintf(stderr, fmt, ap);
  va_end(ap);
}

static bool environment(unsigned cmd, void *data) {
  switch (cmd) {
  case RETRO_ENVIRONMENT_GET_VARIABLE: {
    struct retro_variable *v = data;
    v->value = NULL;
    pthread_mutex_lock(&optionLock);
    if (!strcmp(v->key, "pokedaisy_panel"))
      v->value = panelValue;
    for (int i = 0; i < optionN && !v->value; i++)
      if (!strcmp(options[i].key, v->key))
        v->value = options[i].value;
    pthread_mutex_unlock(&optionLock);
    return v->value != NULL;
  }
  case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
    pthread_mutex_lock(&optionLock);
    *(bool *)data = optionsChanged;
    optionsChanged = false;
    pthread_mutex_unlock(&optionLock);
    return true;
  case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:
    *(unsigned *)data = 2;
    return true;
  case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2:
    options_v2(data ? ((const struct retro_core_options_v2 *)data)->definitions : NULL);
    return true;
  case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2_INTL: {
    const struct retro_core_options_v2_intl *i = data;
    options_v2(i && i->us ? i->us->definitions : NULL);
    return true;
  }
  case RETRO_ENVIRONMENT_SET_CORE_OPTIONS:
    options_v1(data);
    return true;
  case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_INTL:
    options_v1(data ? ((const struct retro_core_options_intl *)data)->us : NULL);
    return true;
  case RETRO_ENVIRONMENT_SET_VARIABLES:
    options_variables(data);
    return true;
  case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:
  case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
  case RETRO_ENVIRONMENT_SET_SUPPORT_ACHIEVEMENTS:
    return true;
  case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
    enum retro_pixel_format f = *(const enum retro_pixel_format *)data;
    if (f != RETRO_PIXEL_FORMAT_XRGB8888 && f != RETRO_PIXEL_FORMAT_RGB565 && f != RETRO_PIXEL_FORMAT_0RGB1555)
      return false;
    pixelFormat = f;
    return true;
  }
  case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
    *(const char **)data = saveDir;
    return true;
  case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
    *(const char **)data = systemDir;
    return true;
  case RETRO_ENVIRONMENT_SET_GEOMETRY:
    av.geometry = *(const struct retro_game_geometry *)data;
    return true;
  case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO:
    av = *(const struct retro_system_av_info *)data;
    return true;
  case RETRO_ENVIRONMENT_GET_CAN_DUPE:
    *(bool *)data = true;
    return true;
  case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
    return true;
  case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
    ((struct retro_log_callback *)data)->log = core_log;
    return true;
  case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE:
    if (data)
      *(int *)data = 3;
    return true;
  case RETRO_ENVIRONMENT_GET_FASTFORWARDING:
    *(bool *)data = atomic_load(&ffOn);
    return true;
  case RETRO_ENVIRONMENT_GET_THROTTLE_STATE: {
    struct retro_throttle_state *t = data;
    bool ff = atomic_load(&ffOn);
    t->mode = ff ? RETRO_THROTTLE_FAST_FORWARD : RETRO_THROTTLE_VSYNC;
    t->rate = (float)(av.timing.fps * (ff ? atomic_load(&ffSpeed) : 1));
    return true;
  }
  default:
    return false;
  }
}

/* ---- video: converted to BGRX on the emulation thread, handed over under a lock ---- */
static pthread_mutex_t frameLock = PTHREAD_MUTEX_INITIALIZER;
static uint32_t *frameBuf;
static size_t frameCap;
static int frameW, frameH;
static uint64_t frameSerial;

static void video_refresh(const void *data, unsigned w, unsigned h, size_t pitch) {
  if (!data || !w || !h) // a dupe: the last frame stays
    return;
  pthread_mutex_lock(&frameLock);
  size_t need = (size_t)w * h;
  if (need > frameCap) {
    uint32_t *n = realloc(frameBuf, need * 4);
    if (!n) {
      pthread_mutex_unlock(&frameLock);
      return;
    }
    frameBuf = n;
    frameCap = need;
  }
  for (unsigned y = 0; y < h; y++) {
    const uint8_t *row = (const uint8_t *)data + y * pitch;
    uint32_t *out = frameBuf + (size_t)y * w;
    if (pixelFormat == RETRO_PIXEL_FORMAT_XRGB8888) {
      memcpy(out, row, (size_t)w * 4);
    } else if (pixelFormat == RETRO_PIXEL_FORMAT_RGB565) {
      const uint16_t *p = (const uint16_t *)row;
      for (unsigned x = 0; x < w; x++) {
        uint32_t c = p[x], r = (c >> 11) & 31, g = (c >> 5) & 63, b = c & 31;
        out[x] = 0xFF000000u | ((r << 3 | r >> 2) << 16) | ((g << 2 | g >> 4) << 8) | (b << 3 | b >> 2);
      }
    } else {
      const uint16_t *p = (const uint16_t *)row;
      for (unsigned x = 0; x < w; x++) {
        uint32_t c = p[x], r = (c >> 10) & 31, g = (c >> 5) & 31, b = c & 31;
        out[x] = 0xFF000000u | ((r << 3 | r >> 2) << 16) | ((g << 3 | g >> 2) << 8) | (b << 3 | b >> 2);
      }
    }
  }
  frameW = (int)w;
  frameH = (int)h;
  frameSerial++;
  pthread_mutex_unlock(&frameLock);
}

/* The DS's touch screen, copied after each frame. */
#define TOUCH_W 256
#define TOUCH_H 192
static uint32_t touchBuf[TOUCH_W * TOUCH_H];
static uint64_t touchSerial;
static void touch_grab(void) {
  const uint32_t *p = pdds_touch_screen();
  if (!p)
    return;
  pthread_mutex_lock(&frameLock);
  if (!touchSerial || memcmp(touchBuf, p, sizeof touchBuf)) {
    memcpy(touchBuf, p, sizeof touchBuf);
    touchSerial++;
  }
  pthread_mutex_unlock(&frameLock);
}
uint64_t pd_touch_copy(uint64_t have, void *out) {
  pthread_mutex_lock(&frameLock);
  uint64_t serial = touchSerial;
  if (serial && serial != have && out)
    memcpy(out, touchBuf, sizeof touchBuf);
  pthread_mutex_unlock(&frameLock);
  return serial;
}

uint64_t pd_frame_copy(uint64_t have, void *out, int maxW, int maxH, int *w, int *h) {
  pthread_mutex_lock(&frameLock);
  uint64_t serial = frameSerial;
  if (serial && serial != have && out && frameW <= maxW && frameH <= maxH) {
    memcpy(out, frameBuf, (size_t)frameW * frameH * 4);
    *w = frameW;
    *h = frameH;
  } else if (serial != have) {
    serial = have; // too big for the buffer: try again with a bigger one
  }
  pthread_mutex_unlock(&frameLock);
  return serial;
}

/* ---- audio: a ring of stereo frames ---- */
#define RING_FRAMES 16384
static int16_t ring[RING_FRAMES * 2];
static size_t ringRead, ringWrite; // in frames, unbounded counters
static pthread_mutex_t ringLock = PTHREAD_MUTEX_INITIALIZER;

static size_t audio_batch(const int16_t *data, size_t frames) {
  if (atomic_load(&ffOn)) // fast-forward plays nothing (STEADY music is the app's, not here yet)
    return frames;
  pthread_mutex_lock(&ringLock);
  for (size_t i = 0; i < frames; i++) {
    if (ringWrite - ringRead >= RING_FRAMES)
      ringRead++; // full: the oldest goes
    size_t at = (ringWrite % RING_FRAMES) * 2;
    ring[at] = data[i * 2];
    ring[at + 1] = data[i * 2 + 1];
    ringWrite++;
  }
  pthread_mutex_unlock(&ringLock);
  return frames;
}
static void audio_sample(int16_t l, int16_t r) {
  int16_t f[2] = {l, r};
  audio_batch(f, 1);
}
size_t pd_audio_read(int16_t *out, size_t frames) {
  pthread_mutex_lock(&ringLock);
  size_t n = ringWrite - ringRead;
  if (n > frames)
    n = frames;
  for (size_t i = 0; i < n; i++) {
    size_t at = ((ringRead + i) % RING_FRAMES) * 2;
    out[i * 2] = ring[at];
    out[i * 2 + 1] = ring[at + 1];
  }
  ringRead += n;
  pthread_mutex_unlock(&ringLock);
  return n;
}
size_t pd_audio_queued(void) {
  pthread_mutex_lock(&ringLock);
  size_t n = ringWrite - ringRead;
  pthread_mutex_unlock(&ringLock);
  return n;
}

/* ---- input ---- */
static void input_poll(void) {}
static int16_t input_state(unsigned port, unsigned device, unsigned index, unsigned id) {
  (void)index;
  if (port)
    return 0;
  if (device == RETRO_DEVICE_JOYPAD) {
    uint32_t k = atomic_load(&keys);
    return id == RETRO_DEVICE_ID_JOYPAD_MASK ? (int16_t)k : (int16_t)((k >> id) & 1);
  }
  if (device == RETRO_DEVICE_POINTER)
    return id == RETRO_DEVICE_ID_POINTER_X         ? (int16_t)atomic_load(&pointerX)
           : id == RETRO_DEVICE_ID_POINTER_Y       ? (int16_t)atomic_load(&pointerY)
           : id == RETRO_DEVICE_ID_POINTER_PRESSED ? pointerFrame
                                                   : 0;
  return 0;
}

/* ---- the game ---- */
bool pd_load(const char *path, const void *rom, size_t size) {
  if (loaded)
    pd_unload();
  core = pd_is_ds_path(path) ? &dsCore : &gbCore;
  if (!core->path_only && (!rom || !size))
    return false;
  pthread_mutex_lock(&optionLock);
  optionN = 0; // each core declares its own
  pthread_mutex_unlock(&optionLock);
  pixelFormat = RETRO_PIXEL_FORMAT_0RGB1555;
  if (core == &dsCore)
    pdds_host_screens(true);
  core->set_environment(environment);
  core->set_video_refresh(video_refresh);
  core->set_audio_sample(audio_sample);
  core->set_audio_sample_batch(audio_batch);
  core->set_input_poll(input_poll);
  core->set_input_state(input_state);
  core->init();
  free(romCopy);
  romCopy = NULL;
  struct retro_game_info gi = {path, NULL, 0, NULL};
  if (!core->path_only) {
    romCopy = malloc(size);
    if (!romCopy) {
      core->deinit();
      return false;
    }
    memcpy(romCopy, rom, size);
    gi.data = romCopy;
    gi.size = size;
  }
  if (!core->load_game(&gi)) {
    core->deinit();
    free(romCopy);
    romCopy = NULL;
    return false;
  }
  core->get_system_av_info(&av);
  atomic_store(&frameMs, 0);
  atomic_store(&frameMsPeak, 0);
  atomic_store(&framesRun, 0);
  pthread_mutex_lock(&ringLock);
  ringRead = ringWrite = 0;
  pthread_mutex_unlock(&ringLock);
  loaded = true;
  return true;
}
void pd_unload(void) {
  if (!loaded)
    return;
  core->unload_game();
  core->deinit();
  free(romCopy);
  romCopy = NULL;
  loaded = false;
  atomic_store(&penDown, false);
  atomic_store(&penPressed, false);
  pthread_mutex_lock(&frameLock);
  touchSerial = 0;
  frameSerial = 0;
  pthread_mutex_unlock(&frameLock);
}
bool pd_loaded(void) { return loaded; }
void pd_reset(void) {
  if (loaded)
    core->reset();
}
void pd_run(void) {
  // A tap that went down and up between two frames still reaches the core as one frame down, then up: the core
  // takes a tap on the quick menu / companion when the finger lifts.
  pointerFrame = atomic_load(&pointerDown) || atomic_exchange(&pointerPressed, false);
  if (!loaded)
    return;
  if (core == &dsCore) // a quick tap is one frame down, as the pointer's
    pdds_set_pen(atomic_load(&penDown) || atomic_exchange(&penPressed, false), atomic_load(&penX), atomic_load(&penY));
  struct timespec a, b;
  clock_gettime(CLOCK_MONOTONIC, &a);
  core->run();
  clock_gettime(CLOCK_MONOTONIC, &b);
  if (core == &dsCore)
    touch_grab();
  double ms = (double)(b.tv_sec - a.tv_sec) * 1000 + (double)(b.tv_nsec - a.tv_nsec) / 1e6;
  double avg = atomic_load(&frameMs);
  atomic_store(&frameMs, avg > 0 ? avg + (ms - avg) / 30 : ms);
  if (ms > atomic_load(&frameMsPeak))
    atomic_store(&frameMsPeak, ms);
  atomic_fetch_add(&framesRun, 1);
}
double pd_frame_ms(void) { return atomic_load(&frameMs); }
double pd_frame_ms_peak(void) { return atomic_exchange(&frameMsPeak, 0); }
uint64_t pd_frames_run(void) { return atomic_load(&framesRun); }
double pd_fps(void) { return av.timing.fps > 0 ? av.timing.fps : 59.7275; }
double pd_sample_rate(void) { return av.timing.sample_rate > 0 ? av.timing.sample_rate : 32768; }

size_t pd_save_image(void *out) {
  if (!loaded)
    return 0;
  void *mem = core->get_memory_data(RETRO_MEMORY_SAVE_RAM), *rtc = core->get_memory_data(RETRO_MEMORY_RTC);
  size_t n = mem ? core->get_memory_size(RETRO_MEMORY_SAVE_RAM) : 0;
  size_t rn = rtc ? core->get_memory_size(RETRO_MEMORY_RTC) : 0;
  if (out && n)
    memcpy(out, mem, n);
  if (out && n && rn)
    memcpy((uint8_t *)out + n, rtc, rn);
  return n ? n + rn : 0;
}
void pd_save_load(const void *d, size_t fn) {
  if (!loaded || !d)
    return;
  void *mem = core->get_memory_data(RETRO_MEMORY_SAVE_RAM), *rtc = core->get_memory_data(RETRO_MEMORY_RTC);
  size_t n = mem ? core->get_memory_size(RETRO_MEMORY_SAVE_RAM) : 0;
  size_t rn = rtc ? core->get_memory_size(RETRO_MEMORY_RTC) : 0;
  if (!n)
    return;
  memcpy(mem, d, fn < n ? fn : n);
  if (rn && fn > n)
    memcpy(rtc, (const uint8_t *)d + n, fn - n < rn ? fn - n : rn);
}
size_t pd_state_size(void) { return loaded ? core->serialize_size() : 0; }
bool pd_state_save(void *out, size_t size) { return loaded && out && core->serialize(out, size); }
bool pd_state_load(const void *data, size_t size) { return loaded && data && core->unserialize(data, size); }
