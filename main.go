package main

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"
)

var ROOT string

func main() {
	if len(os.Args) < 2 {
		fmt.Println("Usage: 输入根路径（绝对路径），如：\n \tcommand <absPath> ")
		return
	}
	// 从命令行参数获取文件路径
	root := os.Args[1]
	// 判断root路径存在且是否为文件夹，如果不是则退出
	if _, err := os.Stat(root); err != nil {
		if os.IsNotExist(err) {
			fmt.Printf("根路径不存在, Error: %s", err)
			return
		}
	}
	// 路径分隔符
	const fsp string = string(filepath.Separator)
	// 如果root不以"/"字符结尾，则追加"/"
	if root[len(root)-1:] != fsp {
		root = root + fsp
	}
	ROOT = root
	// http监听
	mux := http.NewServeMux()
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		requestPath := r.URL.Path
		path := ROOT + requestPath

		// 判断文件路径是否合法
		pathInfo, err := os.Stat(path)
		if err != nil {
			w.Header().Set("Content-Type", "application/text")
			if os.IsNotExist(err) {
				w.Header().Set("Content-Type", "application/text")
				w.WriteHeader(http.StatusNotFound)
				fmt.Fprintf(w, "文件路径不存在")
			} else {
				w.WriteHeader(http.StatusInternalServerError)
				fmt.Fprintf(w, "文件路径不存在")
			}
			return
		}
		// 组合起来的路径如果是文件夹，则执行 walkDir(path)
		if pathInfo.IsDir() {
			fileList, _ := walkDir(path)
			w.Header().Set("Content-Type", "text/html")
			fmt.Fprint(w, wrapHtmlContent(requestPath, fileList))
			return
		} else {
			// 否则执行文件下载
			w.Header().Set("Content-Disposition", fmt.Sprintf("attachment; filename=\"%s\"", pathInfo.Name()))
			w.Header().Set("Content-Type", "application/octet-stream")
			http.ServeFile(w, r, path)
			return
		}
	})

	fmt.Printf("Starting filewalker on port 8888... \n")
	err := http.ListenAndServe(":8888", mux)
	if err != nil {
		fmt.Println("文件服务启动失败: ", err)
	}
}

type FileInfo struct {
	Name    string    `json:"name"`
	Size    int64     `json:"size"`
	IsDir   bool      `json:"isDir"`
	ModTime time.Time `json:"modTime"`
	Path    string    `json:"path"`
	Ext     string    `json:"ext"`
}

func walkDir(dirPath string) ([]FileInfo, error) {
	fmt.Printf("请求路径: %s \n", dirPath)
	var infos []FileInfo
	items, err := ioutil.ReadDir(dirPath)
	if err != nil {
		return nil, err
	}
	for _, item := range items {
		infos = append(infos, FileInfo{
			Name:    item.Name(),
			IsDir:   item.IsDir(),
			Size:    item.Size(),
			ModTime: item.ModTime(),
		})
	}
	return infos, nil
}

var htmlPattern = `
<!DOCTYPE html>
<html lang="zh">

<head>
  <title>file walker</title>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="Content-Type" content="charset=utf-8">
  <style>
	li {
		line-height: 30px;
		white-space: nowrap;
		overflow: hidden;
		width: 500px;
		
		text-overflow: ellipsis;
		list-style-type: none;
		display: list-item;
		unicode-bidi: isolate;
	}
	a {
	    display: inline-block;
	}
	.left {
	    float: left;
	}
	.right {
	    float: right;
	}
  </style>
</head>

<body>
  <div>
	<ul>
	%s
	</ul>
  </div>
</body>

</html>`

var dirLinePattern = "<li><span class=\"left\">📂</span><a href=\"%s\" ><span class=\"left\">%s</span></a> <span class=\"right\">-</span></li>\n"
var filLinePattern = "<li><span class=\"left\">📄</span><a href=\"%s\" ><span class=\"left\">%s</span></a> <span class=\"right\">%s</span></li>\n"

var b int64 = 1
var kb int64 = b << 10
var mb int64 = kb << 10
var gb int64 = mb << 10
var tb int64 = gb << 10
var pb int64 = tb << 10

func toHumanSize(size int64) string {
	if size > pb {
		return fmt.Sprintf("%.3fPB", float64(size)/float64(pb))
	} else if size > tb {
		return fmt.Sprintf("%.3fTB", float64(size)/float64(tb))
	} else if size > gb {
		return fmt.Sprintf("%.3fGB", float64(size)/float64(gb))
	} else if size > mb {
		return fmt.Sprintf("%.3fMB", float64(size)/float64(mb))
	} else if size > kb {
		return fmt.Sprintf("%.3fKB", float64(size)/float64(kb))
	} else {
		return fmt.Sprintf("%dB", size)
	}
}

func wrapHtmlContent(uri string, files []FileInfo) string {
	var content string = ""
	content += fmt.Sprintf(dirLinePattern, uri, ".")
	if uri != "/" {
		pre := uri[0:strings.LastIndex(uri, "/")]
		if pre == "" {
			pre = "/"
		}
		content += fmt.Sprintf(dirLinePattern, pre, "..")
	} else {
		uri = ""
	}
	for _, file := range files {
		if file.IsDir {
			content += fmt.Sprintf(dirLinePattern, uri+"/"+file.Name, file.Name)
		} else {
			content += fmt.Sprintf(filLinePattern, uri+"/"+file.Name, file.Name, toHumanSize(file.Size))
		}
	}
	return fmt.Sprintf(htmlPattern, content)
}
