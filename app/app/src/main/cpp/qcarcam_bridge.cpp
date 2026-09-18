// Qualcomm AIS / QCarCam client for the BYD Shark 6 (DiLink 5, SA8155P).
// Loads /vendor/lib64/libais_client.so at runtime and streams raw UYVY frames.
// Multi stream: one slot per camera input so the recorder can run four cameras at once.
// ABI (struct layouts, ION heap, stride) verified on this head unit, see research/camera-poc.

#include <jni.h>
#include <android/log.h>
#include <android/dlext.h>
#include <cerrno>
#include <cstring>
#include <dlfcn.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <unistd.h>
#include <array>
#include <cstdint>
#include <map>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>

#define LOG_TAG "SharkCam"
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

extern "C" android_namespace_t* __loader_android_get_exported_namespace(const char*) __attribute__((weak));

namespace {

using InitializeFn = int (*)(void*);
using QueryInputsFn = int (*)(void*, unsigned int, unsigned int*);
using OpenFn = void* (*)(int);
using SetBuffersFn = int (*)(void*, void*);
using StartFn = int (*)(void*);
using GetFrameFn = int (*)(void*, void*, uint64_t, uint32_t);
using ReleaseFrameFn = int (*)(void*, uint32_t);
using StopFn = int (*)(void*);
using CloseFn = int (*)(void*);
using UninitializeFn = int (*)();

struct QCarCamPlane { uint32_t width, height, stride, size; void* buffer; };
struct QCarCamBuffer { QCarCamPlane planes[3]; uint32_t nPlanes; uint32_t flags; };
struct QCarCamBuffers { uint32_t colorFormat; uint32_t flags; QCarCamBuffer* buffers; uint32_t count; uint32_t reserved; };
struct QCarCamFrameInfo { uint32_t bufferIndex; uint8_t vendorData[44]; };
struct IonAllocationData { uint64_t length; uint32_t heapMask; uint32_t flags; int32_t fd; uint32_t unused; };
static_assert(sizeof(QCarCamPlane) == 0x18, "plane ABI");
static_assert(sizeof(QCarCamBuffer) == 0x50, "buffer ABI");
static_assert(sizeof(QCarCamBuffers) == 0x18, "buffers ABI");
static_assert(sizeof(QCarCamFrameInfo) == 0x30, "frame ABI");
static_assert(sizeof(IonAllocationData) == 0x18, "ION ABI");

constexpr unsigned long ION_IOC_ALLOC = _IOWR('I', 0, IonAllocationData);
constexpr uint32_t kBufferCount = 5;
constexpr size_t kInputInfoSize = 0x140;

struct Lib {
    void* handle = nullptr;
    InitializeFn initialize = nullptr; QueryInputsFn queryInputs = nullptr; OpenFn open = nullptr;
    SetBuffersFn setBuffers = nullptr; StartFn start = nullptr; GetFrameFn getFrame = nullptr;
    ReleaseFrameFn releaseFrame = nullptr; StopFn stop = nullptr; CloseFn close = nullptr; UninitializeFn uninitialize = nullptr;
    bool initialized = false;
    std::string path;
};

struct Stream {
    int id = -1;
    void* camera = nullptr;
    bool streaming = false;
    uint32_t width = 0, height = 0, stride = 0, frameSize = 0;
    size_t allocationSize = 0;
    QCarCamBuffers qBuffers{};
    std::array<QCarCamBuffer, kBufferCount> buffers{};
    std::array<int, kBufferCount> fds{{-1, -1, -1, -1, -1}};
    std::array<void*, kBufferCount> maps{{MAP_FAILED, MAP_FAILED, MAP_FAILED, MAP_FAILED, MAP_FAILED}};
    std::mutex mutex;
};

Lib lib;
std::mutex libMutex;
std::map<int, Stream*> streams;
std::mutex streamsMutex;

template<typename T> T sym(const char* n) { return reinterpret_cast<T>(dlsym(lib.handle, n)); }

bool loadLib(std::ostringstream& r) {
    if (lib.handle) return true;
    const char* candidates[] = {"/vendor/lib64/libais_client.so", "/vendor/lib64/libais_hidl_client.so", "libais_client.so"};
    for (auto p : candidates) { dlerror(); lib.handle = dlopen(p, RTLD_NOW | RTLD_LOCAL); if (lib.handle) { lib.path = p; break; } }
    if (!lib.handle) {
        using GetNs = android_namespace_t* (*)(const char*);
        auto getNs = reinterpret_cast<GetNs>(dlsym(RTLD_DEFAULT, "android_get_exported_namespace"));
        if (!getNs) getNs = reinterpret_cast<GetNs>(dlsym(RTLD_DEFAULT, "__loader_android_get_exported_namespace"));
        if (!getNs && __loader_android_get_exported_namespace) getNs = __loader_android_get_exported_namespace;
        if (getNs) for (auto nsName : {"sphal", "vendor"}) {
            auto ns = getNs(nsName); if (!ns) continue;
            android_dlextinfo ext{}; ext.flags = ANDROID_DLEXT_USE_NAMESPACE; ext.library_namespace = ns;
            for (auto p : candidates) { dlerror(); lib.handle = android_dlopen_ext(p, RTLD_NOW | RTLD_LOCAL, &ext); if (lib.handle) { lib.path = std::string(p) + " ns=" + nsName; break; } }
            if (lib.handle) break;
        }
    }
    if (!lib.handle) { r << "AIS_NOT_FOUND"; return false; }
    lib.initialize = sym<InitializeFn>("qcarcam_initialize"); lib.queryInputs = sym<QueryInputsFn>("qcarcam_query_inputs");
    lib.open = sym<OpenFn>("qcarcam_open"); lib.setBuffers = sym<SetBuffersFn>("qcarcam_s_buffers"); lib.start = sym<StartFn>("qcarcam_start");
    lib.getFrame = sym<GetFrameFn>("qcarcam_get_frame"); lib.releaseFrame = sym<ReleaseFrameFn>("qcarcam_release_frame");
    lib.stop = sym<StopFn>("qcarcam_stop"); lib.close = sym<CloseFn>("qcarcam_close"); lib.uninitialize = sym<UninitializeFn>("qcarcam_uninitialize");
    if (!lib.initialize || !lib.open || !lib.setBuffers || !lib.start || !lib.getFrame || !lib.releaseFrame || !lib.stop || !lib.close) { r << "AIS_INCOMPATIBLE"; dlclose(lib.handle); lib = Lib{}; return false; }
    r << "library=" << lib.path << "\nAIS_READY\n";
    return true;
}

bool initLib(std::ostringstream& r) {
    if (lib.initialized) return true;
    int res = lib.initialize(nullptr); r << "initialize=" << res << '\n';
    lib.initialized = res == 0; return lib.initialized;
}

bool inputFormat(int id, uint32_t& w, uint32_t& h, uint32_t& fmt, std::ostringstream& r) {
    if (!lib.queryInputs) return false;
    unsigned int count = 0;
    if (lib.queryInputs(nullptr, 0, &count) != 0 || count == 0 || count > 64) return false;
    std::string e(count * kInputInfoSize, '\0'); unsigned int ret = count;
    if (lib.queryInputs(e.data(), count, &ret) != 0) return false;
    for (unsigned i = 0; i < ret && i < count; ++i) {
        auto p = reinterpret_cast<const uint8_t*>(e.data()) + i * kInputInfoSize;
        uint32_t iid; memcpy(&iid, p, 4); if (iid != (uint32_t) id) continue;
        memcpy(&w, p + 0xa4, 4); memcpy(&h, p + 0xa8, 4); memcpy(&fmt, p + 0x120, 4);
        r << "mode=" << w << 'x' << h << " fmt=0x" << std::hex << fmt << std::dec << '\n';
        return w > 0 && h > 0 && fmt != 0;
    }
    return false;
}

void freeBuffers(Stream& s) {
    for (size_t i = 0; i < kBufferCount; ++i) {
        if (s.maps[i] != MAP_FAILED) { munmap(s.maps[i], s.allocationSize); s.maps[i] = MAP_FAILED; }
        if (s.fds[i] >= 0) { close(s.fds[i]); s.fds[i] = -1; }
    }
    s.qBuffers = {}; s.buffers = {}; s.allocationSize = 0;
}

bool allocBuffers(Stream& s, uint32_t fmt, std::ostringstream& r) {
    s.stride = (s.width * 2u + 63u) & ~63u; s.frameSize = s.stride * s.height;
    s.allocationSize = ((size_t) s.frameSize + 4095u) & ~4095u;
    int ion = open("/dev/ion", O_RDONLY | O_CLOEXEC);
    if (ion < 0) { r << "ion open failed errno=" << errno << '\n'; return false; }
    for (uint32_t i = 0; i < kBufferCount; ++i) {
        IonAllocationData a{s.allocationSize, 0x02000000u, 0, -1, 0};
        if (ioctl(ion, ION_IOC_ALLOC, &a) != 0) { r << "ION_IOC_ALLOC[" << i << "] errno=" << errno << '\n'; close(ion); freeBuffers(s); return false; }
        s.fds[i] = a.fd;
        s.maps[i] = mmap(nullptr, s.allocationSize, PROT_READ | PROT_WRITE, MAP_SHARED, a.fd, 0);
        if (s.maps[i] == MAP_FAILED) { r << "mmap[" << i << "] errno=" << errno << '\n'; close(ion); freeBuffers(s); return false; }
        s.buffers[i].planes[0] = {s.width, s.height, s.stride, s.frameSize, reinterpret_cast<void*>((intptr_t) a.fd)};
        s.buffers[i].nPlanes = 1;
    }
    close(ion);
    s.qBuffers.colorFormat = fmt; s.qBuffers.buffers = s.buffers.data(); s.qBuffers.count = kBufferCount;
    return true;
}

Stream* findStream(int id) { std::lock_guard<std::mutex> l(streamsMutex); auto it = streams.find(id); return it == streams.end() ? nullptr : it->second; }

void stopStream(Stream* s, std::ostringstream& r) {
    std::lock_guard<std::mutex> l(s->mutex);
    if (s->streaming && s->camera) r << "stop=" << lib.stop(s->camera) << ' ';
    s->streaming = false;
    if (s->camera) { r << "close=" << lib.close(s->camera); s->camera = nullptr; }
    freeBuffers(*s);
}

jstring js(JNIEnv* env, const std::string& v) { return env->NewStringUTF(v.c_str()); }
inline uint8_t clamp8(int v) { return (uint8_t) (v < 0 ? 0 : v > 255 ? 255 : v); }

} // namespace

