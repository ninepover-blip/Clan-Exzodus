import base64
import hashlib
import json
import os
import platform
import subprocess
import sys
import threading
import time
import tkinter as tk
from tkinter import ttk, messagebox, filedialog
from pathlib import Path

import urllib.request
import urllib.error

try:
    import minecraft_launcher_lib
    from minecraft_launcher_lib import command as mc_command
    from minecraft_launcher_lib import microsoft_account
    from minecraft_launcher_lib import fabric as mc_fabric
except Exception as e:  # pragma: no cover
    minecraft_launcher_lib = None

APP_NAME = "EXZODUS"
APP_VERSION = "2.0.0"
API_BASE = "https://clan-exzodus.vercel.app/api"

SOFTWARE = {
    "infinity": {"name": "Infinity 1.21.4 (Fabric)", "version": "1.21.4", "loader": "0.16.14", "default_mod": "infinyty.jar"},
    "lobok": {"name": "Lobok Client 1.16.5", "version": "1.16.5", "loader": "0.14.25", "default_mod": "lobok.jar"},
}

HOME = Path(os.getenv("LOCALAPPDATA", str(Path.home()))) / "EXZODUS"
HOME.mkdir(parents=True, exist_ok=True)
GAME_DIR = HOME / ".minecraft"
GAME_DIR.mkdir(parents=True, exist_ok=True)
CONFIG_FILE = HOME / "config.json"
LAUNCHER_BG = "#0a0015"
ACCENT = "#b388ff"
ACCENT2 = "#7c4dff"


def hwid():
    import uuid as _uuid
    mac = _uuid.getnode()
    node = ":".join(format((mac >> (8 * i)) & 0xff, "02x") for i in range(6))
    return hashlib.sha256(node.encode()).hexdigest()[:32]


def load_config():
    try:
        return json.loads(CONFIG_FILE.read_text(encoding="utf-8"))
    except Exception:
        return {}


def save_config(cfg):
    CONFIG_FILE.write_text(json.dumps(cfg, ensure_ascii=False, indent=2), encoding="utf-8")


def api_request(path, payload, token=""):
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        API_BASE + path,
        data=data,
        headers={"content-type": "application/json", **( {"authorization": "Bearer " + token} if token else {})},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read().decode("utf-8"))


