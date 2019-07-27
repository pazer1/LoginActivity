package com.example.loginactivity.async;

import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.Header;
import org.apache.http.entity.mime.MinimalField;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by lyang on 2017-08-07.
 */

public class HttpStrictMultipart extends AbstractMultipartForm {

    private final List<FormBodyPart> parts;

    public HttpStrictMultipart(
            final String subType,
            final Charset charset,
            final String boundary,
            final List<FormBodyPart> parts) {
        super(subType, charset, boundary);
        this.parts = parts;
    }

    @Override
    public List<FormBodyPart> getBodyParts() {
        return this.parts;
    }

    @Override
    protected void formatMultipartHeader(
            final FormBodyPart part,
            final OutputStream out) throws IOException {

        // For strict, we output all fields with MIME-standard encoding.
        final Header header = part.getHeader();
        for (final MinimalField field: header) {
            writeField(field, out);
        }
    }

}