extern "C" JNIEXPORT jstring JNICALL Java_nz_lonewolf_shark_camera_QCarCam_nativeProbe(JNIEnv* env, jclass) {
    std::lock_guard<std::mutex> l(libMutex);
    std::ostringstream r;
    if (loadLib(r) && initLib(r) && lib.queryInputs) {
        unsigned int count = 0; int q = lib.queryInputs(nullptr, 0, &count);
        r << "inputs=" << count << '\n';
        if (q == 0 && count > 0 && count <= 64) {
            std::string e(count * kInputInfoSize, '\0'); unsigned int ret = count;
            if (lib.queryInputs(e.data(), count, &ret) == 0)
                for (unsigned i = 0; i < ret && i < count; ++i) {
                    auto w = reinterpret_cast<const uint32_t*>(e.data() + i * kInputInfoSize);
                    float fps = 0; memcpy(&fps, e.data() + i * kInputInfoSize + 0xac, 4);
                    r << "id " << w[0] << ": " << w[0xa4 / 4] << 'x' << w[0xa8 / 4] << " @ " << fps << '\n';
                }
        }
    }
    LOGW("%s", r.str().c_str());
    return js(env, r.str());
}

/** Open and start one input. Safe to call for an id that is already streaming. */
extern "C" JNIEXPORT jstring JNICALL Java_nz_lonewolf_shark_camera_QCarCam_nativeOpen(JNIEnv* env, jclass, jint id) {
    std::ostringstream r;
    { std::lock_guard<std::mutex> l(libMutex); if (!loadLib(r) || !initLib(r)) { r << "ERROR init"; return js(env, r.str()); } }
    Stream* s = findStream(id);
    if (!s) { std::lock_guard<std::mutex> l(streamsMutex); s = new Stream(); s->id = id; streams[id] = s; }
    std::lock_guard<std::mutex> l(s->mutex);
    if (s->streaming) { r << "STREAM_STARTED id=" << id << " (already)"; return js(env, r.str()); }
    uint32_t fmt = 0;
    if (!inputFormat(id, s->width, s->height, fmt, r)) { r << "ERROR unknown input " << id; return js(env, r.str()); }
    s->camera = lib.open(id);
    if (!s->camera) { r << "ERROR open returned null"; return js(env, r.str()); }
    if (!allocBuffers(*s, fmt, r)) { lib.close(s->camera); s->camera = nullptr; r << "ERROR buffers"; return js(env, r.str()); }
    int res = lib.setBuffers(s->camera, &s->qBuffers); r << "s_buffers=" << res << ' ';
    if (res == 0) { res = lib.start(s->camera); r << "start=" << res << '\n'; }
    if (res != 0) { lib.close(s->camera); s->camera = nullptr; freeBuffers(*s); r << "ERROR start"; return js(env, r.str()); }
    s->streaming = true;
    LOGW("stream %d started %ux%u stride %u", id, s->width, s->height, s->stride);
    return js(env, "STREAM_STARTED id=" + std::to_string(id) + "\n" + r.str());
}

