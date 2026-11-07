import pathlib, shutil, sys, hashlib, re, json, argparse
root=pathlib.Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser()
parser.add_argument('tag')
parser.add_argument('--source-dir',type=pathlib.Path,default=root/'androidApp/build/outputs/apk/release')
parser.add_argument('--output-dir',type=pathlib.Path,default=root/'build/publish-release')
args=parser.parse_args()
tag=args.tag
version=re.search(r'version-name\s*=\s*"([^"]+)"',(root/'gradle/libs.versions.toml').read_text()).group(1)
if tag!='v'+version:raise SystemExit('Release tag must match the compiled app version')
out=args.output_dir;out.mkdir(parents=True,exist_ok=True)
for abi in ['arm64-v8a','armeabi-v7a','x86_64','universal']:
 source=args.source_dir/f'androidApp-{abi}-release.apk'
 shutil.copy2(source,out/f'Meloqis-Music-{version}-{abi}.apk')
 aliases={'arm64-v8a':'arm64','armeabi-v7a':'armeabi-v7a','x86_64':'x86_64','universal':None}
 name='Meloqis-Music-latest'+('-'+aliases[abi] if aliases[abi] else '')+'.apk'
 shutil.copy2(source,out/name)
with (out/'SHA256SUMS.txt').open('w') as checks:
 for file in sorted(out.glob('*.apk')):checks.write(hashlib.file_digest(file.open('rb'),'sha256').hexdigest()+'  '+file.name+'\n')
shutil.copy2(root/'website/festivals.json',out/'festivals.json')
(out/'update-policy.json').write_text(json.dumps({'schemaVersion':1,'required':True,'minimumVersion':version,'minimumPlayVersion':'0.0.0','latestVersion':version},indent=2)+'\n')
