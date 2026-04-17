--- acquire_token.lua
--- Atomic Multi-Plan Token Bucket Implementation
--- KEYS: alternating [bucket_key_1, config_key_1, bucket_key_2, config_key_2, ...]
--- ARGV[1]: requested tokens

local requested = tonumber(ARGV[1])

if #KEYS == 0 or #KEYS % 2 ~= 0 then
    return redis.error_reply("KEYS must be alternating pairs of bucket_key and config_key")
end

-- 1. Call TIME once for all plans
local time_res = redis.call('TIME') 
local now_ms = (tonumber(time_res[1]) * 1000) + math.floor(tonumber(time_res[2]) / 1000)

local allow_all = true
local min_remaining = math.huge
local max_wait_ms = 0
local temp_updates = {} -- Store planned updates: bucket_key -> remaining

-- 2. Read and Calculate Phase
for i = 1, #KEYS, 2 do
    local bucket_key = KEYS[i]
    local config_key = KEYS[i+1]
    
    local config = redis.call('HMGET', config_key, 'capacity', 'refillRate')
    local capacity = tonumber(config[1])
    local refill_rate = tonumber(config[2])

    if not capacity or not refill_rate then
        return redis.error_reply("Rate limit configuration missing for: " .. config_key)
    end

    local state = redis.call('HMGET', bucket_key, 't', 'ts')
    local current_tokens = tonumber(state[1]) or capacity
    local last_refill = tonumber(state[2]) or 0

    local delta_ms = math.max(0, now_ms - last_refill)
    local refill = delta_ms * (refill_rate / 1000.0)
    local updated_tokens = math.min(capacity, current_tokens + refill)

    if updated_tokens >= requested then
        -- This plan allows it
        local remaining = updated_tokens - requested
        min_remaining = math.min(min_remaining, remaining)
        temp_updates[bucket_key] = remaining
    else
        -- This plan denies it
        allow_all = false
        min_remaining = math.min(min_remaining, updated_tokens)
        local wait_ms = math.ceil((requested - updated_tokens) * (1000.0 / refill_rate))
        max_wait_ms = math.max(max_wait_ms, wait_ms)
    end
end

-- 3. Commit Phase (All-or-Nothing)
if not allow_all then
    return {0, min_remaining, max_wait_ms}
end

for bucket_key, remaining in pairs(temp_updates) do
    redis.call('HSET', bucket_key, 't', remaining, 'ts', now_ms)
end

return {1, min_remaining, 0}