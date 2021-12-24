package top.jach.tes.plugin.jhkt.arcsmell.mv;

import lombok.Getter;
import lombok.Setter;
import top.jach.tes.plugin.tes.code.git.commit.GitCommit;

import java.util.Set;

/**
 * @author:AdminChen
 * @date:2020/8/12
 * @description:
 */
@Getter
@Setter
public class MvCommit {
    private String sourceFile;//存在mv的文件
    private int sourceCommitIndex;//该文件存在于哪次提交里面，根据提交找到sha，从而知道发生mv的提交有哪些，这些提交是因为哪些文件而发生mv的
    private String targetFile;
    private int targetCommitIndex;

    public MvCommit(String file, int i, String tfile, int j) {
        this.sourceFile=file;
        this.sourceCommitIndex=i;
        this.targetFile=tfile;
        this.targetCommitIndex=j;
    }
}
