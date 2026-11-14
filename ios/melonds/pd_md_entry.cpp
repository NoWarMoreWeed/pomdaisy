// PomDaisy (iOS): melonDS - the Android app's DS core (app/src/main/cpp/nds/pk_nds.cpp: melonDS 1.1 with our patch,
// EXPAND, the field reads) - behind libretro-shaped entry points of its own (pd_md_core.h), so the app's bridge drives
// it like the other two cores. No JIT (an App Store / TestFlight build can't run one), no GPU renderer.
//
// What the bridge gets: the top screen as the frame - with EXPAND (on by default) the overworld drawn past the screen's
// edges to fill the shape of the view the app shows it in (the pokedaisy_panel option) - and the touch screen through
// pdmd_touch_screen. The battery save is the core's own file (<save dir>/<game>.sav, the same file the DSperate core
// uses); states go through a file of their own, since the core takes and reads them from files.
#include "libretro.h"
#include "../App/Sources/Bridge/pd_md_core.h"
#include "pk_nds.h"
#include "pk_nds_cores.h"
#include "pk_nds_field.h"
#include "pk_nds_wide.h"

#include <sys/stat.h>

#include <atomic>

#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

namespace {
retro_environment_t env;
retro_video_refresh_t video;
retro_audio_sample_batch_t audioBatch;
retro_input_poll_t inputPoll;
retro_input_state_t inputState;

constexpr int SAMPLE_RATE = 32768;
constexpr double FPS = 59.8261;

std::string saveDir, statePath;
bool loaded = false, expand = true;
int panelW = 0, panelH = 0;
int winX = 0, winY = 0, winW = PK_NDS_W, winH = PK_NDS_H;
bool penDown = false;
int penX = 0, penY = 0;
size_t stateSize = 0;
std::atomic<int> fieldBits {0}; // the field read after the last frame, for the app's thread
const PkNdsFieldGame* field = nullptr; // the cart's field-read table (null: none)
std::vector<uint32_t> frame(PK_NDS_WIDE_MAX_W * PK_NDS_WIDE_MAX_H);
uint32_t touchScreen[PK_NDS_W * PK_NDS_H];
int16_t audio[8192 * 2];

// The core's pixels are 0xAABBGGRR (mGBA's layout), the bridge's XRGB8888 0x00RRGGBB.
inline uint32_t xrgb(uint32_t p) { return (p & 0xFF00u) | ((p & 0xFFu) << 16) | ((p >> 16) & 0xFFu); }

std::string dirFrom(unsigned cmd)
{
    const char* d = nullptr;
    return env && env(cmd, &d) && d ? d : ".";
}

// The view's shape (pokedaisy_panel, "WxH" in pixels) decides EXPAND's window: as wide as the view at the screen's
// 192 rows, or (a view taller than 4:3, upright) as tall as it at 256 columns - within EXPAND's largest margins.
void readPanel()
{
    retro_variable v {"pokedaisy_panel", nullptr};
    int w = 0, h = 0;
    if (env && env(RETRO_ENVIRONMENT_GET_VARIABLE, &v) && v.value && sscanf(v.value, "%dx%d", &w, &h) == 2 && w > 0 && h > 0) {
        panelW = w;
        panelH = h;
    }
}

void updateWindow()
{
    int w = PK_NDS_W, h = PK_NDS_H;
    if (panelW > 0 && panelH > 0) {
        if (panelW * 3 >= panelH * 4) {
            w = (PK_NDS_H * panelW / panelH) & ~1;
            if (w > PK_NDS_WIDE_MAX_W) w = PK_NDS_WIDE_MAX_W;
        } else {
            h = (PK_NDS_W * panelH / panelW) & ~1;
            if (h > PK_NDS_WIDE_MAX_H) h = PK_NDS_WIDE_MAX_H;
        }
    }
    int x = -(w - PK_NDS_W) / 2, y = -(h - PK_NDS_H) / 2;
    if (x == winX && y == winY && w == winW && h == winH) return;
    winX = x;
    winY = y;
    winW = w;
    winH = h;
    pk_ndsm_wide_window(x, y, w, h);
}

// GBA key bits 0-9 (A, B, SELECT, START, RIGHT, LEFT, UP, DOWN, R, L) plus X (10) and Y (11), from libretro's joypad.
uint32_t keys()
{
    static const unsigned map[12] = {RETRO_DEVICE_ID_JOYPAD_A, RETRO_DEVICE_ID_JOYPAD_B, RETRO_DEVICE_ID_JOYPAD_SELECT,
        RETRO_DEVICE_ID_JOYPAD_START, RETRO_DEVICE_ID_JOYPAD_RIGHT, RETRO_DEVICE_ID_JOYPAD_LEFT, RETRO_DEVICE_ID_JOYPAD_UP,
        RETRO_DEVICE_ID_JOYPAD_DOWN, RETRO_DEVICE_ID_JOYPAD_R, RETRO_DEVICE_ID_JOYPAD_L, RETRO_DEVICE_ID_JOYPAD_X,
        RETRO_DEVICE_ID_JOYPAD_Y};
    uint32_t k = 0;
    for (int i = 0; i < 12; i++)
        if (inputState && inputState(0, RETRO_DEVICE_JOYPAD, 0, map[i])) k |= 1u << i;
    return k;
}

// The scenes EXPAND shows wide: the overworld (with a text box over it), a fade between two maps, and a battle where
// the core fills the margins (Black / White's all-3D battles; Gen 4's from the battle background, as pk_nds.cpp decides).
// A menu screen and a cart without field reads keep the screen's own 4:3.
bool wideScene()
{
    if ((fieldBits.load() & PK_NDS_FIELD_BATTLE) && field)
        return pk_nds_battle_3d(field) || pk_nds_battle_margins(field);
    int scene = pk_ndsm_wide_scene();
    return scene == 0 || scene == 1 || scene == 5;
}
} // namespace

