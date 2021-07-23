local shutdown = cct_eval.shutdown
local do_shutdown  = function()
    shutdown()
    while true do coroutine.yield() end
end
os.reboot, os.shutdown = do_shutdown, do_shutdown

_G.cct_eval = nil
_G.http = setmetatable({}, {__index = function() error("HTTP is currently disabled on eval.tweaked.cc", 2) end})

term.clear()
term.setCursorPos(1, 1)

local h = fs.open("code.lua", "r")
local contents = h.readAll()
h.close()

fs.delete("startup.lua")
fs.delete("code.lua")

local fn, err = load("return " .. contents, "@startup.lua", nil, _ENV)
if not fn then fn, err = load(contents, "@startup.lua", nil, _ENV) end

if not fn then
    printError(err)
else
    local result = table.pack(pcall(fn))
    if result[1] then
        local pretty = require "cc.pretty"
        for i = 2, result.n do
            local ok, serialised = pcall(pretty.pretty, result[i], { function_args = true })
            if ok then
                pretty.print(serialised)
            else
                print(tostring(result[i]))
            end
        end
    else
        printError(result[2])
    end
end

do_shutdown()
