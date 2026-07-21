'use client';

import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Users,
  UserPlus,
  UserMinus,
  RefreshCw,
  AlertTriangle,
  CheckCircle,
  Database,
  Network,
  Shield,
  Loader2,
} from 'lucide-react';
import * as api from '@/services/api';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

export default function MembersPage() {
  const queryClient = useQueryClient();
  const [mounted, setMounted] = useState(false);

  // Modal / Input states
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [isRemoveOpen, setIsRemoveOpen] = useState(false);
  const [newNodeId, setNewNodeId] = useState('');
  const [newNodeAddress, setNewNodeAddress] = useState('');
  const [selectedRemoveNodeId, setSelectedRemoveNodeId] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    setMounted(true);
  }, []);

  // Fetch status and members
  const {
    data: status,
    isLoading: isStatusLoading,
    error: statusError,
  } = useQuery({
    queryKey: ['cluster', 'status'],
    queryFn: api.getClusterStatus,
    refetchInterval: 3000,
    enabled: mounted,
  });

  const {
    data: members,
    isLoading: isMembersLoading,
    error: membersError,
  } = useQuery({
    queryKey: ['cluster', 'members'],
    queryFn: api.getMembers,
    refetchInterval: 3000,
    enabled: mounted,
  });

  // Mutator: Add member
  const addMemberMutation = useMutation({
    mutationFn: api.addMember,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cluster'] });
      setIsAddOpen(false);
      setNewNodeId('');
      setNewNodeAddress('');
      setErrorMsg('');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to add cluster member');
    },
  });

  // Mutator: Remove member
  const removeMemberMutation = useMutation({
    mutationFn: api.removeMember,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cluster'] });
      setIsRemoveOpen(false);
      setSelectedRemoveNodeId('');
      setErrorMsg('');
    },
    onError: (err: any) => {
      setErrorMsg(err.message || 'Failed to remove cluster member');
    },
  });

  if (!mounted) return null;

  const isTransitionActive = members?.jointConsensusActive || false;
  const isPending = addMemberMutation.isPending || removeMemberMutation.isPending;
  const isControlsDisabled = isTransitionActive || isPending;

  const handleAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newNodeId.trim()) return;
    setErrorMsg('');
    addMemberMutation.mutate({
      nodeId: newNodeId.trim(),
      address: newNodeAddress.trim() || undefined,
    });
  };

  const handleRemoveSubmit = () => {
    if (!selectedRemoveNodeId) return;
    setErrorMsg('');
    removeMemberMutation.mutate(selectedRemoveNodeId);
  };

  // Compose list of members
  const memberList = members?.members || [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight text-white/90">
            Cluster Membership
          </h1>
          <p className="text-sm text-white/30 mt-0.5">
            Manage distributed cluster configuration nodes using Joint Consensus protocol
          </p>
        </div>

        <Button
          onClick={() => {
            setErrorMsg('');
            setIsAddOpen(true);
          }}
          disabled={isControlsDisabled}
          className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold h-9 px-4 gap-1.5"
        >
          <UserPlus className="h-4 w-4" />
          Add Member
        </Button>
      </div>

      {/* Joint Consensus Banner / Notice */}
      {isTransitionActive && (
        <Card className="border-amber-500/20 bg-amber-500/[0.03] animate-pulse">
          <CardContent className="p-4 flex items-center gap-3">
            <AlertTriangle className="h-5 w-5 text-amber-500 shrink-0" />
            <div className="flex-1">
              <span className="text-xs font-bold text-amber-400 block uppercase tracking-wider">
                Joint Consensus Active (Transitional Phase)
              </span>
              <p className="text-[11px] text-white/50 mt-0.5 leading-relaxed">
                Raft is executing a configuration shift. Operations require majorities from both the Old ($C_{`{old}`}$) and New ($C_{`{new}`}$) membership groups. Cluster operations are temporarily restricted to protect safety guarantees.
              </p>
            </div>
            <Badge className="bg-amber-500/20 text-amber-400 border-amber-500/30 text-[9px] font-semibold py-0.5 uppercase">
              Restricted
            </Badge>
          </CardContent>
        </Card>
      )}

      {/* Membership configurations visual grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Side: Active Members List (2 cols) */}
        <div className="lg:col-span-2 space-y-4">
          <Card className="border-white/[0.06] bg-[#111113]">
            <CardContent className="p-5 space-y-4">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-white/35 block">
                Active Cluster Nodes
              </span>

              {isMembersLoading || isStatusLoading ? (
                <div className="py-8 flex justify-center">
                  <Loader2 className="h-6 w-6 text-white/20 animate-spin" />
                </div>
              ) : memberList.length === 0 ? (
                <div className="py-8 text-center text-xs text-white/25">
                  No active cluster members found
                </div>
              ) : (
                <div className="divide-y divide-white/[0.04]">
                  {memberList.map((nodeId) => {
                    const isLocal = status?.nodeId === nodeId;
                    const isLeader = members?.leaderId === nodeId;
                    const role = isLocal ? status?.role : (isLeader ? 'LEADER' : 'FOLLOWER');
                    const isHealthy = isLocal ? status?.healthy : true; // Assumed healthy if present in active cluster list

                    return (
                      <div
                        key={nodeId}
                        className="flex items-center justify-between py-3.5 first:pt-0 last:pb-0"
                      >
                        <div className="flex items-center gap-3">
                          <div className={`h-8 w-8 rounded-lg flex items-center justify-center shrink-0 ${
                            isLeader
                              ? 'bg-amber-500/10 border border-amber-500/20 text-amber-400'
                              : 'bg-white/[0.03] border border-white/[0.06] text-white/40'
                          }`}>
                            <Database className="h-4 w-4" />
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-semibold text-white/80 font-mono">
                                {nodeId}
                              </span>
                              {isLocal && (
                                <Badge className="bg-blue-500/10 text-blue-400 border-blue-500/20 text-[9px] font-medium py-0 px-1 select-none">
                                  Local
                                </Badge>
                              )}
                            </div>
                            <span className="text-[10px] text-white/30 font-mono block">
                              Role: {role}
                            </span>
                          </div>
                        </div>

                        <div className="flex items-center gap-4">
                          {/* Health status indicator */}
                          <div className="flex items-center gap-1.5 text-xs text-white/60">
                            <div className={`h-1.5 w-1.5 rounded-full ${isHealthy ? 'bg-emerald-500' : 'bg-rose-500'}`} />
                            <span className="text-[10px] text-white/40 font-semibold uppercase">
                              {isHealthy ? 'Healthy' : 'Offline'}
                            </span>
                          </div>

                          {/* Remove member button */}
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => {
                              setSelectedRemoveNodeId(nodeId);
                              setIsRemoveOpen(true);
                            }}
                            disabled={isControlsDisabled || isLocal}
                            className="h-8 w-8 p-0 text-white/35 hover:text-rose-400 hover:bg-rose-500/10"
                            title="Remove node from cluster"
                          >
                            <UserMinus className="h-3.5 w-3.5" />
                          </Button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Right Side: Configuration Group Details (1 col) */}
        <div className="space-y-6">
          <Card className="border-white/[0.06] bg-[#111113] p-5">
            <span className="text-[11px] font-semibold uppercase tracking-wider text-white/35 block mb-4">
              Joint Consensus Config Sets
            </span>

            <div className="space-y-4">
              {/* Old Members List */}
              <div className="space-y-2">
                <span className="text-[10px] font-bold text-white/20 uppercase tracking-wider block">
                  Old Members Set ($C_{`{old}`}$)
                </span>
                <div className="border border-white/[0.06] bg-black/20 rounded-lg p-3 space-y-1.5 min-h-[50px]">
                  {members?.oldMembers && members.oldMembers.length > 0 ? (
                    members.oldMembers.map((m) => (
                      <div key={m} className="flex items-center gap-2">
                        <div className="h-1.5 w-1.5 rounded-full bg-white/30" />
                        <span className="text-xs font-mono text-white/50">{m}</span>
                      </div>
                    ))
                  ) : (
                    <span className="text-[10px] italic text-white/25 block">Not Active</span>
                  )}
                </div>
              </div>

              {/* New Members List */}
              <div className="space-y-2">
                <span className="text-[10px] font-bold text-white/20 uppercase tracking-wider block">
                  New Members Set ($C_{`{new}`}$)
                </span>
                <div className="border border-white/[0.06] bg-black/20 rounded-lg p-3 space-y-1.5 min-h-[50px]">
                  {members?.newMembers && members.newMembers.length > 0 ? (
                    members.newMembers.map((m) => (
                      <div key={m} className="flex items-center gap-2">
                        <div className="h-1.5 w-1.5 rounded-full bg-white/30" />
                        <span className="text-xs font-mono text-white/50">{m}</span>
                      </div>
                    ))
                  ) : (
                    <span className="text-[10px] italic text-white/25 block">Not Active</span>
                  )}
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>

      {/* Add Member Dialog */}
      <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
        <DialogContent className="border-white/[0.06] bg-[#111113] text-white">
          <form onSubmit={handleAddSubmit}>
            <DialogHeader>
              <DialogTitle className="text-white/90 text-sm font-semibold">
                Add Node to Cluster
              </DialogTitle>
              <DialogDescription className="text-white/30 text-xs">
                Enter node ID and optional network endpoint to begin Joint Consensus protocol.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 py-4">
              {errorMsg && (
                <div className="bg-rose-500/10 border border-rose-500/25 rounded p-2.5 text-xs text-rose-400">
                  {errorMsg}
                </div>
              )}

              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold uppercase tracking-wider text-white/35">
                  Node ID
                </label>
                <Input
                  required
                  placeholder="e.g. node2"
                  value={newNodeId}
                  onChange={(e) => setNewNodeId(e.target.value)}
                  className="bg-white/[0.02] border-white/[0.08] text-xs h-9 text-white/80 placeholder:text-white/20"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold uppercase tracking-wider text-white/35">
                  Network Address (Optional)
                </label>
                <Input
                  placeholder="e.g. localhost:9092"
                  value={newNodeAddress}
                  onChange={(e) => setNewNodeAddress(e.target.value)}
                  className="bg-white/[0.02] border-white/[0.08] text-xs h-9 text-white/80 placeholder:text-white/20"
                />
              </div>
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="ghost"
                onClick={() => {
                  setIsAddOpen(false);
                  setErrorMsg('');
                }}
                className="text-xs text-white/40 hover:text-white"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={addMemberMutation.isPending}
                className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold h-9 px-4 gap-1.5"
              >
                {addMemberMutation.isPending && <Loader2 className="h-3 w-3 animate-spin" />}
                Add Node
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Remove Member Confirmation Dialog */}
      <Dialog open={isRemoveOpen} onOpenChange={setIsRemoveOpen}>
        <DialogContent className="border-white/[0.06] bg-[#111113] text-white">
          <DialogHeader>
            <DialogTitle className="text-white/90 text-sm font-semibold">
              Confirm Node Removal
            </DialogTitle>
            <DialogDescription className="text-white/30 text-xs">
              Are you sure you want to remove node <strong className="font-mono text-white/50">{selectedRemoveNodeId}</strong>?
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="bg-rose-500/10 border border-rose-500/25 rounded p-2.5 text-xs text-rose-400 my-2">
              {errorMsg}
            </div>
          )}

          <div className="bg-amber-500/[0.03] border border-amber-500/20 rounded p-3 text-[11px] text-amber-400/90 leading-relaxed my-2">
            Removing a node will trigger a cluster-wide joint consensus change. If the remaining active membership drops below a quorum, cluster operations will halt.
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setIsRemoveOpen(false);
                setSelectedRemoveNodeId('');
                setErrorMsg('');
              }}
              className="text-xs text-white/40 hover:text-white"
            >
              Cancel
            </Button>
            <Button
              onClick={handleRemoveSubmit}
              disabled={removeMemberMutation.isPending}
              className="bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold h-9 px-4 gap-1.5"
            >
              {removeMemberMutation.isPending && <Loader2 className="h-3 w-3 animate-spin" />}
              Remove Node
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
