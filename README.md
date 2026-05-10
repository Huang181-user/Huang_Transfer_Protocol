# HUANG QUIC Transfer Protocol (HQTP)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Go Version](https://img.shields.io/badge/Go-1.25+-00ADD8?logo=go)](https://golang.org)
[![Android Support](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?logo=android)](https://developer.android.com)

---

## Multilingual Support / Hỗ trợ đa ngôn ngữ / 多语言支持
* [English](#english)
* [Tiếng Việt](#tiếng-việt)
* [简体中文](#简体中文)

---

<a name="english"></a>
## English

### Introduction
**HUANG QUIC Transfer Protocol (HQTP)** is a high-performance, secure file transfer system designed for modern network environments. It leverages the **HTTP/3 (QUIC)** protocol to provide ultra-fast speeds, low latency, and resilience against packet loss. Built with a "Zero-Hardcode" security philosophy, it is ideal for managing large-scale storage (e.g., 17TB+ home servers) over both LAN and VPN (Tailscale).

### Key Features
* **QUIC (HTTP/3) Engine:** High-speed data transfer optimized for various network conditions.
* **Security-First:** No hardcoded IPs, ports, or credentials. Everything is managed via encrypted `config.json` (ignored by Git).
* **Tailscale Integration:** Seamlessly switches between local LAN and Tailscale VPN with automatic process binding.
* **Binary Search MTU Discovery:** Automatically calculates the optimal MTU size to maximize throughput.
* **Android Document Provider:** Integrate your remote server directly into the Android System File Manager.
* **SFTP Fallback:** Reliable backup protocol when QUIC is restricted by network firewalls.

---

<a name="tiếng-việt"></a>
## Tiếng Việt

### Giới thiệu
**HUANG QUIC Transfer Protocol (HQTP)** là hệ thống truyền tải dữ liệu hiệu suất cao, được thiết kế tối ưu cho môi trường mạng hiện đại. Dự án sử dụng giao thức **HTTP/3 (QUIC)** nhằm mang lại tốc độ vượt trội, độ trễ thấp và khả năng chống mất gói tin tuyệt vời. Với triết lý bảo mật "Zero-Hardcode", hệ thống này là giải pháp hoàn hảo để quản lý máy chủ lưu trữ lớn (17TB+) qua mạng nội bộ hoặc VPN (Tailscale).

### Tính năng chính
* **Lõi QUIC (HTTP/3):** Tối ưu hóa tốc độ truyền tải, đặc biệt hiệu quả trong môi trường mạng không ổn định.
* **Bảo mật tuyệt đối:** Không lưu cứng IP, Port hay mật khẩu trong code. Tất cả cấu hình nằm trong file `config.json` (đã được chặn bởi Git).
* **Tích hợp Tailscale:** Tự động chuyển đổi giữa LAN và VPN, hỗ trợ ép luồng dữ liệu (Process Binding) vào hầm Tailscale.
* **Dò MTU nhị phân:** Tự động tìm kiếm kích thước MTU tối ưu để đạt băng thông cao nhất.
* **Tích hợp File Manager:** Hiển thị ổ cứng server trực tiếp trong trình quản lý tệp của Android (DocumentsProvider).
* **SFTP Dự phòng:** Tự động hạ cấp xuống SFTP khi môi trường mạng chặn giao thức UDP/QUIC.

---

<a name="简体中文"></a>
## 简体中文

### 项目简介
**HUANG QUIC 传输协议 (HQTP)** 是一款专为现代网络环境设计的高性能、安全文件传输系统。该项目利用 **HTTP/3 (QUIC)** 协议提供极速传输、低延迟以及卓越的丢包恢复能力。秉承“零硬编码”的安全理念，它是通过局域网 (LAN) 或 VPN (Tailscale) 管理大规模存储（如 17TB+ 家庭服务器）的理想解决方案。

### 核心功能
* **QUIC (HTTP/3) 引擎：** 针对各种网络状况优化的极速数据传输。
* **安全第一：** 代码中无任何硬编码的 IP、端口或凭据。所有敏感信息均通过 `config.json` 管理。
* **Tailscale 集成：** 在局域网和 Tailscale VPN 之间无缝切换，支持进程自动绑定。
* **二分法 MTU 探测：** 自动计算最佳 MTU 大小以最大化吞吐量。
* **安卓文档提供程序：** 将远程服务器直接集成到安卓系统文件管理器中。
* **SFTP 后备协议：** 当 QUIC 协议受限时，自动切换至可靠的 SFTP 传输。

---

## Installation & Setup / Cài đặt

### Server
1.  Navigate to `quic_server/`.
2.  Create `config.json` based on the template.
3.  Build: `go build -o quic_server_bin server.go`.

### Android
1.  Add your `config.json` to `app/src/main/assets/`.
2.  Build the `.aar` library and place it in `app/libs/`.
3.  Sync Gradle and run.

---

## License
MIT License.
