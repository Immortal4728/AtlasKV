package com.atlaskv.storage.wal;

import com.atlaskv.core.LogEntry;
import com.atlaskv.core.storage.CorruptedStorageException;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * Encoder and decoder for WAL log entry records.
 * Binary format: [Header 1B 0x57][Index 8B][Term 8B][CmdLen 4B][Cmd Payload][CRC32 8B]
 */
final class WalRecordCodec {

    static final byte RECORD_HEADER = 0x57; // 'W'
    static final int HEADER_AND_CHECKSUM_SIZE = 1 + 8 + 8 + 4 + 8; // 29 bytes

    private WalRecordCodec() {
    }

    /**
     * Encodes a {@link LogEntry} into a byte array payload with CRC32 checksum.
     *
     * @param entry log entry to encode
     * @return encoded byte array
     */
    static byte[] encode(LogEntry entry) {
        Objects.requireNonNull(entry, "Entry must not be null");
        byte[] command = entry.command();
        int recordSize = HEADER_AND_CHECKSUM_SIZE + command.length;
        ByteBuffer buffer = ByteBuffer.allocate(recordSize);

        buffer.put(RECORD_HEADER);
        buffer.putLong(entry.index());
        buffer.putLong(entry.term());
        buffer.putInt(command.length);
        buffer.put(command);

        CRC32 crc = new CRC32();
        crc.update(buffer.array(), 1, recordSize - 9);
        buffer.putLong(crc.getValue());

        return buffer.array();
    }

    /**
     * Decodes a record from a ByteBuffer.
     *
     * @param buffer buffer positioned at start of record
     * @return decoded LogEntry
     * @throws CorruptedStorageException if record header or CRC32 is invalid
     */
    static LogEntry decode(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "Buffer must not be null");
        int startPos = buffer.position();

        byte header = buffer.get();
        if (header != RECORD_HEADER) {
            throw new CorruptedStorageException("Invalid WAL record header byte: 0x" + Integer.toHexString(header));
        }

        long index = buffer.getLong();
        long term = buffer.getLong();
        int cmdLen = buffer.getInt();

        if (cmdLen < 0 || buffer.remaining() < cmdLen + 8) {
            throw new CorruptedStorageException("Truncated command payload in WAL record at index " + index);
        }

        byte[] command = new byte[cmdLen];
        buffer.get(command);

        long storedCrc = buffer.getLong();
        int endPos = buffer.position();

        CRC32 crc = new CRC32();
        crc.update(buffer.array(), startPos + 1, (endPos - startPos) - 9);

        if (crc.getValue() != storedCrc) {
            throw new CorruptedStorageException("CRC32 checksum mismatch in WAL record at index "
                    + index + " (expected " + storedCrc + ", computed " + crc.getValue() + ")");
        }

        return new LogEntry(index, term, command);
    }
}
