"""Package the current release sources, including local changes, without local secrets."""
from pathlib import Path
import subprocess
import zipfile

root = Path(__file__).resolve().parent.parent
allowed_roots = {'androidApp', 'composeApp', 'core', 'cast', 'crashlytics', 'lastfm', 'gradle', 'config', 'docs', 'scripts', 'website', 'assets', 'icons', '.github'}
allowed_files = {'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat', 'LICENSE', 'README.md', 'CONTRIBUTING.md', 'PERFORMANCE.md', '.gitignore'}
names = subprocess.check_output(['git', 'ls-files', '-z', '--cached', '--others', '--exclude-standard'], cwd=root).decode('utf-8').split('\0')
names.append('gradle/wrapper/gradle-wrapper.jar')
destination = root / 'launch-package/hosting/meloqis/source/Meloqis-Music-source.zip'
destination.parent.mkdir(parents=True, exist_ok=True)
count = 0
with zipfile.ZipFile(destination, 'w', zipfile.ZIP_DEFLATED) as archive:
    for name in sorted(set(names)):
        relative = Path(name)
        if not name or not (root / relative).is_file():
            continue
        if relative.parts[0] not in allowed_roots and name not in allowed_files:
            continue
        if any(part in {'build', '.gradle', '.kotlin', '.vercel', '.wrangler', 'node_modules', 'release', 'release-private', '__pycache__'} for part in relative.parts):
            continue
        if relative.suffix in {'.jks', '.keystore', '.apk', '.aab', '.pptx'} or relative.name in {'local.properties', 'signing.json'} or relative.name.startswith('.env'):
            continue
        archive.write(root / relative, name)
        count += 1
print(f'Packaged {count} source files: {destination}')
