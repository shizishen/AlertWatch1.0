package com.longdo.mjpegview.utils;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.longdo.mjpegviewer.MjpegViewError;
import com.longdo.mjpegviewer.MjpegViewStateChangeListener;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MjpegDecoder extends Thread {


    private MjpegViewStateChangeListener listener = null;
    private String url;
    private static final int CHUNK_SIZE = 4096;
    private static final String DEFAULT_BOUNDARY_REGEX = "[_a-zA-Z0-9]*boundary";
    private final String tag = getClass().getSimpleName();
    private static final int WAIT_AFTER_READ_IMAGE_ERROR_MSEC = 5000;
    private int msecWaitAfterReadImageError = WAIT_AFTER_READ_IMAGE_ERROR_MSEC;
    byte[] currentImageBody = new byte[(int) 1e6];
    int currentImageBodyLength = 0;
    private boolean run = true;

    public void cancel() {
        run = false;
    }

    public boolean isRunning() {
        return run;
    }

    public MjpegViewStateChangeListener getStateChangeListener() {
        return this.listener;
    }
    public void setStateChangeListener(MjpegViewStateChangeListener listener) {
        this.listener = listener;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void run() {
        if (listener != null) {
            listener.onStreamDownloadStart();
        }
        while (run) {
            HttpURLConnection connection = null;
            BufferedInputStream bis = null;
            URL serverUrl;

            try {
                serverUrl = new URL(url);
                connection = (HttpURLConnection) serverUrl.openConnection();
                connection.setDoInput(true);
                connection.connect();

                if (listener != null) {
                    listener.onServerConnected();
                }

                String headerBoundary = DEFAULT_BOUNDARY_REGEX;

                try {
                    // Try to extract a boundary from HTTP header first.
                    // If the information is not presented, throw an exception and use default value instead.
                    String contentType = connection.getHeaderField("Content-Type");
                    if (contentType == null) {
                        throw new Exception("Unable to get content type");
                    }

                    String[] types = contentType.split(";");
                    if (types.length == 0) {
                        throw new Exception("Content type was empty");
                    }

                    String extractedBoundary = null;
                    for (String ct : types) {
                        String trimmedCt = ct.trim();
                        if (trimmedCt.startsWith("boundary=")) {
                            extractedBoundary = trimmedCt.substring(9); // Content after 'boundary='
                        }
                    }

                    if (extractedBoundary == null) {
                        throw new Exception("Unable to find mjpeg boundary.");
                    }

                    headerBoundary = extractedBoundary;
                } catch (Exception e) {
                    Log.w(tag, "Cannot extract a boundary string from HTTP header with message: " + e.getMessage() + ". Use a default value instead.");
                    if (listener != null) {
                        listener.onError(new MjpegViewError(MjpegViewError.ERROR_CODE_EXTRACT_BOUNDARY_FAILED, e.getMessage()));
                    }
                }

                // determine boundary pattern
                // use the whole header as separator in case boundary locate in difference chunks 创建一个正则表达式模式，用于查找MJPEG流中图像帧的分界点。
                Pattern pattern = Pattern.compile("--" + headerBoundary + "\\s+(.*)\\r\\n\\r\\n", Pattern.DOTALL);
                Matcher matcher;

                //创建一个缓冲输入流以从HTTP连接中读取数据。
                bis = new BufferedInputStream(connection.getInputStream());
                //创建一个字节数组，用于读取数据的缓冲区。
                byte[] read = new byte[CHUNK_SIZE];
                int readByte, boundaryIndex;
                String checkHeaderStr, boundary;

                //always keep reading images from server
                while (run) {
                    try {
                        //从输入流中读取数据并将其存储在read缓冲区中。
                        readByte = bis.read(read);
                        if (readByte == -1) {
                            break;
                        }
                        // 将读取的数据添加到当前图像的字节数组中。
                        addByte(read, 0, readByte, false);
                        checkHeaderStr = new String(currentImageBody, 0, currentImageBodyLength, StandardCharsets.US_ASCII);
                        matcher = pattern.matcher(checkHeaderStr); //使用正则表达式匹配器查找数据中的分界点。
                        if (matcher.find()) { //如果找到了分界点，就执行以下操作：
                            // delete and re-add because if body contains boundary, it means body is over one image already
                            // we want exact one image content
                            delByte(readByte); //删除之前读取的分界点之后的数据

                            boundary = matcher.group(0);
                            boundaryIndex = checkHeaderStr.indexOf(boundary);
                            boundaryIndex -= currentImageBodyLength;

                            if (boundaryIndex > 0) {
                                addByte(read, 0, boundaryIndex, false);
                            } else {
                                delByte(boundaryIndex);
                            }

                            Bitmap outputImg = BitmapFactory.decodeByteArray(currentImageBody, 0, currentImageBodyLength);
                            if (outputImg != null) {
                                if (run) {
                                    newFrame(outputImg); //如果位图不为空，就将其传递给newFrame()方法，可能是用于显示图像的回调。
                                }
                            } else {
                                Log.e(tag, "Read image error");
                            }

                            int headerIndex = boundaryIndex + boundary.length();

                            addByte(read, headerIndex, readByte - headerIndex, true);
                        }
                    } catch (Exception e) {
                        if (e.getMessage() != null) {
                            Log.w(tag, e.getMessage());
                        }
                        if (listener != null) {
                            listener.onError(new MjpegViewError(MjpegViewError.ERROR_CODE_READ_IMAGE_STREAM_FAILED, e.getMessage()));
                        }
                        break;
                    }
                }

            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.e(tag, e.getMessage());
                }
                if (listener != null) {
                    listener.onError(new MjpegViewError(MjpegViewError.ERROR_CODE_OPEN_CONNECTION_FAILED, e.getMessage()));
                }
            }

            try {
                if (bis != null) {
                    bis.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
                Log.i(tag, "disconnected with " + url);
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.e(tag, e.getMessage());
                }
            }

            if (msecWaitAfterReadImageError > 0) {
                try {
                    Thread.sleep(msecWaitAfterReadImageError);
                } catch (InterruptedException e) {
                    if (e.getMessage() != null) {
                        Log.e(tag, e.getMessage());
                    }
                }
            }

        }
    }
    private void addByte ( byte[] src, int srcPos, int length, boolean reset){
        if (reset) {
            System.arraycopy(src, srcPos, currentImageBody, 0, length);
            currentImageBodyLength = 0;
        } else {
            System.arraycopy(src, srcPos, currentImageBody, currentImageBodyLength, length);
        }
        currentImageBodyLength += length;
    }
    private void delByte(int del) {
        currentImageBodyLength -= del;
    }

    private void newFrame(Bitmap bitmap) {
        //setBitmap(bitmap);
//        if (newBitmapListener != null) {
//            newBitmapListener.onNewBitmap(bitmap);
//        }
        if (listener != null) {
            listener.onNewFrame(bitmap);
        }
    }
}
