package com.example.pagtest;


import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;

import org.libpag.PAGFile;
import org.libpag.PAGPlayer;
import org.libpag.PAGSurface;

import java.io.File;
import java.io.FileDescriptor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


//一个很古老的的mediacodec编码，用在生产环境请优化下代码
public class PagRecorder {

    //保存状态的回调
    public interface Listener {
        void onSuccess();

        void onProgress(float progress);

        void onError(Exception e);
    }

    private Listener mListener;

    //视频进度占总进度的百分比
    private static final float VIDEO_PROGRESS_PERCENT = 95f;

    private static final String TAG = "PAG_TEST_TAG";

    private static final boolean DEBUG = BuildConfig.DEBUG;

    // 视频编码
    public static final String MIME_TYPE_VIDEO = MediaFormat.MIMETYPE_VIDEO_AVC;

    // 视频文件的宽高
    private final int mVideoWidth;
    private final int mVideoHeight;
    // 视频码率
    private final int mVideoBitRate;


    //视频时长 microsecond
    public long mVideoTime;

    // 帧率
    private final int mFrameRate;
    // 总帧数
    private final int mFrameNum;


    // 编码器
    private MediaCodec mVideoEncoder;
    //容器
    private MediaMuxer mMuxer;

    private int mVideoTrackIndex;//video 轨道
    private boolean mMuxerStarted;

    private static final int TIMEOUT_USEC = 1000;

    //时间戳计数器
    private long mPTSCount = 0;

    // allocate one of these up front so we don't need to do it every time
    private MediaCodec.BufferInfo mBufferInfo;


    //PAG相关
    private PAGPlayer mPagPlayer;
    private final PAGFile mPagFile;

    private boolean mIsForceStop = false;


    public PagRecorder(PAGFile pagFile) {
        mPagFile = pagFile;

        mFrameRate = (int) pagFile.frameRate();
        mVideoTime = pagFile.duration();

        mFrameNum = (int) (mFrameRate * mVideoTime / 1000 / 1000);

        mVideoWidth = mPagFile.width() / 2 * 2;
        mVideoHeight = mPagFile.height() / 2 * 2;

        mVideoBitRate = mVideoWidth * mVideoHeight * 8 * mFrameRate / 30;

        if (DEBUG) {
            Log.d(TAG, "mFrameRate: " + mFrameRate);
            Log.d(TAG, "mVideoTime: " + mVideoTime);
            Log.d(TAG, "mFrameNum: " + mFrameNum);
            Log.d(TAG, "mVideoWidth: " + mVideoWidth);
            Log.d(TAG, "mVideoHeight: " + mVideoHeight);
            Log.d(TAG, "mVideoBitRate: " + mVideoBitRate);
        }


    }


    /**
     * 录制opengl到MP4,程序入口
     * 30及以上，outPath, 30以下，传入outDir
     * 资源音乐传入musicAFD，或者musicPath，或者context + uri， 三选一
     */
    public void record(
            String outPath,
            FileDescriptor outFD,
            String musicPath,
            AssetFileDescriptor musicAFD,
            Uri musicUri,
            Context musicUriContext,
            Listener listener
    ) {

        mListener = listener;
        try {
            //初始化视频
            initVideo(outPath, outFD, mVideoWidth, mVideoHeight);
            //初始化音频
            initAudio(musicPath, musicAFD, musicUri, musicUriContext);

            mPagFrameChanged = new boolean[mFrameNum];

            //视频编码
            for (int i = 0; i < mFrameNum; i++) {
                drawPAG(i);
                drainEncoder(false);
                if (mListener != null && !mIsForceStop) {
                    //视频进度回调，占整体的90%
                    float progress = VIDEO_PROGRESS_PERCENT * i / (mFrameNum - 1);
                    mListener.onProgress(progress);
                }
            }
            //视频编码结束
            drainEncoder(true);

            //音频编码
            if (hasAudio()) {
                while (mAudioStarted) {
                    stepPipeline();
                }
            }
            //释放编码器
            releaseEncoder();
            if (mListener != null) {
                mListener.onProgress(100f);
                mListener.onSuccess();
            }
        } catch (Exception e) {
            e.printStackTrace();
            //记录下record error
            if (DEBUG) Log.d(TAG, "record error: " + e.getMessage());
            releaseEncoder();
            if (mListener != null) mListener.onError(e);
        }
    }

//    private long mPagPts = 0L;