extern "C" JNIEXPORT jstring JNICALL Java_nz_lonewolf_shark_camera_QCarCam_nativeStopOne(JNIEnv* env, jclass, jint id) {
    Stream* s = findStream(id);
    std::ostringstream r;
    if (s) { stopStream(s, r); LOGW("stream %d stopped", id); } else r << "not open";
    return js(env, r.str());
}

extern "C" JNIEXPORT jstring JNICALL Java_nz_lonewolf_shark_camera_QCarCam_nativeStop(JNIEnv* env, jclass) {
    std::ostringstream r;
    std::vector<Stream*> all;
    { std::lock_guard<std::mutex> l(streamsMutex); for (auto& kv : streams) all.push_back(kv.second); }
    for (auto s : all) { stopStream(s, r); r << " [" << s->id << "]\n"; }
    // No qcarcam_uninitialize here: it tears down the AIS link that BYD's own camera
    // pipeline shares with us and their 360 view goes blank until the head unit restarts.
    r << "STREAM_STOPPED";
    LOGW("all streams stopped");
    return js(env, r.str());
}

extern "C" JNIEXPORT jintArray JNICALL Java_nz_lonewolf_shark_camera_QCarCam_nativeStreamSize(JNIEnv* env, jclass, jint id) {
    Stream* s = findStream(id);
    jintArray out = env->NewIntArray(2);
    jint v[2] = {s ? (jint) s->width : 0, s ? (jint) s->height : 0};
    env->SetIntArrayRegion(out, 0, 2, v);
    return out;
}

