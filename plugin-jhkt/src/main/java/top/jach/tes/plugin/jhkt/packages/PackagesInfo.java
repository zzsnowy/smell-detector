package top.jach.tes.plugin.jhkt.packages;

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
 */
@Getter
@Setter
public class PackagesInfo extends Info implements WithRepo {
    Long reposId;
    String repoName;
    String version;
    List<Packages> packages;

    public static PackagesInfo createInfo(){
        PackagesInfo info=new PackagesInfo();
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
