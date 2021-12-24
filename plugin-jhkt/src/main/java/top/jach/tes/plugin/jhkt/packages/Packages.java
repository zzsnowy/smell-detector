package top.jach.tes.plugin.jhkt.packages;

import lombok.Data;
import top.jach.tes.plugin.jhkt.files.Files;

import java.util.List;

/**
 * @author:AdminChen
 * @date:2020/6/28
 * @description:记录每个包的信息，每个包下包含哪些文件
 */
@Data
public class Packages {
    String name;
    String path;
    Long codeLineNum;
    List<Files> files;
}
