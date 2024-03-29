package com.example.loginactivity.async;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.Args;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * MultipartEntityBuilder 에서 Progress listener 를 사용하기 위해 생성.
 * MultipartFormEntity 객체를 최종 생성하는데 필요한 변수 참조가 안되서
 * MultipartEntityBuilder 소스를 기반으로 재 구성함.
 */

public class CustomMultipartEntityBuilder {
    /**
     * The pool of ASCII chars to be used for generating a multipart boundary.
     */
    private final static char[] MULTIPART_CHARS =
            "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    .toCharArray();

    private final static String DEFAULT_SUBTYPE = "form-data";

    private String subType = DEFAULT_SUBTYPE;
    private HttpMultipartMode mode = HttpMultipartMode.STRICT;
    private String boundary = null;
    private Charset charset = null;
    private List<FormBodyPart> bodyParts = null;
    CustomMultipartFormEntity.ProgressListener mListener;

    public static CustomMultipartEntityBuilder create() {
        return new CustomMultipartEntityBuilder();
    }

    CustomMultipartEntityBuilder() {
        super();
    }

    CustomMultipartEntityBuilder(CustomMultipartFormEntity.ProgressListener listener) {
        super();
        mListener = listener;
    }

    public CustomMultipartEntityBuilder setMode(final HttpMultipartMode mode) {
        this.mode = mode;
        return this;
    }

    public CustomMultipartEntityBuilder setLaxMode() {
        this.mode = HttpMultipartMode.BROWSER_COMPATIBLE;
        return this;
    }

    public CustomMultipartEntityBuilder setStrictMode() {
        this.mode = HttpMultipartMode.STRICT;
        return this;
    }

    public CustomMultipartEntityBuilder setBoundary(final String boundary) {
        this.boundary = boundary;
        return this;
    }

    public CustomMultipartEntityBuilder setCharset(final Charset charset) {
        this.charset = charset;
        return this;
    }

    CustomMultipartEntityBuilder addPart(final FormBodyPart bodyPart) {
        if (bodyPart == null) {
            return this;
        }
        if (this.bodyParts == null) {
            this.bodyParts = new ArrayList<FormBodyPart>();
        }
        this.bodyParts.add(bodyPart);
        return this;
    }

    public CustomMultipartEntityBuilder addPart(final String name, final ContentBody contentBody) {
        Args.notNull(name, "Name");
        Args.notNull(contentBody, "Content body");
        return addPart(new FormBodyPart(name, contentBody));
    }

    public CustomMultipartEntityBuilder addTextBody(
            final String name, final String text, final ContentType contentType) {
        return addPart(name, new StringBody(text, contentType));
    }

    public CustomMultipartEntityBuilder addTextBody(
            final String name, final String text) {
        return addTextBody(name, text, ContentType.DEFAULT_TEXT);
    }

    public CustomMultipartEntityBuilder addBinaryBody(
            final String name, final byte[] b, final ContentType contentType, final String filename) {
        return addPart(name, new ByteArrayBody(b, contentType, filename));
    }

    public CustomMultipartEntityBuilder addBinaryBody(
            final String name, final byte[] b) {
        return addBinaryBody(name, b, ContentType.DEFAULT_BINARY, null);
    }

    public CustomMultipartEntityBuilder addBinaryBody(
            final String name, final File file, final ContentType contentType, final String filename) {
        return addPart(name, new FileBody(file, contentType, filename));
    }

    public CustomMultipartEntityBuilder addBinaryBody(
            final String name, final File file) {
        return addBinaryBody(name, file, ContentType.DEFAULT_BINARY, file != null ? file.getName() : null);
    }

    public CustomMultipartEntityBuilder addBinaryBody(
            final String name, final InputStream stream, final ContentType contentType,
            final String filename) {
        return addPart(name, new InputStreamBody(stream, contentType, filename));
    }

    public CustomMultipartEntityBuilder addBinaryBody(final String name, final InputStream stream) {
        return addBinaryBody(name, stream, ContentType.DEFAULT_BINARY, null);
    }

    private String generateContentType(
            final String boundary,
            final Charset charset) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("multipart/form-data; boundary=");
        buffer.append(boundary);
        if (charset != null) {
            buffer.append("; charset=");
            buffer.append(charset.name());
        }
        return buffer.toString();
    }

    private String generateBoundary() {
        final StringBuilder buffer = new StringBuilder();
        final Random rand = new Random();
        final int count = rand.nextInt(11) + 30; // a random size from 30 to 40
        for (int i = 0; i < count; i++) {
            buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
        }
        return buffer.toString();
    }

    CustomMultipartFormEntity buildEntity() {
        final String st = subType != null ? subType : DEFAULT_SUBTYPE;
        final Charset cs = charset;
        final String b = boundary != null ? boundary : generateBoundary();
        final List<FormBodyPart> bps = bodyParts != null ? new ArrayList<FormBodyPart>(bodyParts) :
                Collections.<FormBodyPart>emptyList();
        final HttpMultipartMode m = mode != null ? mode : HttpMultipartMode.STRICT;
        final AbstractMultipartForm form;
        switch (m) {
            case BROWSER_COMPATIBLE:
                form = new HttpBrowserCompatibleMultipart(st, cs, b, bps);
                break;
            case RFC6532:
                form = new HttpRFC6532Multipart(st, cs, b, bps);
                break;
            default:
                form = new HttpStrictMultipart(st, cs, b, bps);
        }
        return new CustomMultipartFormEntity(form, generateContentType(b, cs), form.getTotalLength(), mListener);
    }

    public HttpEntity build() {
        return buildEntity();
    }
}