/** Preview: nearest neighbour UYVY to ARGB into a Java int array. */
extern "C" JNIEXPORT jintArray JNICALL Java_nz_lonewolf_shark_camera_QCarCam_nativeReadFrame(JNIEnv* env, jclass, jint id, jint ow, jint oh) {
    Stream* s = findStream(id);
    if (!s || ow <= 0 || oh <= 0) return nullptr;
    std::lock_guard<std::mutex> l(s->mutex);
    if (!s->streaming) return nullptr;
    QCarCamFrameInfo f{};
    if (lib.getFrame(s->camera, &f, 500000000ULL, 0) != 0 || f.bufferIndex >= kBufferCount) return nullptr;
    auto src = static_cast<const uint8_t*>(s->maps[f.bufferIndex]);
    jintArray px = env->NewIntArray(ow * oh);
    auto dst = static_cast<jint*>(env->GetPrimitiveArrayCritical(px, nullptr));
    if (dst) {
        std::vector<uint32_t> xmap(ow); for (int x = 0; x < ow; ++x) xmap[x] = ((uint32_t) x * s->width / ow) & ~1u;
        for (int y = 0; y < oh; ++y) {
            const uint8_t* row = src + (size_t) ((uint32_t) y * s->height / oh) * s->stride;
            jint* drow = dst + (size_t) y * ow;
            for (int x = 0; x < ow; ++x) {
                const uint8_t* p = row + xmap[x] * 2;
                int u = p[0] - 128, yy = p[1] - 16, v = p[2] - 128; if (yy < 0) yy = 0;
                int r = (298 * yy + 409 * v + 128) >> 8, g = (298 * yy - 100 * u - 208 * v + 128) >> 8, b = (298 * yy + 516 * u + 128) >> 8;
                drow[x] = (jint) (0xff000000u | (clamp8(r) << 16) | (clamp8(g) << 8) | clamp8(b));
            }
        }
        env->ReleasePrimitiveArrayCritical(px, dst, 0);
    }
    lib.releaseFrame(s->camera, f.bufferIndex);
    return px;
}

