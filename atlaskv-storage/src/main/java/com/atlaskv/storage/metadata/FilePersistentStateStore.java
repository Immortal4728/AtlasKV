package com.atlaskv.storage.metadata;

import com.atlaskv.core.NodeId;
import com.atlaskv.core.PersistentState;
import com.atlaskv.core.storage.CorruptedStorageException;
import com.atlaskv.core.storage.PersistentStateStore;
import com.atlaskv.core.storage.StorageException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * File-backed implementation of {@link PersistentStateStore} using atomic file updates
 * and CRC32 checksums to ensure crash safety and state integrity.
 */
public final class FilePersistentStateStore implements PersistentStateStore {

    private static final int MAGIC_HEADER = 0x41544C53; // "ATLS"
    private static final int VERSION = 1;

    private final Path filePath;

    /**
     * Constructs a FilePersistentStateStore backed by the given file path.
     *
     * @param filePath path to metadata file
     */
    public FilePersistentStateStore(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "FilePath must not be null");
    }

    @Override
    public synchronized void saveState(PersistentState state) {
        Objects.requireNonNull(state, "State must not be null");
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new StorageException("Failed to create parent directories for: " + filePath, e);
            }
        }

        Path fileName = filePath.getFileName();
        if (fileName == null) {
            throw new StorageException("Invalid file path without filename: " + filePath);
        }
        Path tempPath = filePath.resolveSibling(fileName.toString() + ".tmp");
        byte[] candidateBytes = state.votedFor() != null
                ? state.votedFor().value().getBytes(StandardCharsets.UTF_8)
                : new byte[0];
        int candidateLen = state.votedFor() != null ? candidateBytes.length : -1;

        int payloadSize = 4 + 4 + 8 + 4 + (state.votedFor() != null ? candidateBytes.length : 0);
        ByteBuffer buffer = ByteBuffer.allocate(payloadSize + 8);

        buffer.putInt(MAGIC_HEADER);
        buffer.putInt(VERSION);
        buffer.putLong(state.currentTerm());
        buffer.putInt(candidateLen);
        if (candidateLen > 0) {
            buffer.put(candidateBytes);
        }

        CRC32 crc = new CRC32();
        crc.update(buffer.array(), 4, payloadSize - 4);
        buffer.putLong(crc.getValue());
        buffer.flip();

        try {
            try (FileChannel channel = FileChannel.open(tempPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            Files.move(tempPath, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to save persistent state to: " + filePath, e);
        }
    }

    @Override
    public synchronized PersistentState loadState() {
        if (!Files.exists(filePath)) {
            return PersistentState.initial();
        }

        try {
            long size = Files.size(filePath);
            if (size == 0) {
                return PersistentState.initial();
            }

            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
                while (buffer.hasRemaining()) {
                    if (channel.read(buffer) == -1) {
                        break;
                    }
                }
            }
            buffer.flip();

            if (buffer.remaining() < 28) {
                throw new CorruptedStorageException("State metadata file too small: " + buffer.remaining() + " bytes");
            }

            int magic = buffer.getInt();
            if (magic != MAGIC_HEADER) {
                throw new CorruptedStorageException("Invalid magic header in metadata file: 0x"
                        + Integer.toHexString(magic));
            }

            int version = buffer.getInt();
            if (version != VERSION) {
                throw new CorruptedStorageException("Unsupported metadata version: " + version);
            }

            long term = buffer.getLong();
            int candidateLen = buffer.getInt();

            NodeId votedFor = null;
            if (candidateLen >= 0) {
                if (buffer.remaining() < candidateLen + 8) {
                    throw new CorruptedStorageException("Truncated candidateId in metadata file");
                }
                byte[] candidateBytes = new byte[candidateLen];
                buffer.get(candidateBytes);
                votedFor = NodeId.of(new String(candidateBytes, StandardCharsets.UTF_8));
            }

            long storedCrc = buffer.getLong();

            CRC32 crc = new CRC32();
            crc.update(buffer.array(), 4, buffer.position() - 12);
            if (crc.getValue() != storedCrc) {
                throw new CorruptedStorageException("CRC32 checksum mismatch in metadata file (expected "
                        + storedCrc + ", computed " + crc.getValue() + ")");
            }

            return new PersistentState(term, votedFor);
        } catch (IOException e) {
            throw new StorageException("Failed to read persistent state from: " + filePath, e);
        }
    }

    @Override
    public void close() {
        // No persistent resources to release
    }
}
