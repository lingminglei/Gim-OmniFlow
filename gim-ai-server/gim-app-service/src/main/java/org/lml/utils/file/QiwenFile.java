package org.lml.utils.file;

import com.qiwenshare.ufop.util.UFOPUtils;
import org.apache.commons.io.FilenameUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author MAC
 * @version 1.0
 * @description: TODO
 * @date 2022/4/21 12:08
 */
public class QiwenFile {

    private final String path;
    public static final String separator = "/";
    private boolean isDirectory;

    public static Executor exec = Executors.newFixedThreadPool(20);

    public QiwenFile(String pathname, boolean isDirectory) {
//        if (StringUtils.isEmpty(pathname)) {
//            throw new QiwenException("file name format error，pathname:" + pathname);
//        }
        this.path = formatPath(pathname);
        this.isDirectory = isDirectory;
    }

    public QiwenFile(String parent, String child, boolean isDirectory) {
//        if (StringUtils.isEmpty(child)) {
//            throw new QiwenException("file name format error，parent:" + parent +", child:" + child);
//        }
        if (parent != null) {
            String parentPath = separator.equals(formatPath(parent)) ? "" : formatPath(parent);
            String childPath = formatPath(child);
            if (childPath.startsWith(separator)) {
                childPath = childPath.replaceFirst(separator, "");
            }
            this.path = parentPath + separator + childPath;
        } else {
            this.path = formatPath(child);
        }
        this.isDirectory = isDirectory;
    }

    /**
     * 对路径做格式化处理：
     * 1、将所以不规范的 分隔符 转为 / 分隔符（将连续多个不符合规范的 分隔符 转为 单个）
     *
     * 2、如果路径头没有 “/”，那么就在路径前 添加 /
     *
     * 3、check 路径最后有没有 /,如果有就删除
     * @param path
     * @return
     */
    public static String formatPath(String path) {
        path = UFOPUtils.pathSplitFormat(path);
        if ("/".equals(path)) {
            return path;
        }
        if (!path.startsWith(separator)) {
            path = separator + path;
        }
        if (path.endsWith("/")) {
            int length = path.length();
            return path.substring(0, length - 1);
        }

        return path;
    }

    /**
     * 获取当前路径的父路径
     * @return
     */
    public String getParent() {
        if (separator.equals(this.path)) {
            return null;
        }
        if (!this.path.contains("/")) {
            return null;
        }
        int index = path.lastIndexOf(separator);
        if (index == 0) {
            return separator;
        }
        return path.substring(0, index);
    }

    /**
     * 它的作用是根据当前文件或目录的路径，创建并返回一个表示其“父目录”的QiwenFile对象。
     * @return
     */
    public QiwenFile getParentFile() {
        String parentPath = this.getParent();
        return new QiwenFile(parentPath, true);
    }

    /**
     * 它的作用是从当前路径中提取“文件名”或“目录名”
     * @return
     */
    public String getName() {
        int index = path.lastIndexOf(separator);
        if (!path.contains(separator)) {
            return path;
        }
        return path.substring(index + 1);
    }

    /**
     * 它的作用是获取当前路径的文件扩展名。
     * @return
     */
    public String getExtendName() {
        return FilenameUtils.getExtension(getName());
    }

    /**
     * 它的作用是获取当前路径的文件名（不包含扩展名）。
     * @return
     */
    public String getNameNotExtend() {
        return FilenameUtils.removeExtension(getName());
    }

    public String getPath() {
        return path;
    }

    /**
     * 它的作用是判断当前对象是否表示一个目录。
     * @return
     */
    public boolean isDirectory() {
       return isDirectory;
    }

    /**
     * 判断是否表示一个文件
     * @return
     */
    public boolean isFile() {
        return !isDirectory;
    }


}
