-- Redis Lua script for Token Bucket Rate Limiter (Experiment 6)
-- KEYS[1]: bucket key (e.g. rate_limit:login:ip_or_user)
-- ARGV[1]: capacity (max tokens)
-- ARGV[2]: refillRate (tokens per second)
-- ARGV[3]: requested (tokens to consume, usually 1)
-- ARGV[4]: currentTime (current timestamp in seconds as float)

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

local allowed = 0
local remainingTokens = tokens

if tokens >= requested then
    allowed = 1
    tokens = tokens - requested
    remainingTokens = tokens
    redis.call('HMSET', key, 'tokens', tostring(tokens), 'lastRefill', tostring(lastRefill))
    redis.call('EXPIRE', key, 3600)
else
    allowed = 0
    redis.call('HMSET', key, 'tokens', tostring(tokens), 'lastRefill', tostring(lastRefill))
    redis.call('EXPIRE', key, 3600)
end

return { allowed, tostring(remainingTokens), tostring(capacity) }
