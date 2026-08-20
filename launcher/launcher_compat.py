import base64
import hashlib
import json
import math
import os
import platform
import re
import shutil
import socket
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
import webbrowser
import zipfile
from pathlib import Path
import tkinter as tk
from tkinter import filedialog, messagebox, ttk

import minecraft_launcher_lib


APP_NAME = "LayF Infinyty Launcher"
API_BASE = "https://clan-exzodus.vercel.app/api"
APP_VERSION = "1.6.11"
GAME_VERSION = "1.21.4"
FABRIC_LOADER_VERSION = "0.16.14"
MOD_URL = "https://clan-exzodus.vercel.app/downloads/layf-infinyty.jar"
APP_DIR = Path(os.getenv("LOCALAPPDATA", Path.home())) / "LayFInfinytyLauncher"
GAME_DIR = Path(os.getenv("LOCALAPPDATA", Path.home())) / "LayFInfinyty" / "game"
CONFIG_FILE = APP_DIR / "config.json"


def bundled_file(name):
    base = Path(getattr(sys, "_MEIPASS", Path(__file__).resolve().parent))
    return base / name


class RoundedButton(tk.Canvas):
    def __init__(self, parent, text, command, active=False, height=42):
        self.normal = "#6d28d9" if active else "#211b2c"
        self.hover = "#8b3ff0" if active else "#392650"
        self.text_color = "#ffffff" if active else "#f1ecf8"
        self.border_color = "#9b63ff" if active else "#514163"
        self.disabled = False
        super().__init__(parent, height=height, bg=parent.cget("bg"),
                         highlightthickness=0, cursor="hand2")
        self.label = text
        self.command = command
        self.bind("<Configure>", lambda _event: self.draw(self.normal))
        self.bind("<Enter>", lambda _event: self.draw(self.hover))
        self.bind("<Leave>", lambda _event: self.draw(self.normal))
        self.bind("<Button-1>", lambda _event: None if self.disabled else self.command())

    def state(self, states):
        if "disabled" in states:
            self.disabled = True
            self.draw("#25222c")
        if "!disabled" in states:
            self.disabled = False
            self.draw(self.normal)

    def draw(self, color):
        self.delete("all")
        width = max(self.winfo_width(), 20)
        height = max(self.winfo_height(), 20)
        radius = min(15, max(7, height // 3))

        def rounded_rectangle(x1, y1, x2, y2, r, **kwargs):
            points = [
                x1 + r, y1, x2 - r, y1, x2, y1, x2, y1 + r,
                x2, y2 - r, x2, y2, x2 - r, y2, x1 + r, y2,
                x1, y2, x1, y2 - r, x1, y1 + r, x1, y1
            ]
            return self.create_polygon(points, smooth=True, splinesteps=24, **kwargs)

        rounded_rectangle(2, 5, width - 2, height - 1, radius, fill="#050407", outline="")
        rounded_rectangle(1, 1, width - 2, height - 5, radius, fill=color,
                          outline=self.border_color, width=2)
        highlight = "#a87aff" if color != "#211b2c" else "#49385d"
        self.create_line(18, 4, width - 18, 4, fill=highlight, width=2)
        self.create_text(width / 2, height / 2, text=self.label, fill=self.text_color,
                         font=("Bahnschrift SemiBold", 11))

class MemorySlider(tk.Canvas):
    def __init__(self, parent, variable, command=None):
        super().__init__(parent, height=58, bg=parent.cget("bg"), highlightthickness=0,
                         cursor="hand2")
        self.variable = variable
        self.command = command
        self.values = list(range(2048, 16385, 2048))
        self.bind("<Configure>", self.redraw)
        self.bind("<Button-1>", self.pick)
        self.bind("<B1-Motion>", self.pick)

    def pick(self, event):
        width = max(self.winfo_width() - 28, 1)
        ratio = min(1, max(0, (event.x - 14) / width))
        index = round(ratio * (len(self.values) - 1))
        self.variable.set(self.values[index])
        if self.command:
            self.command(self.values[index])
        self.redraw()

    def redraw(self, _event=None):
        self.delete("all")
        width = max(self.winfo_width(), 120)
        left, right, y = 14, width - 14, 17
        self.create_line(left, y, right, y, fill="#282431", width=8, capstyle="round")
        index = min(range(len(self.values)), key=lambda i: abs(self.values[i] - self.variable.get()))
        knob_x = left + (right - left) * index / (len(self.values) - 1)
        self.create_line(left, y, knob_x, y, fill="#7433df", width=8, capstyle="round")
        self.create_oval(knob_x - 8, y - 8, knob_x + 8, y + 8,
                         fill="#b895ff", outline="#f1eaff", width=2)
        for i, value in enumerate(self.values):
            x = left + (right - left) * i / (len(self.values) - 1)
            self.create_text(x, 43, text=str(value // 1024), fill="#8f899b",
                             font=("Bahnschrift SemiBold", 8))


class ModernProgress(tk.Canvas):
    def __init__(self, parent, maximum=100):
        super().__init__(parent, height=34, bg=parent.cget("bg"), highlightthickness=0)
        self.maximum = maximum
        self.value = 0
        self.bind("<Configure>", self.redraw)

    def configure(self, cnf=None, **kwargs):
        if "maximum" in kwargs:
            self.maximum = kwargs.pop("maximum")
        result = super().configure(cnf, **kwargs)
        self.redraw()
        return result

    def __setitem__(self, key, value):
        if key == "value":
            self.value = value
            self.redraw()
        else:
            super().__setitem__(key, value)

    def redraw(self, _event=None):
        self.delete("all")
        width = max(self.winfo_width(), 20)
        self.create_line(9, 16, width - 9, 16, fill="#211e29", width=14, capstyle="round")
        ratio = 0 if not self.maximum else min(1, max(0, float(self.value) / self.maximum))
        if ratio > 0:
            self.create_line(9, 16, 9 + (width - 18) * ratio, 16,
                             fill="#7c3aed", width=14, capstyle="round")
        percent = int(round(ratio * 100))
        self.create_text(width / 2, 16, text=f"{percent}%", fill="#ffffff",
                         font=("Bahnschrift SemiBold", 9))


def api_request(path, payload, token=""):
    data = json.dumps(payload).encode("utf-8")
    headers = {"Content-Type": "application/json", "User-Agent": "LayF-Launcher/1.0"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(API_BASE + path, data=data, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=12) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        try:
            body = json.load(error)
            raise RuntimeError(body.get("message", "Ошибка сервера"))
        except json.JSONDecodeError:
            raise RuntimeError(f"Ошибка сервера: HTTP {error.code}")
    except urllib.error.URLError:
        raise RuntimeError("Сервер LayF недоступен")


def launcher_hwid():
    source = "|".join([
        os.getenv("COMPUTERNAME", ""),
        os.getenv("PROCESSOR_IDENTIFIER", ""),
        os.getenv("SystemDrive", ""),
        "Windows 10",
        "amd64",
        os.getenv("USERNAME", "")
    ])
    return hashlib.sha256(source.encode("utf-8")).hexdigest()


def offline_uuid(nickname):
    digest = hashlib.md5(("OfflinePlayer:" + nickname).encode("utf-8")).digest()
    return str(uuid.UUID(bytes=digest, version=3)).replace("-", "")


class LayFLauncher(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title(APP_NAME)
        self.geometry("1120x760")
        self.minsize(980, 680)
        self.configure(bg="#050507")
        try:
            self.iconbitmap(str(bundled_file("layf-launcher-v2.ico")))
        except tk.TclError:
            pass
        try:
            self.launcher_background = tk.PhotoImage(file=str(bundled_file("launcher-bg.png")))
        except tk.TclError:
            self.launcher_background = None
        try:
            self.jarvis_core_image = tk.PhotoImage(file=str(bundled_file("jarvis-core-hq.png")))
        except tk.TclError:
            self.jarvis_core_image = None
        self.protocol("WM_DELETE_WINDOW", self.destroy)

        APP_DIR.mkdir(parents=True, exist_ok=True)
        GAME_DIR.mkdir(parents=True, exist_ok=True)
        self.config_data = self.load_config()
        self.token = self.config_data.get("token", "")
        self.user = None
        self.busy = False
        self.setup_style()

        if self.token:
            threading.Thread(target=self.restore_session, daemon=True).start()
            self.show_loading("Проверяем сессию...")
        else:
            self.show_auth()

    def setup_style(self):
        style = ttk.Style(self)
        style.theme_use("clam")
        style.configure("TEntry", fieldbackground="#111117", foreground="#ffffff",
                        bordercolor="#302b38", insertcolor="#ffffff", padding=11)
        style.configure("TButton", background="#5b21b6", foreground="#ffffff",
                        borderwidth=0, padding=(16, 12), font=("Segoe UI Semibold", 10))
        style.map("TButton", background=[("active", "#7130d8"), ("disabled", "#35323d")])
        style.configure("Secondary.TButton", background="#14131a", foreground="#e5e5ec",
                        borderwidth=1, bordercolor="#302b38")
        style.map("Secondary.TButton", background=[("active", "#24212d")])
        style.configure("TProgressbar", troughcolor="#111117", background="#6527c8", borderwidth=0)
        style.configure("TScale", background="#0b0b10", troughcolor="#292630")

    def create_jarvis_core(self, parent, size=235):
        if self.jarvis_core_image is not None:
            core = tk.Label(parent, image=self.jarvis_core_image, bg=parent.cget("bg"), borderwidth=0)
            core.pack(pady=(22, 2))
            return core
        canvas = tk.Canvas(parent, width=size, height=size, bg=parent.cget("bg"),
                           highlightthickness=0)
        canvas.pack(pady=(22, 2))
        center = size / 2
        canvas.create_oval(22, 22, size - 22, size - 22, outline="#292435", width=1)
        canvas.create_oval(35, 35, size - 35, size - 35, outline="#4c2d82", width=2)
        canvas.create_oval(57, 57, size - 57, size - 57, outline="#7c3aed", width=2)
        canvas.create_oval(78, 78, size - 78, size - 78, fill="#130d21",
                           outline="#a78bfa", width=2)
        canvas.create_oval(91, 91, size - 91, size - 91, fill="#7c3aed",
                           outline="#d8ccff", width=2)
        canvas.create_text(center, center, text="L", fill="white",
                           font=("Segoe UI Black", max(18, size // 8)))
        arcs = [
            canvas.create_arc(16, 16, size - 16, size - 16, start=15, extent=62,
                              style="arc", outline="#7c3aed", width=4),
            canvas.create_arc(43, 43, size - 43, size - 43, start=175, extent=95,
                              style="arc", outline="#b18cff", width=3),
            canvas.create_arc(66, 66, size - 66, size - 66, start=285, extent=48,
                              style="arc", outline="#6d28d9", width=4),
        ]
        for angle in range(0, 360, 30):
            rad = math.radians(angle)
            r1, r2 = size * .40, size * .44
            canvas.create_line(
                center + math.cos(rad) * r1, center + math.sin(rad) * r1,
                center + math.cos(rad) * r2, center + math.sin(rad) * r2,
                fill="#65557d", width=2)

        phase = 0
        def animate():
            nonlocal phase
            try:
                canvas.itemconfigure(arcs[0], start=15 + phase)
                canvas.itemconfigure(arcs[1], start=175 - phase * .7)
                canvas.itemconfigure(arcs[2], start=285 + phase * 1.4)
                canvas.itemconfigure(arcs[0], outline="#bda7ff" if (phase // 12) % 2 else "#7c3aed")
                phase = (phase + 3) % 360
                canvas.after(45, animate)
            except tk.TclError:
                pass
        animate()
        return canvas

    def clear(self):
        for widget in self.winfo_children():
            widget.destroy()

    def load_config(self):
        try:
            return json.loads(CONFIG_FILE.read_text(encoding="utf-8"))
        except Exception:
            return {"ram": 4096, "license_key": "", "local_mod": "", "test_mod": False}

    def save_config(self):
        CONFIG_FILE.write_text(json.dumps(self.config_data, ensure_ascii=False, indent=2), encoding="utf-8")

    def toggle_test_mod(self):
        if not self.user.get("admin"):
            return
        value = bool(getattr(self, "test_mod_var", tk.BooleanVar(value=False)).get())
        self.config_data["test_mod"] = value
        self.save_config()
        # Убираем метку версии, чтобы при следующем запуске мод скачался из нужного канала.
        try:
            metadata_file = (GAME_DIR / "mods" / ".layf-mod-version.json")
            metadata_file.unlink(missing_ok=True)
        except OSError:
            pass
        if hasattr(self, "status_label"):
            if value:
                self.status_label.config(
                    text="Включена тестовая сборка. При следующем запуске мод будет загружен из тестового канала.")
            else:
                self.status_label.config(
                    text="Выключена тестовая сборка. При следующем запуске будет загружен стабильный мод.")

    def show_loading(self, text):
        self.clear()
        frame = tk.Frame(self, bg="#08080c")
        frame.pack(expand=True)
        self.create_jarvis_core(frame, 170)
        tk.Label(frame, text="LayF Infinyty", bg="#08080c", fg="white",
                 font=("Segoe UI Semibold", 30)).pack(pady=(0, 14))
        tk.Label(frame, text=text, bg="#08080c", fg="#b3b3c0",
                 font=("Segoe UI", 11)).pack()

    def restore_session(self):
        try:
            result = api_request("/auth", {"action": "profile"}, self.token)
            self.user = result["user"]
            self.after(0, self.show_main)
        except Exception:
            self.token = ""
            self.config_data.pop("token", None)
            self.save_config()
            self.after(0, self.show_auth)

    def show_auth(self):
        self.clear()
        root = tk.Frame(self, bg="#08080c")
        root.pack(fill="both", expand=True)
        left = tk.Frame(root, bg="#0d0d13", width=360,
                        highlightbackground="#2c203d", highlightthickness=1)
        left.pack(side="left", fill="y")
        left.pack_propagate(False)
        self.create_jarvis_core(left, 230)
        tk.Label(left, text="LAYF", bg="#0d0d13", fg="white",
                 font=("Segoe UI Black", 36)).pack(anchor="w", padx=42, pady=(5, 0))
        tk.Label(left, text="INFINYTY", bg="#0d0d13", fg="#a78bfa",
                 font=("Segoe UI Semibold", 22)).pack(anchor="w", padx=45)
        tk.Label(left, text="Приватный клиент\nв отдельном лаунчере", justify="left",
                 bg="#0d0d13", fg="#bdb8c8", font=("Segoe UI", 11)).pack(anchor="w", padx=45, pady=20)

        panel = tk.Frame(root, bg="#08080c")
        panel.pack(side="left", fill="both", expand=True, padx=90, pady=75)
        self.auth_title = tk.Label(panel, text="Вход в аккаунт", bg="#08080c", fg="white",
                                   font=("Segoe UI Semibold", 25))
        self.auth_title.pack(anchor="w", pady=(0, 28))
        tk.Label(panel, text="Логин", bg="#08080c", fg="#adadb9").pack(anchor="w")
        self.login_entry = ttk.Entry(panel)
        self.login_entry.pack(fill="x", pady=(6, 15))
        tk.Label(panel, text="Пароль", bg="#08080c", fg="#adadb9").pack(anchor="w")
        self.password_entry = ttk.Entry(panel, show="•")
        self.password_entry.pack(fill="x", pady=(6, 15))
        self.nickname_label = tk.Label(panel, text="Ник Minecraft", bg="#08080c", fg="#adadb9")
        self.nickname_entry = ttk.Entry(panel)
        self.auth_error = tk.Label(panel, text="", bg="#08080c", fg="#ff667a", wraplength=420)
        self.auth_error.pack(fill="x", pady=(2, 10))
        self.auth_submit = ttk.Button(panel, text="Войти", command=self.submit_auth)
        self.auth_submit.pack(fill="x")
        self.register_mode = False
        self.auth_switch = ttk.Button(panel, text="Создать аккаунт", style="Secondary.TButton",
                                      command=self.toggle_register)
        self.auth_switch.pack(fill="x", pady=(10, 0))
        self.login_entry.focus_set()

    def toggle_register(self):
        self.register_mode = not self.register_mode
        if self.register_mode:
            self.auth_title.config(text="Регистрация")
            self.nickname_label.pack(anchor="w", before=self.auth_error)
            self.nickname_entry.pack(fill="x", pady=(6, 15), before=self.auth_error)
            self.auth_submit.config(text="Зарегистрироваться")
            self.auth_switch.config(text="У меня уже есть аккаунт")
        else:
            self.auth_title.config(text="Вход в аккаунт")
            self.nickname_label.pack_forget()
            self.nickname_entry.pack_forget()
            self.auth_submit.config(text="Войти")
            self.auth_switch.config(text="Создать аккаунт")

    def submit_auth(self):
        if self.busy:
            return
        self.busy = True
        self.auth_request_id = getattr(self, "auth_request_id", 0) + 1
        request_id = self.auth_request_id
        self.auth_submit.state(["disabled"])
        self.auth_error.config(text="Подключение...")
        payload = {
            "action": "register" if self.register_mode else "login",
            "login": self.login_entry.get().strip(),
            "password": self.password_entry.get()
        }
        if self.register_mode:
            payload["nickname"] = self.nickname_entry.get().strip()

        def timeout_guard():
            if self.busy and request_id == self.auth_request_id:
                self.auth_request_id += 1
                self.busy = False
                self.auth_submit.state(["!disabled"])
                self.auth_error.config(text="Сервер не ответил за 15 секунд. Повторите попытку.")

        self.after(15000, timeout_guard)

        def worker():
            try:
                result = api_request("/auth", payload)
                if request_id != self.auth_request_id:
                    return
                self.token = result["token"]
                self.user = result["user"]
                self.config_data["token"] = self.token
                self.save_config()
                self.after(0, self.show_main)
            except Exception as error:
                if request_id == self.auth_request_id:
                    message = str(error)
                    self.after(0, lambda text=message: self.auth_error.config(text=text))
            finally:
                if request_id == self.auth_request_id:
                    self.busy = False
                    self.after(0, lambda: self.auth_submit.state(["!disabled"]))
        threading.Thread(target=worker, daemon=True).start()

    def show_main(self, section="game"):
        self.clear()
        self.current_section = section
        shell = tk.Frame(self, bg="#08080c")
        shell.pack(fill="both", expand=True)
        sidebar = tk.Frame(shell, bg="#101016", width=235)
        sidebar.pack(side="left", fill="y")
        sidebar.pack_propagate(False)
        self.create_jarvis_core(sidebar, 125)
        tk.Label(sidebar, text="LayF", bg="#101016", fg="white",
                 font=("Segoe UI Black", 28)).pack(anchor="w", padx=28, pady=(2, 0))
        tk.Label(sidebar, text="INFINYTY LAUNCHER", bg="#101016", fg="#a78bfa",
                 font=("Segoe UI Semibold", 9)).pack(anchor="w", padx=30)
        tk.Label(sidebar, text=f"@{self.user['login']}", bg="#101016", fg="#aaaab6",
                 font=("Segoe UI", 10)).pack(anchor="w", padx=30, pady=(22, 4))
        tk.Label(sidebar, text=self.user["nickname"], bg="#101016", fg="white",
                 font=("Segoe UI Semibold", 14)).pack(anchor="w", padx=30)
        navigation = tk.Frame(sidebar, bg="#101016")
        navigation.pack(fill="x", padx=20, pady=(24, 0))
        for label, page in [("▶  ИГРА", "game"), ("◇  МАГАЗИН", "shop"), ("▦  ФАЙЛЫ", "files")]:
            button = RoundedButton(
                navigation, label, lambda target=page: self.show_main(target),
                active=section == page)
            button.pack(fill="x", pady=4)
        if self.user.get("admin"):
            admin_button = RoundedButton(navigation, "⚙  АДМИН-ПАНЕЛЬ", self.open_admin)
            admin_button.pack(fill="x", pady=4)
        ttk.Button(sidebar, text="Выйти", style="Secondary.TButton", command=self.logout).pack(
            side="bottom", fill="x", padx=24, pady=25)

        # A Frame keeps the page geometry stable. Using an image Label as a
        # container made Tk recalculate its requested size while progress text
        # changed, which caused the game page to jump and become half-empty.
        content = tk.Frame(shell, bg="#08080c", borderwidth=0)
        content.pack(side="left", fill="both", expand=True, padx=38, pady=28)
        if self.launcher_background is not None:
            background = tk.Label(content, image=self.launcher_background, bg="#050507",
                                  borderwidth=0)
            background.place(x=0, y=0, relwidth=1, relheight=1)
        header = tk.Frame(content, bg="#08080c")
        header.pack(fill="x", pady=(0, 20))
        title_block = tk.Frame(header, bg="#08080c")
        title_block.pack(side="left")
        tk.Label(title_block, text="LAYF CONTROL CENTER", bg="#08080c", fg="#8b5cf6",
                 font=("Segoe UI Semibold", 9)).pack(anchor="w")
        page_title = {"game": "Готов к запуску", "shop": "Магазин доступа",
                      "files": "Файлы Minecraft"}.get(section, "LayF Infinyty")
        tk.Label(title_block, text=page_title, bg="#08080c", fg="white",
                 font=("Segoe UI Semibold", 25)).pack(anchor="w")
        tk.Label(title_block, text=f"Minecraft {GAME_VERSION}  •  Fabric  •  LayF Infinyty",
                 bg="#08080c", fg="#8d8d9a", font=("Segoe UI", 10)).pack(anchor="w", pady=(2, 0))
        system_badge = tk.Frame(header, bg="#15151c", highlightbackground="#373743",
                                highlightthickness=1)
        system_badge.pack(side="right", anchor="n", pady=4)
        tk.Label(system_badge, text="●", bg="#15151c", fg="#45dc9a",
                 font=("Segoe UI", 9)).pack(side="left", padx=(12, 5), pady=8)
        tk.Label(system_badge, text="SYSTEM ONLINE", bg="#15151c", fg="#c6c6cf",
                 font=("Segoe UI Semibold", 8)).pack(side="left", padx=(0, 12), pady=8)

        cards = tk.Frame(content, bg="#08080c")
        cards.pack(fill="x")
        profile = tk.Frame(cards, bg="#15151c", highlightbackground="#373743", highlightthickness=1)
        profile.pack(side="left", fill="both", expand=True, padx=(0, 8))
        license_card = tk.Frame(cards, bg="#15151c", highlightbackground="#373743", highlightthickness=1)
        license_card.pack(side="left", fill="both", expand=True, padx=(8, 0))

        tk.Label(profile, text="ПРОФИЛЬ", bg="#15151c", fg="#a78bfa",
                 font=("Segoe UI Semibold", 9)).pack(anchor="w", padx=18, pady=(16, 10))
        self.nick_entry = ttk.Entry(profile)
        self.nick_entry.insert(0, self.user["nickname"])
        self.nick_entry.pack(fill="x", padx=18)
        ttk.Button(profile, text="СМЕНИТЬ НИК MINECRAFT", style="Secondary.TButton",
                   command=self.save_nickname).pack(fill="x", padx=18, pady=(10, 17))
        self.nick_entry.bind("<Return>", lambda _event: self.save_nickname())

        tk.Label(license_card, text="ЛИЦЕНЗИЯ", bg="#15151c", fg="#a78bfa",
                 font=("Segoe UI Semibold", 9)).pack(anchor="w", padx=18, pady=(16, 10))
        self.key_entry = ttk.Entry(license_card)
        self.key_entry.insert(0, self.config_data.get("license_key", ""))
        self.key_entry.pack(fill="x", padx=18)
        self.license_status = tk.Label(license_card, text="Ключ будет проверен перед запуском",
                                       bg="#15151c", fg="#aaaab6", font=("Segoe UI", 9))
        self.license_status.pack(anchor="w", padx=18, pady=(10, 17))

        buy = tk.Frame(content, bg="#15151c", highlightbackground="#373743", highlightthickness=1)
        buy.pack(fill="x", pady=16)
        tk.Label(buy, text="КУПИТЬ ДОСТУП", bg="#15151c", fg="white",
                 font=("Segoe UI Semibold", 12)).pack(anchor="w", padx=18, pady=(14, 9))
        buttons = tk.Frame(buy, bg="#15151c")
        buttons.pack(fill="x", padx=14, pady=(0, 14))
        for column in range(4):
            buttons.grid_columnconfigure(column, weight=1, uniform="payments")
        for column, (text, currency, admin) in enumerate([
                ("₴ Гривны", "UAH", "HET_CTPAXA_x"),
                ("★ Telegram Stars", "STARS", "HET_CTPAXA_x"),
                ("₮ USDT", "USDT", "HET_CTPAXA_x"),
                ("₽ Рубли", "RUB", "KC_HOMEP")]):
            RoundedButton(buttons, text=text,
                          command=lambda c=currency, a=admin: self.open_prices(c, a),
                          height=52).grid(row=0, column=column, sticky="ew", padx=5)

        files = tk.Frame(content, bg="#15151c", highlightbackground="#373743", highlightthickness=1)
        files.pack(fill="x", pady=(0, 16))
        files_header = tk.Frame(files, bg="#15151c")
        files_header.pack(fill="x", padx=18, pady=(13, 8))
        tk.Label(files_header, text="ФАЙЛЫ И ДОПОЛНЕНИЯ", bg="#15151c", fg="white",
                 font=("Segoe UI Semibold", 11)).pack(side="left")
        tk.Label(files_header, text="Resource Packs • Mods • Config",
                 bg="#15151c", fg="#686876", font=("Segoe UI", 9)).pack(side="right")
        file_buttons = tk.Frame(files, bg="#15151c")
        file_buttons.pack(fill="x", padx=14, pady=(0, 14))
        for column in range(5):
            file_buttons.grid_columnconfigure(column, weight=1, uniform="files")
        for column, (text, folder) in enumerate([
                ("⌂ Папка игры", ""),
                ("▦ Resource Packs", "resourcepacks"),
                ("◆ Mods", "mods"),
                ("⚙ Config", ".options/configs"),
                ("▣ Готовые конфиги", "shared-configs")]):
            RoundedButton(file_buttons, text=text,
                          command=lambda f=folder: self.open_config_library() if f == "shared-configs"
                          else self.open_game_folder(f),
                          height=48).grid(row=0, column=column, sticky="ew", padx=4)

        settings = tk.Frame(content, bg="#08080c")
        settings.pack(fill="x")
        tk.Label(settings, text="Оперативная память:", bg="#08080c", fg="#b3b3c0").pack(side="left")
        self.ram_var = tk.IntVar(value=int(self.config_data.get("ram", 4096)))
        self.ram_label = tk.Label(settings, text=f"{self.ram_var.get() // 1024} GB",
                                  bg="#08080c", fg="white", width=5)
        self.ram_label.pack(side="right")
        MemorySlider(
            settings, self.ram_var,
            command=lambda value: self.ram_label.config(text=f"{int(value) // 1024} GB")
        ).pack(side="right", fill="x", expand=True, padx=15)

        self.progress = ModernProgress(content, maximum=100)
        self.progress.pack(fill="x", pady=(20, 6))
        self.status_label = tk.Label(content, text="Введите ключ и нажмите «Играть»",
                                     bg="#08080c", fg="#aaaab6", font=("Segoe UI", 9))
        self.status_label.pack(anchor="w")
        self.play_button = RoundedButton(content, text="ИГРАТЬ  →", command=self.start_game,
                                         active=True, height=58)
        self.play_button.pack(fill="x", pady=(14, 0))
        auto_mod_label = tk.Label(
            content, text="Мод LayF Infinyty устанавливается автоматически",
            bg="#08080c", fg="#76698d", font=("Segoe UI", 9))
        auto_mod_label.pack(anchor="w", pady=(8, 0))

        test_row = None
        if self.user.get("admin"):
            test_row = tk.Frame(content, bg="#08080c")
            test_row.pack(fill="x", pady=(8, 0))
            self.test_mod_var = tk.BooleanVar(value=bool(self.config_data.get("test_mod")))
            ttk.Checkbutton(
                test_row,
                text="Тестовая сборка мода (только для админов)",
                variable=self.test_mod_var,
                command=self.toggle_test_mod
            ).pack(side="left")
            test_state = tk.Label(test_row, text="", bg="#08080c", fg="#a78bfa",
                                  font=("Segoe UI", 9))
            test_state.pack(side="left", padx=(12, 0))
            if self.config_data.get("test_mod"):
                auto_mod_label.config(text="Тестовая сборка мода — загружается только у вас")
                test_state.config(text="включена")

        if section == "game":
            buy.pack_forget()
            files.pack_forget()
        elif section == "shop":
            cards.pack_forget()
            files.pack_forget()
            settings.pack_forget()
            self.progress.pack_forget()
            self.status_label.pack_forget()
            self.play_button.pack_forget()
            auto_mod_label.pack_forget()
            if test_row is not None:
                test_row.pack_forget()
            buy.pack(fill="x", pady=(8, 16))
        elif section == "files":
            cards.pack_forget()
            buy.pack_forget()
            settings.pack_forget()
            self.progress.pack_forget()
            self.status_label.pack_forget()
            self.play_button.pack_forget()
            auto_mod_label.pack_forget()
            if test_row is not None:
                test_row.pack_forget()
            files.pack(fill="x", pady=(8, 16))
        # Native canvas buttons are ready in the first frame; force geometry
        # once so page switches never show a black/empty intermediate state.
        self.update_idletasks()

    def animate_panel(self, panel):
        steps = [58, 54, 50, 47, 44, 42, 40, 39, 38]
        def frame(index=0):
            try:
                panel.pack_configure(padx=steps[index])
                if index + 1 < len(steps):
                    self.after(18, lambda: frame(index + 1))
            except tk.TclError:
                pass
        frame()

    def open_prices(self, currency, admin):
        prices = {
            "UAH": ("Гривны", "₴", ["30", "150", "200", "250", "600"]),
            "USDT": ("USDT", "₮", ["0.67", "3.34", "4.45", "5.56", "13.34"]),
            "STARS": ("Telegram Stars", "★", ["40", "196", "262", "327", "785"]),
            "RUB": ("Рубли", "₽", ["51", "256", "342", "427", "1025"]),
        }
        periods = ["1 день", "7 дней", "14 дней", "30 дней", "Навсегда"]
        title, symbol, values = prices[currency]
        window = tk.Toplevel(self)
        window.title(f"LayF Infinyty — цены в {title}")
        window.geometry("520x520")
        window.resizable(False, False)
        window.configure(bg="#08080c")
        window.transient(self)
        window.grab_set()

        tk.Label(window, text="ОФОРМЛЕНИЕ ЗАКАЗА", bg="#08080c", fg="#8b5cf6",
                 font=("Bahnschrift SemiBold", 10)).pack(anchor="w", padx=30, pady=(28, 3))
        tk.Label(window, text=f"Оплата: {title}", bg="#08080c", fg="white",
                 font=("Bahnschrift SemiBold", 24)).pack(anchor="w", padx=29)
        tk.Label(window, text="Выберите срок — сообщение для Telegram заполнится автоматически",
                 bg="#08080c", fg="#8d8d9a", font=("Bahnschrift", 10)).pack(
                     anchor="w", padx=30, pady=(3, 18))

        card = tk.Frame(window, bg="#121118", highlightbackground="#302b38", highlightthickness=1)
        card.pack(fill="x", padx=30)
        tk.Label(card, text="СРОК ДОСТУПА", bg="#121118", fg="#9f98aa",
                 font=("Bahnschrift SemiBold", 9)).pack(anchor="w", padx=20, pady=(18, 7))
        selected_period = ttk.Combobox(card, values=periods, state="readonly")
        selected_period.current(0)
        selected_period.pack(fill="x", padx=20)
        amount_label = tk.Label(card, text=f"{values[0]} {symbol}", bg="#121118", fg="#b895ff",
                                font=("Bahnschrift SemiBold", 28))
        amount_label.pack(anchor="w", padx=20, pady=(18, 18))

        def update_amount(_event=None):
            amount_label.config(text=f"{values[selected_period.current()]} {symbol}")

        selected_period.bind("<<ComboboxSelected>>", update_amount)

        def open_order():
            index = selected_period.current()
            message = (
                "Чит Infinity\n"
                f"Валюта: {title}\n"
                f"Сумма: {values[index]} {symbol}\n"
                f"Количество дней: {periods[index]}\n"
                f"Пользователь: @{self.user.get('login', '')}\n"
                f"Ник Minecraft: {self.user.get('nickname', '')}"
            )
            query = urllib.parse.urlencode({"text": message})
            webbrowser.open(f"https://t.me/{admin}?{query}")

        RoundedButton(window, text=f"ПЕРЕЙТИ К ОПЛАТЕ • @{admin}",
                      command=open_order,
                      active=True, height=54).pack(fill="x", padx=30, pady=(18, 8))
        tk.Label(window, text="В Telegram останется только проверить сообщение и нажать «Отправить».",
                 bg="#08080c", fg="#777181", font=("Bahnschrift", 9)).pack()

    def open_game_folder(self, folder=""):
        target = GAME_DIR / folder if folder else GAME_DIR
        target.mkdir(parents=True, exist_ok=True)
        try:
            os.startfile(str(target))
        except OSError as error:
            messagebox.showerror(APP_NAME, f"Не удалось открыть папку:\n{error}")

    def open_config_library(self):
        window = tk.Toplevel(self)
        window.title("LayF Infinyty — готовые конфиги")
        window.geometry("760x520")
        window.configure(bg="#08080c")
        window.transient(self)
        tk.Label(window, text="Готовые конфиги", bg="#08080c", fg="white",
                 font=("Segoe UI Semibold", 25)).pack(anchor="w", padx=30, pady=(28, 4))
        tk.Label(window, text="Конфиги от команды LayF. Установка выполняется в .options/configs",
                 bg="#08080c", fg="#9993a4").pack(anchor="w", padx=31, pady=(0, 18))
        filters = tk.Frame(window, bg="#08080c")
        filters.pack(fill="x", padx=30, pady=(0, 12))
        search_var = tk.StringVar()
        search_entry = ttk.Entry(filters, textvariable=search_var)
        search_entry.pack(side="left", fill="x", expand=True, padx=(0, 8))
        search_entry.insert(0, "")
        category_var = tk.StringVar(value="Все категории")
        category_box = ttk.Combobox(
            filters, textvariable=category_var,
            values=["Все категории", "Legit", "Rage", "HvH"], state="readonly", width=18)
        category_box.pack(side="right")
        config_list = tk.Listbox(window, bg="#121118", fg="#eeeaf5", selectbackground="#5b21b6",
                                 borderwidth=0, highlightthickness=1,
                                 highlightbackground="#302b38", font=("Segoe UI", 11))
        config_list.pack(fill="both", expand=True, padx=30)
        status = tk.Label(window, text="Загрузка списка…", bg="#08080c", fg="#9b94a5")
        status.pack(fill="x", padx=30, pady=8)
        rows = []
        filtered_rows = []

        def render_filtered(_event=None):
            query = search_var.get().strip().lower()
            category = category_var.get()
            filtered_rows.clear()
            filtered_rows.extend(item for item in rows if
                                 (not query or query in item["name"].lower()) and
                                 (category == "Все категории" or item.get("category", "Legit") == category))
            config_list.delete(0, "end")
            for item in filtered_rows:
                size = round(int(item["size"]) / 1024, 1)
                config_list.insert(
                    "end",
                    f"  [{item.get('category','Legit')}]  {item['name']}   •   {size} KB   •   {item.get('description','')}")
            status.config(text=f"Найдено конфигов: {len(filtered_rows)}", fg="#9b94a5")

        search_var.trace_add("write", lambda *_args: render_filtered())
        category_box.bind("<<ComboboxSelected>>", render_filtered)

        def install():
            selection = config_list.curselection()
            if not selection:
                messagebox.showwarning(APP_NAME, "Выберите конфиг", parent=window)
                return
            item = filtered_rows[selection[0]]
            install_button.state(["disabled"])

            def worker():
                try:
                    parts = []
                    for index in range(int(item["chunk_count"])):
                        result = api_request("/configs", {
                            "action": "chunk", "configId": item["id"], "index": index
                        })
                        parts.append(base64.b64decode(result["data"]))
                        self.after(0, lambda i=index: status.config(
                            text=f"Скачивание части {i + 1} из {item['chunk_count']}…"))
                    target_dir = GAME_DIR / ".options" / "configs"
                    target_dir.mkdir(parents=True, exist_ok=True)
                    target = target_dir / Path(item["file_name"]).name
                    target.write_bytes(b"".join(parts))
                    self.after(0, lambda: status.config(
                        text=f"✓ Установлено: {target.name}", fg="#52dca0"))
                    self.after(0, lambda: messagebox.showinfo(
                        APP_NAME, "Конфиг установлен и доступен в моде.", parent=window))
                except Exception as error:
                    self.after(0, lambda text=str(error): status.config(
                        text="Ошибка: " + text, fg="#ff6b82"))
                finally:
                    self.after(0, lambda: install_button.state(["!disabled"]))
            threading.Thread(target=worker, daemon=True).start()

        install_button = RoundedButton(window, text="УСТАНОВИТЬ В МОД",
                                       command=install, active=True, height=54)
        install_button.pack(fill="x", padx=30, pady=(4, 8))

        def delete_selected():
            selection = config_list.curselection()
            if not selection:
                messagebox.showwarning(APP_NAME, "Выберите конфиг", parent=window)
                return
            item = filtered_rows[selection[0]]
            if not messagebox.askyesno(APP_NAME, f"Удалить конфиг «{item['name']}»?", parent=window):
                return
            try:
                api_request("/configs", {
                    "action": "delete", "configId": item["id"]
                }, self.token)
                rows.remove(item)
                render_filtered()
                status.config(text="✓ Конфиг удалён", fg="#52dca0")
            except Exception as error:
                status.config(text="Ошибка: " + str(error), fg="#ff6b82")

        if self.user.get("admin"):
            RoundedButton(window, text="УДАЛИТЬ ВЫБРАННЫЙ КОНФИГ",
                          command=delete_selected, height=46).pack(
                              fill="x", padx=30, pady=(0, 18))
        else:
            tk.Frame(window, bg="#08080c", height=16).pack()

        def load():
            try:
                result = api_request("/configs", {"action": "list"})
                rows.extend(result.get("configs", []))
                self.after(0, render_filtered)
            except Exception as error:
                self.after(0, lambda text=str(error): status.config(
                    text="Ошибка: " + text, fg="#ff6b82"))
        threading.Thread(target=load, daemon=True).start()

    def save_nickname(self):
        nickname = self.nick_entry.get().strip()
        if not re.fullmatch(r"[A-Za-z0-9_]{3,16}", nickname):
            messagebox.showwarning(APP_NAME, "Ник должен содержать 3–16 символов: латиница, цифры или _")
            return
        try:
            result = api_request("/auth", {"action": "nickname", "nickname": nickname}, self.token)
            self.user = result["user"]
            messagebox.showinfo(APP_NAME, f"Ник изменён на {nickname}.\nОн применится при следующем запуске игры.")
            self.show_main()
        except Exception as error:
            messagebox.showerror(APP_NAME, str(error))

    def logout(self):
        try:
            api_request("/auth", {"action": "logout"}, self.token)
        except Exception:
            pass
        self.token = ""
        self.config_data.pop("token", None)
        self.save_config()
        self.show_auth()

    def choose_mod(self):
        path = filedialog.askopenfilename(title="Выберите LayF Infinyty JAR",
                                          filetypes=[("Java Archive", "*.jar")])
        if path:
            self.config_data["local_mod"] = path
            self.save_config()
            self.status_label.config(text="Выбран локальный мод: " + Path(path).name)

    def open_admin(self):
        window = tk.Toplevel(self)
        window.title("LayF Infinyty — Админ-панель")
        window.geometry("860x590")
        window.configure(bg="#08080c")
        window.transient(self)

        admin_actions = tk.Frame(window, bg="#08080c")
        admin_actions.pack(fill="x", padx=26, pady=(18, 0))
        RoundedButton(admin_actions, text="ОБНОВЛЕНИЕ МОДА",
                      command=self.open_mod_publisher, active=True, height=48).pack(
                          side="right", fill="x", expand=True, padx=(6, 0))
        RoundedButton(admin_actions, text="ЗАГРУЗИТЬ КОНФИГ",
                      command=self.open_config_publisher, height=48).pack(
                          side="right", fill="x", expand=True, padx=(0, 6))

        tk.Label(window, text="Управление лицензиями", bg="#08080c", fg="white",
                 font=("Segoe UI Semibold", 22)).pack(anchor="w", padx=26, pady=(22, 4))
        tk.Label(window, text="Создание, отзыв ключей и сброс привязки HWID",
                 bg="#08080c", fg="#aaaab6").pack(anchor="w", padx=27, pady=(0, 18))

        create_row = tk.Frame(window, bg="#15151c", highlightbackground="#373743", highlightthickness=1)
        create_row.pack(fill="x", padx=26, pady=(0, 12))
        tk.Label(create_row, text="Срок ключа", bg="#15151c", fg="#b3b3c0").pack(
            side="left", padx=(16, 8), pady=15)
        duration = ttk.Combobox(create_row, values=["1", "7", "14", "30", "forever"],
                                state="readonly", width=12)
        duration.set("30")
        duration.pack(side="left", padx=8)
        generated = ttk.Entry(create_row)
        generated.pack(side="left", fill="x", expand=True, padx=8)

        def create_key():
            try:
                result = api_request("/admin", {"action": "create", "days": duration.get()}, self.token)
                generated.delete(0, "end")
                generated.insert(0, result["key"])
                self.clipboard_clear()
                self.clipboard_append(result["key"])
                refresh()
                messagebox.showinfo(APP_NAME, "Ключ создан и скопирован")
            except Exception as error:
                messagebox.showerror(APP_NAME, str(error))

        ttk.Button(create_row, text="Создать", command=create_key).pack(side="right", padx=12, pady=10)

        manage = tk.Frame(window, bg="#15151c", highlightbackground="#373743", highlightthickness=1)
        manage.pack(fill="x", padx=26, pady=(0, 12))
        key_manage = ttk.Entry(manage)
        key_manage.pack(side="left", fill="x", expand=True, padx=14, pady=12)

        def change_key(action):
            key_value = key_manage.get().strip()
            if not key_value:
                messagebox.showwarning(APP_NAME, "Вставьте полный ключ")
                return
            try:
                result = api_request("/admin", {"action": action, "key": key_value}, self.token)
                if not result.get("changed"):
                    raise RuntimeError("Ключ не найден")
                refresh()
                messagebox.showinfo(APP_NAME, "Готово")
            except Exception as error:
                messagebox.showerror(APP_NAME, str(error))

        ttk.Button(manage, text="Сбросить HWID", style="Secondary.TButton",
                   command=lambda: change_key("reset")).pack(side="right", padx=4, pady=9)
        ttk.Button(manage, text="Отозвать", command=lambda: change_key("revoke")).pack(
            side="right", padx=4, pady=9)

        list_frame = tk.Frame(window, bg="#15151c")
        list_frame.pack(fill="both", expand=True, padx=26, pady=(0, 22))
        license_list = tk.Listbox(list_frame, bg="#15151c", fg="#e5e5ec",
                                  selectbackground="#7c3aed", borderwidth=0,
                                  highlightthickness=1, highlightbackground="#373743",
                                  font=("Consolas", 10))
        license_list.pack(fill="both", expand=True)
        license_rows = []

        def selected_key():
            selection = license_list.curselection()
            if not selection or selection[0] >= len(license_rows):
                return ""
            return license_rows[selection[0]].get("key") or ""

        def select_license(_event=None):
            key_value = selected_key()
            key_manage.delete(0, "end")
            if key_value:
                key_manage.insert(0, key_value)

        def copy_selected(_event=None):
            key_value = selected_key()
            if not key_value:
                messagebox.showwarning(
                    APP_NAME,
                    "Для старого ключа сохранён только хэш — полный ключ восстановить нельзя.\n"
                    "Создайте новый ключ после обновления Vercel.")
                return
            self.clipboard_clear()
            self.clipboard_append(key_value)
            messagebox.showinfo(APP_NAME, "Полный ключ скопирован")

        license_list.bind("<<ListboxSelect>>", select_license)
        license_list.bind("<Double-Button-1>", copy_selected)
        ttk.Button(list_frame, text="КОПИРОВАТЬ ВЫБРАННЫЙ ПОЛНЫЙ КЛЮЧ",
                   command=copy_selected).pack(fill="x", pady=(10, 0))

        def refresh():
            def worker():
                try:
                    result = api_request("/admin", {"action": "list"}, self.token)
                    def render():
                        license_rows.clear()
                        license_rows.extend(result.get("licenses", []))
                        license_list.delete(0, "end")
                        for item in license_rows:
                            term = "навсегда" if item["durationDays"] is None else f"{item['durationDays']} дн."
                            state = "ОТОЗВАН" if item["revoked"] else (
                                "активен" if item["activatedAt"] else "не активирован")
                            hwid = "HWID" if item["hwidBound"] else "свободен"
                            shown_key = item.get("key") or f"СТАРЫЙ …{item['hint']}"
                            license_list.insert(
                                "end", f"{shown_key:38} | {term:9} | {state:15} | {hwid}")
                    self.after(0, render)
                except Exception as error:
                    self.after(0, lambda: messagebox.showerror(APP_NAME, str(error)))
            threading.Thread(target=worker, daemon=True).start()
        refresh()

    def open_config_publisher(self):
        window = tk.Toplevel(self)
        window.title("LayF Infinyty — публикация конфига")
        window.geometry("700x570")
        window.configure(bg="#08080c")
        window.transient(self)
        tk.Label(window, text="Публикация конфига", bg="#08080c", fg="white",
                 font=("Segoe UI Semibold", 24)).pack(anchor="w", padx=30, pady=(28, 6))
        card = tk.Frame(window, bg="#121118", highlightbackground="#302b38", highlightthickness=1)
        card.pack(fill="both", expand=True, padx=30, pady=(10, 28))
        tk.Label(card, text="Название", bg="#121118", fg="#bbb5c5").pack(
            anchor="w", padx=22, pady=(20, 5))
        name_entry = ttk.Entry(card)
        name_entry.pack(fill="x", padx=22)
        tk.Label(card, text="Описание", bg="#121118", fg="#bbb5c5").pack(
            anchor="w", padx=22, pady=(13, 5))
        description_entry = ttk.Entry(card)
        description_entry.pack(fill="x", padx=22)
        tk.Label(card, text="Категория", bg="#121118", fg="#bbb5c5").pack(
            anchor="w", padx=22, pady=(13, 5))
        category_entry = ttk.Combobox(card, values=["Legit", "Rage", "HvH"], state="readonly")
        category_entry.set("Legit")
        category_entry.pack(fill="x", padx=22)
        selected = tk.StringVar()
        file_label = tk.Label(card, text="Файл не выбран", bg="#121118", fg="#8d8796")
        file_label.pack(anchor="w", padx=22, pady=(16, 7))

        def choose():
            path = filedialog.askopenfilename(
                parent=window, title="Выберите конфиг",
                filetypes=[("LayF configs", "*.json *.cfg *.txt"), ("Все файлы", "*.*")])
            if path:
                selected.set(path)
                file_label.config(text=Path(path).name, fg="#ded8e8")
                if not name_entry.get().strip():
                    name_entry.insert(0, Path(path).stem)

        RoundedButton(card, text="ВЫБРАТЬ ФАЙЛ КОНФИГА", command=choose, height=48).pack(
            fill="x", padx=22)
        status = tk.Label(card, text="", bg="#121118", fg="#a9a3b4")
        status.pack(fill="x", padx=22, pady=(12, 4))

        def publish():
            path = selected.get()
            if not path or not Path(path).is_file() or not name_entry.get().strip():
                messagebox.showwarning(APP_NAME, "Укажите название и выберите файл", parent=window)
                return
            publish_button.state(["disabled"])

            def worker():
                try:
                    data = Path(path).read_bytes()
                    chunk_size = 750000
                    count = math.ceil(len(data) / chunk_size)
                    config_id = f"cfg-{int(time.time()*1000)}-{hashlib.sha256(data).hexdigest()[:10]}"
                    api_request("/configs", {
                        "action": "begin", "id": config_id, "name": name_entry.get().strip(),
                        "fileName": Path(path).name, "description": description_entry.get().strip(),
                        "category": category_entry.get(), "size": len(data), "chunkCount": count
                    }, self.token)
                    for index in range(count):
                        chunk = data[index*chunk_size:(index+1)*chunk_size]
                        api_request("/configs", {
                            "action": "upload", "configId": config_id, "index": index,
                            "data": base64.b64encode(chunk).decode("ascii")
                        }, self.token)
                        self.after(0, lambda i=index: status.config(
                            text=f"Загрузка {i+1}/{count}…"))
                    api_request("/configs", {
                        "action": "publish", "configId": config_id
                    }, self.token)
                    self.after(0, lambda: status.config(
                        text="✓ Конфиг опубликован", fg="#52dca0"))
                except Exception as error:
                    self.after(0, lambda text=str(error): status.config(
                        text="Ошибка: " + text, fg="#ff6b82"))
                finally:
                    self.after(0, lambda: publish_button.state(["!disabled"]))
            threading.Thread(target=worker, daemon=True).start()

        publish_button = RoundedButton(card, text="ОПУБЛИКОВАТЬ ДЛЯ ВСЕХ",
                                       command=publish, active=True, height=54)
        publish_button.pack(fill="x", padx=22, pady=(8, 20))

    def open_mod_publisher(self):
        window = tk.Toplevel(self)
        window.title("LayF Infinyty — публикация обновления")
        window.geometry("720x470")
        window.configure(bg="#08080c")
        window.transient(self)

        tk.Label(window, text="Обновление мода", bg="#08080c", fg="white",
                 font=("Segoe UI Semibold", 24)).pack(anchor="w", padx=30, pady=(28, 5))
        tk.Label(window, text="Выберите JAR и канал публикации. Тестовый канал видят только админы.",
                 bg="#08080c", fg="#9792a3").pack(anchor="w", padx=31, pady=(0, 22))

        card = tk.Frame(window, bg="#121118", highlightbackground="#302b38", highlightthickness=1)
        card.pack(fill="both", expand=True, padx=30, pady=(0, 28))

        tk.Label(card, text="Номер версии", bg="#121118", fg="#bbb5c5").pack(
            anchor="w", padx=22, pady=(20, 5))
        version_entry = ttk.Entry(card)
        version_entry.pack(fill="x", padx=22)

        tk.Label(card, text="Канал публикации", bg="#121118", fg="#bbb5c5").pack(
            anchor="w", padx=22, pady=(13, 5))
        channel_combo = ttk.Combobox(card, state="readonly", values=[
            "Стабильный (для всех)", "Тестовый (только для админов)"])
        channel_combo.set("Стабильный (для всех)")
        channel_combo.pack(fill="x", padx=22)

        selected_path = tk.StringVar(value="")
        file_label = tk.Label(card, text="JAR ещё не выбран", bg="#121118", fg="#8d8796")
        file_label.pack(anchor="w", padx=22, pady=(18, 8))

        def choose_file():
            path = filedialog.askopenfilename(parent=window, title="Выберите JAR мода",
                                              filetypes=[("Java Archive", "*.jar")])
            if path:
                selected_path.set(path)
                file_label.config(text=Path(path).name, fg="#d7d2df")
                if not version_entry.get().strip():
                    match = re.search(r"(\d+\.\d+(?:\.\d+)?)", Path(path).name)
                    version_entry.insert(0, match.group(1) if match else f"1.0.{int(time.time()) % 10000}")

        ttk.Button(card, text="ВЫБРАТЬ JAR", style="Secondary.TButton",
                   command=choose_file).pack(fill="x", padx=22)
        publish_progress = ttk.Progressbar(card, maximum=100)
        publish_progress.pack(fill="x", padx=22, pady=(20, 8))
        publish_status = tk.Label(card, text="", bg="#121118", fg="#a9a3b4", wraplength=620)
        publish_status.pack(fill="x", padx=22)

        def publish():
            path = selected_path.get()
            version = version_entry.get().strip()
            if not path or not Path(path).is_file():
                messagebox.showwarning(APP_NAME, "Выберите JAR-файл", parent=window)
                return
            if not version:
                messagebox.showwarning(APP_NAME, "Укажите номер версии", parent=window)
                return
            publish_button.state(["disabled"])

            def set_state(text, value=None, color="#a9a3b4"):
                def update():
                    publish_status.config(text=text, fg=color)
                    if value is not None:
                        publish_progress["value"] = value
                self.after(0, update)

            def worker():
                try:
                    data = Path(path).read_bytes()
                    digest = hashlib.sha256(data).hexdigest()
                    chunk_size = 750000
                    chunk_count = math.ceil(len(data) / chunk_size)
                    channel = "test" if "Тестовый" in channel_combo.get() else "stable"
                    channel_label = "тестовая сборка" if channel == "test" else "стабильная версия"
                    release_id = f"{int(time.time() * 1000)}-{digest[:12]}"
                    set_state("Подготовка обновления…", 1)
                    api_request("/mod-update", {
                        "action": "begin", "id": release_id, "version": version,
                        "sha256": digest, "size": len(data), "chunkCount": chunk_count,
                        "channel": channel
                    }, self.token)
                    for index in range(chunk_count):
                        chunk = data[index * chunk_size:(index + 1) * chunk_size]
                        api_request("/mod-update", {
                            "action": "upload", "releaseId": release_id, "index": index,
                            "data": base64.b64encode(chunk).decode("ascii")
                        }, self.token)
                        set_state(f"Загрузка части {index + 1} из {chunk_count}…",
                                  round((index + 1) / chunk_count * 95))
                    api_request("/mod-update", {
                        "action": "publish", "releaseId": release_id, "channel": channel
                    }, self.token)
                    set_state(f"✓ {channel_label} {version} опубликована", 100, "#52dca0")
                    self.after(0, lambda: messagebox.showinfo(
                        APP_NAME,
                        ("Опубликована тестовая сборка. Она загрузится только у админов "
                         "с включённой тестовой сборкой в лаунчере.")
                        if channel == "test"
                        else "Обновление опубликовано. Лаунчеры получат его автоматически.",
                        parent=window))
                except Exception as error:
                    set_state("Ошибка: " + str(error), 0, "#ff6b82")
                finally:
                    self.after(0, lambda: publish_button.state(["!disabled"]))

            threading.Thread(target=worker, daemon=True).start()

        publish_button = ttk.Button(card, text="ЗАГРУЗИТЬ И ОПУБЛИКОВАТЬ", command=publish)
        publish_button.pack(fill="x", padx=22, pady=(12, 20))

    def set_progress(self, status=None, value=None, maximum=None):
        def update():
            if status is not None:
                shown_value = self.progress.value if value is None else value
                shown_maximum = self.progress.maximum if maximum is None else maximum
                percent = int(max(0, min(100, shown_value / max(1, shown_maximum) * 100)))
                self.status_label.config(text=f"{status}   •   {percent}%")
                if self.busy and hasattr(self, "play_button"):
                    self.play_button.label = f"ПОДГОТОВКА...  {percent}%"
                    self.play_button.draw("#4b267e")
            if maximum is not None:
                self.progress.configure(maximum=max(1, maximum))
            if value is not None:
                self.progress["value"] = value
        self.after(0, update)

    def start_game(self):
        if self.busy:
            return
        key = self.key_entry.get().strip().upper()
        if not key:
            self.license_status.config(text="Введите лицензионный ключ", fg="#ff667a")
            return
        self.busy = True
        self.play_button.state(["disabled"])
        self.play_button.label = "ПОДГОТОВКА...  0%"
        self.play_button.draw("#4b267e")
        self.config_data["ram"] = int(self.ram_var.get())
        self.config_data["license_key"] = key
        self.save_config()
        threading.Thread(target=self.install_and_launch, args=(key,), daemon=True).start()

    def install_and_launch(self, key):
        try:
            self.set_progress("Проверяем лицензию...", 2, 100)
            license_result = api_request("/validate", {"key": key, "hwid": launcher_hwid(), "clientVersion": APP_VERSION})
            self.download_grant = str(license_result.get("downloadToken") or "")
            if not license_result.get("valid"):
                raise RuntimeError(license_result.get("message", "Ключ недействителен"))
            self.after(0, lambda: self.license_status.config(text="Лицензия активна", fg="#45dc9a"))

            java = self.ensure_java()
            install_state = {"value": 0, "maximum": 1, "status": "Подготовка файлов Minecraft..."}

            def install_status(text):
                install_state["status"] = str(text)

            def install_maximum(value):
                install_state["maximum"] = max(1, int(value))

            def install_progress(value):
                install_state["value"] = int(value)
                ratio = install_state["value"] / install_state["maximum"]
                mapped = 15 + int(max(0, min(1, ratio)) * 40)
                self.set_progress(install_state["status"], mapped, 100)

            callback = {
                "setStatus": install_status,
                "setProgress": install_progress,
                "setMax": install_maximum
            }
            self.set_progress("Устанавливаем Minecraft...", 5, 100)
            minecraft_launcher_lib.install.install_minecraft_version(GAME_VERSION, str(GAME_DIR), callback)
            loader = FABRIC_LOADER_VERSION
            fabric_version = f"fabric-loader-{loader}-{GAME_VERSION}"
            fabric_profile = GAME_DIR / "versions" / fabric_version / f"{fabric_version}.json"
            if fabric_profile.exists():
                self.set_progress(f"Fabric Loader {loader} уже установлен", 35, 100)
            else:
                self.set_progress(f"Устанавливаем Fabric Loader {loader}...", 35, 100)
                try:
                    minecraft_launcher_lib.fabric.install_fabric(
                        GAME_VERSION, str(GAME_DIR), loader_version=loader,
                        callback=callback, java=str(java))
                except Exception as install_error:
                    if not fabric_profile.exists():
                        raise RuntimeError(
                            "Не удалось установить Fabric. Закройте Minecraft и повторите запуск."
                        ) from install_error
            self.install_patched_fabric_loader()

            mods = GAME_DIR / "mods"
            mods.mkdir(parents=True, exist_ok=True)
            self.install_fabric_api(mods)
            self.install_mod(mods)
            self.stabilize_libraries()
            options_dir = GAME_DIR / ".options"
            options_dir.mkdir(parents=True, exist_ok=True)
            (options_dir / "license-key.txt").write_text(key + "\n", encoding="utf-8")
            (options_dir / "license-server.txt").write_text(API_BASE + "\n", encoding="utf-8")

            nickname = self.user["nickname"]
            version_id = f"fabric-loader-{loader}-{GAME_VERSION}"
            options = {
                "username": nickname,
                "uuid": offline_uuid(nickname),
                "token": "0",
                "executablePath": str(java),
                "defaultExecutablePath": str(java),
                "jvmArguments": [f"-Xmx{int(self.ram_var.get())}M", "-Xms1024M"],
                "launcherName": "LayF Infinyty Launcher",
                "launcherVersion": "1.0",
                "gameDirectory": str(GAME_DIR)
            }
            command = minecraft_launcher_lib.command.get_minecraft_command(version_id, str(GAME_DIR), options)
            self.set_progress("Запускаем Minecraft...", 100, 100)
            game_process = subprocess.Popen(
                command, cwd=str(GAME_DIR), creationflags=subprocess.CREATE_NO_WINDOW)
            self.after(0, self.withdraw)
            game_process.wait()
            self.after(0, self.deiconify)
            self.after(100, self.lift)
            self.set_progress("Minecraft закрыт — лаунчер снова активен", 0, 100)
        except Exception as error:
            error_text = str(error).strip()
            if not error_text or error_text == "None":
                error_text = f"{type(error).__name__}: установка не завершена"
            self.set_progress("Ошибка: " + error_text, 0, 100)
            self.after(0, lambda text=error_text: messagebox.showerror(APP_NAME, text))
        finally:
            self.busy = False
            def restore_button():
                self.play_button.label = "ИГРАТЬ  →"
                self.play_button.state(["!disabled"])
            self.after(0, restore_button)

    def stabilize_libraries(self):
        self.set_progress("Проверяем библиотеки Fabric...", 90, 100)
        libraries = GAME_DIR / "libraries"
        failed = []
        for jar in libraries.rglob("*.jar"):
            accessible = False
            for attempt in range(5):
                try:
                    os.chmod(jar, 0o666)
                    jar.resolve(strict=True)
                    with jar.open("rb") as source:
                        if source.read(2) != b"PK":
                            raise OSError("повреждённый JAR")
                    accessible = True
                    break
                except (OSError, PermissionError):
                    time.sleep(0.5 + attempt * 0.4)
            if not accessible:
                failed.append(jar.name)
        if failed:
            raise RuntimeError(
                "Windows заблокировала библиотеки Fabric: "
                + ", ".join(failed[:3])
                + ". Закройте запущенный Minecraft и попробуйте снова.")
        time.sleep(1.5)

    def install_patched_fabric_loader(self):
        patched = bundled_file("fabric-loader-0.16.14-layf-patched.jar")
        target = (GAME_DIR / "libraries" / "net" / "fabricmc" / "fabric-loader"
                  / FABRIC_LOADER_VERSION / f"fabric-loader-{FABRIC_LOADER_VERSION}.jar")
        if not patched.exists() or patched.stat().st_size < 500_000:
            return
            raise RuntimeError("В лаунчере отсутствует исправленный Fabric Loader")
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(patched, target)

    def ensure_java(self):
        java = APP_DIR / "runtime" / "bin" / "javaw.exe"
        if java.exists():
            return java
        runtime_root = APP_DIR / "runtime"
        runtime_root.mkdir(parents=True, exist_ok=True)
        archive = APP_DIR / "temurin21.zip"
        self.set_progress("Скачиваем Java 21...", 10, 100)
        java_request = urllib.request.Request(
            "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse",
            headers={"User-Agent": "LayF-Infinyty-Launcher/1.1"})
        with urllib.request.urlopen(java_request, timeout=120) as response, archive.open("wb") as output:
            shutil.copyfileobj(response, output)
        with zipfile.ZipFile(archive) as source:
            members = source.namelist()
            root = members[0].split("/")[0]
            temp_root = runtime_root.parent / "runtime-temp"
            shutil.rmtree(temp_root, ignore_errors=True)
            source.extractall(temp_root)
        extracted = temp_root / root
        if runtime_root.exists():
            shutil.rmtree(runtime_root)
        extracted.rename(runtime_root)
        shutil.rmtree(temp_root, ignore_errors=True)
        archive.unlink(missing_ok=True)
        return java

    def install_fabric_api(self, mods):
        self.set_progress("Устанавливаем Fabric API 0.119.4...", 65, 100)
        exact_name = "fabric-api-0.119.4+1.21.4.jar"
        target = mods / exact_name
        bundled = bundled_file(exact_name)
        for old_api in mods.glob("fabric-api*.jar"):
            if old_api.name != exact_name:
                try:
                    old_api.unlink()
                except OSError:
                    pass
        if bundled.exists() and bundled.stat().st_size > 100_000:
            shutil.copy2(bundled, target)
            return
        query = urllib.parse.urlencode({
            "game_versions": json.dumps([GAME_VERSION]),
            "loaders": json.dumps(["fabric"])
        })
        request = urllib.request.Request(
            f"https://api.modrinth.com/v2/project/fabric-api/version?{query}",
            headers={"User-Agent": "LayF-Infinyty-Launcher/1.1 (launcher support)"})
        with urllib.request.urlopen(request, timeout=30) as response:
            versions = json.load(response)
        if not versions:
            raise RuntimeError("Fabric API для этой версии не найден")
        matching = next(
            (version for version in versions if version.get("version_number") == "0.119.4+1.21.4"),
            versions[0])
        file_info = next(item for item in matching["files"] if item.get("primary"))
        file_request = urllib.request.Request(
            file_info["url"], headers={"User-Agent": "LayF-Infinyty-Launcher/1.1"})
        with urllib.request.urlopen(file_request, timeout=60) as response, target.open("wb") as output:
            shutil.copyfileobj(response, output)
        if target.stat().st_size < 100_000:
            raise RuntimeError("Скачанный Fabric API повреждён")

    def install_mod(self, mods):
        is_admin = bool((self.user or {}).get("admin"))
        channel = "test" if (self.config_data.get("test_mod") and is_admin) else "stable"
        label = "тестовую сборку мода" if channel == "test" else "LayF Infinyty"
        self.set_progress(f"Проверяем обновление {label}...", 80, 100)
        target = mods / "layf-infinyty1.21.4.jar"
        temporary = mods / "layf-infinyty.download"
        bundled = bundled_file("layf-infinyty.jar")
        metadata_file = mods / ".layf-mod-version.json"
        for old_mod in mods.glob("layf-infinyty*.jar"):
            if old_mod.name != target.name:
                try:
                    old_mod.unlink()
                except OSError:
                    pass

        def valid_jar(path):
            try:
                if not path.exists() or path.stat().st_size < 100_000:
                    return False
                with zipfile.ZipFile(path) as archive:
                    names = set(archive.namelist())
                    return "fabric.mod.json" in names
            except (OSError, zipfile.BadZipFile):
                return False

        try:
            token = getattr(self, "download_grant", "")
            # Тестовый канал доступен только админам: сервер проверяет JWT.
            auth = self.token if channel == "test" else ""
            manifest = api_request("/mod-update", {
                "action": "manifest", "downloadToken": token, "channel": channel
            }, auth).get("release")
            if channel == "test" and not manifest:
                raise RuntimeError("Тестовая сборка ещё не опубликована")
            if manifest:
                current = {}
                try:
                    current = json.loads(metadata_file.read_text(encoding="utf-8"))
                except Exception:
                    pass
                if (current.get("sha256") != manifest["sha256"]
                        or current.get("channel") != channel
                        or not valid_jar(target)):
                    temporary.unlink(missing_ok=True)
                    chunk_count = int(manifest.get("chunk_count", manifest.get("chunkCount", 0)))
                    with temporary.open("wb") as output:
                        for index in range(chunk_count):
                            part = api_request("/mod-update", {
                                "action": "chunk", "releaseId": manifest["id"], "index": index,
                                "downloadToken": token, "channel": channel
                            }, auth)
                            output.write(base64.b64decode(part["data"]))
                            self.set_progress(
                                f"Обновляем {label} {manifest['version']}...",
                                80 + int((index + 1) / chunk_count * 8), 100)
                    digest = hashlib.sha256(temporary.read_bytes()).hexdigest()
                    if digest != manifest["sha256"] or not valid_jar(temporary):
                        raise RuntimeError("Проверка обновления мода не пройдена")
                    temporary.replace(target)
                    metadata_file.write_text(json.dumps({
                        "version": manifest["version"], "sha256": digest, "channel": channel
                    }), encoding="utf-8")
                return
        except Exception as error:
            temporary.unlink(missing_ok=True)
            if channel == "test":
                raise RuntimeError(
                    "Не удалось загрузить тестовую сборку мода: " + str(error)
                ) from error

        # Стабильный канал: резервная загрузка с публичного URL или встроенной копии.
        try:
            temporary.unlink(missing_ok=True)
            request = urllib.request.Request(MOD_URL, headers={"User-Agent": "LayF-Launcher/1.1"})
            with urllib.request.urlopen(request, timeout=60) as response, temporary.open("wb") as output:
                shutil.copyfileobj(response, output)
            if not valid_jar(temporary):
                raise RuntimeError("сервер вернул повреждённый JAR")
            temporary.replace(target)
            metadata_file.write_text(json.dumps({
                "version": "public", "sha256": "public", "channel": "stable"
            }), encoding="utf-8")
        except Exception as download_error:
            temporary.unlink(missing_ok=True)
            if valid_jar(bundled):
                shutil.copy2(bundled, target)
                self.set_progress("Используем встроенную копию мода...", 85, 100)
            elif valid_jar(target):
                self.set_progress("Используем ранее скачанный мод...", 85, 100)
            else:
                raise RuntimeError(f"Не удалось автоматически скачать мод: {download_error}") from download_error


def version_tuple(value):
    try:
        parts = tuple(int(part) for part in str(value).split("."))
        return parts if len(parts) == 3 else (0, 0, 0)
    except ValueError:
        return (0, 0, 0)


def enforce_launcher_update():
    policy = api_request("/launcher-update", {"action": "manifest"})
    release = policy.get("release") or {}
    remote = str(policy.get("minimumVersion") or release.get("version") or "0.0.0")
    if version_tuple(remote) <= version_tuple(APP_VERSION):
        return
    if not policy.get("required"):
        return
    count = int(release.get("chunk_count") or release.get("chunkCount") or 0)
    if not release.get("id") or count < 1:
        raise RuntimeError("Сервер не передал файлы обязательного обновления")
    target = Path(sys.executable).resolve()
    downloaded = target.with_name("Infinyty-Launcher-update.exe")
    digest = hashlib.sha256()
    with downloaded.open("wb") as output:
        for index in range(count):
            part = api_request("/launcher-update", {
                "action": "chunk", "releaseId": release["id"], "index": index
            })
            chunk = base64.b64decode(part["data"])
            output.write(chunk)
            digest.update(chunk)
    if digest.hexdigest().lower() != str(release.get("sha256", "")).lower():
        downloaded.unlink(missing_ok=True)
        raise RuntimeError("Проверка обязательного обновления не пройдена")
    updater = target.with_name("Infinyty-update.cmd")
    updater.write_text(
        "@echo off\r\n"
        "set PYINSTALLER_RESET_ENVIRONMENT=1\r\n"
        "ping 127.0.0.1 -n 3 >nul\r\n"
        f'move /Y "{downloaded}" "{target}" >nul\r\n'
        f'start "" "{target}"\r\n'
        'del "%~f0"\r\n', encoding="utf-8")
    os.environ["PYINSTALLER_RESET_ENVIRONMENT"] = "1"
    subprocess.Popen(["cmd.exe", "/c", str(updater)], creationflags=subprocess.CREATE_NO_WINDOW)
    raise SystemExit(0)


if __name__ == "__main__":
    try:
        enforce_launcher_update()
    except SystemExit:
        raise
    except Exception as error:
        messagebox.showerror(APP_NAME, "Не удалось установить обязательное обновление:\n" + str(error))
        raise SystemExit(1)
    LayFLauncher().mainloop()
