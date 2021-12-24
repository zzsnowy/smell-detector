package top.jach.tes.plugin.jhkt.arcsmell.mv;

import lombok.Getter;
import lombok.Setter;

/**
 * @author:AdminChen
 * @date:2020/12/18
 * @description:
 */
@Getter
@Setter
public class MvDetail {
    String sourceMicro;
    String targetMicro;
    String sourceFile;
    String targetFile;

    public MvDetail(String sm,String tm,String sf,String tf){
        this.sourceMicro=sm;
        this.targetMicro=tm;
        this.sourceFile=sf;
        this.targetFile=tf;
    }
}
