local shutdown = cct_eval.shutdown
local do_shutdown  = function()
    shutdown()
    while true do coroutine.yield() end
end
os.reboot, os.shutdown = do_shutdown, do_shutdown

_G.cct_eval = nil
if not _G.http then
    _G.http = setmetatable({}, {__index = function() error("HTTP is currently disabled on eval.tweaked.cc", 2) end})
end

term.clear()
term.setCursorPos(1, 1)

local h = fs.open("code.lua", "r")
local contents = h.readAll()
h.close()

fs.delete("startup.lua")
fs.delete("code.lua")

local offset, fn, err = 7, load("return " .. contents, "@/startup.lua", nil, _ENV)
if not fn then offset, fn, err = 0, load(contents, "@/startup.lua", nil, _ENV) end

if not fn then
    -- Naughty, but I can get away with it :p.
    local parser = require "cc.internal.syntax"
    if parser.parse_repl(contents) then printError(err) end
else
    local exception = require "cc.internal.exception"

    local result = table.pack(exception.try(fn))
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
        exception.report(result[2], result[3], {
            ["@/startup.lua"] = { contents = contents, offset = offset }
        })
    end
end

do_shutdown()
