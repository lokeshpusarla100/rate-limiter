--- acquire_token.lua
--- Atomic Token Bucket Implementation for Distributed Rate Limiting
--- 
--- Purpose: Executes a single token bucket check and consumption operation atomically.
--- Adheres to: ADR-004 (Redis Strategy), ADR-005 (Configuration), ADR-007 (Time Consistency)
---
--- KEYS[1]: bucket_key  - The Redis key for the bucket state (e.g., ratelimiter:default:user_1:gold)
--- KEYS[2]: config_key  - The Redis key for the plan configuration (e.g., config:plan:gold)
--- ARGV[1]: requested   - The number of tokens to consume for this request

local bucket_key = KEYS[1]
local config_key = KEYS[2]
local requested = tonumber(ARGV[1])

-------------------------------------------------------------------------------
-- 1. FETCH CONFIGURATION
-- We retrieve capacity and refill rate from a separate configuration key.
-- This allows for 'Hot Reloading' of limits without application restart.
-------------------------------------------------------------------------------
local config = redis.call('HMGET', config_key, 'capacity', 'refillRate')
local capacity = tonumber(config[1])
local refill_rate = tonumber(config[2])

-- Validate config exists; if not, we cannot proceed (Fail-Fast within script)
if not capacity or not refill_rate then
    return redis.error_reply("Rate limit configuration missing for: " .. config_key)
end

-------------------------------------------------------------------------------
-- 2. FETCH BUCKET STATE
-- 't'  : Current token count (double precision stored as string/binary)
-- 'ts' : Last refill timestamp in epoch milliseconds
-------------------------------------------------------------------------------
local state = redis.call('HMGET', bucket_key, 't', 'ts')
local current_tokens = tonumber(state[1]) or capacity
local last_refill = tonumber(state[2]) or 0

-------------------------------------------------------------------------------
-- 3. DISTRIBUTED TIME SYNCHRONIZATION
-- We use the Redis server's time to eliminate clock skew between app instances.
-------------------------------------------------------------------------------
local time_res = redis.call('TIME') -- returns {seconds, microseconds}
local now_ms = (tonumber(time_res[1]) * 1000) + math.floor(tonumber(time_res[2]) / 1000)

-------------------------------------------------------------------------------
-- 4. REFILL CALCULATION
-- Calculate tokens to add based on elapsed time since the last refill.
-------------------------------------------------------------------------------
local delta_ms = math.max(0, now_ms - last_refill)
local refill = delta_ms * (refill_rate / 1000.0)
local updated_tokens = math.min(capacity, current_tokens + refill)

-------------------------------------------------------------------------------
-- 5. CONSUMPTION LOGIC
-------------------------------------------------------------------------------
local allowed = 0
local remaining = updated_tokens
local wait_ms = 0

if updated_tokens >= requested then
    -- Successful acquisition
    allowed = 1
    remaining = updated_tokens - requested
    
    -- 6. PERSIST UPDATED STATE
    -- We save the new token count and the current timestamp for the next refill.
    redis.call('HSET', bucket_key, 't', remaining, 'ts', now_ms)
else
    -- Acquisition denied
    -- Calculate how many milliseconds the client should wait before retrying.
    wait_ms = math.ceil((requested - updated_tokens) * (1000.0 / refill_rate))
end

-------------------------------------------------------------------------------
-- 7. RETURN RESULT
-- Format: [allowed (boolean-like long), remainingTokens (double), waitMillis (long)]
-------------------------------------------------------------------------------
return {allowed, remaining, wait_ms}