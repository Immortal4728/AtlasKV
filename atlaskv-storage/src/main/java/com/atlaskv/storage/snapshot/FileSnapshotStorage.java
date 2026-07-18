package com.atlaskv.storage.snapshot;

import com.atlaskv.core.storage.CorruptedStorageException;
import com.atlaskv.core.storage.Snapshot;
import com.atlaskv.core.storage.SnapshotMetadata;
import com.atlaskv.core.storage.SnapshotStorage;
import com.atlaskv.core.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.CRC32;

/**
 * Production-quality file-based implementation of {@link SnapshotStorage}.
 * Guarantees crash consistency via atomic temp-file replacement, CRC32 verification,
 * and automatic cleanup of obsolete snapshot files.
 */
public final class FileSnapshotStorage implements SnapshotStorage {

    private static final Logger LOG = LoggerFactory.getLogger(FileSnapshotStorage.class);

    private static final int SNAP_MAGIC = 0x41534E50; // "ASNP"
    private static final int SNAP_VERSION_1 = 1;
    private static final int SNAP_VERSION_2 = 2;
    private static final int SNAP_VERSION = SNAP_VERSION_2;

    private final Path snapshotDir;
    private boolean closed = false;

    public FileSnapshotStorage(Path snapshotDir) {
        this.snapshotDir = Objects.requireNonNull(snapshotDir, "Snapshot directory must not be null");
        try {
            if (!Files.exists(snapshotDir)) {
                Files.createDirectories(snapshotDir);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to create snapshot directory at: " + snapshotDir, e);
        }
    }

    @Override
    public synchronized void saveSnapshot(Snapshot snapshot) {
        ensureOpen();
        Objects.requireNonNull(snapshot, "Snapshot must not be null");

        SnapshotMetadata metadata = snapshot.metadata();
        byte[] data = snapshot.data();

        byte[] membershipBytes = metadata.membership() != null
                ? com.atlaskv.core.config.ClusterMembershipCodec.encode(metadata.membership())
                : new byte[0];

        String baseName = String.format("snapshot-%d-%d", metadata.lastIncludedIndex(), metadata.lastIncludedTerm());
        Path tempFile = snapshotDir.resolve(baseName + ".tmp");
        Path snapFile = snapshotDir.resolve(baseName + ".snap");

        try {
            int headerSize = 4 + 4 + 8 + 8 + 4 + membershipBytes.length + 4; // magic, ver, idx, term, memLen, mem, dataLen
            ByteBuffer buffer = ByteBuffer.allocate(headerSize + data.length + 8);
            buffer.putInt(SNAP_MAGIC);
            buffer.putInt(SNAP_VERSION);
            buffer.putLong(metadata.lastIncludedIndex());
            buffer.putLong(metadata.lastIncludedTerm());
            buffer.putInt(membershipBytes.length);
            buffer.put(membershipBytes);
            buffer.putInt(data.length);
            buffer.put(data);

            CRC32 crc = new CRC32();
            crc.update(buffer.array(), 0, headerSize + data.length);
            long checksum = crc.getValue();

            buffer.putLong(checksum);
            buffer.flip();

            try (FileChannel channel = FileChannel.open(tempFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }

            Files.move(tempFile, snapFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            cleanOldSnapshots(snapFile);

        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // Ignore secondary cleanup error
            }
            throw new StorageException("Failed to save snapshot to: " + snapFile, e);
        }
    }

    @Override
    public synchronized Optional<Snapshot> loadLatestSnapshot() {
        ensureOpen();
        List<Path> snapFiles = getSortedSnapshotFiles();
        for (Path snapFile : snapFiles) {
            try {
                return Optional.of(readAndValidateSnapshot(snapFile));
            } catch (CorruptedStorageException e) {
                LOG.warn("Snapshot file {} is corrupted, attempting older snapshots: {}", snapFile, e.getMessage());
            } catch (IOException e) {
                throw new StorageException("Failed to read snapshot file: " + snapFile, e);
            }
        }
        return Optional.empty();
    }

    @Override
    public synchronized Optional<SnapshotMetadata> getLatestSnapshotMetadata() {
        ensureOpen();
        return loadLatestSnapshot().map(Snapshot::metadata);
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    private Snapshot readAndValidateSnapshot(Path snapFile) throws IOException {
        long fileSize = Files.size(snapFile);
        if (fileSize < 28) {
            throw new CorruptedStorageException("Snapshot file smaller than header: " + fileSize);
        }

        byte[] allBytes = Files.readAllBytes(snapFile);
        ByteBuffer buffer = ByteBuffer.wrap(allBytes);

        int magic = buffer.getInt();
        int version = buffer.getInt();

        if (magic != SNAP_MAGIC) {
            throw new CorruptedStorageException("Invalid snapshot magic bytes: 0x" + Integer.toHexString(magic));
        }

        long lastIncludedIndex = buffer.getLong();
        long lastIncludedTerm = buffer.getLong();
        com.atlaskv.core.config.ClusterMembership membership = null;

        int dataLength;
        int headerSize;

        if (version == SNAP_VERSION_1) {
            headerSize = 4 + 4 + 8 + 8 + 4;
            dataLength = buffer.getInt();
        } else if (version == SNAP_VERSION_2) {
            int memLen = buffer.getInt();
            if (memLen > 0) {
                byte[] memBytes = new byte[memLen];
                buffer.get(memBytes);
                membership = com.atlaskv.core.config.ClusterMembershipCodec.decode(memBytes).orElse(null);
            }
            dataLength = buffer.getInt();
            headerSize = 4 + 4 + 8 + 8 + 4 + memLen + 4;
        } else {
            throw new CorruptedStorageException("Unsupported snapshot version: " + version);
        }

        if (dataLength < 0 || headerSize + dataLength + 8 != allBytes.length) {
            throw new CorruptedStorageException("Invalid snapshot data length field: " + dataLength);
        }

        byte[] data = new byte[dataLength];
        buffer.get(data);
        long storedChecksum = buffer.getLong();

        CRC32 crc = new CRC32();
        crc.update(allBytes, 0, headerSize + dataLength);
        long computedChecksum = crc.getValue();

        if (storedChecksum != computedChecksum) {
            throw new CorruptedStorageException(String.format(
                    "Snapshot CRC mismatch: expected 0x%x, got 0x%x", storedChecksum, computedChecksum));
        }

        return new Snapshot(new SnapshotMetadata(lastIncludedIndex, lastIncludedTerm, membership), data);
    }

    private List<Path> getSortedSnapshotFiles() {
        try (var stream = Files.list(snapshotDir)) {
            return stream
                    .filter(path -> {
                        Path fileName = path.getFileName();
                        return fileName != null && fileName.toString().endsWith(".snap");
                    })
                    .sorted(Comparator.comparingLong(this::extractIndexFromFileName).reversed())
                    .toList();
        } catch (IOException e) {
            throw new StorageException("Failed to list snapshot files in: " + snapshotDir, e);
        }
    }

    private long extractIndexFromFileName(Path file) {
        Path name = file.getFileName();
        if (name == null) {
            return -1L;
        }
        String fileName = name.toString();
        try {
            String[] parts = fileName.replace(".snap", "").split("-");
            if (parts.length >= 2) {
                return Long.parseLong(parts[1]);
            }
        } catch (NumberFormatException ignored) {
            // Ignore malformed names
        }
        return -1L;
    }

    private void cleanOldSnapshots(Path currentSnapshot) {
        long currentIndex = extractIndexFromFileName(currentSnapshot);
        try (var stream = Files.list(snapshotDir)) {
            List<Path> filesToDelete = stream
                    .filter(path -> !path.equals(currentSnapshot))
                    .filter(path -> {
                        Path fn = path.getFileName();
                        if (fn == null) {
                            return false;
                        }
                        String name = fn.toString();
                        if (name.endsWith(".tmp")) {
                            return true;
                        }
                        if (name.endsWith(".snap")) {
                            long index = extractIndexFromFileName(path);
                            return index >= 0 && index < currentIndex;
                        }
                        return false;
                    })
                    .toList();

            for (Path path : filesToDelete) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    LOG.warn("Failed to delete obsolete snapshot file: {}", path, e);
                }
            }
        } catch (IOException e) {
            LOG.warn("Failed to clean old snapshot files in: {}", snapshotDir, e);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new StorageException("FileSnapshotStorage is closed");
        }
    }
}
