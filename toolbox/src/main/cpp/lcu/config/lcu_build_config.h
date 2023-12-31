#pragma once
#ifndef _LCU_BUILD_CONFIG_H
#define _LCU_BUILD_CONFIG_H

//Attention: do not change this file directly, the header file will generate by cmake.

#ifdef _WIN32

#define LCU_WIN_PTHREAD_IMPLEMENT_MODE_SIMPLE (0)
#define LCU_WIN_PTHREAD_IMPLEMENT_MODE_LIB    (1)

//#define _LCU_CFG_WIN_PTHREAD_MODE 1
#define _WIN_PTHREAD_MODE 1

#if(_WIN_PTHREAD_MODE ==  LCU_WIN_PTHREAD_IMPLEMENT_MODE_SIMPLE)
  #define _LCU_CFG_WIN_PTHREAD_MODE LCU_WIN_PTHREAD_IMPLEMENT_MODE_SIMPLE
#else
  #if (_WIN_PTHREAD_MODE == LCU_WIN_PTHREAD_IMPLEMENT_MODE_LIB)
    #define PTW32_STATIC_LIB
  #endif
  #define _LCU_CFG_WIN_PTHREAD_MODE LCU_WIN_PTHREAD_IMPLEMENT_MODE_LIB
#endif 

#endif // _WIN32


#endif // !_LCU_BUILD_CONFIG_H
