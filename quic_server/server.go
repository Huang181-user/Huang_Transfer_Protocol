package main

import (
	"fmt"
	"net"
	"net/http"
	"time"

	"github.com/quic-go/quic-go"
	"github.com/quic-go/quic-go/http3"
)

func logEvent(method, client, path, status, protocol string) {
	fmt.Printf("[%s] ⚡ [%s] %s | %s | %s\n", time.Now().Format("15:04:05"), protocol, method, client, status)
}

func startMTUProbe() {
	addr, _ := net.ResolveUDPAddr("udp", ":4434")
	conn, _ := net.ListenUDP("udp", addr)
	defer conn.Close()

	fmt.Println("📡 [MTU-SERVER] Radar dung lượng thực READY (Port 4434)")
	buf := make([]byte, 2048)
	for {
		n, remoteAddr, _ := conn.ReadFromUDP(buf)
		response := []byte(fmt.Sprintf("RECV:%d", n))
		conn.WriteToUDP(response, remoteAddr)
	}
}

func fileHandler(w http.ResponseWriter, r *http.Request) {
	// NHÉT VÉ VÀO MỌI LUỒNG ĐỂ CRONET LUÔN NHỚ ĐƯỜNG (Giữ nguyên)
	w.Header().Set("Alt-Svc", `h3=":4433"; ma=86400`)

	protocol := "TCP"
	if r.ProtoMajor == 3 {
		protocol = "QUIC"
	}
	logEvent(r.Method, r.RemoteAddr, r.URL.Path, "🚀 Đang kéo...", protocol)

	// Bóc đường dẫn file từ URL (Ví dụ: /mnt/HDD_merge/...)
	filePath := r.URL.Path

	// Lệnh tối thượng: Bưng file thật từ ổ cứng ném cho điện thoại
	http.ServeFile(w, r, filePath)
}

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/", fileHandler)

	fmt.Println("\n=================================================================")
	fmt.Println("🚀 [ZhiServer] KHỞI ĐỘNG HỆ THỐNG TRUYỀN TẢI ĐA GIAO THỨC (ABM)")
	fmt.Println("=================================================================")

	go startMTUProbe()

	certFile := "zhiserver.tailc979c1.ts.net.crt"
	keyFile := "zhiserver.tailc979c1.ts.net.key"

	ipTailscale := "100.125.141.48:4433"
	ipLAN := "192.168.1.83:4433" // Ný nhớ check lại IP LAN con Server chuẩn chưa nha

	// 1. MỞ LUỒNG QUIC CHO TAILSCALE
	serverTS := &http3.Server{
		Addr:       ipTailscale,
		Handler:    mux,
		QUICConfig: &quic.Config{MaxIdleTimeout: 30 * time.Second, DisablePathMTUDiscovery: true},
	}
	go func() {
		fmt.Printf(">>> [DEBUG-MAIN] 🚀 ĐANG MỞ CỔNG QUIC (UDP) TRÊN TAILSCALE: %s...\n", ipTailscale)
		err := serverTS.ListenAndServeTLS(certFile, keyFile)
		if err != nil {
			fmt.Printf("\n[DEBUG-MAIN-FATAL] 💀 LỖI QUIC TS: %v\n", err)
		}
	}()

	// 2. MỞ LUỒNG QUIC CHO MẠNG LAN (Khôi phục)
	serverLAN := &http3.Server{
		Addr:       ipLAN,
		Handler:    mux,
		QUICConfig: &quic.Config{MaxIdleTimeout: 30 * time.Second},
	}
	go func() {
		fmt.Printf(">>> [DEBUG-MAIN] 🚀 ĐANG MỞ CỔNG QUIC (UDP) TRÊN MẠNG LAN: %s...\n", ipLAN)
		err := serverLAN.ListenAndServeTLS(certFile, keyFile)
		if err != nil {
			fmt.Printf("\n[DEBUG-MAIN-FATAL] 💀 LỖI QUIC LAN: %v\n", err)
		}
	}()

	// // 3. LUỒNG TCP DỰ PHÒNG (Cho mọi mạng)
	// fmt.Println(">>> [DEBUG-MAIN] ⏳ Đang kích hoạt luồng TCP dự phòng (port 4433)...")
	// err := http.ListenAndServeTLS("0.0.0.0:4433", certFile, keyFile, mux)
	// if err != nil {
	// 	fmt.Printf("\n[DEBUG-MAIN-FATAL] ❌ LỖI TCP CHÍNH: %v\n", err)
	// }

	// 3. MỞ HÉ CỬA TCP: Bẫy "Đóng Băng" (Treo luồng TCP để câu giờ cho QUIC)
	fmt.Println(">>> [DEBUG-MAIN] ⏳ Đang mở bẫy đóng băng TCP (port 4433)...")

	tcpMux := http.NewServeMux()
	tcpMux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		// 1. Nhét bùa QUIC vào não Cronet để nó ưu tiên QUIC ở lần sau
		w.Header().Set("Alt-Svc", `h3=":4433"; ma=86400`)

		// 2. Không trả file, cũng không báo lỗi.
		// Ép cái luồng TCP này phải NẰM CHỜ 15 GIÂY!
		// Trong 15 giây này, thằng QUIC sẽ bay tới và lấy file thành công.
		time.Sleep(15 * time.Second)

		// Sau 15 giây, nếu QUIC xui xẻo rớt mạng, thì TCP mới nhả ra một lỗi nhẹ nhàng
		http.Error(w, "QUIC is the King", http.StatusServiceUnavailable)
	})

	err := http.ListenAndServeTLS("0.0.0.0:4433", certFile, keyFile, tcpMux)
	if err != nil {
		fmt.Printf("\n[DEBUG-MAIN-FATAL] ❌ LỖI TCP CHÍNH: %v\n", err)
	}
}
