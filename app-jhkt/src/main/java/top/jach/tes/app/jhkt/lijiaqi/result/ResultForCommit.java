package top.jach.tes.app.jhkt.lijiaqi.result;

import lombok.Getter;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvCommitInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author:AdminChen
 * @date:2020/8/16
 * @description:
 */
@Getter
public class ResultForCommit {
    Map<String, MvCommitInfo> results=new LinkedHashMap<>();

    public ResultForCommit(){}

    public void put(String version,MvCommitInfo mvCommitInfo){
        results.put(version,mvCommitInfo);
    }
}
