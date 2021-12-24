package top.jach.tes.plugin.jhkt.files;

import lombok.Data;
import lombok.NoArgsConstructor;
import top.jach.tes.plugin.tes.code.git.tree.TreesInfo;

/**
 * @author:AdminChen
 * @date:2020/6/28
 * @description:
 * 抽象层面表示一个代码文件的类，按计划是要作为Tree的父类的，但Tree直接继承Files没道理啊
 * 直接将TreesInfo作为Files的一个私有属性，若当前Files现有属性满足不了需求时，随时把TreesInfo拿出来取更丰富的数据
 */

@Data
@NoArgsConstructor
public class Files {
    String fileName;
    String filePath;
    FileType fileType;
    Long codeLineNum;
    String packagePath;
    private TreesInfo treesInfo;

}