class Loader(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title(f"{APP_NAME} Launcher {APP_VERSION}")
        self.configure(bg=LAUNCHER_BG)
        self.geometry("900x560")
        self.token = ""
        self.cfg = load_config()
        self.token = self.cfg.get("token", "")
        self.software = self.cfg.get("software", "infinity")
        self.key = self.cfg.get("key", "")
        self.busy = False
        self._build_login() if not self.token else self._build_main()

    # ---------- helpers ----------
    def set_status(self, text):
        try:
            self.status_var.set(text)
        except Exception:
            pass

    def show_loading(self, message):
        self._loading = LoadingScreen(self, message)

    def hide_loading(self):
        try:
            self._loading.destroy()
        except Exception:
            pass

    # ---------- login ----------
    def _build_login(self):
        f = tk.Frame(self, bg=LAUNCHER_BG)
        f.place(relx=0.5, rely=0.5, anchor="center")
        tk.Label(f, text=APP_NAME, font=("Segoe UI", 34, "bold"), fg=ACCENT, bg=LAUNCHER_BG).pack(pady=6)
        tk.Label(f, text="Войдите в аккаунт EXZODUS", fg="#cbb3ff", bg=LAUNCHER_BG).pack(pady=4)
        self.login_var = tk.StringVar()
        self.pass_var = tk.StringVar()
        tk.Entry(f, textvariable=self.login_var, width=28, bg="#1a0a2e", fg="white", insertbackground=ACCENT).pack(pady=4)
        tk.Entry(f, textvariable=self.pass_var, width=28, show="*", bg="#1a0a2e", fg="white", insertbackground=ACCENT).pack(pady=4)
        tk.Button(f, text="Войти", command=self._do_login, bg=ACCENT2, fg="white", width=20).pack(pady=8)
        self.status_var = tk.StringVar(value="")
        tk.Label(f, textvariable=self.status_var, fg="#ff8a8a", bg=LAUNCHER_BG).pack()

    def _do_login(self):
        try:
            j = api_request("/auth", {"action": "login", "login": self.login_var.get(), "password": self.pass_var.get()}, "")
            self.token = j["token"]
            self.cfg["token"] = self.token
            save_config(self.cfg)
            for w in self.winfo_children():
                w.destroy()
            self._build_main()
        except Exception as e:
            self.set_status(str(e))

    # ---------- main ----------
    def _build_main(self):
        tk.Label(self, text=APP_NAME, font=("Segoe UI", 22, "bold"), fg=ACCENT, bg=LAUNCHER_BG).place(x=20, y=14)
        tk.Button(self, text="Выйти", command=self._logout, bg="#2a1140", fg="#cbb3ff").place(x=800, y=18)

        tk.Label(self, text="Выберите софт:", fg="#cbb3ff", bg=LAUNCHER_BG).place(x=20, y=70)
        self.soft_var = tk.StringVar(value=self.software)
        for i, (sid, info) in enumerate(SOFTWARE.items()):
            tk.Radiobutton(self, text=info["name"], variable=self.soft_var, value=sid, fg="white", bg=LAUNCHER_BG,
                           selectcolor=ACCENT2, command=self._on_software).place(x=20 + i * 280, y=96)

        tk.Label(self, text="Лицензионный ключ:", fg="#cbb3ff", bg=LAUNCHER_BG).place(x=20, y=150)
        self.key_var = tk.StringVar(value=self.key)
        tk.Entry(self, textvariable=self.key_var, width=40, bg="#1a0a2e", fg="white", insertbackground=ACCENT).place(x=20, y=174)
        tk.Button(self, text="Сохранить ключ", command=self._save_key).place(x=320, y=172)

        tk.Label(self, text="Аккаунт Minecraft (Microsoft):", fg="#cbb3ff", bg=LAUNCHER_BG).place(x=20, y=220)
        self.mc_var = tk.StringVar(value=self.cfg.get("mc_email", ""))
        tk.Entry(self, textvariable=self.mc_var, width=40, bg="#1a0a2e", fg="white", insertbackground=ACCENT).place(x=20, y=244)
        tk.Button(self, text="Авторизовать", command=self._auth_mc).place(x=320, y=242)

        tk.Button(self, text="▶  ИГРАТЬ", command=self._play, bg=ACCENT2, fg="white", font=("Segoe UI", 14, "bold"), width=22, height=2).place(x=20, y=300)

        self.status_var = tk.StringVar(value="Готово")
        tk.Label(self, textvariable=self.status_var, fg="#cbb3ff", bg=LAUNCHER_BG, wraplength=820, justify="left").place(x=20, y=400)

    def _logout(self):
        self.token = ""
        self.cfg.pop("token", None)
        save_config(self.cfg)
        for w in self.winfo_children():
            w.destroy()
        self._build_login()

    def _on_software(self):
        self.software = self.soft_var.get()
        self.cfg["software"] = self.software
        save_config(self.cfg)

    def _save_key(self):
        self.key = self.key_var.get().strip().upper()
        self.cfg["key"] = self.key
        save_config(self.cfg)
        self.set_status("Ключ сохранён")

    def _auth_mc(self):
        email = self.mc_var.get().strip()
        if not email:
            self.set_status("Введите email Microsoft")
            return
        self.cfg["mc_email"] = email
        save_config(self.cfg)
        self.set_status("Откройте браузер и войдите в Microsoft...")
        threading.Thread(target=self._auth_mc_thread, daemon=True).start()

    def _auth_mc_thread(self):
        try:
            if minecraft_launcher_lib is None:
                raise RuntimeError("minecraft_launcher_lib не установлен")
            auth = microsoft_account.login(self.mc_var.get().strip())
            self.cfg["mc_uuid"] = auth.get("uuid")
            self.cfg["mc_name"] = auth.get("username")
            save_config(self.cfg)
            self.set_status("Microsoft-аккаунт авторизован: " + str(auth.get("username", "")))
        except Exception as e:
            self.set_status("Ошибка Microsoft-авторизации: " + str(e))

    # ---------- play ----------
    def _play(self):
        if self.busy:
            return
        self.busy = True
        threading.Thread(target=self._play_thread, daemon=True).start()

    def _play_thread(self):
        try:
            self.show_loading("Проверка ключа...")
            key = self.key_var.get().strip().upper()
            if not key:
                raise RuntimeError("Введите лицензионный ключ")
            v = api_request("/validate", {"key": key, "hwid": hwid(), "software": self.software}, "")
            if not v.get("valid"):
                raise RuntimeError("Ключ недействителен: " + str(v.get("message", "")))
            self.key = key
            self.cfg["key"] = key
            save_config(self.cfg)

            # пишем ключ для телеметрии мода
            opts = GAME_DIR / "options"
            opts.mkdir(parents=True, exist_ok=True)
            (opts / "license-key.txt").write_text(key, encoding="utf-8")

            self._loading.set_message("Установка Fabric и загрузка мода...")
            self._prepare_mod()

            self._loading.set_message("Запуск Minecraft...")
            self._launch()
            self.hide_loading()
            self.set_status("Minecraft запущен")
        except Exception as e:
            self.hide_loading()
            self.set_status("Ошибка: " + str(e))
            messagebox.showerror(APP_NAME, str(e))
        finally:
            self.busy = False

    def _prepare_mod(self):
        info = SOFTWARE[self.software]
        # убеждаемся, что Fabric установлен для нужной версии
        if minecraft_launcher_lib is not None:
            try:
                mc_fabric.install_fabric(info["version"], info["loader"])
            except Exception as e:
                self.set_status("Не удалось установить Fabric: " + str(e))
        # скачиваем мод через API (per-software)
        manifest = api_request("/mod-update", {"action": "manifest", "software": self.software, "channel": "stable"}, "")
        rel = manifest.get("release")
        if not rel:
            raise RuntimeError("Мод для " + info["name"] + " ещё не опубликован")
        mods_dir = GAME_DIR / "mods"
        mods_dir.mkdir(parents=True, exist_ok=True)
        target = mods_dir / rel["filename"]
        if not (target.exists() and _sha256(target) == rel["sha256"].lower()):
            chunks = []
            for i in range(int(rel["chunk_count"])):
                c = api_request("/mod-update", {"action": "chunk", "releaseId": rel["id"], "index": i}, "")
                chunks.append(base64.b64decode(c["data"]))
                self._loading.set_message(f"Загрузка мода {i+1}/{rel['chunk_count']}")
            blob = b"".join(chunks)
            if hashlib.sha256(blob).hexdigest().lower() != rel["sha256"].lower():
                raise RuntimeError("Контрольная сумма мода не совпала")
            target.write_bytes(blob)

    def _launch(self):
        info = SOFTWARE[self.software]
        if minecraft_launcher_lib is None:
            raise RuntimeError("minecraft_launcher_lib не установлен — запуск невозможен")
        version_id = f"fabric-loader-{info['loader']}-{info['version']}"
        options = {
            "username": self.cfg.get("mc_name", "Player"),
            "uuid": self.cfg.get("mc_uuid", ""),
            "token": self.cfg.get("mc_token", ""),
            "jvmArguments": ["-Xmx2G"],
        }
        # актуальный токен из сохранённой сессии Microsoft
        try:
            auth = microsoft_account.login(self.cfg.get("mc_email", ""))
            options["token"] = auth.get("access_token", options["token"])
            options["uuid"] = auth.get("uuid", options["uuid"])
            options["username"] = auth.get("username", options["username"])
        except Exception:
            pass
        cmd = mc_command.get_minecraft_command(version_id, str(GAME_DIR), options)
        subprocess.Popen(cmd)

    # ---------- self update ----------
    def check_self_update(self):
        try:
            m = api_request("/launcher-update", {"action": "manifest"}, "")
            remote = m.get("release", {}).get("version")
            if remote and _version_tuple(remote) > _version_tuple(APP_VERSION):
                self.after(0, lambda: messagebox.showinfo(APP_NAME, "Доступно обновление лаунчера " + remote))
        except Exception:
            pass


def _sha256(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()


def _version_tuple(v):
    try:
        return tuple(int(x) for x in str(v).split("."))
    except Exception:
        return (0, 0, 0)


class LoadingScreen(tk.Toplevel):
    def __init__(self, parent, message):
        super().__init__(parent)
        self.parent = parent
        self.configure(bg=LAUNCHER_BG)
        self.overrideredirect(True)
        self.geometry("420x260+%d+%d" % (parent.winfo_x() + 240, parent.winfo_y() + 150))
        self._build()
        self._msg = message
        self._p = 0
        self._animate()

    def _build(self):
        tk.Label(self, text="EXZODUS", font=("Segoe UI", 28, "bold"), fg=ACCENT, bg=LAUNCHER_BG).pack(pady=24)
        self.bar = tk.Canvas(self, width=300, height=10, bg="#1a0a2e", highlightthickness=0)
        self.bar.pack()
        self.fill = self.bar.create_rectangle(0, 0, 0, 10, fill=ACCENT2, width=0)
        self.msg = tk.StringVar(value="Загрузка...")
        tk.Label(self, textvariable=self.msg, fg="#cbb3ff", bg=LAUNCHER_BG).pack(pady=14)

    def set_message(self, text):
        try:
            self.msg.set(text)
        except Exception:
            pass

    def _animate(self):
        self._p = min(100, self._p + 3)
        self.bar.coords(self.fill, 0, 0, 300 * self._p / 100, 10)
        if self._p < 100:
            self.after(120, self._animate)


if __name__ == "__main__":
    app = Loader()
    threading.Thread(target=app.check_self_update, daemon=True).start()
    app.mainloop()
