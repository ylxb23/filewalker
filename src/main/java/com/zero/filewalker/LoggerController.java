package com.zero.filewalker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zero
 * @since 2025/2/18 14:32
 */
@RestController
@RequestMapping(path = "/loggers")
public class LoggerController {
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(LoggerController.class);

    @GetMapping()
    public ResponseEntity<?> getLoggers() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        // 获取系统中定义的所有 logger
        List<Map<String, String>> loggers = loggerContext.getLoggerList().stream().map(logger -> {
            // 映射为 Map，key 是 logger 名称，value 是其日志级别
            // logger名称 = logger有效级别
            return Collections.singletonMap(logger.getName(), logger.getEffectiveLevel().levelStr);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(loggers);
    }

    @PostMapping
    public ResponseEntity<?> setLevel(@RequestParam String name, @RequestParam String level) {
        // 获取到 LoggerContext
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // 根据指定的名称获取 logger
        Logger log = loggerContext.exists(name);
        if (log == null) {
            return ResponseEntity.badRequest().body(name + " 日志记录器不存在");
        }

        // 解析 level 参数，第二个参数表示当 level 参数非法时的默认值
        Level newLevel = Level.toLevel(level, null);

        if (newLevel == null) {
            return ResponseEntity.badRequest().body(level + " 不是合法的日志级别");
        }
        List<Logger> list = log.getLoggerContext().getLoggerList();
        List<String> names = new ArrayList<>();
        for(Logger item : list) {
            if(item.getName().startsWith(name)) {
                names.add(item.getName());
            }
        }
        // 重写设置 logger 的 level
        log.setLevel(newLevel);
        Map<String, Object> result = new HashMap<>();
        result.put("names", names);
        result.put("level", newLevel.toString());
        return ResponseEntity.ok(result);
    }
}
