package com.atlaskv.transport.grpc;

import com.atlaskv.core.LogEntry;
import com.atlaskv.core.NodeId;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;
import com.atlaskv.core.rpc.RequestVoteArgs;
import com.atlaskv.core.rpc.RequestVoteReply;
import com.atlaskv.transport.proto.AppendEntriesProtoArgs;
import com.atlaskv.transport.proto.AppendEntriesProtoReply;
import com.atlaskv.transport.proto.LogEntryProto;
import com.atlaskv.transport.proto.RequestVoteProtoArgs;
import com.atlaskv.transport.proto.RequestVoteProtoReply;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcProtoCodecTest {

    @Test
    @DisplayName("RequestVoteArgs bi-directional conversion correctness")
    void requestVoteArgsCodec() {
        RequestVoteArgs original = new RequestVoteArgs(5L, NodeId.of("node-1"), 10L, 4L);
        RequestVoteProtoArgs proto = GrpcProtoCodec.toProto(original);
        RequestVoteArgs decoded = GrpcProtoCodec.fromProto(proto);

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    @DisplayName("RequestVoteReply bi-directional conversion correctness")
    void requestVoteReplyCodec() {
        RequestVoteReply original = new RequestVoteReply(5L, true);
        RequestVoteProtoReply proto = GrpcProtoCodec.toProto(original);
        RequestVoteReply decoded = GrpcProtoCodec.fromProto(proto);

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    @DisplayName("LogEntry bi-directional conversion correctness")
    void logEntryCodec() {
        LogEntry original = new LogEntry(12L, 3L, "set k v".getBytes(StandardCharsets.UTF_8));
        LogEntryProto proto = GrpcProtoCodec.toProto(original);
        LogEntry decoded = GrpcProtoCodec.fromProto(proto);

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    @DisplayName("AppendEntriesArgs bi-directional conversion correctness")
    void appendEntriesArgsCodec() {
        LogEntry e1 = new LogEntry(1L, 1L, "cmd1".getBytes(StandardCharsets.UTF_8));
        LogEntry e2 = new LogEntry(2L, 1L, "cmd2".getBytes(StandardCharsets.UTF_8));
        AppendEntriesArgs original = new AppendEntriesArgs(2L, NodeId.of("leader-1"), 0L, 0L, List.of(e1, e2), 1L);

        AppendEntriesProtoArgs proto = GrpcProtoCodec.toProto(original);
        AppendEntriesArgs decoded = GrpcProtoCodec.fromProto(proto);

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    @DisplayName("AppendEntriesReply bi-directional conversion correctness")
    void appendEntriesReplyCodec() {
        AppendEntriesReply original = new AppendEntriesReply(2L, true, 5L);
        AppendEntriesProtoReply proto = GrpcProtoCodec.toProto(original);
        AppendEntriesReply decoded = GrpcProtoCodec.fromProto(proto);

        assertThat(decoded).isEqualTo(original);
    }
}
