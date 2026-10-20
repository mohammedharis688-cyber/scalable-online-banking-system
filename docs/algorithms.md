# System Design Algorithms & Implementation Details
## Scalable Online Banking System — UCS3513 System Design Laboratory

This document explains the core algorithms implemented across the banking platform, complete with mathematical formulas, architectural rationale, and pseudocode.

---

## 1. Token-Bucket Rate Limiter (Experiment 6)

### 1.1 Formula and Theory
The token bucket algorithm controls request rates by adding tokens to a bucket of maximum capacity $C$ at a constant refill rate $R$ (tokens per second).

When a request arrives at timestamp $t_{\text{now}}$ requiring $k$ tokens:
1. Calculate elapsed time: $\Delta t = \max(0, t_{\text{now}} - t_{\text{last}})$.
2. Calculate replenished tokens:
   $$\text{tokens} = \min\left(C, \text{oldTokens} + \Delta t \times R\right)$$
3. If $\text{tokens} \ge k$:
   - $\text{tokens} = \text{tokens} - k$
   - $t_{\text{last}} = t_{\text{now}}$
   - Allow request (HTTP 200).
4. Else:
   - Reject request with **HTTP 429 Too Many Requests**.

### 1.2 Redis Lua Script Implementation (`token_bucket.lua`)
```lua
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local requested = tonumber(ARGV[3])
local now = tonumber(ARGV[4])

local data = redis.call('HMGET', key, 'tokens', 'lastRefill')
local tokens = tonumber(data[1])
local lastRefill = tonumber(data[2])

if tokens == nil or lastRefill == nil then
    tokens = capacity
    lastRefill = now
else
    local elapsed = math.max(0, now - lastRefill)
    tokens = math.min(capacity, tokens + (elapsed * refillRate))
    lastRefill = now
end

if tokens >= requested then
    tokens = tokens - requested
    redis.call('HMSET', key, 'tokens', tostring(tokens), 'lastRefill', tostring(lastRefill))
    redis.call('EXPIRE', key, 3600)
    return { 1, tostring(tokens), tostring(capacity) }
else
    redis.call('HMSET', key, 'tokens', tostring(tokens), 'lastRefill', tostring(lastRefill))
    redis.call('EXPIRE', key, 3600)
    return { 0, tostring(tokens), tostring(capacity) }
end
```

---

## 2. Consistent Hashing with Virtual Nodes (Experiment 5)

### 2.1 The Hash Ring Concept
A circular hash ring maps the output of a 64-bit hash function ($0$ to $2^{64}-1$).
- Each physical node $S_i$ is mapped to $V$ virtual nodes on the ring:
  $$\text{Hash}(S_i \# \text{VN}_j) \quad \text{for } j \in [0, V-1]$$
- When a key $K$ arrives, its hash $H(K)$ is calculated.
- The ring is traversed clockwise (via `tailMap` in Java `TreeMap`) to find the first virtual node whose hash is $\ge H(K)$. If no such node exists, it wraps around to `firstKey()`.

### 2.2 Minimal Key Remapping Property
When adding node $N+1$, only keys whose hashes fall in the segments newly covered by the new node are moved:
$$\text{Expected Remapped Keys} \approx \frac{K}{N+1}$$
In a traditional modulo hash ($hash(K) \pmod N$), adding one node remaps nearly $100\%$ of keys, causing massive cache stampedes or data migrations.

---

## 3. Trie-Based Autocomplete (Experiment 7)

### 3.1 Data Structure & Traversal
A Trie (prefix tree) stores character sequences along edges.
- Each node contains:
  - `Map<Character, TrieNode> children`
  - `List<AutocompleteSuggestion> suggestions` (Pre-sorted top $M$ ranked suggestions)
  - `boolean isEndOfWord`

### 3.2 Time Complexity
- **Insertion**: $O(L \log M)$ where $L$ is the word length and $M$ is the suggestion limit.
- **Prefix Search**: $O(P)$ where $P$ is the prefix length! Retrieval is independent of the total dictionary size $N$.

### 3.3 Redis Cache-Aside Layer
To avoid traversing the in-memory Trie for high-frequency prefixes (e.g., `"elec"`, `"wat"`), prefix results are cached in Redis:
1. Lookup `cache:autocomplete:{normalizedPrefix}` in Redis.
2. If hit: return cached suggestions immediately ($< 1$ ms).
3. If miss: traverse Java Trie, return result, and save to Redis with a 300-second TTL.

---

## 4. URL Shortener with Collision Handling (Experiments 3 & 4)

### 4.1 MD5 6-Character Hash & Collision Resolution
1. Compute MD5 digest of permitted URL path (e.g., `/synthetic/page1.html`).
2. Take first 6 hexadecimal characters:
   $$\text{code} = \text{MD5}(URL)[0..5]$$
3. Query MongoDB for existing mapping:
   - If found and target matches: return existing code.
   - If found with **different** target URL (Collision!):
     - Append deterministic salt: $\text{MD5}(URL + \text{"#salt"} + \text{attempt})[0..5]$
     - Increment collision resolution metric.
4. Store in MongoDB and cache in Redis (`cache:shorturl:{code}`).

---

## 5. Synthetic Web Crawler (Experiment 8)

### 5.1 Breadth-First Traversal (BFS)
```text
Initialize Queue Q with seedUrl
Initialize Redis Set VisitedSet

While Q is not empty and pagesProcessed < maxPages:
    currentUrl = Q.dequeue()
    If currentUrl in VisitedSet:
        duplicatesAvoided++
        continue
    
    Add currentUrl to VisitedSet
    pagesProcessed++
    
    HTML = FetchLocalClasspathResource(currentUrl)
    doc = Jsoup.parse(HTML)
    
    // Extract Records
    For each card in doc.select(".biller-card"):
        Save extracted biller record
        
    // Discover Links
    For each link in doc.select("a[href]"):
        If link starts with "/synthetic/":
            If link not in VisitedSet:
                Q.enqueue(link)
            Else:
                duplicatesAvoided++
```
- **SSRF Prevention**: The crawler strictly verifies that URLs begin with the permitted local prefix `/synthetic/` and loads resources exclusively from internal classpath static assets, completely preventing Server-Side Request Forgery.
