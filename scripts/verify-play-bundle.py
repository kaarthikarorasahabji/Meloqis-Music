"""Verify final bundle metadata, permissions and 64-bit ELF page alignment.

Run after bundletool dumps the final manifest to launch-package/bundle-manifest.xml.
This static inspection does not replace testing on a 16 KB device or emulator.
"""
import hashlib
import json
from pathlib import Path
import struct
import sys
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parent.parent
out = root / 'launch-package'
bundle = out / 'Meloqis-Music-play-release.aab'
manifest = ET.parse(out / 'bundle-manifest.xml').getroot()
android = '{http://schemas.android.com/apk/res/android}'
permissions = sorted(e.get(android + 'name') for e in manifest.findall('uses-permission'))
sdk = manifest.find('uses-sdk')
report = {
    'bundle': bundle.name,
    'sha256': hashlib.file_digest(bundle.open('rb'), 'sha256').hexdigest(),
    'package': manifest.get('package'),
    'versionName': manifest.get(android + 'versionName'),
    'versionCode': manifest.get(android + 'versionCode'),
    'minSdk': sdk.get(android + 'minSdkVersion'),
    'targetSdk': sdk.get(android + 'targetSdkVersion'),
    'permissions': permissions,
    'nativeLibraries': [],
    'limitations': ['Static checks only; device QA and Play Console review remain required.'],
}
errors = []
config = json.loads((out / 'bundle-config.json').read_text(encoding='utf-8-sig'))
report['bundleNativeAlignment'] = config.get('optimizations', {}).get('uncompressNativeLibraries', {}).get('alignment')
if report['bundleNativeAlignment'] != 'PAGE_ALIGNMENT_16K':
    errors.append('Bundle does not request 16 KB native library alignment')
for forbidden in ['android.permission.REQUEST_INSTALL_PACKAGES', 'com.google.android.gms.permission.AD_ID', 'android.permission.CAMERA', 'android.permission.FOREGROUND_SERVICE_CAMERA']:
    if forbidden in permissions:
        errors.append('Forbidden Play distribution permission: ' + forbidden)
application = manifest.find('application')
if application.get(android + 'debuggable', 'false') != 'false':
    errors.append('Bundle is debuggable')
if int(report['targetSdk']) < 36:
    errors.append('Target SDK is below this release configuration')
with zipfile.ZipFile(bundle) as archive:
    if any('mediapipe' in name.lower() or 'hand_landmarker' in name.lower() for name in archive.namelist()):
        errors.append('Removed hand-detection assets or libraries are still bundled')
    for name in sorted(archive.namelist()):
        if not name.endswith('.so') or not any('/' + abi + '/' in name for abi in ['arm64-v8a', 'x86_64']):
            continue
        data = archive.read(name)
        if data[:4] != b'\x7fELF' or data[4] != 2:
            errors.append('Invalid 64-bit ELF: ' + name)
            continue
        endian = '<' if data[5] == 1 else '>'
        offset = struct.unpack_from(endian + 'Q', data, 32)[0]
        size, count = struct.unpack_from(endian + 'HH', data, 54)
        alignments = []
        compatible = True
        for index in range(count):
            program = struct.unpack_from(endian + 'IIQQQQQQ', data, offset + index * size)
            if program[0] == 1:
                alignments.append(program[7])
                compatible &= program[7] >= 16384 and (program[2] - program[3]) % 16384 == 0
        compatible &= bool(alignments)
        report['nativeLibraries'].append({'name': name, 'loadAlignments': alignments, 'supports16KBAlignment': compatible})
        if not compatible:
            errors.append('16 KB alignment failure: ' + name)
if not report['nativeLibraries']:
    errors.append('No 64-bit native libraries inspected')
report['errors'] = errors
report['staticChecksPassed'] = not errors
(out / 'verification-report.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps({key: report[key] for key in ['package', 'versionName', 'versionCode', 'targetSdk', 'staticChecksPassed', 'errors']}, indent=2))
print(f"Inspected {len(report['nativeLibraries'])} 64-bit native libraries.")
sys.exit(bool(errors))
