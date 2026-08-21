import os, sys, json, hashlib, base64, uuid, threading, subprocess, time, webbrowser, tempfile, shutil, re
import tkinter as tk
from tkinter import ttk, messagebox, filedialog, scrolledtext

try:
    import requests
except Exception as e:
    requests = None

try:
    import minecraft_launcher_lib
except Exception:
    minecraft_launcher_lib = None

try:
    import microsoft_account
except Exception:
    microsoft_account = None

# ---------------------------------------------------------------- constants
APP_NAME = "EXZODUS"
APP_VERSION = "2.1.1"
API_BASE = "https://clan-exzodus.vercel.app/api"

SOFTWARE = {
    "infinity": {"name": "Infinity 1.21.4 (Fabric)", "version": "1.21.4", "loader": "fabric", "loader_version": "0.16.14", "telegram": "https://t.me/HET_CTPAXA_x"},
    "lobok":    {"name": "Lobok Client 1.16.5",      "version": "1.16.5", "loader": "fabric", "loader_version": "0.14.25", "telegram": "https://t.me/LobokClient"},
}

CONFIG_DIR = os.path.join(os.path.expanduser("~"), ".exodus_launcher")
CONFIG_PATH = os.path.join(CONFIG_DIR, "config.json")
DEFAULT_CONFIG = {"software": "infinity", "ram": 2048, "fullscreen": True,
                  "width": 1920, "height": 1080, "jvm_args": "", "mc_dir": "",
                  "token": "", "validated_key": "", "validated_sw": ""}


def resource_path(rel):
    base = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(base, rel)


def hwid():
    mac = uuid.getnode()
    node = ":".join(format((mac >> (8 * i)) & 0xff, "02x") for i in range(6))
    return hashlib.sha256(node.encode()).hexdigest()[:32]


def load_config():
    try:
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            c = json.load(f)
        for k, v in DEFAULT_CONFIG.items():
            c.setdefault(k, v)
        return c
    except Exception:
        return dict(DEFAULT_CONFIG)


def save_config(c):
    try:
        os.makedirs(CONFIG_DIR, exist_ok=True)
        with open(CONFIG_PATH, "w", encoding="utf-8") as f:
            json.dump(c, f, indent=2, ensure_ascii=False)
    except Exception:
        pass


CONFIG = load_config()


def api_request(path, body=None, token=None, method="POST", binary=False, timeout=30):
    if requests is None:
        return {"error": "requests недоступен"}
    url = API_BASE + path
    headers = {"content-type": "application/json"}
    if token:
        headers["authorization"] = "Bearer " + token
    data = json.dumps(body).encode("utf-8") if body is not None else None
    try:
        r = requests.request(method, url, headers=headers, data=data, timeout=timeout)
    except Exception as e:
        return {"error": str(e)}
    if binary:
        return r
    try:
        return r.json()
    except Exception:
        return {"_raw": r.text, "ok": r.ok, "status": r.status_code}


def mc_directory():
    if CONFIG.get("mc_dir"):
        return CONFIG["mc_dir"]
    if minecraft_launcher_lib:
        return minecraft_launcher_lib.utils.get_minecraft_directory()
    return os.path.join(os.environ.get("APPDATA", os.path.expanduser("~")), ".minecraft")


# ---------------------------------------------------------------- UI theme
class Style:
    BG = "#0e0e14"
    PANEL = "#16161f"
    PANEL2 = "#1d1d28"
    ACCENT = "#7c3aed"
    ACCENT2 = "#9b6bff"
    TEXT = "#e7e7ef"
    MUTED = "#9a9ab0"
    OK = "#36d399"
    ERR = "#f87272"
    BORDER = "#2a2a38"


