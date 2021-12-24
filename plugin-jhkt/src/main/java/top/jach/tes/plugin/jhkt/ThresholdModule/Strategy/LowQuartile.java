package top.jach.tes.plugin.jhkt.ThresholdModule.Strategy;

import top.jach.tes.plugin.jhkt.ThresholdModule.Interface.interface_threshold;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author:AdminChen
 * @date:2020/10/9
 * @description:
 */
public class LowQuartile implements interface_threshold {
    private List<Double> data;
    private double result;

    public LowQuartile(){}

    public LowQuartile(List<Double> data){
        this.data=data;
        calculate();
    }

    public void setData(List<Double> data) {
        this.data=data;
    }

@Override
    public double getResult() {
        return result;
    }
@Override
    public void calculate(){
        // 判断传入的数据个数是否>=4
        if(data.size()<4){
            result=0d;
            return;
        }
        ArrayList<Double> temp = new ArrayList<>(data);
        Collections.sort(temp);
        int count = temp.size();
        double q1_index = (count + 1) * 0.25;
        double q3_index = (count + 1) * 0.75;
        double q1, q3;
        // 数据总数+1能够被4整除
        if ((count+1)%4==0) {
            q1 = temp.get((int) q1_index - 1);
            q3 = temp.get((int) q3_index - 1);
        }
        // 数据总数+1不能被4整除
        else {
            double q1_p2 = q1_index - (int) q1_index;
            double q1_p1 = (int) q1_index + 1 - q1_index;
            double q3_p2 = q3_index - (int) q3_index;
            double q3_p1 = (int) q3_index + 1 - q3_index;
            q1 = temp.get((int) q1_index - 1) * q1_p1 + temp.get((int) q1_index) * q1_p2;
            q3 = temp.get((int) q3_index - 1) * q3_p1 + temp.get((int) q3_index) * q3_p2;
        }
        double iqr = q3 - q1;
        result= q1 - iqr * 1.5;
    }
}
