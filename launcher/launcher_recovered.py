import marshal
import sys
from pathlib import Path

# Explicit imports are required because the recovered launcher module is loaded
# from a marshalled code object and PyInstaller cannot inspect it statically.
import base64, hashlib, json, math, os, platform, re, shutil, socket, subprocess
import threading, time, urllib.error, urllib.parse, urllib.request, uuid, webbrowser, zipfile
import tkinter
from tkinter import filedialog, messagebox, ttk
import minecraft_launcher_lib


def resource(name):
    return Path(getattr(sys, "_MEIPASS", Path(__file__).resolve().parent)) / name


namespace = {
    "__name__": "embedded_infinyty_launcher",
    "__file__": str(Path(__file__).resolve()),
}
with resource("launcher.bin").open("rb") as source:
    exec(marshal.loads(source.read()), namespace)

Launcher = namespace["LayFLauncher"]
original_api_request = namespace["api_request"]
download_grant = ""
active_launcher = None
download_totals = {"/launcher-update": 0, "/mod-update": 0}


def show_download_progress(path, completed, total):
    launcher = active_launcher
    if launcher is None or total < 1:
        return
    percent = min(100, max(0, round(completed * 100 / total)))
    kind = "лаунчера" if path == "/launcher-update" else "мода"
    message = f"Загрузка обновления {kind}: {percent}%"

    def render():
        try:
            launcher.title(f"Infinyty Launcher — {message}")
            launcher.set_progress(status=message, value=completed, maximum=total)
            status_var = getattr(launcher, "status_var", None)
            if status_var is not None and hasattr(status_var, "set"):
                status_var.set(message)
            status_label = getattr(launcher, "status_label", None)
            if status_label is not None and hasattr(status_label, "configure"):
                status_label.configure(text=message)
        except Exception:
            pass

    try:
        launcher.after(0, render)
    except Exception:
        pass


def api_request(path, payload, token=""):
    """Attach the short-lived mod grant obtained from a successful HWID check."""
    global download_grant
    request_payload = dict(payload or {})
    if path == "/validate":
        request_payload["clientVersion"] = CURRENT_VERSION
    if path == "/mod-update":
        request_payload["downloadToken"] = download_grant
    last_error = None
    for attempt in range(3):
        try:
            result = original_api_request(path, request_payload, token)
            break
        except RuntimeError as error:
            last_error = error
            if "недоступен" not in str(error).lower() or attempt == 2:
                raise
            time.sleep(0.6 * (attempt + 1))
    else:
        raise last_error or RuntimeError("Не удалось связаться с сервисом Infinyty")
    if path in download_totals:
        if request_payload.get("action") == "manifest":
            release = result.get("release") or {}
            download_totals[path] = int(release.get("chunk_count") or release.get("chunkCount") or 0)
        elif request_payload.get("action") == "chunk":
            show_download_progress(path, int(request_payload.get("index") or 0) + 1, download_totals[path])
    if path == "/validate" and result.get("valid"):
        download_grant = str(result.get("downloadToken") or "")
    return result


namespace["api_request"] = api_request
original_show_main = Launcher.show_main
original_install_and_launch = Launcher.install_and_launch
original_logout = Launcher.logout
original_check_self_update = Launcher.check_self_update
CURRENT_VERSION = "1.6.9"


def version_tuple(value):
    """Accept only normal release versions such as 1.6.5."""
    value = str(value or "").strip()
    if not re.fullmatch(r"\d+\.\d+\.\d+", value):
        return None
    return tuple(int(part) for part in value.split("."))


def check_self_update_safe(self):
    """Ignore malformed manifests and never reinstall the current version."""
    try:
        policy = api_request("/launcher-update", {"action": "manifest"})
        remote = str(policy.get("minimumVersion") or policy.get("release", {}).get("version") or "")
        remote_version = version_tuple(remote)
        current_version = version_tuple(CURRENT_VERSION)
        if remote_version is None or current_version is None or remote_version <= current_version:
            return False
    except Exception:
        return False
    global active_launcher
    active_launcher = self
    # A restarted one-file PyInstaller build must not inherit the archive
    # identity of the old launcher process. Without this flag some systems
    # fail before Python starts with "failed to obtain executable path for
    # parent process".
    os.environ["PYINSTALLER_RESET_ENVIRONMENT"] = "1"
    return original_check_self_update(self)


