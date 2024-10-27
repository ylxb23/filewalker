package com.zero.filewalker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;

/**
 * @author zero
 * @since 2024/7/2 11:33
 */
@EnableAutoConfiguration
@ComponentScan(basePackages = { "com.zero.filewalker" })
@SpringBootConfiguration
public class App {
    public static String ROOT = "";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar fileWalker.jar -Droot.path=<绝对路径> ");
            System.exit(1);
        }
        String rootPath = args[0];
        File root = new File(rootPath);
        if (!root.exists() || !root.isDirectory()) {
            System.err.println("根路径[" + rootPath + "]非法!");
            System.exit(1);
        }
        ROOT = rootPath;
        SpringApplication.run(App.class);
        System.out.println("根路径为：" + rootPath);
    }
}
