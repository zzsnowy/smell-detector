package top.jach.tes.plugin.jhkt.arcsmell.mv;

import lombok.Data;

import java.util.List;

@Data
public class MvValue {
    Double dependency;//微服务涉及mv的次数，包括自己是source和target
    Double doubleDependency; // 双方概率都超过阈值
    Integer file;//微服务涉及mv的文件个数（不重复的计数）
    Integer doubleFile;
    List<MvDetail> mvDetailList;//存储每个微服务下存在mv的文件对信息

}
