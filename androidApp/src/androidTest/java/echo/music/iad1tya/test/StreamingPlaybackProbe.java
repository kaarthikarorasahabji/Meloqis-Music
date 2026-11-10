package echo.music.iad1tya.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.BaseDataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.cache.CacheSpan;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import echo.music.iad1tya.media3.di.PlaybackStreaming;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Real ExoPlayer reading a slow synthetic source through the production cache/load policy. */
public final class StreamingPlaybackProbe extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result=new Bundle(); boolean passed=false;
        try {
            String normal=check(false,false), locked=check(true,false), primed=check(false,true);
            checkNoRanges();
            result.putString("stream", "STREAMING_OK: "+normal+"; "+locked+"; "+primed+"; no-range header-only check passed\n"); passed=true;
        } catch(Throwable t) { result.putString("stream", "STREAMING_FAILED: "+t+"\n"); }
        finish(passed?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
    private String check(boolean locked, boolean primed) throws Exception {
        File root=new File(getTargetContext().getCacheDir(),"stream-probe-"+System.nanoTime());
        StandaloneDatabaseProvider db=new StandaloneDatabaseProvider(getTargetContext());
        SimpleCache downloads=new SimpleCache(new File(root,"downloads"),new NoOpCacheEvictor(),db);
        SimpleCache playback=new SimpleCache(new File(root,"playback"),new NoOpCacheEvictor(),db);
        byte[] wav=wave(); AtomicLong bytes=new AtomicLong(); AtomicBoolean playing=new AtomicBoolean();
        ExoPlayer[] player=new ExoPlayer[1]; CacheSpan span=null;
        try {
            if(locked)span=playback.startReadWrite("probe",0,wav.length);
            androidx.media3.datasource.cache.CacheDataSource.Factory factory=PlaybackStreaming.INSTANCE.cacheFactory(
                downloads,playback,()->new SlowWave(wav,bytes));
            if(primed) {
                PlaybackStreaming.INSTANCE.prime(factory,"probe","https://fixture.invalid/probe.wav",Long.valueOf(wav.length),()->kotlin.Unit.INSTANCE);
                if(bytes.get()>128*1024)throw new AssertionError("Preparation exceeded its byte budget");
                if(playback.getCacheSpace()<=0)throw new AssertionError("Preparation did not commit the cache");
                if(!playback.isCached("probe",wav.length-65536,65536))throw new AssertionError("Index tail was not cached");
            }
            runOnMainSync(()->{
                player[0]=new ExoPlayer.Builder(getTargetContext())
                    .setLoadControl(PlaybackStreaming.INSTANCE.loadControl(false))
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(factory)) .build();
                player[0].setVolume(0);
                player[0].setMediaItem(new MediaItem.Builder().setUri("https://fixture.invalid/probe.wav")
                    .setMimeType("audio/wav").setCustomCacheKey("probe").build());
                player[0].prepare();player[0].play();
            });
            long start=System.currentTimeMillis(),end=start+8000;
            while(System.currentTimeMillis()<end) {
                runOnMainSync(()->playing.set(player[0].isPlaying()));
                if(playing.get())break;
                Thread.sleep(50);
            }
            long read=bytes.get(),elapsed=System.currentTimeMillis()-start;
            if(!playing.get())throw new AssertionError("Playback did not start; locked="+locked);
            if(read>=wav.length)throw new AssertionError("Waited for complete file");
            runOnMainSync(()->{player[0].release();player[0]=null;});
            if(!locked && playback.getCacheSpace()<=0)throw new AssertionError("Stream was not cached");
            return (locked?"locked cache bypass":primed?"bounded prepared stream":"stream + cache")+" started in "+elapsed+"ms after "+read+"/"+wav.length+" bytes";
        } finally {
            if(player[0]!=null)runOnMainSync(()->player[0].release());
            if(span!=null)playback.releaseHoleSpan(span);
            downloads.release();playback.release();db.close();delete(root);
        }
    }
    private void checkNoRanges() throws Exception {
        File root=new File(getTargetContext().getCacheDir(),"stream-no-range-"+System.nanoTime());
        StandaloneDatabaseProvider db=new StandaloneDatabaseProvider(getTargetContext());
        SimpleCache downloads=new SimpleCache(new File(root,"downloads"),new NoOpCacheEvictor(),db);
        SimpleCache playback=new SimpleCache(new File(root,"playback"),new NoOpCacheEvictor(),db);
        AtomicLong count=new AtomicLong();byte[] wav=wave();
        try {
            PlaybackStreaming.INSTANCE.prime(PlaybackStreaming.INSTANCE.cacheFactory(downloads,playback,()->new SlowWave(wav,count,false)),
                "probe","https://fixture.invalid/probe.wav",Long.valueOf(wav.length),()->kotlin.Unit.INSTANCE);
            if(count.get()!=65536)throw new AssertionError("A server without range support must only prepare the header");
        } finally {downloads.release();playback.release();db.close();delete(root);}
    }
    private static void delete(File f) { File[] files=f.listFiles();if(files!=null)for(File child:files)delete(child);f.delete(); }
    private static byte[] wave() {
        int size=8000*2*60;
        ByteBuffer b=ByteBuffer.allocate(size+44).order(ByteOrder.LITTLE_ENDIAN);
        b.put(new byte[]{'R','I','F','F'}).putInt(size+36).put(new byte[]{'W','A','V','E','f','m','t',' '})
            .putInt(16).putShort((short)1).putShort((short)1).putInt(8000).putInt(16000)
            .putShort((short)2).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt(size);
        return b.array();
    }
    private static final class SlowWave extends BaseDataSource {
        private final byte[] wave;private final AtomicLong count;private int position,end;private Uri uri;private boolean opened;private String range;private final boolean supportsRanges;
        SlowWave(byte[] wave,AtomicLong count){this(wave,count,true);}
        SlowWave(byte[] wave,AtomicLong count,boolean supportsRanges){super(true);this.wave=wave;this.count=count;this.supportsRanges=supportsRanges;}
        @Override public long open(DataSpec spec){
            if(!supportsRanges && spec.position>0)throw new AssertionError("Attempted a tail seek on a server without byte ranges");
            transferInitializing(spec);uri=spec.uri;position=(int)spec.position;
            end=spec.length==C.LENGTH_UNSET?wave.length:(int)Math.min(wave.length,position+spec.length);
            range="bytes "+position+"-"+(end-1)+"/"+wave.length;
            opened=true;transferStarted(spec);return end-position;}
        @Override public int read(byte[] out,int offset,int length)throws java.io.IOException {
            if(length==0)return 0;if(position>=end)return C.RESULT_END_OF_INPUT;
            try{Thread.sleep(10);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new java.io.InterruptedIOException();}
            int n=Math.min(512,Math.min(length,end-position));System.arraycopy(wave,position,out,offset,n);
            position+=n;count.addAndGet(n);bytesTransferred(n);return n;
        }
        @Override public java.util.Map<String,java.util.List<String>> getResponseHeaders(){return supportsRanges?java.util.Map.of("Content-Range",java.util.List.of(range)):java.util.Map.of();}
        @Override public Uri getUri(){return uri;}
        @Override public void close(){if(opened){opened=false;transferEnded();}uri=null;}
    }
}
