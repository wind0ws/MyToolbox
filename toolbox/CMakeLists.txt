# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

project(toolbox VERSION 1.0.0 LANGUAGES C CXX)
message(STATUS "\n Current PROJECT_VERSION=${PROJECT_VERSION}, build_type=${CMAKE_BUILD_TYPE} \n")

set(CMAKE_VERBOSE_MAKEFILE ON)
set(CMAKE_COLOR_MAKEFILE ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED TRUE)
#set(CMAKE_C_EXTENSIONS FALSE)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED TRUE)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        toolbox

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/jni_toolbox.c
        src/main/cpp/jni_ring_buffer.c
        src/main/cpp/jni_msg_queue_handler.c
        src/main/cpp/jni_assets.c)

# Specifies a path to native header files.
include_directories(src/main/cpp/
        src/main/cpp/lcu)

# import static lib.
# lcu code from https://github.com/wind0ws/libcutils
add_library(lcu STATIC IMPORTED)
set_target_properties(lcu
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/cpp/libs/${ANDROID_ABI}/liblcu_a.a)

# for import .so
#add_library(hi-common SHARED IMPORTED)
#set_target_properties(hi-common
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/src/main/cpp/libs/${ANDROID_ABI}/libhi_common.so)

add_library( # Sets the name of the library.
        local_socket_server

        # Sets the library as a shared library.
        STATIC

        # Provides a relative path to your source file(s).
        src/main/cpp/local_socket_server.c)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

find_library(
        z-lib

        z)

find_library(
        android-lib

        android)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        local_socket_server

        lcu

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})

target_link_libraries( # Specifies the target library.
        toolbox

        # do not use ${xxx} reference module, because it's not variable
        #local_socket_server

        lcu

        # Links the target library to the log library
        # included in the NDK.
        ${android-lib}
        ${log-lib})
