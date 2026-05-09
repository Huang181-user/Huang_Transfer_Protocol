package main

import (
	"encoding/json"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"github.com/quic-go/quic-go"
	"github.com/quic-go/quic-go/http3"
)

type FileItem struct {
	Name  string `json:"name"`
	Size  int64  `json:"size"`
	IsDir bool   `json:"is_dir"`
	Path  string `json:"path"`
}

func main() {
	logPath := "/mnt/HDD_GB/Huang_Datas/Works/QUIC_test/quic_server/quic.log"
	logFile, _ := os.OpenFile(logPath, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0666)
	multiWriter := io.MultiWriter(os.Stdout, logFile)
	log.SetOutput(multiWriter)

	mux := http.NewServeMux()

	// 📂 API 1: LIST - Liệt kê (Giữ nguyên)
	mux.HandleFunc("/api/list", func(w http.ResponseWriter, r *http.Request) {
		p := r.URL.Query().Get("path")
		entries, err := os.ReadDir(p)
		if err != nil { http.Error(w, err.Error(), 500); return }
		var res []FileItem
		for _, e := range entries {
			info, _ := e.Info()
			res = append(res, FileItem{Name: e.Name(), Size: info.Size(), IsDir: e.IsDir(), Path: filepath.Join(p, e.Name())})
		}
		json.NewEncoder(w).Encode(res)
	})

	// 🔍 API 2: STAT - Lấy thông tin 1 file (MỚI ĐỂ FIX ZALO)
	mux.HandleFunc("/api/stat", func(w http.ResponseWriter, r *http.Request) {
		p := r.URL.Query().Get("path")
		info, err := os.Stat(p)
		if err != nil {
			log.Printf("[STAT_ERROR] ❌ Không thấy file: %s", p)
			http.Error(w, "Not Found", 404); return
		}
		log.Printf("[STAT_INFO] 📊 Báo cáo cho Zalo: %s (%d bytes)", p, info.Size())
		json.NewEncoder(w).Encode(FileItem{Name: info.Name(), Size: info.Size(), IsDir: info.IsDir(), Path: p})
	})

	// 📥 API 3: DOWNLOAD (Giữ nguyên)
	fileHandler := http.StripPrefix("/download/", http.FileServer(http.Dir("/")))
	mux.HandleFunc("/download/", func(w http.ResponseWriter, r *http.Request) {
		log.Printf("[DOWNLOAD] 📥 Đang bàn giao: %s", r.URL.Path)
		fileHandler.ServeHTTP(w, r)
	})

	server := &http3.Server{Addr: ":4433", QUICConfig: &quic.Config{InitialPacketSize: 1200}, Handler: mux}
	log.Println("🚀 SERVER V2.1 - ĐÃ FIX LỖI SHARE FILE RỖNG")
	server.ListenAndServeTLS("zhiserver.tailc979c1.ts.net.crt", "zhiserver.tailc979c1.ts.net.key")
}
