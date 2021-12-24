package top.jach.tes.app.jhkt.chenjiali;

import java.util.*;

/**
 * @author:AdminChen
 * @date:2020/10/16
 * @description:
 */
public class AnalysisFor {
    public static void main(String[] args) {

    }

    public static double[] getfiqr(List<Double> list){
        Double[] arrtmp=list.toArray(new Double[list.size()]);
        double[] arr=new  double[arrtmp.length];
        for(int i=0;i<arrtmp.length;i++){
            arr[i]=arrtmp[i];
        }
        double[] arr1=new double[4];
        if(arr.length<4){
            for(int i=0;i<arr.length;i++){
                arr1[i]=arr[i];
            }

            for(int k=arr.length;k<4;k++){
                arr1[k]=arr1[k];
            }
            return arr1;
        }

        double[] tempArr=Arrays.copyOf(arr,arr.length);
        Arrays.sort(tempArr);
        double[] quartiles=new double[3];
        double[] yichang=new double[2];
        int n=arr.length;




        return yichang;
    }


}
