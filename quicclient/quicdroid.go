package quicclient

import (
	"crypto/tls"
	"io"
	"net/http"
	"os"

	"github.com/quic-go/quic-go/http3"
)

func DownloadFast(url string, savePath string) string {
	tr := &http3.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	defer tr.Close()
	client := &http.Client{Transport: tr}

	resp, err := client.Get(url)
	if err != nil {
		return "LỖI_KẾT_NỐI: " + err.Error()
	}
	defer resp.Body.Close()

	out, err := os.Create(savePath)
	if err != nil {
		return "LỖI_TẠO_FILE: " + err.Error()
	}
	defer out.Close()

	_, err = io.Copy(out, resp.Body)
	if err != nil {
		return "LỖI_TẢI_FILE: " + err.Error()
	}

	return "QUIC_SUCCESS"
}
