cmake_minimum_required(VERSION 3.22.1)
project(sharkcam)

add_library(sharkcam SHARED qcarcam_bridge.cpp)

# The BYD vendor binaries declare three Android-internal libraries even though they do not
# import a single symbol from them (verified from their ELF symbol tables). Normal APK linker
# namespaces hide those framework libraries. Tiny name-compatible shims satisfy DT_NEEDED;
# actual libc/liblog symbols continue resolving from Android's public runtime.
add_library(vendor_libcpp_compat SHARED empty_vendor_compat.c)
set_target_properties(vendor_libcpp_compat PROPERTIES OUTPUT_NAME "c++")
add_library(vendor_libutils_compat SHARED empty_vendor_compat.c)
set_target_properties(vendor_libutils_compat PROPERTIES OUTPUT_NAME "utils")
add_library(vendor_libcutils_compat SHARED empty_vendor_compat.c)
set_target_properties(vendor_libcutils_compat PROPERTIES OUTPUT_NAME "cutils")

find_library(log-lib log)
find_library(android-lib android)
target_link_libraries(sharkcam ${log-lib} ${android-lib} dl)
