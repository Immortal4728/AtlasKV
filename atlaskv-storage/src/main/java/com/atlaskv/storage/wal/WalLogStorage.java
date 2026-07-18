package com.atlaskv.storage.wal;

import com.atlaskv.core.LogEntry;
import com.atlaskv.core.storage.CorruptedStorageException;
import com.atlaskv.core.storage.LogStorage;
import com.atlaskv.core.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Production-quality, durable implementation of {@link LogStorage} backed by a Write-Ahead Log (WAL).
 * Supports automatic startup replay, crash recovery of partial writes, and physical file truncation.
 */
public final class WalLogStorage implements LogStorage {

    private static final Logger LOG = LoggerFactory.getLogger(WalLogStorage.class);

    private static final int WAL_MAGIC = 0x4157414C; // "AWAL"
    private static final int WAL_VERSION = 1;
    private static final int FILE_HEADER_SIZE = 8;

    private final Path logFilePath;
    private final boolean syncOnAppend;
    private final FileChannel channel;
    private final Map<Long, Long> indexToOffsetMap = new HashMap<>();

    private long lastLogIndex = 0L;
    private long lastLogTerm = 0L;
    private long lastIncludedIndex = 0L;
    private long lastIncludedTerm = 0L;
    private boolean closed = false;

    /**
     * Constructs a WalLogStorage with fsync enabled on every append.
     *
     * @param logFilePath path to WAL log file
     */
    public WalLogStorage(Path logFilePath) {
        this(logFilePath, true);
    }

    /**
     * Constructs a WalLogStorage with configurable fsync behavior.
     *
     * @param logFilePath path to WAL log file
     * @param syncOnAppend if true, force sync on append operations
     */
    public WalLogStorage(Path logFilePath, boolean syncOnAppend) {
        this.logFilePath = Objects.requireNonNull(logFilePath, "LogFilePath must not be null");
        this.syncOnAppend = syncOnAppend;

        try {
            Path parent = logFilePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            this.channel = FileChannel.open(logFilePath,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE);

            initAndRecover();
        } catch (IOException e) {
            throw new StorageException("Failed to open or initialize WAL at: " + logFilePath, e);
        }
    }

