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
#include <iomanip>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>

#define LOG_TAG "SharkCam"
#define LOGI(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// bionic's libdl uses this weak trampoline on Android 11. Declaring it weak lets the
// platform linker resolve it during relocation even though dlsym hides linker64 symbols.
extern "C" android_namespace_t* __loader_android_get_exported_namespace(const char*)
        __attribute__((weak));

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

struct QCarCamPlane {
    uint32_t width;
    uint32_t height;
    uint32_t stride;
    uint32_t size;
    void* buffer;
};

struct QCarCamBuffer {
    QCarCamPlane planes[3];
    uint32_t nPlanes;
    uint32_t flags;
};

struct QCarCamBuffers {
    uint32_t colorFormat;
    uint32_t flags;
    QCarCamBuffer* buffers;
    uint32_t count;
    uint32_t reserved;
};

struct QCarCamFrameInfo {
    uint32_t bufferIndex;
    uint8_t vendorData[44];
};

struct IonAllocationData {
    uint64_t length;
    uint32_t heapMask;
    uint32_t flags;
    int32_t fd;
    uint32_t unused;
};

static_assert(sizeof(QCarCamPlane) == 0x18, "unexpected QCarCam plane ABI");
static_assert(sizeof(QCarCamBuffer) == 0x50, "unexpected QCarCam buffer ABI");
static_assert(sizeof(QCarCamBuffers) == 0x18, "unexpected QCarCam buffers ABI");
static_assert(sizeof(QCarCamFrameInfo) == 0x30, "unexpected QCarCam frame ABI");
static_assert(sizeof(IonAllocationData) == 0x18, "unexpected ION ABI");

constexpr unsigned long ION_IOC_ALLOC = _IOWR('I', 0, IonAllocationData);
constexpr uint32_t kBufferCount = 5;

struct Client {
    void* library = nullptr;
    void* camera = nullptr;
    InitializeFn initialize = nullptr;
    QueryInputsFn queryInputs = nullptr;
    OpenFn open = nullptr;
    SetBuffersFn setBuffers = nullptr;
    StartFn start = nullptr;
    GetFrameFn getFrame = nullptr;
    ReleaseFrameFn releaseFrame = nullptr;
    StopFn stop = nullptr;
    CloseFn close = nullptr;
    UninitializeFn uninitialize = nullptr;
    bool initialized = false;
    bool streaming = false;
    std::string libraryPath;
    QCarCamBuffers qBuffers{};
    std::array<QCarCamBuffer, kBufferCount> buffers{};
    std::array<int, kBufferCount> bufferFds{{-1, -1, -1, -1, -1}};
    std::array<void*, kBufferCount> mappings{{MAP_FAILED, MAP_FAILED, MAP_FAILED,
                                              MAP_FAILED, MAP_FAILED}};
    size_t allocationSize = 0;
};

Client client;
std::mutex clientMutex;

template<typename T>
T symbol(const char* name) {
    return reinterpret_cast<T>(dlsym(client.library, name));
}

void releaseBuffersLocked() {
    for (size_t i = 0; i < client.bufferFds.size(); ++i) {
        if (client.mappings[i] != MAP_FAILED) {
            munmap(client.mappings[i], client.allocationSize);
            client.mappings[i] = MAP_FAILED;
        }
        if (client.bufferFds[i] >= 0) {
            close(client.bufferFds[i]);
            client.bufferFds[i] = -1;
        }
    }
    client.qBuffers = {};
    client.buffers = {};
    client.allocationSize = 0;
}

void unloadLocked() {
    if (client.streaming && client.camera && client.stop) {
        client.stop(client.camera);
    }
    if (client.camera && client.close) {
        client.close(client.camera);
    }
    releaseBuffersLocked();
    if (client.initialized && client.uninitialize) {
        client.uninitialize();
    }
    if (client.library) {
        dlclose(client.library);
    }
    client = Client{};
}

