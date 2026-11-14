#!/usr/bin/env python3
"""Fetches PomDaisy's newest TestFlight feedback (crash reports with their logs, screenshot feedback) from the App
Store Connect API into a folder. Run by .github/workflows/ios-feedback.yml, which encrypts the folder before uploading
it (the repo is public, feedback carries the tester's details): only the owner of the private key can read it.

Usage: testflight_feedback.py OUT_DIR  (env: ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH)
"""
import json
import os
import sys
import time
import urllib.request

import jwt  # PyJWT[crypto]

API = "https://api.appstoreconnect.apple.com"
BUNDLE = "com.nowarmoreweed.pomdaisy"


def token():
    key = open(os.environ["ASC_KEY_PATH"]).read()
    now = int(time.time())
    return jwt.encode({"iss": os.environ["ASC_ISSUER_ID"], "iat": now, "exp": now + 1200, "aud": "appstoreconnect-v1"},
                      key, algorithm="ES256", headers={"kid": os.environ["ASC_KEY_ID"], "typ": "JWT"})


TOKEN = None


def get(path):
    req = urllib.request.Request(path if path.startswith("http") else API + path,
                                 headers={"Authorization": "Bearer " + TOKEN})
    try:
        with urllib.request.urlopen(req) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        print(f"{path}: HTTP {e.code}", file=sys.stderr)
        return None


def fetch(url, dest):
    try:
        with urllib.request.urlopen(url) as r, open(dest, "wb") as f:
            f.write(r.read())
    except Exception as e:  # noqa: BLE001
        print(f"{dest}: {e}", file=sys.stderr)


def main():
    global TOKEN
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    TOKEN = token()
    apps = get(f"/v1/apps?filter[bundleId]={BUNDLE}")
    if not apps or not apps["data"]:
        sys.exit("app not found")
    app = apps["data"][0]["id"]
    crashes = get(f"/v1/apps/{app}/betaFeedbackCrashSubmissions?sort=-createdDate&limit=10") or {"data": []}
    for i, c in enumerate(crashes["data"]):
        a = c["attributes"]
        name = f"crash-{i:02d}-{a.get('createdDate', '')[:19].replace(':', '')}"
        with open(os.path.join(out, name + ".json"), "w") as f:
            json.dump(a, f, indent=1)
        log = get(f"/v1/betaFeedbackCrashSubmissions/{c['id']}/crashLog")
        if log and log.get("data"):
            with open(os.path.join(out, name + ".crash"), "w") as f:
                f.write(log["data"]["attributes"].get("logText") or "")
    shots = get(f"/v1/apps/{app}/betaFeedbackScreenshotSubmissions?sort=-createdDate&limit=10") or {"data": []}
    for i, s in enumerate(shots["data"]):
        a = s["attributes"]
        name = f"feedback-{i:02d}-{a.get('createdDate', '')[:19].replace(':', '')}"
        with open(os.path.join(out, name + ".json"), "w") as f:
            json.dump(a, f, indent=1)
        for j, sh in enumerate(a.get("screenshots") or []):
            if sh.get("url"):
                fetch(sh["url"], os.path.join(out, f"{name}-{j}.png"))
    print(f"{len(crashes['data'])} crash reports, {len(shots['data'])} screenshot feedbacks")


if __name__ == "__main__":
    main()
