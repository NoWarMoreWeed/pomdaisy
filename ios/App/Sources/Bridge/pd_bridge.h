/* PomDaisy: the libretro frontend's C side. The core's callbacks land here (C function pointers are easier in C than
 * in Swift); Swift drives it through these functions. Everything but pd_frame_copy / pd_audio_read runs on the
 * emulation thread. */
#ifndef PD_BRIDGE_H
#define PD_BRIDGE_H
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

/* Joypad bits, RETRO_DEVICE_ID_JOYPAD_* numbering. */
enum {
  PD_KEY_B = 0, PD_KEY_Y = 1, PD_KEY_SELECT = 2, PD_KEY_START = 3, PD_KEY_UP = 4, PD_KEY_DOWN = 5, PD_KEY_LEFT = 6,
  PD_KEY_RIGHT = 7, PD_KEY_A = 8, PD_KEY_X = 9, PD_KEY_L = 10, PD_KEY_R = 11, PD_KEY_L2 = 12, PD_KEY_R2 = 13,
  PD_KEY_L3 = 14, PD_KEY_R3 = 15
};

/* Directories (system = BIOS / core files, saves = where the core may write its own). Before pd_load. */
void pd_set_dirs(const char *systemDir, const char *saveDir);
/* A Nintendo DS game (by its file name): the DS core plays it, and reads it from its path itself. */
bool pd_is_ds_path(const char *path);
/* Loads the core for the game and the game. A Game Boy game's bytes are copied; a DS game takes rom NULL / size 0. */
bool pd_load(const char *path, const void *rom, size_t size);
void pd_unload(void);
bool pd_loaded(void);
void pd_reset(void);
/* One frame. */
void pd_run(void);
double pd_fps(void);
/* How long the core takes for one frame, in milliseconds (a running average). Over 1000 / pd_fps() the game runs slow. */
double pd_frame_ms(void);
/* The slowest frame since the last call, in milliseconds (0 = none ran). */
double pd_frame_ms_peak(void);
/* Frames run since the game loaded: two readings a while apart give the frames per second. */
uint64_t pd_frames_run(void);
double pd_sample_rate(void);

/* Input, from any thread. */
void pd_set_keys(uint32_t mask);
/* The pointer in libretro's range (-0x7FFF..0x7FFF over the frame). */
void pd_set_pointer(int x, int y, bool down);
/* The screen area the game is shown in, in pixels: the core picks its canvas for it (pokedaisy_panel). */
void pd_set_panel(int w, int h);
void pd_set_fast_forward(bool on, int speed);

/* The newest frame as 32-bit BGRX (CGImage: byteOrder32Little | noneSkipFirst). Returns its serial (0 = none yet);
 * copies only when the serial differs from `have`. out must hold maxW * maxH * 4 bytes. */
uint64_t pd_frame_copy(uint64_t have, void *out, int maxW, int maxH, int *w, int *h);
/* Stereo int16 samples (frames of two), from the audio thread. Returns frames read. */
size_t pd_audio_read(int16_t *out, size_t frames);
size_t pd_audio_queued(void);

/* The battery save image (save memory, then the cartridge clock after it: mGBA's .sav layout). NULL out = size. */
size_t pd_save_image(void *out);
void pd_save_load(const void *data, size_t size);
/* States: NULL out = size. */
size_t pd_state_size(void);
bool pd_state_save(void *out, size_t size);
bool pd_state_load(const void *data, size_t size);

#endif