bool loadLocked(std::ostringstream& report) {
    if (client.library) return true;

    constexpr std::array<const char*, 6> candidates = {
            "/vendor/lib64/libais_client.so",
            "/vendor/lib64/libais_hidl_client.so",
            "/system/lib64/libais_hidl_client.so",
            "/system/system_ext/lib64/libais_hidl_client.so",
            "libais_client.so",
            "libais_hidl_client.so"
    };
    for (const char* path : candidates) {
        dlerror();
        client.library = dlopen(path, RTLD_NOW | RTLD_LOCAL);
        if (client.library) {
            client.libraryPath = path;
            break;
        }
        const char* error = dlerror();
        report << "dlopen " << path << ": " << (error ? error : "failed") << '\n';
    }

    // DiLink keeps AIS outside public.libraries.txt. Android's app namespace therefore
    // rejects a normal dlopen even though the file is readable. Qualcomm vendor libraries
    // and their dependencies live in the exported SP-HAL namespace, so ask the linker to
    // resolve the client there. This does not bypass filesystem permissions or require root.
    if (!client.library) {
        using GetExportedNamespaceFn = android_namespace_t* (*)(const char*);
        auto getNamespace = reinterpret_cast<GetExportedNamespaceFn>(
                dlsym(RTLD_DEFAULT, "android_get_exported_namespace"));
        const char* namespaceApi = "android_get_exported_namespace";
        if (!getNamespace) {
            // Android 11's libdl on this BYD image does not re-export the public alias,
            // while linker64 exposes the loader trampoline itself.
            getNamespace = reinterpret_cast<GetExportedNamespaceFn>(
                    dlsym(RTLD_DEFAULT, "__loader_android_get_exported_namespace"));
            namespaceApi = "__loader_android_get_exported_namespace";
        }
        if (!getNamespace && __loader_android_get_exported_namespace) {
            getNamespace = __loader_android_get_exported_namespace;
            namespaceApi = "weak __loader_android_get_exported_namespace";
        }
        report << namespaceApi << '=' << (getNamespace ? "yes" : "no") << '\n';
        constexpr std::array<const char*, 2> namespaceNames = {"sphal", "vendor"};
        if (getNamespace) {
            for (const char* namespaceName : namespaceNames) {
                android_namespace_t* targetNamespace = getNamespace(namespaceName);
                report << "namespace " << namespaceName << '=' << targetNamespace << '\n';
                if (!targetNamespace) continue;

                android_dlextinfo extinfo{};
                extinfo.flags = ANDROID_DLEXT_USE_NAMESPACE;
                extinfo.library_namespace = targetNamespace;
                for (const char* path : candidates) {
                    dlerror();
                    client.library = android_dlopen_ext(path, RTLD_NOW | RTLD_LOCAL, &extinfo);
                    if (client.library) {
                        client.libraryPath = std::string(path) + " namespace=" + namespaceName;
                        break;
                    }
                    const char* error = dlerror();
                    report << "android_dlopen_ext " << path << ": "
                           << (error ? error : "failed") << '\n';
                }
                if (client.library) break;
            }
        }
    }
    if (!client.library) {
        const char* error = dlerror();
        report << "AIS_NOT_FOUND: " << (error ? error : "dlopen failed");
        return false;
    }

    client.initialize = symbol<InitializeFn>("qcarcam_initialize");
    client.queryInputs = symbol<QueryInputsFn>("qcarcam_query_inputs");
    client.open = symbol<OpenFn>("qcarcam_open");
    client.setBuffers = symbol<SetBuffersFn>("qcarcam_s_buffers");
    client.start = symbol<StartFn>("qcarcam_start");
    client.getFrame = symbol<GetFrameFn>("qcarcam_get_frame");
    client.releaseFrame = symbol<ReleaseFrameFn>("qcarcam_release_frame");
    client.stop = symbol<StopFn>("qcarcam_stop");
    client.close = symbol<CloseFn>("qcarcam_close");
    client.uninitialize = symbol<UninitializeFn>("qcarcam_uninitialize");

    report << "library=" << client.libraryPath << '\n';
    report << "symbols initialize=" << (client.initialize ? "yes" : "no")
           << " query_inputs=" << (client.queryInputs ? "yes" : "no")
           << " open=" << (client.open ? "yes" : "no")
           << " s_buffers=" << (client.setBuffers ? "yes" : "no")
           << " start=" << (client.start ? "yes" : "no")
           << " get_frame=" << (client.getFrame ? "yes" : "no")
           << " release_frame=" << (client.releaseFrame ? "yes" : "no")
           << " stop=" << (client.stop ? "yes" : "no")
           << " close=" << (client.close ? "yes" : "no")
           << " uninitialize=" << (client.uninitialize ? "yes" : "no") << '\n';

    if (!client.initialize || !client.open || !client.setBuffers || !client.start ||
        !client.getFrame || !client.releaseFrame || !client.stop || !client.close) {
        report << "AIS_INCOMPATIBLE: required legacy symbols are missing";
        unloadLocked();
        return false;
    }
    report << "AIS_READY";
    return true;
}

