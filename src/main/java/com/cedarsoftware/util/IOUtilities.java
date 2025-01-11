package com.cedarsoftware.util;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Utility class providing robust I/O operations with built-in error handling and resource management.
 * <p>
 * This class simplifies common I/O tasks such as:
 * </p>
 * <ul>
 *   <li>Stream transfers and copying</li>
 *   <li>Resource closing and flushing</li>
 *   <li>Byte array compression/decompression</li>
 *   <li>URL connection handling</li>
 *   <li>File operations</li>
 * </ul>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Automatic buffer management for optimal performance</li>
 *   <li>GZIP and Deflate compression support</li>
 *   <li>Silent exception handling for close/flush operations</li>
 *   <li>Progress tracking through callback mechanism</li>
 *   <li>Support for XML stream operations</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Copy file to output stream
 * try (InputStream fis = Files.newInputStream(Paths.get("input.txt"))) {
 *     try (OutputStream fos = Files.newOutputStream(Paths.get("output.txt"))) {
 *         IOUtilities.transfer(fis, fos);
 *     }
 * }
 *
 * // Compress byte array
 * byte[] compressed = IOUtilities.compressBytes(originalBytes);
 * byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
 * }</pre>
 *
 * @author Ken Partlow
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class IOUtilities
{
    private static final int TRANSFER_BUFFER = 32768;

    private IOUtilities() { }

    /**
     * Gets an appropriate InputStream from a URLConnection, handling compression if necessary.
     * <p>
     * This method automatically detects and handles various compression encodings:
     * </p>
     * <ul>
     *   <li>GZIP ("gzip" or "x-gzip")</li>
     *   <li>DEFLATE ("deflate")</li>
     * </ul>
     * <p>
     * The returned stream is always buffered for optimal performance.
     * </p>
     *
     * @param c the URLConnection to get the input stream from
     * @return a buffered InputStream, potentially wrapped with a decompressing stream
     * @throws IOException if an I/O error occurs
     */
    public static InputStream getInputStream(URLConnection c) throws IOException {
        InputStream is = c.getInputStream();
        String enc = c.getContentEncoding();

        if ("gzip".equalsIgnoreCase(enc) || "x-gzip".equalsIgnoreCase(enc)) {
            is = new GZIPInputStream(is, TRANSFER_BUFFER);
        } else if ("deflate".equalsIgnoreCase(enc)) {
            is = new InflaterInputStream(is, new Inflater(), TRANSFER_BUFFER);
        }

        return new BufferedInputStream(is);
    }

    /**
     * Transfers the contents of a File to a URLConnection's output stream.
     * <p>
     * Progress can be monitored and the transfer can be cancelled through the callback interface.
     * </p>
     *
     * @param f the source File to transfer
     * @param c the destination URLConnection
     * @param cb optional callback for progress monitoring and cancellation (may be null)
     * @throws Exception if any error occurs during the transfer
     */
    public static void transfer(File f, URLConnection c, TransferCallback cb) throws Exception {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new BufferedInputStream(Files.newInputStream(f.toPath()));
            out = new BufferedOutputStream(c.getOutputStream());
            transfer(in, out, cb);
        } finally {
            close(in);
            close(out);
        }
    }

    /**
     * Transfers the contents of a URLConnection's input stream to a File.
     * <p>
     * Progress can be monitored and the transfer can be cancelled through the callback interface.
     * Automatically handles compressed streams.
     * </p>
     *
     * @param c the source URLConnection
     * @param f the destination File
     * @param cb optional callback for progress monitoring and cancellation (may be null)
     * @throws Exception if any error occurs during the transfer
     */
    public static void transfer(URLConnection c, File f, TransferCallback cb) throws Exception {
        InputStream in = null;
        try {
            in = getInputStream(c);
            transfer(in, f, cb);
        } finally {
            close(in);
        }
    }

    /**
     * Transfers the contents of an InputStream to a File.
     * <p>
     * Progress can be monitored and the transfer can be cancelled through the callback interface.
     * The output stream is automatically buffered for optimal performance.
     * </p>
     *
     * @param s the source InputStream
     * @param f the destination File
     * @param cb optional callback for progress monitoring and cancellation (may be null)
     * @throws Exception if any error occurs during the transfer
     */
    public static void transfer(InputStream s, File f, TransferCallback cb) throws Exception {
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(f.toPath()))) {
            transfer(s, out, cb);
        }
    }

    /**
     * Transfers bytes from an input stream to an output stream with optional progress monitoring.
     * <p>
     * This method does not close the streams; that responsibility remains with the caller.
     * Progress can be monitored and the transfer can be cancelled through the callback interface.
     * </p>
     *
     * @param in the source InputStream
     * @param out the destination OutputStream
     * @param cb optional callback for progress monitoring and cancellation (may be null)
     * @throws IOException if an I/O error occurs during transfer
     */
    public static void transfer(InputStream in, OutputStream out, TransferCallback cb) throws IOException {
        byte[] bytes = new byte[TRANSFER_BUFFER];
        int count;
        while ((count = in.read(bytes)) != -1) {
            out.write(bytes, 0, count);
            if (cb != null) {
                cb.bytesTransferred(bytes, count);
                if (cb.isCancelled()) {
                    break;
                }
            }
        }
    }

    /**
     * Reads exactly the specified number of bytes from an InputStream into a byte array.
     * <p>
     * This method will continue reading until either the byte array is full or the end of the stream is reached.
     * </p>
     *
     * @param in the InputStream to read from
     * @param bytes the byte array to fill
     * @throws IOException if the stream ends before the byte array is filled or if any other I/O error occurs
     */
    public static void transfer(InputStream in, byte[] bytes) throws IOException {
        // Read in the bytes
        int offset = 0;
        int numRead;
        while (offset < bytes.length && (numRead = in.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("Retry:  Not all bytes were transferred correctly.");
        }
    }
    
    /**
     * Transfers all bytes from an input stream to an output stream.
     * <p>
     * This method does not close the streams; that responsibility remains with the caller.
     * Uses an internal buffer for efficient transfer.
     * </p>
     *
     * @param in the source InputStream
     * @param out the destination OutputStream
     * @throws IOException if an I/O error occurs during transfer
     */
    public static void transfer(InputStream in, OutputStream out) throws IOException {
        byte[] bytes = new byte[TRANSFER_BUFFER];
        int count;
        while ((count = in.read(bytes)) != -1) {
            out.write(bytes, 0, count);
        }
    }

    /**
     * Transfers the contents of a File to an OutputStream.
     * <p>
     * The input is automatically buffered for optimal performance.
     * The output stream is flushed after the transfer but not closed.
     * </p>
     *
     * @param file the source File
     * @param out the destination OutputStream
     * @throws IOException if an I/O error occurs during transfer
     */
    public static void transfer(File file, OutputStream out) throws IOException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file.toPath()), TRANSFER_BUFFER)) {
            transfer(in, out);
        } finally {
            flush(out);
        }
    }

    /**
     * Safely closes an XMLStreamReader, suppressing any exceptions.
     *
     * @param reader the XMLStreamReader to close (may be null)
     */
    public static void close(XMLStreamReader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (XMLStreamException ignore) {
        }
    }

    /**
     * Safely closes an XMLStreamWriter, suppressing any exceptions.
     *
     * @param writer the XMLStreamWriter to close (may be null)
     */
    public static void close(XMLStreamWriter writer) {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (XMLStreamException ignore) {
        }
    }

    /**
     * Safely closes any Closeable resource, suppressing any exceptions.
     *
     * @param c the Closeable resource to close (may be null)
     */
    public static void close(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (IOException ignore) {
        }
    }
    
    /**
     * Safely flushes any Flushable resource, suppressing any exceptions.
     *
     * @param f the Flushable resource to flush (may be null)
     */
    public static void flush(Flushable f) {
        try {
            if (f != null) {
                f.flush();
            }
        } catch (IOException ignore) {
        }
    }

    /**
     * Safely flushes an XMLStreamWriter, suppressing any exceptions.
     *
     * @param writer the XMLStreamWriter to flush (may be null)
     */
    public static void flush(XMLStreamWriter writer) {
        try {
            if (writer != null) {
                writer.flush();
            }
        } catch (XMLStreamException ignore) {
        }
    }

    /**
     * Converts an InputStream's contents to a byte array.
     * <p>
     * This method should only be used when the input stream's length is known to be relatively small,
     * as it loads the entire stream into memory.
     * </p>
     *
     * @param in the InputStream to read from
     * @return the byte array containing the stream's contents, or null if an error occurs
     */
    public static byte[] inputStreamToBytes(InputStream in) {
        try {
            FastByteArrayOutputStream out = new FastByteArrayOutputStream(16384);
            transfer(in, out);
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Transfers a byte array to a URLConnection's output stream.
     * <p>
     * The output stream is automatically buffered for optimal performance and properly closed after transfer.
     * </p>
     *
     * @param c the URLConnection to write to
     * @param bytes the byte array to transfer
     * @throws IOException if an I/O error occurs during transfer
     */
    public static void transfer(URLConnection c, byte[] bytes) throws IOException {
        try (OutputStream out = new BufferedOutputStream(c.getOutputStream())) {
            out.write(bytes);
        }
    }

    /**
     * Compresses the contents of one ByteArrayOutputStream into another using GZIP compression.
     * <p>
     * Uses BEST_SPEED compression level for optimal performance.
     * </p>
     *
     * @param original the ByteArrayOutputStream containing the data to compress
     * @param compressed the ByteArrayOutputStream to receive the compressed data
     * @throws IOException if an I/O error occurs during compression
     */
    public static void compressBytes(ByteArrayOutputStream original, ByteArrayOutputStream compressed) throws IOException {
        DeflaterOutputStream gzipStream = new AdjustableGZIPOutputStream(compressed, Deflater.BEST_SPEED);
        original.writeTo(gzipStream);
        gzipStream.flush();
        gzipStream.close();
    }

    /**
     * Compresses the contents of one FastByteArrayOutputStream into another using GZIP compression.
     * <p>
     * Uses BEST_SPEED compression level for optimal performance.
     * </p>
     *
     * @param original the FastByteArrayOutputStream containing the data to compress
     * @param compressed the FastByteArrayOutputStream to receive the compressed data
     * @throws IOException if an I/O error occurs during compression
     */
    public static void compressBytes(FastByteArrayOutputStream original, FastByteArrayOutputStream compressed) throws IOException {
        DeflaterOutputStream gzipStream = new AdjustableGZIPOutputStream(compressed, Deflater.BEST_SPEED);
        gzipStream.write(original.toByteArray(), 0, original.size());
        gzipStream.flush();
        gzipStream.close();
    }

    /**
     * Compresses a byte array using GZIP compression.
     *
     * @param bytes the byte array to compress
     * @return a new byte array containing the compressed data
     * @throws RuntimeException if compression fails
     */
    public static byte[] compressBytes(byte[] bytes) {
        return compressBytes(bytes, 0, bytes.length);
    }

    /**
     * Compresses a portion of a byte array using GZIP compression.
     *
     * @param bytes the source byte array
     * @param offset the starting position in the source array
     * @param len the number of bytes to compress
     * @return a new byte array containing the compressed data
     * @throws RuntimeException if compression fails
     */
    public static byte[] compressBytes(byte[] bytes, int offset, int len) {
        try (FastByteArrayOutputStream byteStream = new FastByteArrayOutputStream()) {
            try (DeflaterOutputStream gzipStream = new AdjustableGZIPOutputStream(byteStream, Deflater.BEST_SPEED)) {
                gzipStream.write(bytes, offset, len);
                gzipStream.flush();
            }
            return Arrays.copyOf(byteStream.toByteArray(), byteStream.size());
        } catch (Exception e) {
            throw new RuntimeException("Error compressing bytes.", e);
        }
    }

    /**
     * Uncompresses a GZIP-compressed byte array.
     * <p>
     * If the input is not GZIP-compressed, returns the original array unchanged.
     * </p>
     *
     * @param bytes the compressed byte array
     * @return the uncompressed byte array, or the original array if not compressed
     * @throws RuntimeException if decompression fails
     */
    public static byte[] uncompressBytes(byte[] bytes) {
        return uncompressBytes(bytes, 0, bytes.length);
    }

    /**
     * Uncompresses a portion of a GZIP-compressed byte array.
     * <p>
     * If the input is not GZIP-compressed, returns the original array unchanged.
     * </p>
     *
     * @param bytes the compressed byte array
     * @param offset the starting position in the source array
     * @param len the number of bytes to uncompress
     * @return the uncompressed byte array, or the original array if not compressed
     * @throws RuntimeException if decompression fails
     */
    public static byte[] uncompressBytes(byte[] bytes, int offset, int len) {
        if (ByteUtilities.isGzipped(bytes)) {
            try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes, offset, len)) {
                try (GZIPInputStream gzipStream = new GZIPInputStream(byteStream, 16384)) {
                    return inputStreamToBytes(gzipStream);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error uncompressing bytes", e);
            }
        }
        return bytes;
    }

    /**
     * Callback interface for monitoring and controlling byte transfers.
     */
    public interface TransferCallback {
        /**
         * Called when bytes are transferred during an operation.
         *
         * @param bytes the buffer containing the transferred bytes
         * @param count the number of bytes actually transferred
         */
        void bytesTransferred(byte[] bytes, int count);

        /**
         * Checks if the transfer operation should be cancelled.
         *
         * @return true if the transfer should be cancelled, false to continue
         */
        boolean isCancelled();
    }
}