    //用来记录pag画面是否发生改变
    //逐帧编码的时候，pag如果不发生改变 则需要跳过这一帧的编码，直到pag发生改变
    //这样才能使编码器的帧数和pag的帧数对应起来，pts不会错
    private boolean[] mPagFrameChanged;

    //pag毫秒绘制
    private void drawPAG(int frameCount) {
        double progress = frameCount * 1.0f / (mFrameNum - 1);
        mPagPlayer.setProgress(progress);
        boolean isChanged = mPagPlayer.flush();
        mPagFrameChanged[frameCount] = isChanged;
    }


    private int mResolutionError = 0;

    /**
     * 初始化视频编码
     * 初始化code surface
     * 初始化mux容器
     */
    private void initVideo(String outPath, FileDescriptor outFD, int width, int height) {

        // 创建一个buffer
        mBufferInfo = new MediaCodec.BufferInfo();

        //-----------------MediaFormat-----------------------
        // mediaCodeC采用的是H.264编码
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE_VIDEO, width, height);
        // 数据来源自surface
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        // 视频码率
        format.setInteger(MediaFormat.KEY_BIT_RATE, mVideoBitRate);
        // fps
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        //设置关键帧的时间
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);//关键帧间隔时间 单位s
        //尝试设置vbr
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);

        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
        format.setInteger("level", MediaCodecInfo.CodecProfileLevel.AVCLevel41);


        //-----------------Encoder-----------------------
        try {
            mVideoEncoder = MediaCodec.createEncoderByType(MIME_TYPE_VIDEO);
            mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            e.printStackTrace();
            if (mResolutionError > 100) throw new RuntimeException("Resolution error!");
            mResolutionError++;
//            //分辨率不支持，缩小分辨率
            int newWidth = 720;
            if (mVideoWidth == 720) {
                newWidth = 540;
            }
            int newHeight = (int) (mVideoHeight * 1.0f / mVideoWidth * newWidth) / 2 * 2;
            initVideo(outPath, outFD, newWidth, newHeight);
            return;
        }

        // 创建一个surface
        Surface surface = mVideoEncoder.createInputSurface();

        //初始化PagPlayer
        PAGSurface pagSurface = PAGSurface.FromSurface(surface);
        mPagPlayer = new PAGPlayer();
        mPagPlayer.setSurface(pagSurface);
        mPagPlayer.setComposition(mPagFile);
        mPagPlayer.setProgress(0);
        mVideoEncoder.start();


        //-----------------MediaMuxer-----------------------
        try {
            if (isHighVersion()) {
                mMuxer = new MediaMuxer(outFD, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } else {
                mMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            }

        } catch (Exception ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }

        mVideoTrackIndex = -1;
        mMuxerStarted = false;
    }


    /**
     * mEncoder从缓冲区取数据，然后交给mMuxer编码
     *
     * @param endOfStream 是否停止录制
     */
    private void drainEncoder(boolean endOfStream) {

        if (mIsForceStop) return;

        if (DEBUG) Log.d(TAG, "drainEncoder(" + endOfStream + ")");

        // 停止录制
        if (endOfStream) {
            if (DEBUG) Log.d(TAG, "end Of Stream, sending EOS to encoder");
            mVideoEncoder.signalEndOfInputStream();
        }
        //拿到输出缓冲区,用于取到编码后的数据
        while (true) {
            //拿到输出缓冲区的索引
            int encoderStatus = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

            if (DEBUG) Log.d(TAG, "encoderStatus:" + encoderStatus);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {
                    if (DEBUG) Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                //
                MediaFormat newFormat = mVideoEncoder.getOutputFormat();
                if (DEBUG) Log.d(TAG, "encoder output format changed: " + newFormat);
                // now that we have the Magic Goodies, start the muxer
                mVideoTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                if (DEBUG) Log.d(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
            } else {
                //获取解码后的数据
                ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(encoderStatus);
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                //
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (DEBUG) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }
                //

                if (mBufferInfo.size < 0
                        || mBufferInfo.offset < 0
                        || (mBufferInfo.offset + mBufferInfo.size) > encodedData.capacity()) {
                    //数据不对的情况,先跳过writeSampleData
                    mBufferInfo.size = 0;
                    String message = "mBufferInfo.size: " + mBufferInfo.size + ", " +
                            "mBufferInfo.offset: " + mBufferInfo.offset + ", " +
                            "capacity: " + encodedData.capacity();
                    if (DEBUG) Log.d(TAG, "record message: " + message);
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    //计算pag的跳帧
                    while ((mPTSCount < mFrameNum - 1) && !mPagFrameChanged[(int) mPTSCount]) {
                        mPTSCount++;
                    }

                    //计算当前帧数的时间戳
                    long pts = mPTSCount * 1_000_000L / mFrameRate;
                    mBufferInfo.presentationTimeUs = pts;

                    mPTSCount++;

                    if (DEBUG) Log.d(TAG, "count: " + mPTSCount + ", pts: " + pts);
                    // 往容器写入数据
                    mMuxer.writeSampleData(mVideoTrackIndex, encodedData, mBufferInfo);
                    if (DEBUG) Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");
                }

                //释放Buffer
                mVideoEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        if (DEBUG) Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (DEBUG) Log.d(TAG, "end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }


    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     * 释放资源
     * 经常会发生释放崩溃，必须try
     */
    public void releaseEncoder() {
        try {
            mIsForceStop = true;
            if (mVideoEncoder != null) {
                mVideoEncoder.stop();
                mVideoEncoder.release();
                mVideoEncoder = null;
            }

            if (mPagPlayer != null) {
                mPagPlayer.release();
            }

            if (mMuxer != null) {
                mMuxer.stop();
                mMuxer.release();
                mMuxer = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //===================================音频相关===================================

    MediaCodec.BufferInfo mAudioBuffInfo;
    ByteBuffer mAudioBuff;


    boolean mAudioStarted = false;//是否结束


    //音频在mux里的轨道
    int mAudioTrackIndex = 0;

    MediaExtractor mAudioExtractor;

    private static final int AUDIO_BUFF_SIZE = 1024 * 1024;


    //初始化音频  把aac交给mux合成mp4
    private void initAudio(String musicPath, AssetFileDescriptor musicAFD, Uri musicUri, Context musicUriContext) {

        try {

            mAudioBuff = ByteBuffer.allocateDirect(AUDIO_BUFF_SIZE).order(ByteOrder.nativeOrder());
            mAudioBuffInfo = new MediaCodec.BufferInfo();

            mAudioExtractor = new MediaExtractor();

            if (musicPath != null) {
                mAudioExtractor.setDataSource(musicPath);
                if (DEBUG) Log.d(TAG, "musicPath:" + musicPath);
            } else if (musicAFD != null) {
                mAudioExtractor.setDataSource(musicAFD.getFileDescriptor(), musicAFD.getStartOffset(), musicAFD.getLength());
                if (DEBUG) Log.d(TAG, "musicAFD:" + musicAFD);
            } else if (musicUri != null && musicUriContext != null) {
                mAudioExtractor.setDataSource(musicUriContext, musicUri, null);
                if (DEBUG) Log.d(TAG, "musicUri:" + musicUri);
            } else {
                //path afd uri都是空，不进行音乐编码
                mAudioStarted = false;
                return;
            }


            int trackCount = mAudioExtractor.getTrackCount();//获得通道数量
            int audiopos = 0;
            if (trackCount > 1) {
                for (int i = 0; i < trackCount; i++) { //遍历所以轨道
                    MediaFormat itemMediaFormat = mAudioExtractor.getTrackFormat(i);
                    String itemMime = itemMediaFormat.getString(MediaFormat.KEY_MIME);
                    if (itemMime.startsWith("audio")) { //获取音频轨道位置
                        audiopos = i;
                        break;
                    }
                }
            }

            mAudioExtractor.selectTrack(audiopos);//选择到音频轨道

            MediaFormat format = mAudioExtractor.getTrackFormat(audiopos);

            mAudioTrackIndex = mMuxer.addTrack(format);
            mAudioStarted = true;
        } catch (Exception e) {
            e.printStackTrace();
            mAudioBuffInfo = null;
            mAudioStarted = false;
        }
    }


    boolean hasAudio() {
        try {
            int trackIndex = mAudioExtractor.getSampleTrackIndex();
            //没有通道
            if (DEBUG) Log.d(TAG, "hasAudio: " + (trackIndex >= 0));
            return trackIndex >= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public void stepPipeline() {

        if (mIsForceStop) return;

        if (!mAudioStarted) return;

        mAudioBuff.clear();
        int chunkSize = mAudioExtractor.readSampleData(mAudioBuff, 0);
        long sampleTime = mAudioExtractor.getSampleTime();//返回当前的时间戳 微秒

        if (DEBUG) Log.d(TAG, "audio sampleTime: " + sampleTime);

        //结束
        if (sampleTime > mVideoTime) {
            if (DEBUG) Log.d(TAG, "mux audio end");
            mAudioBuff.clear();
            mAudioBuffInfo.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            mMuxer.writeSampleData(mAudioTrackIndex, mAudioBuff, mAudioBuffInfo);
            mAudioStarted = false;
            return;
        }

        if (chunkSize > 0) {
            mAudioBuffInfo.set(0, chunkSize, sampleTime, MediaCodec.BUFFER_FLAG_KEY_FRAME);
            mMuxer.writeSampleData(mAudioTrackIndex, mAudioBuff, mAudioBuffInfo);
            mAudioExtractor.advance();
            if (mListener != null) {
                //音频进度回调用
                float progress = VIDEO_PROGRESS_PERCENT + (sampleTime * 1.0f / mVideoTime) * (100f - VIDEO_PROGRESS_PERCENT);
                mListener.onProgress(progress);
            }
        } else {
            //无数据了
            if (DEBUG) Log.d(TAG, "no audio data");
            mAudioBuff.clear();
            mAudioBuffInfo.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            mMuxer.writeSampleData(mAudioTrackIndex, mAudioBuff, mAudioBuffInfo);
            mAudioStarted = false;
        }

    }


    //=============================低版本保存后更新媒体库======================================

    /**
     * copy from EditorBaseActivity
     * 保存到视频到本地，并插入MediaStore以保证相册可以查看到
     * 这是更优化的方法，防止读取的视频获取不到宽高
     *
     * @param context    上下文
     * @param filePath   文件路径
     * @param createTime 创建时间 <=0时为当前时间 ms
     * @param duration   视频长度 ms
     * @param width      宽度
     * @param height     高度
     */
    public static void insertVideoToMediaStore(
            Context context,
            String filePath,
            long createTime,
            int width,
            int height,
            long duration,
            MediaScannerConnection.OnScanCompletedListener callback) {
        if (!new File(filePath).exists())
            return;
        try {
            if (isHighVersion()) {
                ContentValues values = initCommonContentValues(filePath);
                values.put(MediaStore.Video.VideoColumns.DATE_TAKEN, createTime);
                if (duration > 0) values.put(MediaStore.Video.VideoColumns.DURATION, duration);
                if (width > 0) values.put(MediaStore.Video.VideoColumns.WIDTH, width);
                if (height > 0) values.put(MediaStore.Video.VideoColumns.HEIGHT, height);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            }
            MediaScannerConnection.scanFile(context, new String[]{filePath}, null, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 插入时初始化公共字段
     *
     * @param filePath 文件
     * @return ContentValues
     */
    private static ContentValues initCommonContentValues(String filePath) {
        ContentValues values = new ContentValues();
        File saveFile = new File(filePath);
        long timeMillis = System.currentTimeMillis();
        values.put(MediaStore.MediaColumns.TITLE, saveFile.getName());
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, saveFile.getName());
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, timeMillis);
        values.put(MediaStore.MediaColumns.DATE_ADDED, timeMillis);
        values.put(MediaStore.MediaColumns.DATA, saveFile.getAbsolutePath());
        values.put(MediaStore.MediaColumns.SIZE, saveFile.length());
        return values;
    }

    public static boolean isHighVersion() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

}

