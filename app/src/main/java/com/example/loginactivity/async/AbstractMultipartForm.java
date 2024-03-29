package com.example.loginactivity.async;

import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MinimalField;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.util.Args;
import org.apache.http.util.ByteArrayBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;

/**
 * CustomMultipartFormEntity 에서 Progress listener 를 사용하기 위해 생성.
 * MultipartFormEntity 객체를 최종 생성하는데 필요한 변수 참조가 안되서
 * MultipartEntityBuilder 소스를 기반으로 재 구성함.
 * 라이브러리에 있는 AbstractMultipartForm 이 private 이라서 public 으로 생성
 */

public abstract class AbstractMultipartForm {
    private static ByteArrayBuffer encode(
            final Charset charset, final String string) {
        final ByteBuffer encoded = charset.encode(CharBuffer.wrap(string));
        final ByteArrayBuffer bab = new ByteArrayBuffer(encoded.remaining());
        bab.append(encoded.array(), encoded.position(), encoded.remaining());
        return bab;
    }

    private static void writeBytes(
            final ByteArrayBuffer b, final OutputStream out) throws IOException {
        out.write(b.buffer(), 0, b.length());
    }

    private static void writeBytes(
            final String s, final Charset charset, final OutputStream out) throws IOException {
        final ByteArrayBuffer b = encode(charset, s);
        writeBytes(b, out);
    }

    private static void writeBytes(
            final String s, final OutputStream out) throws IOException {
        final ByteArrayBuffer b = encode(MIME.DEFAULT_CHARSET, s);
        writeBytes(b, out);
    }

    protected static void writeField(
            final MinimalField field, final OutputStream out) throws IOException {
        writeBytes(field.getName(), out);
        writeBytes(FIELD_SEP, out);
        writeBytes(field.getBody(), out);
        writeBytes(CR_LF, out);
    }

    protected static void writeField(
            final MinimalField field, final Charset charset, final OutputStream out) throws IOException {
        writeBytes(field.getName(), charset, out);
        writeBytes(FIELD_SEP, out);
        writeBytes(field.getBody(), charset, out);
        writeBytes(CR_LF, out);
    }

    private static final ByteArrayBuffer FIELD_SEP = encode(MIME.DEFAULT_CHARSET, ": ");
    private static final ByteArrayBuffer CR_LF = encode(MIME.DEFAULT_CHARSET, "\r\n");
    private static final ByteArrayBuffer TWO_DASHES = encode(MIME.DEFAULT_CHARSET, "--");

    private final String subType;
    protected final Charset charset;
    private final String boundary;

    /**
     * Creates an instance with the specified settings.
     *
     * @param subType MIME subtype - must not be {@code null}
     * @param charset the character set to use. May be {@code null}, in which case {@link MIME#DEFAULT_CHARSET} - i.e. US-ASCII - is used.
     * @param boundary to use  - must not be {@code null}
     * @throws IllegalArgumentException if charset is null or boundary is null
     */
    public AbstractMultipartForm(final String subType, final Charset charset, final String boundary) {
        super();
        Args.notNull(subType, "Multipart subtype");
        Args.notNull(boundary, "Multipart boundary");
        this.subType = subType;
        this.charset = charset != null ? charset : MIME.DEFAULT_CHARSET;
        this.boundary = boundary;
    }

    public AbstractMultipartForm(final String subType, final String boundary) {
        this(subType, null, boundary);
    }

    public String getSubType() {
        return this.subType;
    }

    public Charset getCharset() {
        return this.charset;
    }

    public abstract List<FormBodyPart> getBodyParts();

    public String getBoundary() {
        return this.boundary;
    }

    void doWriteTo(
            final OutputStream out,
            final boolean writeContent) throws IOException {

        final ByteArrayBuffer boundary = encode(this.charset, getBoundary());
        for (final FormBodyPart part: getBodyParts()) {
            writeBytes(TWO_DASHES, out);
            writeBytes(boundary, out);
            writeBytes(CR_LF, out);

            formatMultipartHeader(part, out);

            writeBytes(CR_LF, out);

            if (writeContent) {
                part.getBody().writeTo(out);
            }
            writeBytes(CR_LF, out);
        }
        writeBytes(TWO_DASHES, out);
        writeBytes(boundary, out);
        writeBytes(TWO_DASHES, out);
        writeBytes(CR_LF, out);
    }

    /**
     * Write the multipart header fields; depends on the style.
     */
    protected abstract void formatMultipartHeader(
            final FormBodyPart part,
            final OutputStream out) throws IOException;

    /**
     * Writes out the content in the multipart/form encoding. This method
     * produces slightly different formatting depending on its compatibility
     * mode.
     */
    public void writeTo(final OutputStream out) throws IOException {
        doWriteTo(out, true);
    }

    /**
     * Determines the total length of the multipart content (content length of
     * individual parts plus that of extra elements required to delimit the parts
     * from one another). If any of the @{link BodyPart}s contained in this object
     * is of a streaming entity of unknown length the total length is also unknown.
     * <p/>
     * This method buffers only a small amount of data in order to determine the
     * total length of the entire entity. The content of individual parts is not
     * buffered.
     *
     * @return total length of the multipart entity if known, <code>-1</code>
     *   otherwise.
     */
    public long getTotalLength() {
        long contentLen = 0;
        for (final FormBodyPart part: getBodyParts()) {
            final ContentBody body = part.getBody();
            final long len = body.getContentLength();
            if (len >= 0) {
                contentLen += len;
            } else {
                return -1;
            }
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            doWriteTo(out, false);
            final byte[] extra = out.toByteArray();
            return contentLen + extra.length;
        } catch (final IOException ex) {
            // Should never happen
            return -1;
        }
    }
}
