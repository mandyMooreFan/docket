package com.mbeebe.docket.leaving;

/**
 * One image that belongs in the archive, named by the address it lives at rather
 * than the bytes it holds — a contributor never touches bytes.
 *
 * <p>{@code path} is filled in by {@link Archive} once it has fetched the image
 * and knows its type, and it is fetched through {@code Images.serve} with the
 * departing Member as the viewer. That is the whole §51 argument: an image only
 * reaches the archive if the /images/{id} audience guard would already hand it to
 * this member on request, so the archive can never be a way round the guard — it
 * is the same guard, asked the same question, by the same person.
 */
public record ExportMedia(long imageId, String kind, String path) {

    public static ExportMedia of(long imageId, String kind) {
        return new ExportMedia(imageId, kind, null);
    }

    ExportMedia at(String path) {
        return new ExportMedia(imageId, kind, path);
    }
}
