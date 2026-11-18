#!/usr/bin/env python3
"""Overlay Water Client's launcher UI into the pinned Amethyst Android engine checkout."""

from pathlib import Path
import re
import shutil
import sys
import xml.etree.ElementTree as ET


if len(sys.argv) != 2:
    raise SystemExit("usage: prepare-runtime.py <amethyst-checkout>")

repo = Path(__file__).resolve().parents[1]
engine = Path(sys.argv[1]).resolve()
app = engine / "app_pojavlauncher"
water_java = repo / "android/app/src/main/java/dev/waterclient/android"
target_java = app / "src/main/java/dev/waterclient/android"
target_java.mkdir(parents=True, exist_ok=True)

for source in water_java.glob("*.java"):
    shutil.copy2(source, target_java / source.name)

bridge_source = repo / "runtime-overlay/net/kdt/pojavlaunch/WaterClientRuntimeBridge.java"
bridge_target = app / "src/main/java/net/kdt/pojavlaunch/WaterClientRuntimeBridge.java"
if not bridge_source.is_file():
    raise SystemExit(f"Missing Water Client runtime bridge: {bridge_source}")
bridge_target.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(bridge_source, bridge_target)

license_source = repo / "docs/THIRD-PARTY-LICENSES/AMETHYST-LICENSE.txt"
license_target = app / "src/main/assets/licenses/AMETHYST-LICENSE.txt"
if not license_source.is_file():
    raise SystemExit(f"Missing Amethyst license text: {license_source}")
license_target.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(license_source, license_target)

# Select Water Client's local profile inside Amethyst after its storage and preferences initialize.
# The bridge keeps Amethyst's online-license requirement for local authentication intact.
storage_activity = app / "src/main/java/net/kdt/pojavlaunch/TestStorageActivity.java"
storage_source = storage_activity.read_text(encoding="utf-8")
storage_anchor = """        Intent intent =  new Intent(this, LauncherActivity.class);
        startActivity(intent);
        finish();"""
storage_replacement = """        WaterClientRuntimeBridge.prepare(this, getIntent());
        Intent intent =  new Intent(this, LauncherActivity.class);
        startActivity(intent);
        finish();"""
if "WaterClientRuntimeBridge.prepare(this, getIntent());" not in storage_source:
    if storage_anchor not in storage_source:
        raise SystemExit("Could not find the expected Amethyst storage-to-launcher handoff")
    storage_activity.write_text(storage_source.replace(storage_anchor, storage_replacement, 1), encoding="utf-8")

water_icon = repo / "android/app/src/main/res/drawable/ic_water.xml"
engine_drawables = app / "src/main/res/drawable"
engine_drawables.mkdir(parents=True, exist_ok=True)
shutil.copy2(water_icon, engine_drawables / "ic_water.xml")

manifest_path = app / "src/main/AndroidManifest.xml"
tree = ET.parse(manifest_path)
root = tree.getroot()
android = "{http://schemas.android.com/apk/res/android}"
application = root.find("application")
if application is None:
    raise SystemExit("Amethyst manifest has no application element")

test_storage = next((node for node in application.findall("activity")
                     if node.get(android + "name") == ".TestStorageActivity"), None)
if test_storage is None:
    raise SystemExit("Could not find Amethyst TestStorageActivity")
for intent_filter in list(test_storage.findall("intent-filter")):
    for child in list(intent_filter):
        if child.tag == "action" and child.get(android + "name") == "android.intent.action.MAIN":
            intent_filter.remove(child)
        elif child.tag == "category" and child.get(android + "name") == "android.intent.category.LAUNCHER":
            intent_filter.remove(child)
    if not list(intent_filter):
        test_storage.remove(intent_filter)

water_activity = next((node for node in application.findall("activity")
                       if node.get(android + "name") == "dev.waterclient.android.MainActivity"), None)
if water_activity is None:
    water_activity = ET.SubElement(application, "activity", {
        android + "name": "dev.waterclient.android.MainActivity",
        android + "configChanges": "orientation|screenSize|keyboardHidden",
        android + "exported": "true",
        android + "screenOrientation": "landscape",
    })
launcher_filter_exists = any(
    any(child.tag == "action" and child.get(android + "name") == "android.intent.action.MAIN"
        for child in item.findall("action"))
    for item in water_activity.findall("intent-filter")
)
if not launcher_filter_exists:
    intent_filter = ET.SubElement(water_activity, "intent-filter")
    ET.SubElement(intent_filter, "action", {android + "name": "android.intent.action.MAIN"})
    ET.SubElement(intent_filter, "category", {android + "name": "android.intent.category.LAUNCHER"})
application.set(android + "icon", "@drawable/ic_water")
application.set(android + "roundIcon", "@drawable/ic_water")
ET.register_namespace("android", "http://schemas.android.com/apk/res/android")
ET.register_namespace("tools", "http://schemas.android.com/tools")
tree.write(manifest_path, encoding="utf-8", xml_declaration=True)

gradle_path = app / "build.gradle"
gradle = gradle_path.read_text(encoding="utf-8")
gradle = gradle.replace('resValue "string", "app_name", "Amethyst (Debug)"',
                        'resValue "string", "app_name", "WATER CLIENT"')
gradle = gradle.replace('resValue "string", "app_short_name", "Amethyst (Debug)"',
                        'resValue "string", "app_short_name", "WATER CLIENT"')
gradle = gradle.replace('resValue "string", "app_name", "Amethyst"',
                        'resValue "string", "app_name", "WATER CLIENT"')
gradle = gradle.replace('resValue "string", "app_short_name", "Amethyst"',
                        'resValue "string", "app_short_name", "WATER CLIENT"')
# androidnsbypass requires Android API 36.1. The SDK platform package is android-36;
# AGP's minorApiLevel selects the 36.1 API surface without changing targetSdk.
gradle = gradle.replace("compileSdk = 37", "compileSdk { version = release(36) { minorApiLevel = 1 } }")
gradle_path.write_text(gradle, encoding="utf-8")

strings_path = app / "src/main/res/values/strings.xml"
strings = strings_path.read_text(encoding="utf-8")
strings = re.sub(r'(<string name="app_name"[^>]*>).*?(</string>)', r'\1WATER CLIENT\2', strings, count=1)
strings = re.sub(r'(<string name="app_short_name"[^>]*>).*?(</string>)', r'\1WATER CLIENT\2', strings, count=1)
strings_path.write_text(strings, encoding="utf-8")

print("Water Client UI, profile bridge, and identity overlaid onto Amethyst Android runtime.")
