package com.cansecs.workholes;

/**
 * A streamer class allows share local download media to browser within the same LAN
 * Takes IP address
 */

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import android.os.Looper;
import android.util.Log;


/**
 * @author      Credit from internet
 * String address - IP
 * String root the document root
 */
public class localStreamer implements Runnable {

    private static final String TAG = "Streamer";

    private String address;
    private Thread thread;
    private boolean isRunning;
    private static ServerSocket socket;
    private int port;
    private String document_root;
    private int maxcount=5;
    private List<Thread> threads = new ArrayList<>();
    private boolean lastRunning = false;

    public void setMax(int count){
        this.maxcount = count;
    }

    public localStreamer(String root) {
        document_root = (root==null||root.isEmpty())?"/":root;
        // Create listening socket
    }

    private void init(String address,int port){
        boolean redo=true;
        if (socket !=null && !socket.isClosed()){
            redo=false;
            if ( this.address != address){ // address changed
                try{
                    socket.close();
                } catch (IOException e) {
                    Log.d(TAG,"Failed to close socket");
                }
                redo=true;
            }
        }
        if ( redo){
            socket = null;
            try {

                InetAddress inet = InetAddress.getByName(address);
                this.address = address;
                socket = new ServerSocket(port, 0, inet);
                socket.setSoTimeout(5000);
                this.port = socket.getLocalPort();
            } catch (UnknownHostException e) { // impossible
            } catch (IOException e) {
                Log.e(TAG, "IOException initializing server", e);
            }

        }
    }

    public void onPause(){
        lastRunning = isRunning();
        stop();
    }

    public void onResume(){
        if ( lastRunning){
            if ( !isRunning()) init(address,port);
        }
    }

    public boolean isRunning(){
        if ( socket !=null && socket.isClosed()){
            stop();
        }
        return this.isRunning;
    }
    public String getUrl() {
        String url = String.format("http://%s:%d/", address, port);

        return url;
    }

    public void start(String address, int port) {
        init(address,port);
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        isRunning = false;
        /*if( socket !=null){
            try{
                socket.close();
            } catch (IOException e) {
                Log.d(TAG,"Cannot close socket",e);
            }
            socket = null;
        }*/
        if ( thread != null ) {
            thread.interrupt();
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (Thread t:threads
                    ) {
                if (t.isAlive()){
                    t.interrupt();
                }
            }
            threads.clear();
        }
    }

