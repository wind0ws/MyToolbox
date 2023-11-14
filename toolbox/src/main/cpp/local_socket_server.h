#pragma once
#ifndef MYTOOLBOX_LOCAL_SOCKET_SERVER_H
#define MYTOOLBOX_LOCAL_SOCKET_SERVER_H

#include "local_socket_protocol.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    CONNECTED = 0,
    DISCONNECTED = 1
} conn_status_t;

typedef struct socket_server_s *server_handle;

typedef void(*socket_server_on_connect_status_changed_cb)(conn_status_t conn_status);

typedef void(*socket_server_on_received_data_cb)(char *data, int length);

typedef struct {
    char socket_name[64];
    int once_read_size;
    socket_server_on_connect_status_changed_cb connect_status_changed_cb;
    socket_server_on_received_data_cb received_data_cb;
} server_config_t;

int local_socket_server_start(__IN server_config_t config, __OUT server_handle *handle_p);

int local_socket_server_stop(__IN server_handle *handle_p);

#ifdef __cplusplus
}
#endif
#endif //MYTOOLBOX_LOCAL_SOCKET_SERVER_H
