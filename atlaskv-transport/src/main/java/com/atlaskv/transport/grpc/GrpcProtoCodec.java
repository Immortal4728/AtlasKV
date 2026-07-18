package com.atlaskv.transport.grpc;

import com.atlaskv.core.LogEntry;
import com.atlaskv.core.NodeId;
import com.atlaskv.core.rpc.AppendEntriesArgs;
import com.atlaskv.core.rpc.AppendEntriesReply;
import com.atlaskv.core.rpc.RequestVoteArgs;
import com.atlaskv.core.rpc.RequestVoteReply;
import com.atlaskv.core.rpc.InstallSnapshotArgs;
import com.atlaskv.core.rpc.InstallSnapshotReply;
import com.atlaskv.transport.proto.AppendEntriesProtoArgs;
import com.atlaskv.transport.proto.AppendEntriesProtoReply;
import com.atlaskv.transport.proto.InstallSnapshotProtoArgs;
import com.atlaskv.transport.proto.InstallSnapshotProtoReply;
import com.atlaskv.transport.proto.LogEntryProto;
import com.atlaskv.transport.proto.RequestVoteProtoArgs;
import com.atlaskv.transport.proto.RequestVoteProtoReply;
import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bi-directional converter between Raft core domain models and gRPC Protobuf messages.
 */
public final class GrpcProtoCodec {

    private GrpcProtoCodec() {
    }

    public static RequestVoteProtoArgs toProto(RequestVoteArgs args) {
        Objects.requireNonNull(args, "Args must not be null");
        return RequestVoteProtoArgs.newBuilder()
                .setTerm(args.term())
                .setCandidateId(args.candidateId().value())
                .setLastLogIndex(args.lastLogIndex())
                .setLastLogTerm(args.lastLogTerm())
                .build();
    }

    public static RequestVoteArgs fromProto(RequestVoteProtoArgs proto) {
        Objects.requireNonNull(proto, "Proto must not be null");
        return new RequestVoteArgs(
                proto.getTerm(),
                NodeId.of(proto.getCandidateId()),
                proto.getLastLogIndex(),
                proto.getLastLogTerm()
        );
    }

    public static RequestVoteProtoReply toProto(RequestVoteReply reply) {
        Objects.requireNonNull(reply, "Reply must not be null");
        return RequestVoteProtoReply.newBuilder()
                .setTerm(reply.term())
                .setVoteGranted(reply.voteGranted())
                .build();
    }

    public static RequestVoteReply fromProto(RequestVoteProtoReply proto) {
        Objects.requireNonNull(proto, "Proto must not be null");
        return new RequestVoteReply(
                proto.getTerm(),
                proto.getVoteGranted()
        );
    }

    public static LogEntryProto toProto(LogEntry entry) {
        Objects.requireNonNull(entry, "Entry must not be null");
        return LogEntryProto.newBuilder()
                .setIndex(entry.index())
                .setTerm(entry.term())
                .setCommand(ByteString.copyFrom(entry.command()))
                .build();
    }

    public static LogEntry fromProto(LogEntryProto proto) {
        Objects.requireNonNull(proto, "Proto must not be null");
        return new LogEntry(
                proto.getIndex(),
                proto.getTerm(),
                proto.getCommand().toByteArray()
        );
    }

    public static AppendEntriesProtoArgs toProto(AppendEntriesArgs args) {
        Objects.requireNonNull(args, "Args must not be null");
        List<LogEntryProto> protoEntries = new ArrayList<>(args.entries().size());
        for (LogEntry entry : args.entries()) {
            protoEntries.add(toProto(entry));
        }

        return AppendEntriesProtoArgs.newBuilder()
                .setTerm(args.term())
                .setLeaderId(args.leaderId().value())
                .setPrevLogIndex(args.prevLogIndex())
                .setPrevLogTerm(args.prevLogTerm())
                .addAllEntries(protoEntries)
                .setLeaderCommit(args.leaderCommit())
                .build();
    }

    public static AppendEntriesArgs fromProto(AppendEntriesProtoArgs proto) {
        Objects.requireNonNull(proto, "Proto must not be null");
        List<LogEntry> entries = new ArrayList<>(proto.getEntriesCount());
        for (LogEntryProto entryProto : proto.getEntriesList()) {
            entries.add(fromProto(entryProto));
        }

        return new AppendEntriesArgs(
                proto.getTerm(),
                NodeId.of(proto.getLeaderId()),
                proto.getPrevLogIndex(),
                proto.getPrevLogTerm(),
                entries,
                proto.getLeaderCommit()
        );
    }

    public static AppendEntriesProtoReply toProto(AppendEntriesReply reply) {
        Objects.requireNonNull(reply, "Reply must not be null");
        return AppendEntriesProtoReply.newBuilder()
                .setTerm(reply.term())
                .setSuccess(reply.success())
                .setMatchIndex(reply.matchIndex())
                .build();
    }

    public static AppendEntriesReply fromProto(AppendEntriesProtoReply proto) {
        Objects.requireNonNull(proto, "Proto must not be null");
        return new AppendEntriesReply(
                proto.getTerm(),
                proto.getSuccess(),
                proto.getMatchIndex()
        );
    }

    public static InstallSnapshotProtoArgs toProto(InstallSnapshotArgs args) {
        Objects.requireNonNull(args, "Args must not be null");
        return InstallSnapshotProtoArgs.newBuilder()
                .setTerm(args.term())
                .setLeaderId(args.leaderId().value())
                .setLastIncludedIndex(args.lastIncludedIndex())
                .setLastIncludedTerm(args.lastIncludedTerm())
                .setOffset(args.offset())
                .setData(ByteString.copyFrom(args.data()))
                .setDone(args.done())
                .build();
    }

    public static InstallSnapshotArgs fromProto(InstallSnapshotProtoArgs proto) {
        Objects.requireNonNull(proto, "Proto must not be null");
        return new InstallSnapshotArgs(
                proto.getTerm(),
                NodeId.of(proto.getLeaderId()),
                proto.getLastIncludedIndex(),
                proto.getLastIncludedTerm(),
                proto.getOffset(),
                proto.getData().toByteArray(),
                proto.getDone()
        );
    }

    public static InstallSnapshotProtoReply toProto(InstallSnapshotReply reply) {
        Objects.requireNonNull(reply, "Reply must not be null");
        return InstallSnapshotProtoReply.newBuilder()
                .setTerm(reply.term())
                .setSuccess(reply.success())
                .build();
    }

    public static InstallSnapshotReply fromProto(InstallSnapshotProtoReply proto) {
        Objects.requireNonNull(proto, "Proto must not be null");
        return new InstallSnapshotReply(
                proto.getTerm(),
                proto.getSuccess()
        );
    }
}