Launcher.check_self_update = check_self_update_safe


def apply_performance_options():
    """Apply low-latency defaults without changing controls, accounts or servers."""
    options_path = Path(namespace["GAME_DIR"]) / "options.txt"
    desired = {
        "graphicsMode": "0",
        "renderDistance": "10",
        "simulationDistance": "6",
        "entityDistanceScaling": "0.75",
        "biomeBlendRadius": "0",
        "mipmapLevels": "2",
        "particles": "1",
        "entityShadows": "false",
        "cloudStatus": "false",
        "ao": "1",
    }
    try:
        options_path.parent.mkdir(parents=True, exist_ok=True)
        lines = options_path.read_text(encoding="utf-8").splitlines() if options_path.exists() else []
        found = set()
        result = []
        for line in lines:
            key = line.split(":", 1)[0]
            if key in desired:
                result.append(f"{key}:{desired[key]}")
                found.add(key)
            else:
                result.append(line)
        result.extend(f"{key}:{value}" for key, value in desired.items() if key not in found)
        options_path.write_text("\n".join(result) + "\n", encoding="utf-8")
    except OSError:
        pass

# Stable Java 21 defaults for long PvP sessions. The selected RAM limit from
# the launcher stays authoritative; these flags only reduce GC stalls.
original_minecraft_command = minecraft_launcher_lib.command.get_minecraft_command
def optimized_minecraft_command(version, directory, options):
    tuned = dict(options or {})
    args = list(tuned.get("jvmArguments") or [])
    args.extend([
        "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=50",
        "-XX:+ParallelRefProcEnabled", "-XX:+DisableExplicitGC",
        "-Dfile.encoding=UTF-8"
    ])
    tuned["jvmArguments"] = list(dict.fromkeys(args))
    return original_minecraft_command(version, directory, tuned)
minecraft_launcher_lib.command.get_minecraft_command = optimized_minecraft_command


def show_main_with_account_license(self, section="game"):
    if getattr(self, "token", ""):
        try:
            profile = api_request("/auth", {"action": "profile"}, self.token)
            self.user = profile["user"]
            licenses = profile.get("licenses") or []
            if licenses and licenses[0].get("key"):
                self.config_data["license_key"] = licenses[0]["key"]
                self.save_config()
        except Exception:
            pass
    return original_show_main(self, section)


Launcher.show_main = show_main_with_account_license


def install_and_launch_ephemeral(self, key):
    """Validate the release, download the mod and keep it available for Fabric."""
    global active_launcher
    active_launcher = self
    try:
        try:
            policy = api_request("/launcher-update", {"action": "manifest"})
            minimum_version = str(policy.get("minimumVersion") or policy.get("release", {}).get("version") or "")
            remote_version = version_tuple(minimum_version)
            current_version = version_tuple(CURRENT_VERSION)
            if policy.get("required") and remote_version and current_version and remote_version > current_version:
                self.check_self_update()
                raise RuntimeError("Доступно обязательное обновление лаунчера. Дождитесь установки новой версии.")
        except RuntimeError as error:
            # License validation below also sends clientVersion and enforces the
            # minimum version. A temporary manifest outage must not silently
            # kill the launch thread or leave the Play button disabled.
            if "недоступен" not in str(error).lower():
                raise
        apply_performance_options()
        # Never accept an arbitrary local development JAR in production.
        self.config_data["local_mod"] = ""
        self.save_config()
        return original_install_and_launch(self, key)
    except Exception as error:
        message = str(error).strip() or "Не удалось запустить Minecraft"
        try:
            self.set_progress("Ошибка запуска: " + message, 0, 100)
        except Exception:
            pass
        self.busy = False

        def restore_after_failure():
            try:
                self.play_button.state(["!disabled"])
                messagebox.showerror("Infinyty Launcher", message)
            except Exception:
                pass

        try:
            self.after(0, restore_after_failure)
        except Exception:
            pass


def logout_resilient(self):
    """Log out locally even when the remote session is already unavailable."""
    global download_grant
    download_grant = ""
    return original_logout(self)


Launcher.install_and_launch = install_and_launch_ephemeral
Launcher.logout = logout_resilient
namespace["APP_VERSION"] = CURRENT_VERSION

if __name__ == "__main__":
    Launcher().mainloop()