extern "C" {
void pdmd_set_environment(retro_environment_t cb) { env = cb; }
void pdmd_set_video_refresh(retro_video_refresh_t cb) { video = cb; }
void pdmd_set_audio_sample(retro_audio_sample_t) {}
void pdmd_set_audio_sample_batch(retro_audio_sample_batch_t cb) { audioBatch = cb; }
void pdmd_set_input_poll(retro_input_poll_t cb) { inputPoll = cb; }
void pdmd_set_input_state(retro_input_state_t cb) { inputState = cb; }
void pdmd_init(void) {}
void pdmd_deinit(void) {}

bool pdmd_load_game(const struct retro_game_info* info)
{
    if (!info || !info->path) return false;
    retro_pixel_format fmt = RETRO_PIXEL_FORMAT_XRGB8888;
    if (!env || !env(RETRO_ENVIRONMENT_SET_PIXEL_FORMAT, &fmt)) return false;
    saveDir = dirFrom(RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY);
    std::string base = info->path;
    base = base.substr(base.find_last_of('/') + 1);
    base = base.substr(0, base.find_last_of('.'));
    statePath = saveDir + "/.melonds-state.tmp";
    if (!pk_ndsm_init(info->path, (saveDir + "/" + base + ".sav").c_str(), SAMPLE_RATE)) return false;
    loaded = true;
    char code[5] = {};
    field = pk_ndsm_rom_code(code) ? pk_nds_field_game(code, pk_ndsm_rom_revision()) : nullptr;
    winX = winY = 0;
    winW = PK_NDS_W;
    winH = PK_NDS_H;
    readPanel();
    pk_ndsm_wide_enable(expand);
    updateWindow();
    return true;
}

void pdmd_unload_game(void)
{
    if (!loaded) return;
    pk_ndsm_finish_save(120); // a save the game is still writing is finished first
    pk_ndsm_deinit();
    remove(statePath.c_str());
    loaded = false;
    fieldBits.store(0);
}

void pdmd_get_system_av_info(struct retro_system_av_info* info)
{
    info->geometry = {PK_NDS_W, PK_NDS_H, PK_NDS_WIDE_MAX_W, PK_NDS_WIDE_MAX_H, 4.0f / 3.0f};
    info->timing = {FPS, (double) SAMPLE_RATE};
}

void pdmd_run(void)
{
    if (!loaded) return;
    bool changed = false;
    if (env && env(RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE, &changed) && changed) {
        readPanel();
        updateWindow();
    }
    if (inputPoll) inputPoll();
    pk_ndsm_set_keys(keys());
    pk_ndsm_touch(penDown, penX, penY);
    pk_ndsm_run_frame();
    fieldBits.store(pk_ndsm_field_bits());

    int n = pk_ndsm_read_audio(audio, 8192);
    if (n > 0 && audioBatch) audioBatch(audio, (size_t) n);

    const uint32_t* bottom = pk_ndsm_bottom();
    if (bottom)
        for (int i = 0; i < PK_NDS_W * PK_NDS_H; i++) touchScreen[i] = xrgb(bottom[i]);

    if (!video) return;
    if (expand && wideScene() && pk_ndsm_wide_draw(frame.data(), winX, winY, winW, winH)) {
        for (int i = 0; i < winW * winH; i++) frame[i] = xrgb(frame[i]);
        video(frame.data(), (unsigned) winW, (unsigned) winH, (size_t) winW * 4);
    } else if (const uint32_t* top = pk_ndsm_top()) {
        for (int i = 0; i < PK_NDS_W * PK_NDS_H; i++) frame[i] = xrgb(top[i]);
        video(frame.data(), PK_NDS_W, PK_NDS_H, PK_NDS_W * 4);
    }
}

void pdmd_reset(void)
{
    // melonDS has no reset of its own here: the game is started again from its file, its save written first.
    // (Not reached from the app's menu for DS games without a ROM path; kept simple.)
}

size_t pdmd_serialize_size(void)
{
    stateSize = 0;
    if (!loaded) return 0;
    pk_ndsm_flush_save();
    struct stat st {};
    if (pk_ndsm_save_state(statePath.c_str()) && stat(statePath.c_str(), &st) == 0) stateSize = (size_t) st.st_size;
    return stateSize;
}

bool pdmd_serialize(void* data, size_t size)
{
    if (!loaded || !stateSize || size < stateSize) return false;
    FILE* f = fopen(statePath.c_str(), "rb");
    if (!f) return false;
    bool ok = fread(data, 1, stateSize, f) == stateSize;
    fclose(f);
    if (ok && size > stateSize) memset(static_cast<uint8_t*>(data) + stateSize, 0, size - stateSize);
    return ok;
}

bool pdmd_unserialize(const void* data, size_t size)
{
    if (!loaded || !data || size < 16) return false;
    FILE* f = fopen(statePath.c_str(), "wb");
    if (!f) return false;
    bool ok = fwrite(data, 1, size, f) == size;
    ok = fclose(f) == 0 && ok;
    return ok && pk_ndsm_load_state(statePath.c_str());
}

// The battery save is the core's own file: nothing for the bridge to write.
void* pdmd_get_memory_data(unsigned) { return nullptr; }
size_t pdmd_get_memory_size(unsigned) { return 0; }

void pdmd_set_pen(bool down, int x, int y)
{
    penDown = down;
    penX = x < 0 ? 0 : x > PK_NDS_W - 1 ? PK_NDS_W - 1 : x;
    penY = y < 0 ? 0 : y > PK_NDS_H - 1 ? PK_NDS_H - 1 : y;
}
const uint32_t* pdmd_touch_screen(void) { return loaded ? touchScreen : nullptr; }

void pdmd_set_expand(bool on)
{
    if (expand == on) return;
    expand = on;
    if (loaded) pk_ndsm_wide_enable(on);
}
int pdmd_field_bits(void) { return loaded ? fieldBits.load() : 0; }
}
