package com.zero.filewalker;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zero
 * @since 2024/7/2 11:37
 */
@RestController
public class FileWalkerController {

    static final String CHARACTER_SET = "UTF-8";

    @RequestMapping(path = "/**", method = RequestMethod.GET)
    public ResponseEntity<?> walk(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        uri = URLDecoder.decode(uri, CHARACTER_SET);
        System.out.println("请求路径：" + uri);
        // 组合路径
        String path = getRootPath() + uri;
        // 判断路径是否存在
        File curr = new File(path);
        if (!curr.exists()) {
            System.out.println("请求的路径不存在为：" + path);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.badRequest().body("请求的路径[" + uri + "]不存在");
        }
        if (curr.isDirectory()) {
            // 返回 html
            response.setContentType("text/html");
            return ResponseEntity.ok(wrapHtmlContent(uri, listFileOfPath(path)));
            // return ResponseEntity.ok(listFileOfPath(path));
        } else {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(curr.getName(), CHARACTER_SET) + "\"");
            try (FileInputStream fis = new FileInputStream(curr);
                    OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                return ResponseEntity.ok("");
            } catch (IOException e) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.setContentType("application/text");
                response.getOutputStream().write("".getBytes(StandardCharsets.UTF_8));
                return ResponseEntity.noContent().build();
            }
        }
    }

    private String wrapHtmlContent(String path, List<FileInfo> fileInfos) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(DIR_LINE_PATTERN, path, ".")); // .
        if ("/".equals(path)) {
            path = "";
        } else {
            String pre = path.substring(0, path.lastIndexOf("/"));
            sb.append(String.format(DIR_LINE_PATTERN, pre.isEmpty() ? "/" : pre, "..")); // ..
        }
        for (FileInfo i : fileInfos) {
            if (i.isDir) {
                sb.append(String.format(DIR_LINE_PATTERN, path + "/" + i.name, i.name));
            } else {
                sb.append(String.format(FIL_LINE_PATTERN, path + "/" + i.name, i.name, toHumanSize(i.size)));
            }
        }
        return String.format(HTML_PATTERN, sb);
    }

    static final long B = 1;
    static final long KB = 1 << 10;
    static final long MB = KB << 10;
    static final long GB = MB << 10;
    static final long TB = GB << 10;
    static final long PB = TB << 10;

    private static String toHumanSize(long size) {
        if (size > PB) {
            return String.format("%.3fPB", (double) (size >> 40) / 1024);
        } else if (size > TB) {
            return String.format("%.3fTB", (double) (size >> 30) / 1024);
        } else if (size > GB) {
            return String.format("%.3fGB", (double) (size >> 20) / 1024);
        } else if (size > MB) {
            return String.format("%.3fMB", (double) (size >> 10) / 1024);
        } else if (size > KB) {
            return String.format("%.3fKB", (double) size / 1024);
        } else {
            return String.format("%dB", size);
        }
    }

    static class FileInfo implements Serializable {
        String name;
        boolean isDir;
        long size;

        public FileInfo() {
        }

        public FileInfo(String n, boolean dir, long s) {
            this.name = n;
            this.isDir = dir;
            this.size = s;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isDir() {
            return isDir;
        }

        public void setDir(boolean dir) {
            isDir = dir;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    private List<FileInfo> listFileOfPath(String path) {
        try {
            List<FileInfo> list = new ArrayList<>();
            Path directoryPath = Paths.get(path);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath)) {
                for (Path file : stream) {
                    list.add(new FileInfo(file.getFileName().toString(), file.toFile().isDirectory(),
                            file.toFile().length()));
                }
                return list;
            } catch (IOException e) {
                System.err.println("An error occurred while listing files: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Invalid directory path.");
        }
        return new ArrayList<>();
    }

    public String getRootPath() {
        if (App.ROOT.endsWith(File.pathSeparator)) {
            return App.ROOT.substring(0, App.ROOT.length() - 1);
        }
        return App.ROOT;
    }

    static final String HTML_PATTERN = "<!DOCTYPE html>\n" +
            "<html lang=\"zh\">\n" +
            "\n" +
            "<head>\n" +
            "  <title>file walker</title>\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "  <meta http-equiv=\"Content-Type\" content=\"charset=utf-8\">\n" +
            "  <style>\n" +
            "\tli {\n" +
            "\t\tline-height: 30px;\n" +
            "\t\twhite-space: nowrap;\n" +
            "\t\toverflow: hidden;\n" +
            "\t\twidth: 500px;\n" +
            "\t\t\n" +
            "\t\ttext-overflow: ellipsis;\n" +
            "\t\tlist-style-type: none;\n" +
            "\t\tdisplay: list-item;\n" +
            "\t\tunicode-bidi: isolate;\n" +
            "\t}\n" +
            "\ta {\n" +
            "\t    display: inline-block;\n" +
            "\t}\n" +
            "\t.left {\n" +
            "\t    float: left;\n" +
            "\t}\n" +
            "\t.right {\n" +
            "\t    float: right;\n" +
            "\t}\n" +
            "  </style>\n" +
            "</head>\n" +
            "\n" +
            "<body>\n" +
            "  <div>\n" +
            "\t<ul>\n" +
            "%s" + // 文件列表
            "\t</ul>\n" +
            "  </div>\n" +
            "</body>\n" +
            "\n" +
            "</html>";
    static final String DIR_LINE_PATTERN = "<li><span class=\"left\">\uD83D\uDCC2</span><a href=\"%s\" ><span class=\"left\">%s</span></a> <span class=\"right\">-</span></li>\n";
    static final String FIL_LINE_PATTERN = "<li><span class=\"left\">\uD83D\uDCC4</span><a href=\"%s\" ><span class=\"left\">%s</span></a> <span class=\"right\">%s</span></li>\n";

}