def apply_theme(root):
    try:
        root.tk.call("source", resource_path("azure.tcl")) if os.path.exists(resource_path("azure.tcl")) else None
    except Exception:
        pass
    s = ttk.Style(root)
    try:
        s.theme_use("clam")
    except Exception:
        pass
    s.configure(".", background=Style.BG, foreground=Style.TEXT, borderwidth=0)
    s.configure("TFrame", background=Style.BG)
    s.configure("Card.TFrame", background=Style.PANEL, relief="flat")
    s.configure("TLabel", background=Style.BG, foreground=Style.TEXT)
    s.configure("Muted.TLabel", background=Style.BG, foreground=Style.MUTED)
    s.configure("Title.TLabel", background=Style.BG, foreground=Style.TEXT, font=("Segoe UI", 16, "bold"))
    s.configure("TButton", background=Style.ACCENT, foreground="white", padding=8, font=("Segoe UI", 10, "bold"))
    s.map("TButton", background=[("active", Style.ACCENT2), ("disabled", "#3a3a48")])
    s.configure("Ghost.TButton", background=Style.PANEL2, foreground=Style.TEXT)
    s.map("Ghost.TButton", background=[("active", Style.PANEL)])
    s.configure("TEntry", fieldbackground=Style.PANEL2, foreground=Style.TEXT, insertcolor=Style.TEXT, padding=6)
    s.configure("TCombobox", fieldbackground=Style.PANEL2, foreground=Style.TEXT, padding=6)
    s.configure("TNotebook", background=Style.BG, borderwidth=0)
    s.configure("TNotebook.Tab", background=Style.PANEL, foreground=Style.MUTED, padding=[14, 8], font=("Segoe UI", 10, "bold"))
    s.map("TNotebook.Tab", background=[("selected", Style.ACCENT)], foreground=[("selected", "white")])
    s.configure("Horizontal.TScale", background=Style.BG, troughcolor=Style.PANEL2)
    s.configure("TCheckbutton", background=Style.BG, foreground=Style.TEXT)
    s.configure("TLabelframe", background=Style.BG, foreground=Style.MUTED)
    s.configure("TLabelframe.Label", background=Style.BG, foreground=Style.ACCENT2)