bool findInputFormatLocked(int cameraId, uint32_t& width, uint32_t& height,
                           uint32_t& colorFormat, std::ostringstream& report) {
    if (!client.queryInputs) return false;
    unsigned int count = 0;
    if (client.queryInputs(nullptr, 0, &count) != 0 || count == 0 || count > 64) return false;
    constexpr size_t inputInfoSize = 0x140;
    std::string entries(count * inputInfoSize, '\0');
    unsigned int returned = count;
    if (client.queryInputs(entries.data(), count, &returned) != 0) return false;
    for (unsigned int index = 0; index < returned && index < count; ++index) {
        const uint8_t* entry = reinterpret_cast<const uint8_t*>(entries.data())
                + index * inputInfoSize;
        uint32_t id;
        memcpy(&id, entry, sizeof(id));
        if (id != static_cast<uint32_t>(cameraId)) continue;
        memcpy(&width, entry + 0xa4, sizeof(width));
        memcpy(&height, entry + 0xa8, sizeof(height));
        memcpy(&colorFormat, entry + 0x120, sizeof(colorFormat));
        report << "mode=" << width << 'x' << height << " format=0x" << std::hex
               << colorFormat << std::dec << '\n';
        return width > 0 && height > 0 && colorFormat != 0;
    }
    return false;
}

bool allocateBuffersLocked(uint32_t width, uint32_t height, uint32_t colorFormat,
                           std::ostringstream& report) {
    // qcarcam_test aligns packed UYVY stride to 64 bytes and allocates from ION heap bit 25.
    uint32_t stride = (width * 2u + 63u) & ~63u;
    uint32_t frameSize = stride * height;
    size_t allocationSize = (static_cast<size_t>(frameSize) + 4095u) & ~4095u;
    int ion = open("/dev/ion", O_RDONLY | O_CLOEXEC);
    report << "open(/dev/ion)=" << ion;
    if (ion < 0) {
        report << " errno=" << errno << ' ' << strerror(errno) << '\n';
        return false;
    }
    report << '\n';

    client.allocationSize = allocationSize;
    for (uint32_t index = 0; index < kBufferCount; ++index) {
        IonAllocationData allocation{allocationSize, 0x02000000u, 0, -1, 0};
        int result = ioctl(ion, ION_IOC_ALLOC, &allocation);
        if (result != 0) {
            report << "ION_IOC_ALLOC[" << index << "]=" << result << " errno=" << errno
                   << ' ' << strerror(errno) << '\n';
            close(ion);
            releaseBuffersLocked();
            return false;
        }
        client.bufferFds[index] = allocation.fd;
        client.mappings[index] = mmap(nullptr, allocationSize, PROT_READ | PROT_WRITE,
                                      MAP_SHARED, allocation.fd, 0);
        if (client.mappings[index] == MAP_FAILED) {
            report << "mmap[" << index << "] errno=" << errno << ' ' << strerror(errno) << '\n';
            close(ion);
            releaseBuffersLocked();
            return false;
        }
        auto& buffer = client.buffers[index];
        buffer.planes[0] = {width, height, stride, frameSize,
                            reinterpret_cast<void*>(static_cast<intptr_t>(allocation.fd))};
        buffer.nPlanes = 1;
    }
    close(ion);
    client.qBuffers.colorFormat = colorFormat;
    client.qBuffers.buffers = client.buffers.data();
    client.qBuffers.count = kBufferCount;
    report << "ION buffers=" << kBufferCount << " stride=" << stride
           << " frameSize=" << frameSize << " allocationSize=" << allocationSize << '\n';
    return true;
}

bool initializeLocked(std::ostringstream& report) {
    if (client.initialized) return true;
    int result = client.initialize(nullptr);
    report << "initialize=" << result << '\n';
    if (result != 0) return false;
    client.initialized = true;
    return true;
}

