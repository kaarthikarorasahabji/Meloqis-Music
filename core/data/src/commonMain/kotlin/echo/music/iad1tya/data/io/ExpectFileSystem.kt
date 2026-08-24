package echo.music.iad1tya.data.io

import okio.FileSystem

expect fun fileSystem(): FileSystem

expect fun fileDir(): String