    @Override
    public synchronized void append(LogEntry entry) {
        ensureOpen();
        Objects.requireNonNull(entry, "Entry must not be null");

        if (entry.index() != lastLogIndex + 1) {
            throw new IllegalArgumentException("Append index out of order: expected "
                    + (lastLogIndex + 1) + ", got " + entry.index());
        }

        try {
            long offset = channel.size();
            byte[] encoded = WalRecordCodec.encode(entry);
            ByteBuffer buffer = ByteBuffer.wrap(encoded);

            channel.position(offset);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }

            if (syncOnAppend) {
                channel.force(true);
            }

            indexToOffsetMap.put(entry.index(), offset);
            lastLogIndex = entry.index();
            lastLogTerm = entry.term();
        } catch (IOException e) {
            throw new StorageException("Failed to append log entry at index " + entry.index(), e);
        }
    }

    @Override
    public synchronized void appendAll(List<LogEntry> entries) {
        ensureOpen();
        Objects.requireNonNull(entries, "Entries must not be null");
        for (LogEntry entry : entries) {
            append(entry);
        }
    }

    @Override
    public synchronized Optional<LogEntry> getEntry(long index) {
        ensureOpen();
        Long offset = indexToOffsetMap.get(index);
        if (offset == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(readEntryAtOffset(offset));
        } catch (IOException e) {
            throw new StorageException("Failed to read log entry at index " + index, e);
        }
    }

    @Override
    public synchronized List<LogEntry> getEntriesFrom(long fromIndex) {
        ensureOpen();
        long start = Math.max(fromIndex, getFirstLogIndex());
        if (start <= 0 || start > lastLogIndex) {
            return List.of();
        }

        List<LogEntry> entries = new ArrayList<>();
        for (long idx = start; idx <= lastLogIndex; idx++) {
            getEntry(idx).ifPresent(entries::add);
        }
        return entries;
    }

    @Override
    public synchronized long getLastLogIndex() {
        ensureOpen();
        if (indexToOffsetMap.isEmpty() && lastIncludedIndex > 0) {
            return lastIncludedIndex;
        }
        return lastLogIndex;
    }

    @Override
    public synchronized long getLastLogTerm() {
        ensureOpen();
        if (indexToOffsetMap.isEmpty() && lastIncludedTerm > 0) {
            return lastIncludedTerm;
        }
        return lastLogTerm;
    }

    @Override
    public synchronized long getFirstLogIndex() {
        ensureOpen();
        return lastIncludedIndex + 1L;
    }

    @Override
    public synchronized long getTermAt(long index) {
        ensureOpen();
        if (index <= 0) {
            return 0L;
        }
        if (index == lastIncludedIndex) {
            return lastIncludedTerm;
        }
        if (index < lastIncludedIndex || index > lastLogIndex) {
            return 0L;
        }
        return getEntry(index).map(LogEntry::term).orElse(0L);
    }

    @Override
    public synchronized void truncateFrom(long fromIndex) {
        ensureOpen();
        if (fromIndex <= lastIncludedIndex) {
            throw new IllegalArgumentException("Cannot truncate before or at lastIncludedIndex: " + lastIncludedIndex);
        }
        if (fromIndex > lastLogIndex) {
            return;
        }

        try {
            if (fromIndex <= 1 && lastIncludedIndex == 0) {
                channel.truncate(FILE_HEADER_SIZE);
                channel.force(true);
                indexToOffsetMap.clear();
                lastLogIndex = 0L;
                lastLogTerm = 0L;
                return;
            }

            Long truncateOffset = indexToOffsetMap.get(fromIndex);
            if (truncateOffset == null) {
                throw new StorageException("Cannot truncate from index " + fromIndex + ": index not mapped");
            }

            channel.truncate(truncateOffset);
            channel.force(true);

            for (long idx = fromIndex; idx <= lastLogIndex; idx++) {
                indexToOffsetMap.remove(idx);
            }

            lastLogIndex = fromIndex - 1;
            lastLogTerm = (lastLogIndex > 0) ? getTermAt(lastLogIndex) : lastIncludedTerm;
        } catch (IOException e) {
            throw new StorageException("Failed to truncate log from index " + fromIndex, e);
        }
    }

    @Override
    public synchronized void compactUpTo(long newLastIncludedIndex, long newLastIncludedTerm) {
        ensureOpen();
        if (newLastIncludedIndex <= lastIncludedIndex) {
            return;
        }

        try {
            List<LogEntry> remaining = getEntriesFrom(newLastIncludedIndex + 1);

            channel.truncate(FILE_HEADER_SIZE);
            channel.force(true);
            indexToOffsetMap.clear();

            this.lastIncludedIndex = newLastIncludedIndex;
            this.lastIncludedTerm = newLastIncludedTerm;
            this.lastLogIndex = newLastIncludedIndex;
            this.lastLogTerm = newLastIncludedTerm;

            for (LogEntry entry : remaining) {
                append(entry);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to compact WAL up to index " + newLastIncludedIndex, e);
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            try {
                if (channel.isOpen()) {
                    channel.force(true);
                    channel.close();
                }
            } catch (IOException e) {
                throw new StorageException("Failed to close WAL channel at: " + logFilePath, e);
            }
        }
    }

    private synchronized void initAndRecover() throws IOException {
        long fileSize = channel.size();
        if (fileSize == 0) {
            writeWalHeader();
            return;
        }

        if (fileSize < FILE_HEADER_SIZE) {
            throw new CorruptedStorageException("WAL file smaller than header size: " + fileSize + " bytes");
        }

        ByteBuffer headerBuf = ByteBuffer.allocate(FILE_HEADER_SIZE);
        channel.position(0);
        channel.read(headerBuf);
        headerBuf.flip();

        int magic = headerBuf.getInt();
        int version = headerBuf.getInt();

        if (magic != WAL_MAGIC) {
            throw new CorruptedStorageException("Invalid WAL magic bytes: 0x" + Integer.toHexString(magic));
        }
        if (version != WAL_VERSION) {
            throw new CorruptedStorageException("Unsupported WAL version: " + version);
        }

        replayAndRecover(fileSize);
    }

    private void writeWalHeader() throws IOException {
        ByteBuffer headerBuf = ByteBuffer.allocate(FILE_HEADER_SIZE);
        headerBuf.putInt(WAL_MAGIC);
        headerBuf.putInt(WAL_VERSION);
        headerBuf.flip();

        channel.position(0);
        while (headerBuf.hasRemaining()) {
            channel.write(headerBuf);
        }
        channel.force(true);
    }

    private synchronized void replayAndRecover(long fileSize) throws IOException {
        long offset = FILE_HEADER_SIZE;

        while (offset < fileSize) {
            long remainingBytes = fileSize - offset;

            if (remainingBytes < WalRecordCodec.HEADER_AND_CHECKSUM_SIZE) {
                truncateTrailingPartialWrite(offset, "Incomplete record header at offset " + offset);
                break;
            }

            ByteBuffer recordHeaderBuf = ByteBuffer.allocate(WalRecordCodec.HEADER_AND_CHECKSUM_SIZE);
            channel.position(offset);
            channel.read(recordHeaderBuf);
            recordHeaderBuf.flip();

            byte header = recordHeaderBuf.get();
            if (header != WalRecordCodec.RECORD_HEADER) {
                truncateTrailingPartialWrite(offset, "Invalid record header byte at offset " + offset);
                break;
            }

            recordHeaderBuf.position(17);
            int cmdLen = recordHeaderBuf.getInt();

            int recordTotalSize = WalRecordCodec.HEADER_AND_CHECKSUM_SIZE + cmdLen;
            if (remainingBytes < recordTotalSize) {
                truncateTrailingPartialWrite(offset, "Partial command payload at offset " + offset);
                break;
            }

            ByteBuffer fullBuf = ByteBuffer.allocate(recordTotalSize);
            channel.position(offset);
            channel.read(fullBuf);
            fullBuf.flip();

            try {
                LogEntry entry = WalRecordCodec.decode(fullBuf);
                indexToOffsetMap.put(entry.index(), offset);
                lastLogIndex = entry.index();
                lastLogTerm = entry.term();
                offset += recordTotalSize;
            } catch (CorruptedStorageException e) {
                if (offset + recordTotalSize == fileSize) {
                    truncateTrailingPartialWrite(offset, "CRC mismatch on final record at offset " + offset);
                    break;
                } else {
                    throw e;
                }
            }
        }
    }

    private void truncateTrailingPartialWrite(long validOffset, String reason) throws IOException {
        LOG.warn("Truncating un-flushed trailing partial write at offset {} due to: {}", validOffset, reason);
        channel.truncate(validOffset);
        channel.force(true);
    }

    private LogEntry readEntryAtOffset(long offset) throws IOException {
        ByteBuffer headerBuf = ByteBuffer.allocate(WalRecordCodec.HEADER_AND_CHECKSUM_SIZE);
        channel.position(offset);
        channel.read(headerBuf);
        headerBuf.flip();

        headerBuf.get(); // Skip header
        headerBuf.getLong(); // Skip index
        headerBuf.getLong(); // Skip term
        int cmdLen = headerBuf.getInt();

        int recordTotalSize = WalRecordCodec.HEADER_AND_CHECKSUM_SIZE + cmdLen;
        ByteBuffer recordBuf = ByteBuffer.allocate(recordTotalSize);
        channel.position(offset);
        channel.read(recordBuf);
        recordBuf.flip();

        return WalRecordCodec.decode(recordBuf);
    }

    private void ensureOpen() {
        if (closed) {
            throw new StorageException("WalLogStorage is closed");
        }
    }
}