jstring asJavaString(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_nz_lonewolf_shark_camera_QCarCam_nativeProbe(JNIEnv* env, jclass) {
    std::lock_guard<std::mutex> lock(clientMutex);
    std::ostringstream report;
    if (loadLocked(report) && initializeLocked(report) && client.queryInputs) {
        unsigned int count = 0;
        int queryResult = client.queryInputs(nullptr, 0, &count);
        report << "query_inputs(count)=" << queryResult << " count=" << count << '\n';
        if (queryResult == 0 && count > 0 && count <= 64) {
            constexpr size_t inputInfoSize = 0x140;
            std::string entries(count * inputInfoSize, '\0');
            unsigned int returned = count;
            queryResult = client.queryInputs(entries.data(), count, &returned);
            report << "query_inputs(data)=" << queryResult << " returned=" << returned << '\n';
            if (queryResult == 0) {
                for (unsigned int index = 0; index < returned && index < count; ++index) {
                    const auto* words = reinterpret_cast<const uint32_t*>(
                            entries.data() + index * inputInfoSize);
                    float fps = 0.0f;
                    memcpy(&fps, entries.data() + index * inputInfoSize + 0xac, sizeof(fps));
                    report << "input id=" << words[0] << ' ' << words[0x0a4 / 4] << 'x'
                           << words[0x0a8 / 4] << " @ " << fps << " format=0x" << std::hex
                           << words[0x120 / 4] << std::dec << '\n';
                }
            }
        }
    }
    LOGI("%s", report.str().c_str());
    return asJavaString(env, report.str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_nz_lonewolf_shark_camera_QCarCam_nativeOpen(JNIEnv* env, jclass, jint cameraId) {
    std::lock_guard<std::mutex> lock(clientMutex);
    std::ostringstream report;
    if (client.streaming) {
        report << "ERROR stream already active";
        return asJavaString(env, report.str());
    }
    if (!loadLocked(report) || !initializeLocked(report)) {
        report << "ERROR initialize failed";
        return asJavaString(env, report.str());
    }

    client.camera = client.open(cameraId);
    report << "open(" << cameraId << ")=" << client.camera << '\n';
    if (!client.camera) {
        report << "ERROR qcarcam_open returned null";
        return asJavaString(env, report.str());
    }

    uint32_t width = 0;
    uint32_t height = 0;
    uint32_t colorFormat = 0;
    if (!findInputFormatLocked(cameraId, width, height, colorFormat, report) ||
        !allocateBuffersLocked(width, height, colorFormat, report)) {
        client.close(client.camera);
        client.camera = nullptr;
        report << "ERROR buffer allocation failed";
        return asJavaString(env, report.str());
    }

    int result = client.setBuffers(client.camera, &client.qBuffers);
    report << "s_buffers=" << result << '\n';
    if (result != 0) {
        client.close(client.camera);
        client.camera = nullptr;
        releaseBuffersLocked();
        report << "ERROR qcarcam_s_buffers failed";
        return asJavaString(env, report.str());
    }

    result = client.start(client.camera);
    report << "start=" << result << '\n';
    if (result != 0) {
        client.close(client.camera);
        client.camera = nullptr;
        releaseBuffersLocked();
        report << "ERROR qcarcam_start failed";
        return asJavaString(env, report.str());
    }
    client.streaming = true;

    QCarCamFrameInfo frame{};
    result = client.getFrame(client.camera, &frame, 1000000000ULL, 0);
    report << "first get_frame=" << result;
    if (result == 0 && frame.bufferIndex < kBufferCount) {
        const auto* bytes = static_cast<const uint8_t*>(client.mappings[frame.bufferIndex]);
        uint32_t checksum = 2166136261u;
        for (size_t offset = 0; offset < client.buffers[frame.bufferIndex].planes[0].size;
             offset += 4096) {
            checksum = (checksum ^ bytes[offset]) * 16777619u;
        }
        report << " buffer=" << frame.bufferIndex << " sampleChecksum=0x" << std::hex
               << checksum << std::dec;
        report << " release=" << client.releaseFrame(client.camera, frame.bufferIndex);
    }
    report << '\n';
    std::ostringstream final;
    final << "STREAM_STARTED inputId=" << cameraId << '\n' << report.str();
    LOGI("stream started inputId=%d", cameraId);
    return asJavaString(env, final.str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_nz_lonewolf_shark_camera_QCarCam_nativeStop(JNIEnv* env, jclass) {
    std::lock_guard<std::mutex> lock(clientMutex);
    std::ostringstream report;
    if (client.streaming && client.camera && client.stop) {
        report << "stop=" << client.stop(client.camera) << '\n';
    }
    client.streaming = false;
    if (client.camera && client.close) {
        report << "close=" << client.close(client.camera) << '\n';
    }
    client.camera = nullptr;
    releaseBuffersLocked();
    if (client.initialized && client.uninitialize) {
        report << "uninitialize=" << client.uninitialize() << '\n';
    }
    client.initialized = false;
    report << "STREAM_STOPPED";
    LOGI("stream stopped");
    return asJavaString(env, report.str());
}

static inline uint8_t clampColor(int value) {
    return static_cast<uint8_t>(value < 0 ? 0 : (value > 255 ? 255 : value));
}

extern "C" JNIEXPORT jintArray JNICALL
Java_nz_lonewolf_shark_camera_QCarCam_nativeReadFrame(
        JNIEnv* env, jclass, jint outputWidth, jint outputHeight) {
    std::lock_guard<std::mutex> lock(clientMutex);
    if (!client.streaming || !client.camera || outputWidth <= 0 || outputHeight <= 0 ||
        outputWidth > 1920 || outputHeight > 1300) {
        return nullptr;
    }

    QCarCamFrameInfo frame{};
    int result = client.getFrame(client.camera, &frame, 500000000ULL, 0);
    if (result != 0 || frame.bufferIndex >= kBufferCount ||
        client.mappings[frame.bufferIndex] == MAP_FAILED) {
        return nullptr;
    }

    const QCarCamPlane& plane = client.buffers[frame.bufferIndex].planes[0];
    const auto* source = static_cast<const uint8_t*>(client.mappings[frame.bufferIndex]);
    const jsize pixelCount = outputWidth * outputHeight;
    jintArray pixels = env->NewIntArray(pixelCount);
    if (!pixels) {
        client.releaseFrame(client.camera, frame.bufferIndex);
        return nullptr;
    }
    auto* destination = static_cast<jint*>(env->GetPrimitiveArrayCritical(pixels, nullptr));
    if (!destination) {
        client.releaseFrame(client.camera, frame.bufferIndex);
        return nullptr;
    }

    for (int y = 0; y < outputHeight; ++y) {
        uint32_t sourceY = static_cast<uint32_t>(y) * plane.height / outputHeight;
        const uint8_t* row = source + static_cast<size_t>(sourceY) * plane.stride;
        for (int x = 0; x < outputWidth; ++x) {
            uint32_t sourceX = static_cast<uint32_t>(x) * plane.width / outputWidth;
            sourceX &= ~1u;
            const uint8_t* uyvy = row + sourceX * 2u;
            int u = static_cast<int>(uyvy[0]) - 128;
            int luminance = static_cast<int>(uyvy[(x * plane.width / outputWidth) & 1 ? 3 : 1]) - 16;
            int v = static_cast<int>(uyvy[2]) - 128;
            if (luminance < 0) luminance = 0;
            int r = (298 * luminance + 409 * v + 128) >> 8;
            int g = (298 * luminance - 100 * u - 208 * v + 128) >> 8;
            int b = (298 * luminance + 516 * u + 128) >> 8;
            destination[y * outputWidth + x] = static_cast<jint>(0xff000000u |
                    (static_cast<uint32_t>(clampColor(r)) << 16) |
                    (static_cast<uint32_t>(clampColor(g)) << 8) | clampColor(b));
        }
    }

    env->ReleasePrimitiveArrayCritical(pixels, destination, 0);
    client.releaseFrame(client.camera, frame.bufferIndex);
    return pixels;
}

extern "C" JNIEXPORT jstring JNICALL
Java_nz_lonewolf_shark_camera_QCarCam_nativeProbeIds(
        JNIEnv* env, jclass, jint firstId, jint lastId) {
    std::lock_guard<std::mutex> lock(clientMutex);
    std::ostringstream report;
    if (client.streaming) {
        return asJavaString(env, "ERROR stop the active stream before probing");
    }
    if (firstId < 0 || lastId < firstId || lastId - firstId > 63) {
        return asJavaString(env, "ERROR invalid probe range");
    }
    if (!loadLocked(report) || !initializeLocked(report)) {
        report << "ERROR initialize failed";
        return asJavaString(env, report.str());
    }

    report << "openable input IDs:";
    bool found = false;
    for (int id = firstId; id <= lastId; ++id) {
        void* handle = client.open(id);
        if (handle) {
            report << ' ' << id;
            found = true;
            client.close(handle);
        }
    }
    if (!found) report << " none";
    report << "\nNote: openable does not prove that frames can be allocated.";
    return asJavaString(env, report.str());
}

JNIEXPORT jint JNI_OnLoad(JavaVM*, void*) {
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM*, void*) {
    std::lock_guard<std::mutex> lock(clientMutex);
    unloadLocked();
}
