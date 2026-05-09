package main

import (
	"encoding/json"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"github.com/quic-go/quic-go"
	"github.com/quic-go/quic-go/http3"
)

// ServerConfig: Bộ não cấu hình của hệ thống
type ServerConfig struct {
	Port     string `json:"port"`
	SafeRoot string `json:"safe_root"`
	LogPath  string `json:"log_path"`
	TLSCrt   string `json:"tls_crt"`
	TLSKey   string `json:"tls_key"`
}

type FileItem struct {
	Name  string `json:"name"`
	Size  int64  `json:"size"`
	IsDir bool   `json:"is_dir"`
	Path  string `json:"path"`
}

func main() {
	// 1. Đọc config.json trước khi làm bất cứ việc gì
	configFile, err := os.ReadFile("config.json")
	if err != nil {
		// In ra console nếu tịt ngay từ vòng gửi xe
		log.Fatalf("[FATAL] ❌ Không tìm thấy config.json: %v", err)
	}

	var config ServerConfig
	if err := json.Unmarshal(configFile, &config); err != nil {
		log.Fatalf("[FATAL] ❌ Lỗi đọc định dạng JSON: %v", err)
	}

	// 2. Cấu hình Logging dựa trên config ẩn
	logFile, err := os.OpenFile(config.LogPath, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0666)
	if err != nil {
		log.Fatalf("[FATAL] ❌ Không thể mở file log tại %s: %v", config.LogPath, err)
	}
	defer logFile.Close()

	// Ép log phải ghi xuống đĩa realtime (Sync)
	multiWriter := io.MultiWriter(os.Stdout, logFile)
	log.SetOutput(multiWriter)

	log.Println("==========================================================================")
	log.Printf("🚀 HUANG QUIC SERVER V3.2 - KHỞI ĐỘNG HỆ THỐNG")
	log.Printf("[CONFIG] Lồng sắt an toàn: %s", config.SafeRoot)
	log.Printf("[CONFIG] Cổng lắng nghe  : %s", config.Port)
	log.Printf("[CONFIG] Nhật ký lưu tại : %s", config.LogPath)
	log.Println("==========================================================================")

	// Hàm kiểm tra bảo mật: Ngăn chặn tuyệt đối việc thoát khỏi lồng sắt
	checkSafePath := func(p string) bool {
		cleanPath := filepath.Clean(p)
		return strings.HasPrefix(cleanPath, config.SafeRoot)
	}

	mux := http.NewServeMux()

	// API LIST - Duyệt file 17TB
	mux.HandleFunc("/api/list", func(w http.ResponseWriter, r *http.Request) {
		p := r.URL.Query().Get("path")
		log.Printf("[LIST_REQ] 🔍 Client yêu cầu: %s", p)

		if !checkSafePath(p) {
			log.Printf("[SECURITY] ⚠️ CẢNH BÁO: Phát hiện truy cập ngoài vùng an toàn: %s", p)
			http.Error(w, "Khu vực cấm!", 403)
			return
		}

		entries, err := os.ReadDir(p)
		if err != nil {
			log.Printf("[ERROR] ❌ Lỗi đọc thư mục: %v", err)
			http.Error(w, err.Error(), 500)
			return
		}

		var res []FileItem
		for _, e := range entries {
			info, _ := e.Info()
			res = append(res, FileItem{
				Name:  e.Name(),
				Size:  info.Size(),
				IsDir: e.IsDir(),
				Path:  filepath.Join(p, e.Name()),
			})
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(res)
		log.Printf("[SUCCESS] ✅ Đã nhả %d mục cho App Android", len(res))
	})

	// API STAT - Phục vụ Zalo Share
	mux.HandleFunc("/api/stat", func(w http.ResponseWriter, r *http.Request) {
		p := r.URL.Query().Get("path")
		if !checkSafePath(p) {
			http.Error(w, "Forbidden", 403)
			return
		}

		info, err := os.Stat(p)
		if err != nil {
			log.Printf("[STAT_ERR] ❌ File không tồn tại: %s", p)
			http.Error(w, "Not Found", 404)
			return
		}

		log.Printf("[STAT_OK] 📊 Báo cáo size file: %s (%d bytes)", p, info.Size())
		json.NewEncoder(w).Encode(FileItem{Name: info.Name(), Size: info.Size(), IsDir: info.IsDir(), Path: p})
	})

	// API DOWNLOAD - Tốc độ bàn thờ QUIC
	mux.HandleFunc("/download/", func(w http.ResponseWriter, r *http.Request) {
		p := strings.TrimPrefix(r.URL.Path, "/download")
		if !checkSafePath(p) {
			log.Printf("[SECURITY] ⚠️ Chặn tải file lậu: %s", p)
			http.Error(w, "Quay xe ný ơi!", 403)
			return
		}

		log.Printf("[TRANSFER] 📥 Đang đẩy file về Mobile: %s", p)
		http.ServeFile(w, r, p)
	})

	// Khởi tạo giao thức HTTP/3 QUIC
	server := &http3.Server{
		Addr:       config.Port,
		QUICConfig: &quic.Config{InitialPacketSize: 1200},
		Handler:    mux,
	}

	// Đưa cụ lên mây
	err = server.ListenAndServeTLS(config.TLSCrt, config.TLSKey)
	if err != nil {
		log.Fatalf("[FATAL] ❌ Server sập nguồn: %v", err)
	}
}
