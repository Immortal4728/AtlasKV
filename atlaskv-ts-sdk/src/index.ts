// Client and Builder
export { AtlasKVClient } from "./client/AtlasKVClient.js";
export { AtlasKVClientBuilder } from "./client/AtlasKVClientBuilder.js";

// Sub-APIs
export { KeyValueApi } from "./api/KeyValueApi.js";
export { LeaseApi } from "./api/LeaseApi.js";
export { HistoryApi } from "./api/HistoryApi.js";
export { ClusterApi } from "./api/ClusterApi.js";
export { WatchApi, WatchListener, WatchSession } from "./api/WatchApi.js";

// Models
export { KeyValue } from "./models/KeyValue.js";
export { Revision } from "./models/Revision.js";
export { Lease } from "./models/Lease.js";
export { ClusterStatus } from "./models/ClusterStatus.js";
export { Metrics } from "./models/Metrics.js";
export { PrefixResult, PrefixEntry } from "./models/PrefixResult.js";
export { WatchEvent } from "./models/WatchEvent.js";

// Errors
export { AtlasKVError } from "./errors/AtlasKVError.js";
export { NotLeaderError } from "./errors/NotLeaderError.js";
export { ConflictError } from "./errors/ConflictError.js";
export { TimeoutError } from "./errors/TimeoutError.js";

// Utilities
export { RetryPolicy, RetryPolicyOptions } from "./utils/RetryPolicy.js";
export { Authentication, AuthenticationApplyFn } from "./utils/HttpClient.js";
export { Validation } from "./utils/Validation.js";
