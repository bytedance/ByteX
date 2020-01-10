package com.ss.android.ugc.bytex.example.closeable;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CloseableCheckTest implements Closeable {
    public String getString(int index) throws IOException {
        InputStream in =new FileInputStream(""+index);
        return in != null ? inputStreamToString(in) : null;
    }
    private static String inputStreamToString(InputStream in) throws IOException {
        return readFully(new InputStreamReader(in, UTF_8));
    }
    public static String readFully(Reader reader) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, count);
            }
            return writer.toString();
        } finally {
            reader.close();
        }
    }


    static String[] mVersion;
    public static String[] getVersion() {
        if (mVersion == null) {
            String[] version = new String[]{"null", "null", "null", "null"};
            String str1 = "/proc/version";

            try {
                FileReader localFileReader = new FileReader(str1);
                BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
                String str2 = localBufferedReader.readLine();
                String[] arrayOfString = str2.split("\\s+");
                version[0] = arrayOfString[2];
                localBufferedReader.close();
            } catch (IOException var6) {
                ;
            }

            version[1] = Build.VERSION.RELEASE;
            version[2] = Build.MODEL;
            version[3] = Build.DISPLAY;
            mVersion = version;
        }

        return mVersion;
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static final String TAG = "CloseableCheckTest";

    private static String readTextFile(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return null;
        }
        Log.v(TAG, "all file path : " + filePaths.toString());

        StringBuilder content = new StringBuilder(); //文件内容字符串
        for (String filePath : filePaths) {
            if (!TextUtils.isEmpty(filePath)) {
                content = new StringBuilder();
                File file = new File(filePath);
                if (file.isFile()) {
                    Log.v(TAG, "available filePath: " + filePath);
                    if (file.isFile()) {
                        InputStream inputStream = null;
                        InputStreamReader streamReader = null;
                        BufferedReader buffreader = null;
                        try {
                            inputStream = new FileInputStream(file);
                            streamReader = new InputStreamReader(inputStream);
                            buffreader = new BufferedReader(streamReader);
                            String line;
                            while ((line = buffreader.readLine()) != null) {
                                content.append(line);
                                content.append("\n");
                            }
                            if (!TextUtils.isEmpty(content)) {
                                break;
                            }
                        } catch (java.io.FileNotFoundException e) {
                            Log.d(TAG, "The File doesn't not exist.");
                        } catch (IOException e) {
                            Log.d(TAG, e.getMessage());
                        } finally {
                            try {
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                                if (streamReader != null) {
                                    streamReader.close();
                                }
                                if (buffreader != null) {
                                    buffreader.close();
                                }
                            } catch (IOException ignore) {

                            }

                        }
                    }
                }
            }
        }

        return content.toString();
    }


    public void test() throws IOException {
        String urlStr = "https://www.baidu.com/";
        URL url = new URL(urlStr);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.connect();
        if (urlConnection.getResponseCode() == 200) {
            InputStream inputStream = urlConnection.getInputStream();
            byte[] temp = new byte[1024];
            int length;
            while ((length = inputStream.read(temp)) != -1) {
                System.out.println(length);
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    BufferedReader mInputStream;
    InputStream mRe;

    public InputStream test2() throws IOException {
        String urlStr = "https://www.baidu.com/";
        URL url = new URL(urlStr);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.connect();
        if (urlConnection.getResponseCode() == 200) {
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(new BufferedInputStream(urlConnection.getInputStream())));
            mInputStream = new BufferedReader(new InputStreamReader(new BufferedInputStream(urlConnection.getInputStream())));
            BufferedInputStream re = new BufferedInputStream(urlConnection.getInputStream());
            inputStream.close();
            mInputStream.close();
            return re;
        }
        return mRe;
    }

    public InputStream test3() {
        try {
            String urlStr = "https://www.baidu.com/";
            URL url = new URL(urlStr);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            if (urlConnection.getResponseCode() == 200) {
                BufferedReader inputStream = new BufferedReader(new InputStreamReader(new BufferedInputStream(urlConnection.getInputStream())));
                mInputStream = new BufferedReader(new InputStreamReader(new BufferedInputStream(urlConnection.getInputStream())));
                BufferedInputStream re = new BufferedInputStream(urlConnection.getInputStream());
                inputStream.close();
                re.close();
                mInputStream.close();
                return re;
            }
            return mRe;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println();
        }
        return null;
    }


    public static String inputStream2String(InputStream inputStream, String encoding) {
        InputStreamReader reader = null;
        StringWriter writer = new StringWriter();
        try {
            if (encoding == null || "".equals(encoding.trim())) {
                reader = new InputStreamReader(inputStream);
            } else {
                reader = new InputStreamReader(inputStream, encoding);
            }
            //将输入流写入输出流
            char[] buffer = new char[8192];
            int n = 0;
            while (-1 != (n = reader.read(buffer))) {
                writer.write(buffer, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        //返回转换结果
        if (writer != null)
            return writer.toString();
        else return null;
    }

    public static boolean writeFile(InputStream is, String path,
                                    boolean recreate) {
        boolean res = false;
        File f = new File(path);
        FileOutputStream fos = null;
        try {
            if (recreate && f.exists()) {
                f.delete();
            }
            if (!f.exists() && null != is) {
                File parentFile = new File(f.getParent());
                parentFile.mkdirs();
                int count = -1;
                byte[] buffer = new byte[1024];
                fos = new FileOutputStream(f);
                while ((count = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, count);
                }
                res = true;
            }
        } catch (Exception e) {

        } finally {
            close(fos);
            close(is);
        }
        return res;
    }

    public static boolean close(Closeable io) {
        if (io != null) {
            try {
                io.close();
            } catch (IOException e) {

            }
        }
        return true;
    }


    public static boolean writeFile(byte[] content, String path, boolean append) {
        boolean res = false;
        File f = new File(path);
        RandomAccessFile raf = null;
        try {
            if (f.exists()) {
                if (!append) {
                    f.delete();
                    f.createNewFile();
                }
            } else {
                f.createNewFile();
            }
            if (f.canWrite()) {
                raf = new RandomAccessFile(f, "rw");
                raf.seek(raf.length());
                raf.write(content);
                res = true;
            }
        } catch (Exception e) {
        } finally {
            close(raf);
        }
        return res;
    }

    @Override
    public void close() throws IOException {

    }


    public static byte[] readResponse(boolean use_gzip, int maxLength, InputStream in, int[] out_off) throws IOException {
        if (maxLength <= 0)
            maxLength = 100;
        if (maxLength < 1024 * 1024)
            maxLength = 1024 * 1024;
        if (in == null) {
            return null;
        }
        try {
            if (use_gzip) {
                in = new GZIPInputStream(in);
            }
            byte[] buf = new byte[8 * 1024];
            int n = 0;
            int off = 0;
            int count = 4 * 1024;
            while (true) {
                // some gateway pack wrong 'chunked' data, without crc32 and isize
                try {
                    if (off + count > buf.length) {
                        byte[] newbuf = new byte[buf.length * 2];
                        System.arraycopy(buf, 0, newbuf, 0, off);
                        buf = newbuf;
                    }
                    n = in.read(buf, off, count);
                    if (n > 0) {
                        off += n;
                    } else {
                        break;
                    }
                    if (maxLength > 0 && off > maxLength) {
                        return null;
                    }
                } catch (EOFException e) {
                    if (use_gzip && off > 0) {
                        break;
                    } else {
                        throw e;
                    }
                } catch (IOException e) {
                    String msg = e.getMessage();
                    if (use_gzip && off > 0 && ("CRC mismatch".equals(msg) || "Size mismatch".equals(msg))) {
                        break;
                    } else {
                        throw e;
                    }
                }
            }
            if (off > 0) {
                out_off[0] = off;
                return buf;
            } else {
                return null;
            }
        } finally {
            safeClose(in);
        }
    }

    public static void safeClose(Closeable c) {
        safeClose(c, null);
    }

    private static void safeClose(Closeable c, String msg) {
        try {
            if (c != null) {
                c.close();
            }
            if (c != null) {
                c.close();
            }else{
                System.out.println(msg);
            }
        } catch (Exception e) {
        }
    }


    public byte[] bytes() {
        final ByteArrayOutputStream output = byteStream();
        return output.toByteArray();
    }

    protected ByteArrayOutputStream byteStream() {
        final int size = hashCode();
        if (size > 0)
            return new ByteArrayOutputStream(size);
        else
            return new ByteArrayOutputStream();
    }

}
