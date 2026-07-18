package com.atlaskv.test;

import com.atlaskv.core.statemachine.StateMachine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Recording StateMachine that captures all applied commands for test assertions.
 */
public final class StubStateMachine implements StateMachine {

    private final List<byte[]> appliedCommands = new ArrayList<>();

    @Override
    public synchronized byte[] apply(byte[] command) {
        appliedCommands.add(command.clone());
        return command;
    }

    @Override
    public synchronized byte[] takeSnapshot() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(appliedCommands.size());
            for (byte[] cmd : appliedCommands) {
                dos.writeInt(cmd.length);
                dos.write(cmd);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize StubStateMachine snapshot", e);
        }
    }

    @Override
    public synchronized void restoreSnapshot(byte[] snapshot) {
        appliedCommands.clear();
        if (snapshot == null || snapshot.length == 0) {
            return;
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(snapshot);
             DataInputStream dis = new DataInputStream(bais)) {
            int size = dis.readInt();
            for (int i = 0; i < size; i++) {
                int len = dis.readInt();
                byte[] cmd = new byte[len];
                dis.readFully(cmd);
                appliedCommands.add(cmd);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize StubStateMachine snapshot", e);
        }
    }

    public synchronized List<byte[]> appliedCommands() {
        return Collections.unmodifiableList(new ArrayList<>(appliedCommands));
    }

    public synchronized int appliedCount() {
        return appliedCommands.size();
    }
}
