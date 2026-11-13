import hashlib, json, os, pathlib, urllib.request, urllib.error, uuid
root = pathlib.Path("release/1.1.0")
jar = root / "towns-and-kingdoms-workshop-1.1.0-neoforge-1.21.1.jar"
token = os.environ.get("CURSEFORGE_TOKEN", "")
if not token:
    raise SystemExit("CURSEFORGE_TOKEN is missing from this repository.")
headers = {"X-Api-Token": token, "User-Agent": "Workshop-Release/1.1.0"}
base = "https://minecraft.curseforge.com"
def get_json(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=60) as r:
        return json.load(r)
selected = ["1.21.1", "NeoForge", "Java 21"]
metadata = {
    "displayName": jar.name,
    "releaseType": "release",
    "changelogType": "html",
    "changelog": (root / "CHANGELOG.html").read_text(encoding="utf-8"),
    "gameVersionNames": selected,
    "relations": {"projects": [
        {"slug": "waystones", "type": "requiredDependency"},
        {"slug": "balm", "type": "requiredDependency"}
    ]}
}
boundary = uuid.uuid4().hex
parts = [
    ("--" + boundary + '\r\nContent-Disposition: form-data; name="metadata"\r\nContent-Type: application/json\r\n\r\n').encode() + json.dumps(metadata).encode(),
    ("--" + boundary + '\r\nContent-Disposition: form-data; name="file"; filename="' + jar.name + '"\r\nContent-Type: application/java-archive\r\n\r\n').encode() + jar.read_bytes(),
]
body = b"\r\n".join(parts) + ("\r\n--" + boundary + "--\r\n").encode()
request = urllib.request.Request(base + "/api/projects/1789377/upload-file", data=body, headers={**headers, "Content-Type": "multipart/form-data; boundary=" + boundary}, method="POST")
# Never automatically retry an upload: an ambiguous timeout may already have created the file.
try:
    with urllib.request.urlopen(request, timeout=180) as r:
        result = json.load(r)
except urllib.error.HTTPError as e:
    raise SystemExit("CurseForge upload failed with HTTP " + str(e.code))
file_id = result.get("id", result.get("data", {}).get("id"))
if not isinstance(file_id, int) or file_id <= 0:
    raise SystemExit("Upload response did not contain a valid CurseForge file ID.")
url = "https://www.curseforge.com/minecraft/mc-mods/towns-kingdoms-workshop/files/" + str(file_id)
receipt = {"file_id": file_id, "url": url, "filename": jar.name, "sha256": hashlib.sha256(jar.read_bytes()).hexdigest(), "gameVersions": selected, "changelogType": "html"}
pathlib.Path("curseforge-upload-result.json").write_text(json.dumps(receipt, indent=2))
print(json.dumps(receipt))
with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as summary:
    summary.write("Uploaded existing Workshop 1.1.0 JAR with full HTML changelog.\n\n" + url + "\n")