    private void cleanUp(){
        for (Thread tt:threads
                ) {
            if (!tt.isAlive()){
                threads.remove(tt);
            }
        }
    }
    @Override
    public void run() {
        Looper.prepare();
        isRunning = true;
        init(this.address,this.port);
        while (isRunning) {
            try {
                cleanUp();
                if (threads.size() > this.maxcount){
                    Log.d(TAG,String.format("Too many threads running now:%s -> %s",threads.size(),this.maxcount));
                    continue;
                }
                final Socket client = socket.accept();
                if (client == null) {
                    continue;
                }
                Log.d(TAG, String.format("client:%s connected",client));
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StreamToMediaPlayerTask task = new StreamToMediaPlayerTask(client);
                        try {
                            if (task.processRequest()) {
                                try {
                                    task.execute();
                                }catch(FileNotFoundException e){
                                    Log.d(TAG,"Cannot found",e);
                                }
                            }
                        } catch (IOException e) {
                            Log.d(TAG,"IO Exception in thread",e);
                        }

                    }
                });
                t.start();

            } catch (SocketTimeoutException e) {
                // Do nothing
            } catch ( SocketException e){
                init(this.address,this.port);
            }catch (IOException e) {
                Log.e(TAG, "Error connecting to client", e);
            }
        }
        Log.d(TAG, "Proxy interrupted. Shutting down.");
    }




    private class StreamToMediaPlayerTask {

        Socket client;
        long cbSkip;
        private Properties parameters;
        private Properties request;
        private Properties requestHeaders;
        private String filePath;

        public StreamToMediaPlayerTask(Socket client) {
            this.client = client;
        }

        public boolean processRequest() throws IOException {
            // Read HTTP headers
            InputStream is = client.getInputStream();
            final int bufferSize = 8192;
            byte[] buffer = new byte[bufferSize];
            int splitByte = 0;
            int readLength = 0;
            {
                int read = is.read(buffer, 0, bufferSize);
                while (read > 0) {
                    readLength += read;
                    splitByte = findHeaderEnd(buffer, readLength);
                    if (splitByte > 0)
                        break;
                    read = is.read(buffer, readLength, bufferSize - readLength);
                }
            }

            // Create a BufferedReader for parsing the header.
            ByteArrayInputStream hbis = new ByteArrayInputStream(buffer, 0, readLength);
            BufferedReader hin = new BufferedReader(new InputStreamReader(hbis));
            request = new Properties();
            parameters = new Properties();
            requestHeaders = new Properties();

            try {
                decodeHeader(hin, request, requestHeaders);
            } catch (InterruptedException e1) {
                Log.e(TAG, "Exception: " + e1.getMessage());
                e1.printStackTrace();
            }
            for (Map.Entry<Object, Object> e : requestHeaders.entrySet()) {
                Log.i(TAG, "Header: " + e.getKey() + " : " + e.getValue());
            }

            String range = requestHeaders.getProperty("range");
            if (range != null) {
                Log.i(TAG, "range is: " + range);
                range = range.substring(6);
                int charPos = range.indexOf('-');
                if (charPos > 0) {
                    range = range.substring(0, charPos);
                }
                cbSkip = Long.parseLong(range);
                Log.i(TAG, "range found!! " + cbSkip);
            }
            String method= (String)request.get("method");
            if( method == null || !method.equals("GET")) {
                Log.e(TAG, "Only GET is supported");
                return false;
            }

            filePath = request.getProperty("uri");
            try {
                filePath= URLDecoder.decode(filePath,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.d(TAG,"Unspported URL"+filePath,e);
            }

            return true;
        }

        protected void execute() throws FileNotFoundException {
            ExternalResourceDataSource dataSource = new ExternalResourceDataSource(new File(document_root,filePath));
            //File dataSource = new File(filePath);
            long fileSize = dataSource.getContentLength();

            String headers = "";
            boolean partial=false;
            if (cbSkip <=0 ) cbSkip=0;
            else partial = true;

            headers += "HTTP/1.1 "+(partial?"206 Partial Content":"200 OK")+"\r\n";
            headers += "Content-Type: " + dataSource.getContentType() + "\r\n";
            headers += "Accept-Ranges: bytes\r\n";
            headers += "Content-Length: " + (fileSize - cbSkip) + "\r\n";
            if (partial ) headers += "Content-Range: bytes " + cbSkip + "-" + (fileSize - 1) + "/" + fileSize + "\r\n";
            headers += "Connection: Keep-Alive\r\n";
            headers += "\r\n";


            Log.i(TAG, "headers: " + headers);

            OutputStream output = null;
            byte[] buff = new byte[64 * 1024];
            try {
                output = new BufferedOutputStream(client.getOutputStream(), 32 * 1024);
                output.write(headers.getBytes());
                InputStream data = dataSource.getInputStream();

                dataSource.skipFully(data, cbSkip);//try to skip as much as possible

                // Loop as long as there's stuff to send and client has not closed
                int cbRead;
                while (!client.isClosed() && (cbRead = data.read(buff, 0, buff.length)) != -1) {
                    output.write(buff, 0, cbRead);
                }
            }
            catch (SocketException socketException) {
                Log.e(TAG, "SocketException() thrown, proxy client has probably closed. This can exit harmlessly");
            }
            catch (Exception e) {
                Log.e(TAG, "Exception thrown from streaming task:");
                Log.e(TAG, e.getClass().getName() + " : " + e.getLocalizedMessage());
            }

            // Cleanup
            try {
                if (output != null) {
                    output.close();
                }
                client.close();
            }
            catch (IOException e) {
                Log.e(TAG, "IOException while cleaning up streaming task:");
                Log.e(TAG, e.getClass().getName() + " : " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        /**
         * Find byte index separating header from body. It must be the last byte of
         * the first two sequential new lines.
         **/
        private int findHeaderEnd(final byte[] buf, int rlen) {
            int splitbyte = 0;
            while (splitbyte + 3 < rlen) {
                if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n'
                        && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n')
                    return splitbyte + 4;
                splitbyte++;
            }
            return 0;
        }


        /**
         * Decodes the sent headers and loads the data into java Properties' key -
         * value pairs
         **/
        private void decodeHeader(BufferedReader in, Properties pre,
                                  Properties header) throws InterruptedException {
            try {
                // Read the request line
                String inLine = in.readLine();
                if (inLine == null)
                    return;
                StringTokenizer st = new StringTokenizer(inLine);
                if (!st.hasMoreTokens())
                    Log.e(TAG,
                            "BAD REQUEST: Syntax error. Usage: GET /example/file.html");

                String method = st.nextToken();
                pre.put("method", method);

                if (!st.hasMoreTokens())
                    Log.e(TAG,
                            "BAD REQUEST: Missing URI. Usage: GET /example/file.html");

                String uri = st.nextToken();

                // If there's another token, it's protocol version,
                // followed by HTTP headers. Ignore version but parse headers.
                // NOTE: this now forces header names lowercase since they are
                // case insensitive and vary by client.
                if (st.hasMoreTokens()) {
                    String line = in.readLine();
                    while (line != null && line.trim().length() > 0) {
                        int p = line.indexOf(':');
                        if (p >= 0)
                            header.put(line.substring(0, p).trim().toLowerCase(),
                                    line.substring(p + 1).trim());
                        line = in.readLine();
                    }
                }

                pre.put("uri", uri);
            } catch (IOException ioe) {
                Log.e(TAG,
                        "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            }
        }



    }



    /**
     * provides meta-data and access to a stream for resources on SD card.
     */
    protected class ExternalResourceDataSource {

        private final File mFileResource;
        long contentLength;

        public ExternalResourceDataSource(File resource) throws FileNotFoundException{
            //it is your duty to ensure that the file exists
            mFileResource = resource;
            if (resource.exists()) {
                contentLength = mFileResource.length();
                Log.i(TAG, "path: " + mFileResource.getPath() + ", exists: " + mFileResource.exists() + ", length: " + contentLength);
            }else{
                contentLength = 0;
                Log.i(TAG,String.format("%s dosn't exist",resource));
                throw new FileNotFoundException(resource.getAbsolutePath());
            }
        }

        /**
         * Discards {@code n} bytes of data from the input stream. This method
         * will block until the full amount has been skipped. Does not close the
         * stream.
         *
         * @param in the input stream to read from
         * @param n the number of bytes to skip
         * @throws EOFException if this stream reaches the end before skipping all
         *     the bytes
         * @throws IOException if an I/O error occurs, or the stream does not
         *     support skipping
         */
        public void skipFully(InputStream in, long n) throws IOException {
            while (n > 0) {
                long amt = in.skip(n);
                if (amt == 0) {
                    // Force a blocking read to avoid infinite loop
                    if (in.read() == -1) {
                        throw new EOFException();
                    }
                    n--;
                } else {
                    n -= amt;
                }
            }
        }


        /**
         * Returns a MIME-compatible content type (e.g. "text/html") for the
         * resource. This method must be implemented.
         *
         * @return A MIME content type.
         */
        public String getContentType() {
            return "video/mp4";
        }

        /**
         * Returns the length of resource in bytes.
         *
         * By default this returns -1, which causes no content-type header to be
         * sent to the client. This would make sense for a stream content of
         * unknown or undefined length. If your resource has a defined length
         * you should override this method and return that.
         *
         * @return The length of the resource in bytes.
         */
        public long getContentLength() {
            return contentLength;
        }

        public InputStream getInputStream() {
            try {
                return new FileInputStream(mFileResource);
            } catch (IOException e) {
                Log.e(TAG, "failed to create input stream", e);
            }

            return null;
        }
    }
}