# ---------------------------------------------------------------- loading screen
class LoadingScreen(tk.Toplevel):
    def __init__(self, parent, title="EXZODUS"):
        super().__init__(parent)
        self.title(title)
        self.geometry("460x260")
        self.configure(bg=Style.BG)
        self.resizable(False, False)
        try:
            self.iconbitmap(resource_path("icon.ico"))
        except Exception:
            pass
        self.overrideredirect(True)
        self.var = tk.DoubleVar(value=0)
        self.status = tk.StringVar(value="Загрузка…")
        self.pct = tk.StringVar(value="")

        tk.Label(self, text="EXZODUS", bg=Style.BG, fg=Style.ACCENT2, font=("Segoe UI", 26, "bold")).pack(pady=(26, 2))
        tk.Label(self, text="LAUNCHER", bg=Style.BG, fg=Style.MUTED, font=("Segoe UI", 11)).pack()
        self._bar = ttk.Progressbar(self, variable=self.var, maximum=100, length=360, mode="indeterminate")
        self._bar.pack(pady=(26, 10))
        tk.Label(self, textvariable=self.status, bg=Style.BG, fg=Style.TEXT, font=("Segoe UI", 10)).pack()
        self._pct = tk.Label(self, textvariable=self.pct, bg=Style.BG, fg=Style.MUTED, font=("Segoe UI", 9))
        self._pct.pack()
        self._pulse = False
        self.center()
        self.update_idletasks()

    def center(self):
        self.update_idletasks()
        w, h = 460, 260
        x = (self.winfo_screenwidth() // 2) - (w // 2)
        y = (self.winfo_screenheight() // 2) - (h // 2)
        self.geometry(f"{w}x{h}+{x}+{y}")

    def set_status(self, text):
        self.status.set(str(text)[:140])
        self.update_idletasks()

    def set_progress(self, value):
        try:
            v = float(value)
        except Exception:
            return
        if self._bar["mode"] != "determinate":
            self._bar.configure(mode="determinate")
        self.var.set(max(0, min(100, v)))
        self.pct.set(f"{int(self.var.get())}%")
        self.update_idletasks()

    def start_pulse(self):
        self._bar.configure(mode="indeterminate")
        self._bar.start(15)

    def stop_pulse(self):
        try:
            self._bar.stop()
        except Exception:
            pass

    def finish(self):
        try:
            self.destroy()
        except Exception:
            pass


# ---------------------------------------------------------------- main app
class Launcher(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title(f"{APP_NAME} Launcher {APP_VERSION}")
        self.geometry("900x600")
        self.minsize(820, 560)
        self.configure(bg=Style.BG)
        try:
            self.iconbitmap(resource_path("icon.ico"))
        except Exception:
            pass
        apply_theme(self)

        self.token = CONFIG.get("token", "")
        self.user = None
        self.validated_key = CONFIG.get("validated_key", "")
        self.validated_sw = CONFIG.get("validated_sw", "")
        self.mc_auth = None
        self.launching = False

        self._build_header()
        self._body = tk.Frame(self, bg=Style.BG)
        self._body.pack(fill="both", expand=True)

        # self-update check (non-blocking)
        threading.Thread(target=None, daemon=True).start() if False else None
        self._pending_update = None
        t = threading.Thread(target=self._startup, daemon=True)
        t.start()

    # ---- header
    def _build_header(self):
        h = tk.Frame(self, bg=Style.PANEL, height=54)
        h.pack(fill="x")
        h.pack_propagate(False)
        tk.Label(h, text="EXZODUS", bg=Style.PANEL, fg=Style.ACCENT2, font=("Segoe UI", 16, "bold")).pack(side="left", padx=18)
        self._account_lbl = tk.Label(h, text="не авторизован", bg=Style.PANEL, fg=Style.MUTED, font=("Segoe UI", 10))
        self._account_lbl.pack(side="right", padx=12)
        tk.Button(h, text="Выйти", bg=Style.PANEL2, fg=Style.TEXT, relief="flat",
                  command=self._logout, font=("Segoe UI", 9)).pack(side="right", padx=4)
        self._menu = tk.Menubutton(h, text="☰", bg=Style.PANEL, fg=Style.TEXT, relief="flat", font=("Segoe UI", 12))
        self._menu.pack(side="right", padx=8)
        m = tk.Menu(self._menu, tearoff=0, bg=Style.PANEL2, fg=Style.TEXT)
        m.add_command(label="Открыть .minecraft", command=lambda: self._open(mc_directory()))
        m.add_command(label="Открыть папку модов", command=lambda: self._open(os.path.join(mc_directory(), "mods")))
        m.add_command(label="Открыть логи", command=lambda: self._open(os.path.join(mc_directory(), "logs")))
        m.add_command(label="Папка лаунчера", command=lambda: self._open(CONFIG_DIR))
        m.add_separator()
        m.add_command(label="Проверить обновление", command=lambda: threading.Thread(target=self._check_update, daemon=True).start())
        m.add_command(label="О сборке", command=lambda: messagebox.showinfo("EXZODUS", f"Версия лаунчера {APP_VERSION}"))
        self._menu.configure(menu=m)

    def _open(self, path):
        try:
            os.makedirs(path, exist_ok=True)
        except Exception:
            pass
        try:
            os.startfile(path)
        except Exception:
            try:
                subprocess.Popen(["xdg-open", path])
            except Exception as e:
                messagebox.showerror("Ошибка", str(e))

    # ---- startup sequence
    def _startup(self):
        self._show_splash("Проверка обновлений…")
        self._check_update(show_splash=True)
        self._hide_splash()
        if self.token:
            self._refresh_session()
        self._show_login_or_main()

    def _show_login_or_main(self):
        self.after(0, self._render)

    # ---- splash
    def _show_splash(self, text):
        self.after(0, lambda: self._splash_set(text))

    def _splash_set(self, text):
        if not hasattr(self, "_splash"):
            self._splash = LoadingScreen(self, "EXZODUS")
            self._splash.start_pulse()
        self._splash.set_status(text)

    def _hide_splash(self):
        self.after(0, self._splash_finish)

    def _splash_finish(self):
        if hasattr(self, "_splash"):
            self._splash.stop_pulse()
            self._splash.finish()
            del self._splash

    # ---- self update
    def _check_update(self, show_splash=False):
        try:
            r = api_request("/launcher-update", {"action": "manifest", "currentVersion": APP_VERSION, "channel": "stable"})
            if r.get("release") and _vtuple(r["release"]["version"]) > _vtuple(APP_VERSION):
                self._pending_update = r["release"]
                if show_splash:
                    self._splash_set("Доступно обновление лаунчера — скачиваю…")
                    self._download_self_update(r["release"])
        except Exception:
            pass

    def _download_self_update(self, release):
        try:
            r = api_request("/download", {"action": "chunk", "releaseId": release["id"], "index": 0}, binary=True)
            # download all chunks
            chunks = []
            for i in range(int(release["chunk_count"])):
                rr = api_request("/download", {"action": "chunk", "releaseId": release["id"], "index": i}, binary=True)
                if not rr.ok:
                    return
                chunks.append(base64.b64decode(rr.json()["data"]))
            data = b"".join(chunks)
            if hashlib.sha256(data).hexdigest().lower() != str(release["sha256"]).lower():
                return
            tmp = os.path.join(tempfile.gettempdir(), "exodus_launcher_new.exe")
            with open(tmp, "wb") as f:
                f.write(data)
            self._pending_update_path = tmp
            # launch new exe with --replace of current path, then exit
            subprocess.Popen([tmp, "--replace", os.path.abspath(sys.argv[0])])
            os._exit(0)
        except Exception:
            pass

    # ---- session
    def _refresh_session(self):
        j = api_request("/auth", {"action": "profile"}, token=self.token)
        if j.get("user"):
            self.user = j["user"]
        else:
            self.token = ""
            CONFIG["token"] = ""
            save_config(CONFIG)
        self.after(0, self._update_account_label)

    def _update_account_label(self):
        if self.user:
            self._account_lbl.configure(text=f"@{self.user.get('login','')}" + (" · админ" if self.user.get("admin") else ""))
        else:
            self._account_lbl.configure(text="не авторизован")

    def _logout(self):
        self.token = ""
        self.user = None
        CONFIG["token"] = ""
        save_config(CONFIG)
        self._update_account_label()
        self._render()

    # ---- render
    def _render(self):
        for w in self._body.winfo_children():
            w.destroy()
        if not self.token or not self.user:
            self._render_login()
        else:
            self._render_main()

    # ---- login
    def _render_login(self):
        f = tk.Frame(self._body, bg=Style.BG)
        f.pack(fill="both", expand=True)
        card = tk.Frame(f, bg=Style.PANEL, padx=30, pady=30, width=380)
        card.place(relx=0.5, rely=0.5, anchor="center")
        tk.Label(card, text="Вход в EXZODUS", bg=Style.PANEL, fg=Style.TEXT, font=("Segoe UI", 16, "bold")).pack(pady=(0, 14))
        tk.Label(card, text="Логин", bg=Style.PANEL, fg=Style.MUTED, anchor="w").pack(fill="x")
        login = ttk.Entry(card)
        login.pack(fill="x", pady=(2, 8))
        tk.Label(card, text="Пароль", bg=Style.PANEL, fg=Style.MUTED, anchor="w").pack(fill="x")
        pwd = ttk.Entry(card, show="*")
        pwd.pack(fill="x", pady=(2, 14))
        status = tk.Label(card, text="", bg=Style.PANEL, fg=Style.ERR, font=("Segoe UI", 9))
        status.pack(fill="x")

        def do_login():
            status.configure(text="")
            j = api_request("/auth", {"action": "login", "login": login.get().strip(), "password": pwd.get()})
            if j.get("token"):
                self.token = j["token"]
                CONFIG["token"] = self.token
                save_config(CONFIG)
                self._refresh_session()
                self._render()
            else:
                status.configure(text=j.get("message", "Ошибка входа"))

        ttk.Button(card, text="Войти", command=do_login).pack(fill="x", pady=(0, 8))
        ttk.Button(card, text="Регистрация", style="Ghost.TButton", command=lambda: self._register_account(login.get(), pwd.get(), status)).pack(fill="x")

    def _register_account(self, login, pwd, status):
        status.configure(text="")
        if len(pwd) < 8:
            status.configure(text="Пароль минимум 8 символов")
            return
        j = api_request("/auth", {"action": "register", "login": login.strip(), "password": pwd, "nickname": login.strip()})
        if j.get("token"):
            self.token = j["token"]
            CONFIG["token"] = self.token
            save_config(CONFIG)
            self._refresh_session()
            self._render()
        else:
            status.configure(text=j.get("message", "Ошибка регистрации"))

    # ---- main
    def _render_main(self):
        self._update_account_label()
        nb = ttk.Notebook(self._body)
        nb.pack(fill="both", expand=True, padx=10, pady=10)
        self._tab_play = tk.Frame(nb, bg=Style.BG)
        self._tab_settings = tk.Frame(nb, bg=Style.BG)
        self._tab_services = tk.Frame(nb, bg=Style.BG)
        nb.add(self._tab_play, text="  Игра  ")
        nb.add(self._tab_settings, text="  Настройки  ")
        nb.add(self._tab_services, text="  Сервисы  ")
        if self.user and self.user.get("admin"):
            self._tab_admin = tk.Frame(nb, bg=Style.BG)
            nb.add(self._tab_admin, text="  Админ  ")
            self._render_admin(self._tab_admin)
        self._render_play(self._tab_play)
        self._render_settings(self._tab_settings)
        self._render_services(self._tab_services)

    # ---- play tab
    def _render_play(self, parent):
        # software selector
        tk.Label(parent, text="Продукт", bg=Style.BG, fg=Style.MUTED).pack(anchor="w", padx=14, pady=(12, 2))
        sw_var = tk.StringVar(value=CONFIG.get("software", "infinity"))
        swf = tk.Frame(parent, bg=Style.BG)
        swf.pack(fill="x", padx=14, pady=(0, 8))
        for sid, s in SOFTWARE.items():
            ttk.Radiobutton(swf, text=s["name"], variable=sw_var, value=sid,
                            command=lambda v=sw_var: self._on_sw_change(v.get())).pack(side="left", padx=8)

        # key
        kf = tk.Frame(parent, bg=Style.BG)
        kf.pack(fill="x", padx=14, pady=8)
        tk.Label(kf, text="Лицензионный ключ", bg=Style.BG, fg=Style.MUTED).pack(anchor="w")
        key_var = tk.StringVar(value=self.validated_key)
        ke = ttk.Entry(kf, textvariable=key_var)
        ke.pack(side="left", fill="x", expand=True, pady=2)
        ttk.Button(kf, text="Проверить", command=lambda: self._validate_key(sw_var.get(), key_var.get())).pack(side="left", padx=6)
        self._key_status = tk.Label(parent, text="", bg=Style.BG, fg=Style.MUTED, font=("Segoe UI", 9))
        self._key_status.pack(anchor="w", padx=14)

        # launch button
        self._launch_btn = ttk.Button(parent, text="▶  ЗАПУСТИТЬ MINECRAFT", command=lambda: self._launch(sw_var.get(), key_var.get()))
        self._launch_btn.pack(pady=18, ipady=8, padx=60, fill="x")

        self._play_status = tk.Label(parent, text="Готов к запуску.", bg=Style.BG, fg=Style.MUTED, font=("Segoe UI", 9))
        self._play_status.pack(anchor="w", padx=14)

        # quick links
        lf = tk.Frame(parent, bg=Style.BG)
        lf.pack(fill="x", padx=14, pady=(20, 0))
        ttk.Button(lf, text="Цены / Telegram", style="Ghost.TButton",
                   command=lambda: webbrowser.open(SOFTWARE[sw_var.get()]["telegram"])).pack(side="left", padx=4)
        ttk.Button(lf, text="Статистика сайта", style="Ghost.TButton",
                   command=lambda: webbrowser.open("https://clan-exzodus.vercel.app/stats.html")).pack(side="left", padx=4)

    def _on_sw_change(self, sw):
        CONFIG["software"] = sw
        save_config(CONFIG)

    def _validate_key(self, sw, key):
        key = key.strip().upper()
        if not key:
            self._key_status.configure(text="Введите ключ", fg=Style.ERR)
            return
        j = api_request("/validate", {"key": key, "software": sw, "hwid": hwid()})
        if j.get("valid"):
            self.validated_key = key
            self.validated_sw = sw
            CONFIG["validated_key"] = key
            CONFIG["validated_sw"] = sw
            save_config(CONFIG)
            exp = j.get("expiresAt") or ("навсегда" if j.get("durationDays") is None else f"{j.get('durationDays')} дн.")
            self._key_status.configure(text=f"✓ Ключ активен ({exp})", fg=Style.OK)
        else:
            self._key_status.configure(text="✗ " + j.get("message", "недействителен"), fg=Style.ERR)

    # ---- settings tab
    def _render_settings(self, parent):
        sf = tk.Frame(parent, bg=Style.BG, padx=16, pady=14)
        sf.pack(fill="both", expand=True)

        tk.Label(sf, text="Оперативная память (RAM)", bg=Style.BG, fg=Style.TEXT, font=("Segoe UI", 11, "bold")).pack(anchor="w", pady=(4, 2))
        ram_var = tk.IntVar(value=CONFIG.get("ram", 2048))
        ram_lbl = tk.Label(sf, text=f"{ram_var.get()} МБ", bg=Style.BG, fg=Style.ACCENT2, font=("Segoe UI", 10))
        ram_lbl.pack(anchor="e")

        def ram_update(v):
            ram_lbl.configure(text=f"{int(float(v))} МБ")
        scale = ttk.Scale(sf, from_=512, to=16384, variable=ram_var, orient="horizontal", command=ram_update)
        scale.pack(fill="x", pady=(0, 10))

        # resolution
        tk.Label(sf, text="Разрешение экрана", bg=Style.BG, fg=Style.TEXT, font=("Segoe UI", 11, "bold")).pack(anchor="w", pady=(6, 2))
        resf = tk.Frame(sf, bg=Style.BG)
        resf.pack(fill="x", pady=(0, 10))
        fs_var = tk.BooleanVar(value=CONFIG.get("fullscreen", True))
        ttk.Checkbutton(resf, text="Полноэкранный режим", variable=fs_var).pack(side="left")
        w_var = tk.StringVar(value=str(CONFIG.get("width", 1920)))
        h_var = tk.StringVar(value=str(CONFIG.get("height", 1080)))
        tk.Label(resf, text="  Ш:", bg=Style.BG, fg=Style.MUTED).pack(side="left")
        ttk.Entry(resf, textvariable=w_var, width=7).pack(side="left")
        tk.Label(resf, text=" В:", bg=Style.BG, fg=Style.MUTED).pack(side="left")
        ttk.Entry(resf, textvariable=h_var, width=7).pack(side="left")

        # jvm args
        tk.Label(sf, text="Дополнительные JVM-аргументы", bg=Style.BG, fg=Style.TEXT, font=("Segoe UI", 11, "bold")).pack(anchor="w", pady=(6, 2))
        jvm_var = tk.StringVar(value=CONFIG.get("jvm_args", ""))
        ttk.Entry(sf, textvariable=jvm_var).pack(fill="x", pady=(0, 10))

        # mc dir
        tk.Label(sf, text="Папка Minecraft (.minecraft)", bg=Style.BG, fg=Style.TEXT, font=("Segoe UI", 11, "bold")).pack(anchor="w", pady=(6, 2))
        dirf = tk.Frame(sf, bg=Style.BG)
        dirf.pack(fill="x", pady=(0, 10))
        dir_var = tk.StringVar(value=CONFIG.get("mc_dir", ""))
        ttk.Entry(dirf, textvariable=dir_var).pack(side="left", fill="x", expand=True)
        ttk.Button(dirf, text="Обзор…", style="Ghost.TButton", command=lambda: self._browse_dir(dir_var)).pack(side="left", padx=4)
        tk.Label(sf, text=f"Текущая: {mc_directory()}", bg=Style.BG, fg=Style.MUTED, font=("Segoe UI", 9)).pack(anchor="w")

        def save():
            try:
                ram = int(float(ram_var.get()))
            except Exception:
                ram = 2048
            CONFIG["ram"] = max(512, min(16384, ram))
            CONFIG["fullscreen"] = fs_var.get()
            CONFIG["width"] = int(w_var.get() or 1920)
            CONFIG["height"] = int(h_var.get() or 1080)
            CONFIG["jvm_args"] = jvm_var.get().strip()
            CONFIG["mc_dir"] = dir_var.get().strip()
            save_config(CONFIG)
            messagebox.showinfo("EXZODUS", "Настройки сохранены.")
        ttk.Button(sf, text="Сохранить настройки", command=save).pack(anchor="w", pady=10)

    def _browse_dir(self, var):
        d = filedialog.askdirectory(title="Выберите папку .minecraft")
        if d:
            var.set(d)

    # ---- services tab
    def _render_services(self, parent):
        sf = tk.Frame(parent, bg=Style.BG, padx=16, pady=14)
        sf.pack(fill="both", expand=True)
        tk.Label(sf, text="Быстрые сервисы", bg=Style.BG, fg=Style.TEXT, font=("Segoe UI", 13, "bold")).pack(anchor="w", pady=(4, 10))
        buttons = [
            ("Открыть .minecraft", os.path.join(mc_directory())),
            ("Открыть папку модов", os.path.join(mc_directory(), "mods")),
            ("Открыть логи", os.path.join(mc_directory(), "logs")),
            ("Открыть папку лаунчера", CONFIG_DIR),
            ("Открыть папку версий", os.path.join(mc_directory(), "versions")),
            ("Открыть asset и ресурсы", os.path.join(mc_directory(), "assets")),
        ]
        grid = tk.Frame(sf, bg=Style.BG)
        grid.pack(fill="x")
        for i, (label, path) in enumerate(buttons):
            r, c = divmod(i, 2)
            ttk.Button(grid, text=label, style="Ghost.TButton", command=lambda p=path: self._open(p)).grid(row=r, column=c, padx=6, pady=6, sticky="ew")
        tk.Label(sf, text=f"Лаунчер: {APP_VERSION}   •   API: {API_BASE}", bg=Style.BG, fg=Style.MUTED, font=("Segoe UI", 9)).pack(anchor="w", pady=(20, 0))

    # ---- admin tab
    def _render_admin(self, parent):
        sf = tk.Frame(parent, bg=Style.BG, padx=16, pady=14)
        sf.pack(fill="both", expand=True)
        tk.Label(sf, text="Админ-панель", bg=Style.BG, fg=Style.ACCENT2, font=("Segoe UI", 14, "bold")).pack(anchor="w", pady=(0, 10))

        # create key
        cf = tk.LabelFrame(sf, text="Создать ключ", bg=Style.BG, fg=Style.MUTED, padx=10, pady=8)
        cf.pack(fill="x", pady=6)
        row = tk.Frame(cf, bg=Style.BG)
        row.pack(fill="x")
        tk.Label(row, text="Продукт", bg=Style.BG, fg=Style.MUTED).pack(side="left")
        swk = tk.StringVar(value="infinity")
        ttk.Combobox(row, textvariable=swk, values=list(SOFTWARE.keys()), state="readonly", width=12).pack(side="left", padx=6)
        tk.Label(row, text="Дней (пусто = навсегда)", bg=Style.BG, fg=Style.MUTED).pack(side="left")
        days = tk.StringVar(value="30")
        ttk.Entry(row, textvariable=days, width=8).pack(side="left", padx=6)
        ttk.Button(row, text="Создать", command=lambda: self._admin_create(swk.get(), days.get())).pack(side="left", padx=6)
        self._admin_key_out = tk.Label(cf, text="", bg=Style.BG, fg=Style.OK, font=("Segoe UI", 9))
        self._admin_key_out.pack(anchor="w", pady=(4, 0))

        # actions
        af = tk.Frame(sf, bg=Style.BG)
        af.pack(fill="x", pady=6)
        ttk.Button(af, text="Список ключей", command=self._admin_list).pack(side="left", padx=4)
        ttk.Button(af, text="Статистика", command=self._admin_stats).pack(side="left", padx=4)
        ttk.Button(af, text="Отозвать ключ", command=self._admin_revoke_dialog).pack(side="left", padx=4)

        self._admin_out = scrolledtext.ScrolledText(sf, bg=Style.PANEL2, fg=Style.TEXT, height=12, width=90, font=("Consolas", 9))
        self._admin_out.pack(fill="both", expand=True, pady=8)

    def _admin_out_write(self, text):
        self._admin_out.delete("1.0", "end")
        self._admin_out.insert("1.0", text)

    def _admin_create(self, sw, days):
        try:
            d = int(days) if str(days).strip() else None
        except Exception:
            d = None
        j = api_request("/admin", {"action": "create", "software": sw, "durationDays": d}, token=self.token)
        if j.get("key"):
            self._admin_key_out.configure(text=f"Ключ: {j['key']} ({j.get('software')}) — скопирован")
            try:
                import tkinter.clipboard as _c
            except Exception:
                pass
            self.clipboard_clear()
            self.clipboard_append(j["key"])
            self.update()
        else:
            self._admin_key_out.configure(text="Ошибка: " + j.get("message", ""), fg=Style.ERR)

    def _admin_list(self):
        j = api_request("/admin", {"action": "list"}, token=self.token)
        if j.get("licenses") is None:
            self._admin_out_write("Ошибка: " + str(j.get("message", "")))
            return
        lines = []
        for l in j["licenses"]:
            exp = l.get("expiresAt") or ("навсегда" if l.get("durationDays") is None else f"{l.get('durationDays')} дн.")
            lines.append(f"{l.get('key')} | {l.get('software')} | {exp} | {'привязан' if l.get('hwidBound') else 'свободен'} | {'ОТОЗВАН' if l.get('revoked') else 'активен'}")
        self._admin_out_write("\n".join(lines) or "Нет ключей")

    def _admin_stats(self):
        j = api_request("/stats", method="GET")
        if j.get("registrations") is None and "error" in j:
            self._admin_out_write("Ошибка: " + str(j))
            return
        text = (f"Регистраций: {j.get('registrations')}\n"
                f"Скачиваний: {j.get('downloads')}\n"
                f"Ключей всего: {j.get('keys')}  (Infinity: {j.get('bySoftware', {}).get('infinity', {}).get('keys')}, Lobok: {j.get('bySoftware', {}).get('lobok', {}).get('keys')})\n"
                f"Серверов за всё время: {j.get('serversTotal')}\n"
                f"Играют сейчас: {j.get('online')}\n"
                f"Запусков: {j.get('launchesTotal')}")
        self._admin_out_write(text)

    def _admin_revoke_dialog(self):
        d = tk.Toplevel(self)
        d.title("Отозвать ключ")
        d.geometry("360x120")
        d.configure(bg=Style.BG)
        try:
            d.iconbitmap(resource_path("icon.ico"))
        except Exception:
            pass
        tk.Label(d, text="Ключ", bg=Style.BG, fg=Style.MUTED).pack(padx=10, pady=(10, 2))
        kv = tk.StringVar()
        ttk.Entry(d, textvariable=kv).pack(fill="x", padx=10, pady=2)

        def do():
            j = api_request("/admin", {"action": "revoke", "key": kv.get().strip().upper()}, token=self.token)
            messagebox.showinfo("EXZODUS", "Отозван: " + str(j.get("changed")))
            d.destroy()
        ttk.Button(d, text="Отозвать", command=do).pack(pady=8)

    # ---- launch
    def _launch(self, sw, key):
        if self.launching:
            return
        key = key.strip().upper()
        if not self.validated_key or self.validated_sw != sw or self.validated_key != key:
            j = api_request("/validate", {"key": key, "software": sw, "hwid": hwid()})
            if not j.get("valid"):
                messagebox.showerror("EXZODUS", "Ключ недействителен: " + j.get("message", ""))
                return
            self.validated_key = key
            self.validated_sw = sw
            CONFIG["validated_key"] = key
            CONFIG["validated_sw"] = sw
            save_config(CONFIG)
        self.launching = True
        self._launch_btn.configure(state="disabled")
        splash = LoadingScreen(self, "EXZODUS — запуск")
        splash.start_pulse()
        threading.Thread(target=self._launch_thread, args=(sw, key, splash), daemon=True).start()

    def _write_license_key(self, key):
        try:
            opt = os.path.join(mc_directory(), ".options")
            os.makedirs(opt, exist_ok=True)
            with open(os.path.join(opt, "license-key.txt"), "w", encoding="utf-8") as f:
                f.write(key)
        except Exception:
            pass

    def _ensure_mod(self, sw, splash):
        splash.set_status("Проверка клиента…")
        r = api_request("/mod-update", {"action": "manifest", "channel": "stable", "software": sw, "currentVersion": "0.0.0", "hwid": hwid()})
        rel = r.get("release")
        if not rel:
            return True  # no mod required / ignore
        mods = os.path.join(mc_directory(), "mods")
        os.makedirs(mods, exist_ok=True)
        target = os.path.join(mods, rel["filename"])
        # download if missing or changed
        need = True
        if os.path.exists(target):
            try:
                with open(target, "rb") as f:
                    if hashlib.sha256(f.read()).hexdigest().lower() == str(rel["sha256"]).lower():
                        need = False
            except Exception:
                need = True
        if need:
            splash.set_status(f"Скачивание {rel['filename']}…")
            chunks = []
            total = int(rel["chunk_count"])
            for i in range(total):
                rr = api_request("/download", {"action": "chunk", "releaseId": rel["id"], "index": i}, binary=True)
                if not rr.ok:
                    return False
                chunks.append(base64.b64decode(rr.json()["data"]))
                splash.set_progress((i + 1) / total * 100)
            data = b"".join(chunks)
            if hashlib.sha256(data).hexdigest().lower() != str(rel["sha256"]).lower():
                return False
            with open(target, "wb") as f:
                f.write(data)
        return True

    def _launch_thread(self, sw, key, splash):
        try:
            self._write_license_key(key)
            if minecraft_launcher_lib is None:
                self.after(0, lambda: messagebox.showerror("EXZODUS", "minecraft_launcher_lib не установлен"))
                return
            s = SOFTWARE[sw]
            md = mc_directory()
            # ensure mod
            if not self._ensure_mod(sw, splash):
                self.after(0, lambda: messagebox.showerror("EXZODUS", "Не удалось скачать клиент"))
                return
            # install fabric
            splash.set_status(f"Установка {s['name']}…")
            fabric_id = f"fabric-loader-{s['loader_version']}-{s['version']}"
            try:
                installed = [v["id"] for v in minecraft_launcher_lib.utils.get_installed_versions(md)]
            except Exception:
                installed = []
            if fabric_id not in installed:
                def cb(*a):
                    for x in a:
                        if isinstance(x, (int, float)) and 0 <= x <= 100:
                            splash.set_progress(x)
                    if a:
                        splash.set_status(str(a[-1])[:120])
                minecraft_launcher_lib.fabric.install_fabric(s["version"], md, loader_version=s["loader_version"], callback=cb)
            # microsoft auth
            splash.set_status("Авторизация Microsoft…")
            if microsoft_account is None:
                self.after(0, lambda: messagebox.showerror("EXZODUS", "microsoft_account недоступен"))
                return
            try:
                auth_info = microsoft_account.login()
            except Exception as e:
                self.after(0, lambda: messagebox.showerror("EXZODUS", "Ошибка входа Microsoft: " + str(e)))
                return
            name = auth_info.get("username") or auth_info.get("name")
            uid = auth_info.get("uuid") or auth_info.get("id")
            token = auth_info.get("access_token")
            # jvm options
            ram = CONFIG.get("ram", 2048)
            jvm = [f"-Xmx{ram}M", f"-Xms{min(ram, 1024)}M"]
            extra = CONFIG.get("jvm_args", "")
            if extra:
                jvm += extra.split()
            options = {"username": name, "uuid": uid, "token": token, "jvmArguments": jvm}
            if not CONFIG.get("fullscreen"):
                options["resolutionWidth"] = str(CONFIG.get("width", 1920))
                options["resolutionHeight"] = str(CONFIG.get("height", 1080))
            splash.set_status("Запуск Minecraft…")
            splash.set_progress(100)
            cmd = minecraft_launcher_lib.command.get_minecraft_command(fabric_id, md, options)
            splash.stop_pulse()
            subprocess.run(cmd)
        except Exception as e:
            self.after(0, lambda: messagebox.showerror("EXZODUS", "Ошибка запуска: " + str(e)))
        finally:
            try:
                splash.finish()
            except Exception:
                pass
            self.launching = False
            self.after(0, lambda: self._launch_btn.configure(state="normal"))


def _vtuple(v):
    try:
        return tuple(int(x) for x in str(v).split("."))
    except Exception:
        return (0,)


def main():
    # self-replace mode
    if "--replace" in sys.argv and len(sys.argv) > sys.argv.index("--replace") + 1:
        try:
            old = sys.argv[sys.argv.index("--replace") + 1]
            if os.path.exists(old) and old.lower() != os.path.abspath(sys.executable).lower():
                shutil.copyfile(os.path.abspath(sys.executable), old)
        except Exception:
            pass
    app = Launcher()
    app.mainloop()


if __name__ == "__main__":
    main()
