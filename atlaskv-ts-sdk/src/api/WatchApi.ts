import { HttpClient } from "../utils/HttpClient.js";
import { WatchEvent } from "../models/WatchEvent.js";
import { Validation } from "../utils/Validation.js";

export interface WatchListener {
  /**
   * Invoked when a state mutation event (PUT, DELETE) is received.
   */
  onEvent(event: WatchEvent): void;
  /**
   * Invoked when an error is encountered in the watch stream.
   */
  onError?(error: Error): void;
  /**
   * Invoked when the connection to the watch SSE endpoint is successfully opened.
   */
  onConnected?(): void;
  /**
   * Invoked when the watch stream disconnects.
   */
  onDisconnected?(): void;
}

export class WatchSession implements AsyncIterable<WatchEvent> {
  private active = true;
  private readonly path: string;
  private readonly httpClient: HttpClient;
  private readonly listener?: WatchListener;

  private queue: WatchEvent[] = [];
  private pendingResolvers: {
    resolve: (value: IteratorResult<WatchEvent>) => void;
    reject: (err: unknown) => void;
  }[] = [];
  private abortController: AbortController | null = null;

  constructor(path: string, httpClient: HttpClient, listener?: WatchListener) {
    this.path = path;
    this.httpClient = httpClient;
    this.listener = listener;
  }

  public start(): void {
    this.runStreamLoop().catch((err) => {
      if (this.active) {
        this.listener?.onError?.(err);
        this.pushError(err);
      }
    });
  }

  public close(): void {
    if (this.active) {
      this.active = false;
      this.abortController?.abort();
      this.pushDone();
    }
  }

  public isActive(): boolean {
    return this.active;
  }

  private pushEvent(event: WatchEvent): void {
    if (this.pendingResolvers.length > 0) {
      const { resolve } = this.pendingResolvers.shift()!;
      resolve({ value: event, done: false });
    } else {
      this.queue.push(event);
    }
  }

  private pushError(err: Error): void {
    while (this.pendingResolvers.length > 0) {
      const { reject } = this.pendingResolvers.shift()!;
      reject(err);
    }
  }

  private pushDone(): void {
    while (this.pendingResolvers.length > 0) {
      const { resolve } = this.pendingResolvers.shift()!;
      resolve({ value: undefined as unknown as WatchEvent, done: true });
    }
  }

  public [Symbol.asyncIterator](): AsyncIterator<WatchEvent> {
    return {
      next: (): Promise<IteratorResult<WatchEvent>> => {
        if (this.queue.length > 0) {
          return Promise.resolve({ value: this.queue.shift()!, done: false });
        }
        if (!this.active) {
          return Promise.resolve({ value: undefined as unknown as WatchEvent, done: true });
        }
        return new Promise<IteratorResult<WatchEvent>>((resolve, reject) => {
          this.pendingResolvers.push({ resolve, reject });
        });
      },
      return: (): Promise<IteratorResult<WatchEvent>> => {
        this.close();
        return Promise.resolve({ value: undefined as unknown as WatchEvent, done: true });
      },
    };
  }

  private async runStreamLoop(): Promise<void> {
    let backoffMs = 500;

    while (this.active) {
      const url = `${this.httpClient.getActiveBaseUrl()}${this.path}`;
      const headers: Record<string, string> = {
        Accept: "text/event-stream",
      };

      this.abortController = new AbortController();

      try {
        const response = await fetch(url, {
          method: "GET",
          headers,
          signal: this.abortController.signal,
        });

        if (response.status === 200) {
          backoffMs = 500; // Reset backoff on success

          if (!response.body) {
            throw new Error("Response body is null");
          }

          // Fallback reading mechanism to support both getReader() and Symbol.asyncIterator
          let reader: { read(): Promise<{ value?: Uint8Array; done: boolean }> };
          if (typeof response.body.getReader === "function") {
            reader = response.body.getReader();
          } else if (Symbol.asyncIterator in response.body) {
            const iterator = (response.body as unknown as AsyncIterable<Uint8Array>)[Symbol.asyncIterator]();
            reader = {
              read: async () => {
                const next = await iterator.next();
                return { value: next.value, done: next.done ?? false };
              },
            };
          } else {
            throw new Error("Response body is not readable");
          }

          const decoder = new TextDecoder();
          let buffer = "";
          let currentEvent: string | null = null;

          while (this.active) {
            const { value, done } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split("\n");
            buffer = lines.pop() ?? "";

            for (const line of lines) {
              if (!this.active) break;
              const trimmed = line.trim();
              if (trimmed.length === 0) {
                currentEvent = null; // Reset event boundary
                continue;
              }

              if (trimmed.startsWith("event:")) {
                currentEvent = trimmed.substring(6).trim();
              } else if (trimmed.startsWith("data:")) {
                const data = trimmed.substring(5).trim();

                if (currentEvent === "status" && data === "connected") {
                  this.listener?.onConnected?.();
                } else if (currentEvent === "message") {
                  try {
                    const event = JSON.parse(data) as WatchEvent;
                    this.listener?.onEvent(event);
                    this.pushEvent(event);
                  } catch (err) {
                    console.error("Failed to parse watch event JSON", data, err);
                  }
                } else if (currentEvent === "error") {
                  this.listener?.onError?.(new Error(`Server SSE error: ${data}`));
                }
              }
            }
          }
        } else if (response.status === 503) {
          console.warn(`Watch stream rejected with status 503 from ${url}`);
        } else {
          console.warn(`Watch stream received HTTP ${response.status}, retrying...`);
        }
      } catch (err) {
        if (!this.active) {
          break; // Normal close
        }
        const error = err as Error;
        console.debug(`Watch connection broken: ${error.message}, reconnecting...`);
      }

      if (this.active) {
        this.listener?.onDisconnected?.();
        await new Promise((resolve) => setTimeout(resolve, backoffMs));
        backoffMs = Math.min(backoffMs * 2, 10000);
      }
    }
  }
}

export class WatchApi {
  private readonly httpClient: HttpClient;

  constructor(httpClient: HttpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Subscribes to real-time events for a single key.
   *
   * @param key the key to watch
   * @param listener callback for events and lifecycle changes
   * @returns a session object that can be closed to stop watching
   */
  public watch(key: string, listener?: WatchListener): WatchSession {
    Validation.validateKey(key);
    const session = new WatchSession(`/api/v1/watch/${key}`, this.httpClient, listener);
    session.start();
    return session;
  }

  /**
   * Subscribes to real-time events for all keys matching a prefix.
   *
   * @param prefix the prefix to watch
   * @param listener callback for events and lifecycle changes
   * @returns a session object that can be closed to stop watching
   */
  public watchPrefix(prefix: string, listener?: WatchListener): WatchSession {
    Validation.validatePrefix(prefix);
    const session = new WatchSession(`/api/v1/watch/prefix/${prefix}`, this.httpClient, listener);
    session.start();
    return session;
  }
}
