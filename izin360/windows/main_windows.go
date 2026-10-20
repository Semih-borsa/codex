package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"syscall"
	"time"
	"unsafe"
)

const address = "http://127.0.0.1:4310"

var client = &http.Client{Timeout: 2 * time.Second}

func message(text string) {
	title, _ := syscall.UTF16PtrFromString("İzin360")
	body, _ := syscall.UTF16PtrFromString(text)
	syscall.NewLazyDLL("user32.dll").NewProc("MessageBoxW").Call(0, uintptr(unsafe.Pointer(body)), uintptr(unsafe.Pointer(title)), 0)
}
func alive() bool {
	res, err := client.Get(address + "/api/health")
	if err != nil {
		return false
	}
	defer res.Body.Close()
	var body struct {
		App string `json:"app"`
	}
	return json.NewDecoder(res.Body).Decode(&body) == nil && body.App == "izin360"
}
func dataDirectory() string { return filepath.Join(os.Getenv("APPDATA"), "Izin360") }
func openWindow() {
	for _, root := range []string{os.Getenv("ProgramFiles(x86)"), os.Getenv("ProgramFiles"), os.Getenv("LOCALAPPDATA")} {
		if root == "" {
			continue
		}
		edge := filepath.Join(root, "Microsoft", "Edge", "Application", "msedge.exe")
		if _, err := os.Stat(edge); err == nil {
			cmd := exec.Command(edge, "--app="+address, "--window-size=1440,980", "--user-data-dir="+filepath.Join(os.Getenv("LOCALAPPDATA"), "Izin360", "Edge"))
			cmd.SysProcAttr = &syscall.SysProcAttr{HideWindow: true}
			if cmd.Start() == nil {
				return
			}
		}
	}
	cmd := exec.Command("rundll32.exe", "url.dll,FileProtocolHandler", address)
	cmd.SysProcAttr = &syscall.SysProcAttr{HideWindow: true}
	if cmd.Start() != nil {
		message("Tarayıcı açılamadı. Şu adresi kendin açabilirsin:\n" + address)
	}
}
func stop() {
	if !alive() {
		message("İzin360 sunucusu zaten kapalı.")
		return
	}
	key, err := os.ReadFile(filepath.Join(dataDirectory(), "connection.key"))
	if err != nil {
		message("Bu bilgisayarın bağlantı anahtarı okunamadı. Sunucu kapatılamadı.")
		return
	}
	req, _ := http.NewRequest("POST", address+"/api/shutdown", bytes.NewBufferString("{}"))
	req.Header.Set("Authorization", "Bearer "+strings.TrimSpace(string(key)))
	res, err := client.Do(req)
	if err != nil || res.StatusCode != 200 {
		message("Sunucu kapatılamadı. Açık uygulamayı kontrol et.")
		return
	}
	res.Body.Close()
	message("İzin360 sunucusu durduruldu. Kayıtların korunur; telefondan eşitleme için yeniden başlat.")
}
func main() {
	executable, err := os.Executable()
	if err != nil {
		return
	}
	if strings.Contains(strings.ToLower(filepath.Base(executable)), "durdur") || len(os.Args) > 1 && os.Args[1] == "--stop" {
		stop()
		return
	}
	if alive() {
		openWindow()
		return
	}
	root := filepath.Dir(executable)
	directory := dataDirectory()
	if err = os.MkdirAll(directory, 0700); err != nil {
		message("Kayıt klasörü oluşturulamadı. Windows kullanıcı izinlerini kontrol et.")
		return
	}
	log, err := os.OpenFile(filepath.Join(directory, "server.log"), os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0600)
	if err != nil {
		message("Sunucu günlük dosyası açılamadı.")
		return
	}
	cmd := exec.Command(filepath.Join(root, "runtime", "node.exe"), filepath.Join(root, "app", "server", "index.mjs"))
	cmd.Dir = filepath.Join(root, "app")
	cmd.Env = append(os.Environ(), "IZIN360_DATA_DIR="+directory, "PORT=4310", "HOST=0.0.0.0")
	cmd.Stdout = log
	cmd.Stderr = log
	cmd.SysProcAttr = &syscall.SysProcAttr{HideWindow: true, CreationFlags: 0x08000000}
	if err = cmd.Start(); err != nil {
		log.Close()
		message("Program başlatılamadı. ZIP dosyasının tamamını ayıkla; runtime/node.exe dosyası aynı pakette kalmalı.")
		return
	}
	go cmd.Wait()
	for i := 0; i < 60; i++ {
		if alive() {
			log.Close()
			openWindow()
			return
		}
		time.Sleep(250 * time.Millisecond)
	}
	log.Close()
	message(fmt.Sprintf("Sunucu hazır olmadı. 4310 portunu ve Windows güvenlik yazılımını kontrol et. Günlük dosyası:\n%s", filepath.Join(directory, "server.log")))
}
