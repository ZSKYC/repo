/* Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

/* This file was generated by generate_constants.cc. */

package org.conscrypt;

public final class NativeConstants {
    public static final boolean IS_BORINGSSL = true;
    public static final int OPENSSL_EC_NAMED_CURVE = 0;
    public static final int POINT_CONVERSION_COMPRESSED = 2;
    public static final int POINT_CONVERSION_UNCOMPRESSED = 4;
    public static final int EXFLAG_CA = 16;
    public static final int EXFLAG_CRITICAL = 512;
    public static final int EVP_PKEY_RSA = 6;
    public static final int EVP_PKEY_HMAC = 855;
    public static final int EVP_PKEY_EC = 408;
    public static final int RSA_PKCS1_PADDING = 1;
    public static final int RSA_NO_PADDING = 3;
    public static final int RSA_PKCS1_OAEP_PADDING = 4;
    public static final int SSL_MODE_SEND_FALLBACK_SCSV = 1024;
    public static final int SSL_MODE_CBC_RECORD_SPLITTING = 256;
    public static final int SSL_MODE_HANDSHAKE_CUTTHROUGH = 128;
    public static final int SSL_OP_NO_TICKET = 16384;
    public static final int SSL_OP_NO_SESSION_RESUMPTION_ON_RENEGOTIATION = 0;
    public static final int SSL_OP_NO_SSLv3 = 33554432;
    public static final int SSL_OP_NO_TLSv1 = 67108864;
    public static final int SSL_OP_NO_TLSv1_1 = 268435456;
    public static final int SSL_OP_NO_TLSv1_2 = 134217728;
    public static final int SSL_SENT_SHUTDOWN = 1;
    public static final int SSL_RECEIVED_SHUTDOWN = 2;
    public static final int TLS_CT_RSA_SIGN = 1;
    public static final int TLS_CT_ECDSA_SIGN = 64;
    public static final int TLS_CT_RSA_FIXED_DH = 3;
    public static final int TLS_CT_RSA_FIXED_ECDH = 65;
    public static final int TLS_CT_ECDSA_FIXED_ECDH = 66;
    public static final int SSL_VERIFY_NONE = 0;
    public static final int SSL_VERIFY_PEER = 1;
    public static final int SSL_VERIFY_FAIL_IF_NO_PEER_CERT = 2;
    public static final int SSL_ST_CONNECT = 4096;
    public static final int SSL_ST_ACCEPT = 8192;
    public static final int SSL_ST_MASK = 4095;
    public static final int SSL_ST_INIT = 12288;
    public static final int SSL_ST_OK = 3;
    public static final int SSL_ST_RENEGOTIATE = 12292;
    public static final int SSL_CB_LOOP = 1;
    public static final int SSL_CB_EXIT = 2;
    public static final int SSL_CB_READ = 4;
    public static final int SSL_CB_WRITE = 8;
    public static final int SSL_CB_ALERT = 16384;
    public static final int SSL_CB_READ_ALERT = 16388;
    public static final int SSL_CB_WRITE_ALERT = 16392;
    public static final int SSL_CB_ACCEPT_LOOP = 8193;
    public static final int SSL_CB_ACCEPT_EXIT = 8194;
    public static final int SSL_CB_CONNECT_LOOP = 4097;
    public static final int SSL_CB_CONNECT_EXIT = 4098;
    public static final int SSL_CB_HANDSHAKE_START = 16;
    public static final int SSL_CB_HANDSHAKE_DONE = 32;
    public static final int SSL3_RT_MAX_PACKET_SIZE = 16709;
}