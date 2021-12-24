package top.jach.tes.plugin.jhkt.arcsmell.concernOverload;

/**
 * @author:AdminChen
 * @date:2020/11/13
 * @description:
 */
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JsonObjectForTrainModel {
    public int K;
    public int M;
    public int V;
    public double[][] theta;
    public double[][] phi;

    public JsonObjectForTrainModel(){}
    public JsonObjectForTrainModel(int K,int M,int V,double[][] theta,double[][] phi){
        this.K=K;
        this.M=M;
        this.V=V;
        this.theta=theta;
        this.phi=phi;
    }

}