/**
 * Encoder path: one frame as NV12 (Y plane then interleaved UV) scaled to ow x oh into a direct
 * ByteBuffer, which is the MediaCodec input buffer. Returns bytes written or 0 on timeout.
 * Nearest neighbour scale; ow and oh must be even.
 */
extern "C" JNIEXPORT jint JNICALL Java_nz_lonewolf_shark_camera_QCarCam_nativeReadNv12(JNIEnv* env, jclass, jint id, jobject buf, jint ow, jint oh) {
    Stream* s = findStream(id);
    if (!s || ow <= 0 || oh <= 0) return 0;
    auto dst = static_cast<uint8_t*>(env->GetDirectBufferAddress(buf));
    jlong cap = env->GetDirectBufferCapacity(buf);
    const jint need = ow * oh * 3 / 2;
    if (!dst || cap < need) return 0;
    std::lock_guard<std::mutex> l(s->mutex);
    if (!s->streaming) return 0;
    QCarCamFrameInfo f{};
    if (lib.getFrame(s->camera, &f, 500000000ULL, 0) != 0 || f.bufferIndex >= kBufferCount) return 0;
    auto src = static_cast<const uint8_t*>(s->maps[f.bufferIndex]);
    std::vector<uint32_t> xmap(ow); for (int x = 0; x < ow; ++x) xmap[x] = ((uint32_t) x * s->width / ow) & ~1u;
    uint8_t* yPlane = dst; uint8_t* uvPlane = dst + (size_t) ow * oh;
    for (int y = 0; y < oh; ++y) {
        const uint8_t* row = src + (size_t) ((uint32_t) y * s->height / oh) * s->stride;
        uint8_t* yrow = yPlane + (size_t) y * ow;
        for (int x = 0; x < ow; x += 2) {
            const uint8_t* p = row + xmap[x] * 2;
            yrow[x] = p[1]; yrow[x + 1] = p[3];
        }
        if ((y & 1) == 0) {
            uint8_t* uvrow = uvPlane + (size_t) (y / 2) * ow;
            for (int x = 0; x < ow; x += 2) { const uint8_t* p = row + xmap[x] * 2; uvrow[x] = p[0]; uvrow[x + 1] = p[2]; }
        }
    }
    lib.releaseFrame(s->camera, f.bufferIndex);
    return need;
}

JNIEXPORT jint JNI_OnLoad(JavaVM*, void*) { return JNI_VERSION_1_6; }
