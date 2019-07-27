package com.example.loginactivity.async;


import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * CustomMultipartFormEntity 에서 Progress listener 를 사용하기 위해 생성.
 * MultipartFormEntity 객체를 최종 생성하는데 필요한 변수 참조가 안되서
 * MultipartEntityBuilder 소스를 기반으로 재 구성함.
 */

public class CustomMultipartFormEntity implements HttpEntity {

    private final ProgressListener mListener;

    private final AbstractMultipartForm multipart;
    private final Header contentType;
    private final long contentLength;

    CustomMultipartFormEntity(
            final AbstractMultipartForm multipart,
            final String contentType,
            final long contentLength,
            final ProgressListener listener) {
        super();
        this.multipart = multipart;
        this.contentType = new BasicHeader(HTTP.CONTENT_TYPE, contentType);
        this.contentLength = contentLength;
        this.mListener = listener;
    }

    AbstractMultipartForm getMultipart() {
        return this.multipart;
    }

    public boolean isRepeatable() {
        return this.contentLength != -1;
    }

    public boolean isChunked() {
        return !isRepeatable();
    }

    public boolean isStreaming() {
        return !isRepeatable();
    }

    public long getContentLength() {
        return this.contentLength;
    }

    public Header getContentType() {
        return this.contentType;
    }

    public Header getContentEncoding() {
        return null;
    }

    public void consumeContent()
            throws IOException, UnsupportedOperationException {
        if (isStreaming()) {
            throw new UnsupportedOperationException(
                    "Streaming entity does not implement #consumeContent()");
        }
    }

    public InputStream getContent() throws IOException {
        throw new UnsupportedOperationException(
                "Multipart form entity does not implement #getContent()");
    }

    public void writeTo(final OutputStream outstream) throws IOException {
        //this.multipart.writeTo(outstream);
        this.multipart.writeTo(new CountingOutputStream(outstream, this.mListener, getContentLength()));
    }

    public static interface ProgressListener {
        void transferred(long num, float totalSize);
    }

    public static class CountingOutputStream extends FilterOutputStream {

        private final ProgressListener listener;
        private long transferred;
        private float totalSize;

        public CountingOutputStream(final OutputStream out, final ProgressListener listener, final long totalSize) {
            super(out);
            this.listener = listener;
            this.transferred = 0;
            this.totalSize = totalSize;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            this.transferred += len;
            this.listener.transferred(this.transferred, totalSize);
        }

        public void write(int b) throws IOException {
            out.write(b);
            this.transferred++;
            this.listener.transferred(this.transferred, totalSize);
        }
    }
}
