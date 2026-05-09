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

// 🛠 VŨ KHÍ MỚI: Bắt log phải Sync realtime xuống đĩa
type syncWriter struct {
	file *os.File
}

func (sw syncWriter) Write(p []byte) (n int, err error) {
	n, err = sw.file.Write(p)
	sw.file.Sync() // 🚀 Ép dữ liệu xuống đĩa ngay lập tức
	return
}

type FileItem struct {
	Name  string `json:"name"`
	Size  int64  `json:"size"`
	IsDir bool   `json:"is_dir"`
	Path  string `json:"path"`
}

func main() {
	logPath := "/mnt/HDD_GB/Huang_Datas/Works/QUIC_test/quic_server/quic.log"
	logFile, err := os.OpenFile(logPath, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0666)
	if err != nil { log.Fatalf("❌ Lỗi log: %v", err) }
	defer logFile.Close()

	// Dùng syncWriter thay vì logFile trực tiếp
	multiWriter := io.MultiWriter(os.Stdout, syncWriter{file: logFile})
	log.SetOutput(multiWriter)

	port := ":4433"
	quicConf := &quic.Config{InitialPacketSize: 1200}
	mux := http.NewServeMux()

	mux.HandleFunc("/api/list", func(w http.ResponseWriter, r *http.Request) {
		targetPath := r.URL.Query().Get("path")
		log.Println("--------------------------------------------------")
		log.Printf("[LIST_REQUEST] 🔍 Duyệt: %s", targetPath)
		entries, err := os.ReadDir(targetPath)
		if err != nil {
			log.Printf("[LIST_ERROR] ❌ %v", err)
			http.Error(w, err.Error(), 500)
			return
		}
		var results []FileItem
		for _, e := range entries {
			info, _ := e.Info()
			results = append(results, FileItem{Name: e.Name(), Size: info.Size(), IsDir: e.IsDir(), Path: filepath.Join(targetPath, e.Name())})
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(results)
		log.Printf("[LIST_SUCCESS] ✅ Đã gửi %d mục", len(results))
	})

	fileHandler := http.StripPrefix("/download/", http.FileServer(http.Dir("/")))
	mux.HandleFunc("/download/", func(w http.ResponseWriter, r *http.Request) {
		log.Println("--------------------------------------------------")
		log.Printf("[DOWNLOAD_START] 📥 Kéo file: %s", r.URL.Path)
		fileHandler.ServeHTTP(w, r)
		log.Printf("[DOWNLOAD_FINISHED] ✅ Xong: %s", r.URL.Path)
	})

	server := &http3.Server{Addr: port, QUICConfig: quicConf, Handler: mux}
	log.Println("==========================================================================")
	log.Println("[ZHISERVER_V2_SYNC] 🚀 LOG ĐÃ ĐƯỢC ÉP SYNC REALTIME!")
	log.Println("==========================================================================")
	err = server.ListenAndServeTLS("zhiserver.tailc979c1.ts.net.crt", "zhiserver.tailc979c1.ts.net.key")
	if err != nil { log.Fatal(err) }
}
