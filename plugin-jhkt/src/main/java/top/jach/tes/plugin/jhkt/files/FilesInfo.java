package top.jach.tes.plugin.jhkt.files;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import top.jach.tes.core.api.domain.info.Info;
import top.jach.tes.plugin.tes.code.repo.WithRepo;

import java.util.List;

/**
 * @author:AdminChen
 * @date:2020/6/28
 * @description:
 * 作为TreesInfo的一般层级，向应用层提供代码仓文件信息的数据，
 * 按设计应作为TreesInfo的父类，目前只新增还未修改继承关系
 * 关于注解问题，为什么有些地方用getter注解，有些用data,直接用data不好吗
 */
@Getter
@Setter
public class FilesInfo extends Info implements WithRepo {
    Long reposId;
    String repoName;
    String version;
    String sha;
    List<Files> files;

    public static FilesInfo createInfo(){
        FilesInfo info=new FilesInfo();
        info.initBuild();
        return info;
    }

    @Override
    public String getRepoName() {
        return null;
    }

    @Override
    public WithRepo setRepoName(String repoName) {
        return null;
    }

    @Override
    public Long getReposId() {
        return null;
    }

    @Override
    public WithRepo setReposId(Long reposId) {
        return null;
    }
}
