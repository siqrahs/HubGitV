#!/usr/bin/env python3
"""Push source HubGitV ke repo GitHub lewat GitHub REST API.

 Dipakai sebagai pengganti `git push`: di perangkat ini biner git dan gh
tidak bisa di-exec (mount app data noexec), jadi upload contents dilakukan
lewat HTTP. Commit dibuat lewat API Trees/Commits agar file yang sudah
dihapus dari workspace ikut terhapus di remote.

Env:
    GITHUB_TOKEN     token (default: dibaca dari file token harness)
    HUBGITV_REPO     owner/repo          (default siqrahs/HubGitV)
    HUBGITV_BRANCH   branch              (default main)
    HUBGITV_MSG      judul commit        (default "Update source")
"""
import base64
import fnmatch
import json
import os
import sys
import urllib.error
import urllib.request

REPO = os.environ.get("HUBGITV_REPO", "siqrahs/HubGitV")
BRANCH = os.environ.get("HUBGITV_BRANCH", "main")
MESSAGE = os.environ.get("HUBGITV_MSG", "Update source").strip()
API = "https://api.github.com"
ROOT = os.path.dirname(os.path.abspath(__file__))

# Shared storage tidak menyimpan bit exec, jadi mode 755 ditulis eksplisit.
EXECUTABLE = {"build.sh", "refetch.sh", "fetch_deps.py", "push_github.py"}
SKIP_DIRS = {".git", "build", "libs", ".harness"}


def token():
    tok = os.environ.get("GITHUB_TOKEN")
    if tok:
        return tok.strip()
    home = os.path.join(os.path.dirname(os.path.dirname(ROOT)), "linux", "home", ".gh-token")
    if os.path.isfile(home):
        with open(home) as fh:
            return fh.read().strip()
    if os.path.isfile(os.path.expanduser("~/.gh-token")):
        with open(os.path.expanduser("~/.gh-token")) as fh:
            return fh.read().strip()
    raise SystemExit("GITHUB_TOKEN tidak ditemukan")


def api(path, method="GET", payload=None):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(
        API + path,
        data=data,
        method=method,
        headers={
            "Authorization": f"Bearer {token()}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "HubGitV-push",
            "Content-Type": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            raw = resp.read()
    except urllib.error.HTTPError as e:
        body = e.read().decode(errors="replace")[:500]
        raise SystemExit(f"{method} {path} -> HTTP {e.code}: {body}")
    return json.loads(raw) if raw else {}


def ignore_patterns():
    path = os.path.join(ROOT, ".gitignore")
    if not os.path.isfile(path):
        return []
    with open(path) as fh:
        return [ln.strip() for ln in fh if ln.strip() and not ln.startswith("#")]


def collect(patterns):
    """path -> bytes untuk semua file yang harus ikut commit."""
    files = {}
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in filenames:
            rel = os.path.relpath(os.path.join(dirpath, name), ROOT)
            if any(fnmatch.fnmatch(rel, p) for p in patterns):
                continue
            if name == "keystore.jks" or name.endswith((".apk", ".tmp")):
                continue
            with open(os.path.join(ROOT, rel), "rb") as fh:
                files[rel] = fh.read()
    return files


def main():
    files = collect(ignore_patterns())
    if not files:
        raise SystemExit("tidak ada file untuk di-push")

    head = api(f"/repos/{REPO}/git/ref/heads/{BRANCH}")["object"]["sha"]
    base_tree = api(f"/repos/{REPO}/git/commits/{head}")["tree"]["sha"]
    remote = {
        e["path"]
        for e in api(f"/repos/{REPO}/git/trees/{base_tree}?recursive=1")["tree"]
        if e["type"] == "blob"
    }

    entries = []
    for path in sorted(remote - set(files)):
        entries.append({"path": path, "mode": "100644", "type": "blob", "sha": None})
        print(f"   hapus  {path}")

    for path, data in sorted(files.items()):
        blob = api(
            f"/repos/{REPO}/git/blobs",
            "POST",
            {"content": base64.b64encode(data).decode(), "encoding": "base64"},
        )
        entries.append({
            "path": path,
            "mode": "100755" if path in EXECUTABLE else "100644",
            "type": "blob",
            "sha": blob["sha"],
        })
        print(f"   update {path}")

    tree = api(f"/repos/{REPO}/git/trees", "POST", {"base_tree": base_tree, "tree": entries})
    commit = api(
        f"/repos/{REPO}/git/commits",
        "POST",
        {"message": MESSAGE, "tree": tree["sha"], "parents": [head]},
    )
    api(f"/repos/{REPO}/git/refs/heads/{BRANCH}", "PATCH", {"sha": commit["sha"]})

    kept = sum(1 for e in entries if e["sha"])
    print(f"\ncommit {commit['sha']} -> {REPO}:{BRANCH} ({kept} file, {len(entries) - kept} dihapus)")


if __name__ == "__main__":
    sys.exit(main())
