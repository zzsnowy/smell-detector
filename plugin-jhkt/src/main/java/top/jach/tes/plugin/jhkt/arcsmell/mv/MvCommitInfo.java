package top.jach.tes.plugin.jhkt.arcsmell.mv;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import top.jach.tes.core.api.domain.info.Info;
import top.jach.tes.plugin.tes.code.git.commit.GitCommit;

import java.util.List;
import java.util.Map;

/**
 * @author:AdminChen
 * @date:2020/8/16
 * @description:
 */
@Getter
@Setter
public class MvCommitInfo extends Info {
    List<GitCommit> commitList;
    Map<String, Map<MvCommit,Integer>> mvCommits;//MvCommit里的下标对应commitList里的下标对应的GitCommit数据

    public MvCommitInfo(List<GitCommit> gitCommits, Map<String, Map<MvCommit, Integer>> resultCommits) {
        this.commitList=gitCommits;
        this.mvCommits=resultCommits;
    }

    public MvCommitInfo() {

    }

    public static MvCommitInfo createInfo(){
        MvCommitInfo info=new MvCommitInfo();
        info.initBuild();
        return info;
    }

}
