package top.jach.tes.app.jhkt.lijiaqi.result;

import lombok.Data;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvAction;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvResult;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvValue;
import top.jach.tes.plugin.jhkt.microservice.Microservice;
import top.jach.tes.plugin.tes.code.git.commit.GitCommit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Mv {
    Integer len;
    Integer minCommitCount;
    Integer minCoCommit;
    Double minPer;
    Map<String, MvValue> mvValues;
    MvResult mvResult;

    //接下来两个方法是新修改的检测mv的算法，名称还是和最后两个方法一样
    public static List<Mv> CalculateMvs(int[] lens,int[] minCommitCounts,int[] minCoCommits,double[] minPers,List<GitCommit> gitCommits, List<Microservice> microservices){
        List<Mv> mvs = new ArrayList<>();
        for (int len:
                lens) {
            mvs.addAll(CalculateMvs(len, minCommitCounts, minCoCommits,minPers, gitCommits, microservices));
        }
        return mvs;
    }

    public static List<Mv> CalculateMvs(int len,int[] minCommitCounts,int[] minCoCommits,double[] minPers,List<GitCommit> gitCommits, List<Microservice> microservices){
        MvResult mvResult = MvAction.detectMvResult(gitCommits, len, microservices); // 所有文件间在相邻提交中同时出现的次数
        List<Mv> mvs = new ArrayList<>();
        for (int mcc :
                minCommitCounts) {
            for(int mco:minCoCommits){
                for (double mp :
                        minPers) {
                    Map<String, MvValue> mvValues= mvResult.calculateMvValues(mcc,mco,mp); // 通过最小提交次数和最小同时提交比例筛出符合不要求的隐形依赖关系
                    Mv mv = new Mv();
                    mv.setLen(len);
                    mv.setMinCommitCount(mcc);
                    mv.setMinCoCommit(mco);
                    mv.setMinPer(mp);
                    mv.setMvValues(mvValues);
                    mv.setMvResult(mvResult);
                    mvs.add(mv);
                }
            }
        }
        return mvs;
    }

//临近 超过最少提交次数才认为可能存在隐形依赖 AB共同提交的次数占
    public static List<Mv> CalculateMvs(int[] lens, int[] minCommitCounts, double[] minPers, List<GitCommit> gitCommits, List<Microservice> microservices){
        List<Mv> mvs = new ArrayList<>();
        for (int len:
                lens) {
            mvs.addAll(CalculateMvs(len, minCommitCounts, minPers, gitCommits, microservices));
        }
        return mvs;
    }

    public static List<Mv> CalculateMvs(int len, int[] minCommitCounts, double[] minPers, List<GitCommit> gitCommits, List<Microservice> microservices){
        MvResult mvResult = MvAction.detectMvResult(gitCommits, len, microservices); // 所有文件间在相邻提交中同时出现的次数
        List<Mv> mvs = new ArrayList<>();
        for (int mcc :
                minCommitCounts) {
            for (double mp :
                    minPers) {
                Map<String, MvValue> mvValues= mvResult.calculateMvValues(mcc, mp); // 通过最小提交次数和最小同时提交比例筛出符合不要求的隐形依赖关系
                Mv mv = new Mv();
                mv.setLen(len);
                mv.setMinCommitCount(mcc);
                mv.setMinPer(mp);
                mv.setMvValues(mvValues);
                mv.setMvResult(mvResult);
                mvs.add(mv);
            }
        }
        return mvs;
    }
}
