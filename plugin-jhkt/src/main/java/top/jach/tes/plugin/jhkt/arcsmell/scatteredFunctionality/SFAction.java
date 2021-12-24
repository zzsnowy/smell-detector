package top.jach.tes.plugin.jhkt.arcsmell.scatteredFunctionality;

import lombok.Getter;
import lombok.Setter;
import top.jach.tes.plugin.jhkt.ThresholdModule.Strategy.HighQuartile;
import top.jach.tes.plugin.jhkt.ThresholdModule.Strategy.LowQuartile;


import java.io.*;
import java.lang.annotation.Target;
import java.util.*;

/**
 * @author:AdminChen
 * @date:2020/10/9
 * @description:
 */
@Getter
@Setter
public class SFAction {

    List<String> input_C;          //input_C,所有微服务的名字的集合              // C, a set of components
    List<String> input_T;//input_T,所有topic序号，比如第一个topic对应字符串1代表第一个topic//T, a set of system concerns
    HashMap<String,List<String>> component_topics;//每个微服务下包含哪些topic
    Map<String, Map<String, Double>> input_P;//每一个微服务名，对应的150个topic在该微服务下的p值                          // service Concern Probabilities
    int th_num;                                                                // threshold used to judge the entry candidate sequence
    double th_qua;                                                             // threshold used to judge whether orthogonal
    Map<String,List<String>> component_mainconcern;                   // main concern of component
    Map<String,List<Double>> concern_value;//input_T中第i个topic下所有word的相关度值                           // concern matrix
    LinkedHashMap<String,ArrayList<String>> SFsmells = new LinkedHashMap<>();  // result of ScatteredFunctionality
    LinkedHashMap<String,Integer> component_concerncount=new LinkedHashMap<>();
    //Map<String,String> zhengjiaomap=new HashMap<>();//记录部分正交的topic对
    boolean ifmanu_thzc;
    double manu_thzc;
    boolean ifmanu_thnum;
    double manu_thnum;
    int ifmanu_thqua;
    double manu_thqua;

    // 不带参数的构造函数
    SFAction() {
    }

    // 带参数的构造函数
    public SFAction(List<String> input_C, List<String> input_T, Map<String, Map<String, Double>> input_P, Map<String, List<Double>> concern_value,
                                 boolean ifmanu_thzc, double manu_thzc,
                                 boolean ifmanu_thnum, int manu_thnum,
                                 int ifmanu_thqua, double manu_thqua) {
        this.input_C = input_C;
        this.input_T = input_T;
        this.input_P = input_P;
        if(ifmanu_thnum)
            this.th_num=manu_thnum;
        else
            this.th_num=1;
        this.concern_value=concern_value;
        this.ifmanu_thzc=ifmanu_thzc;
        this.manu_thzc=manu_thzc;
        this.ifmanu_thnum=ifmanu_thnum;
        this.manu_thnum=manu_thnum;
        this.ifmanu_thqua=ifmanu_thqua;
        this.manu_thqua=manu_thqua;
    }

//新的可执行的SF检测算法
    public void CheckScatteredFunctionality(String path) throws IOException{

        BufferedReader reader = new BufferedReader(new FileReader(path));
        reader.readLine();//第一行不读
        String line = null;
        int index=0;
        Map<String,List<String>> mtopics=new HashMap<>();
        component_mainconcern = new HashMap<>();
        //读取每行，直到为空
        while((line=reader.readLine())!=null){
            String item[] = line.split(",");//CSV格式文件为逗号分隔符文件，这里根据逗号切分
            if(!"".equals(item[0])){
                String micro=item[0];
                int t_count=Integer.valueOf(item[1]);
                List<String> t_list=new ArrayList<>();
                for(int i=0;i<t_count;i++){
                    line=reader.readLine();
                    t_list.add(getDigit(line.split(",")[2]));
                }
                mtopics.put(micro,t_list);
                component_mainconcern.put(micro,t_list);
            }

        }


        /*InputStream ips = new FileInputStream(path);
        HSSFWorkbook wb = new HSSFWorkbook(ips);
        HSSFSheet sheet = wb.getSheetAt(0);
        Map<String,List<String>> mtopics=new HashMap<>();
        int rownum=1;
        while(rownum<=sheet.getLastRowNum()){
            if(sheet.getRow(rownum).getCell(0)!=null){
                String micro=sheet.getRow(rownum).getCell(0).getStringCellValue();
                int tcount=Integer.valueOf(sheet.getRow(rownum).getCell(1).getStringCellValue());
                List<String> t_list=new ArrayList<>();
                for(int i=1;i<tcount;i++){
                    t_list.add(getDigit(sheet.getRow(rownum+i).getCell(2).getStringCellValue()));
                }
                rownum+=tcount;
                //mtopics.put(micro,t_list);
                component_mainconcern.put(micro,t_list);
            }
            rownum++;
        }*/
        //component_mainconcern=mtopics;

   /*     Gson gson=new Gson();
        String str= FileUtils.readFileToString(new File(jsonpath),"utf8");
        JsonObjectForMC jmc=gson.fromJson(str,JsonObjectForMC.class);
        Map<String,List<String>> microTopics=new HashMap<>();//获取每个微服务下包含的topic集合
*/

        // 初始化变量
        SFsmells.clear();
        //根据每个微服务下有哪些topic相关，得到150个topic分别由0个或多个微服务实现的数据，也就是detail_info的内容
        HashMap<String, ArrayList<String>> componentConcerns = new HashMap<>();
        for(String t_str:input_T)//每个topic下与其相关的component集合
            componentConcerns.put(t_str,new ArrayList<>());
        // 遍历input_C
        for (String c : input_C) {
            List<String> Tc =component_mainconcern.get(c);//getComponentConcerns(c)换成直接从json文件里得到微服务下的topics
            for (String t : Tc)
                componentConcerns.get(t).add(c);
        }
     /*   // 统计每一个component的主要concern
        component_mainconcern = new HashMap<>();
        for (String c_str : input_C) {
            ArrayList<String> mainconcern = new ArrayList<>();
            ArrayList<Double> component_pvalue = new ArrayList<>(input_P.get(c_str).values());
            double th_mainconcern = new HighQuartile(component_pvalue).getResult();
            for (String t_str : input_T)
                if (input_P.get(c_str).get(t_str) > th_mainconcern)
                    mainconcern.add(t_str);
            component_mainconcern.put(c_str, mainconcern);
        }*/
        // 计算th_qua
        calculate_th_qua();

        // 遍历input_T
        for (String t : input_T) {
            /*if(!t.equals("22")){
                continue;
            }*/
            ArrayList<String> relatedComponent = new ArrayList<>();//每个topic分别由哪些微服务所实现
            // 获取Mt
            ArrayList<String> Mt = componentConcerns.get(t);
            if (Mt.size() > th_num)
                for (String mt : Mt)
                    relatedComponent.add(mt);
            // 初始化
            SFsmells.put(t, new ArrayList<>());
            // 检查是否存在关注点正交
            for (String c : relatedComponent) {
                if (ifComponentHasQuadatureConcerns(c, t))
                    SFsmells.get(t).add(c);
            }
        }
        // 统计每一个component的SF数量
        for(String c_str:input_C)
            component_concerncount.put(c_str,0);
        for(String t_str:input_T)
            for(String c_str:SFsmells.get(t_str)){
                int result_value=component_concerncount.get(c_str)+1;
                component_concerncount.put(c_str,result_value);
            }
    }
    //获取一个字符串中数字部分
    public String getDigit(String str){
        str=str.trim();
        String res="";
        if(str!=null&&!"".equals(str)){
            for(int i=0;i<str.length();i++){
                if(str.charAt(i)>=48&&str.charAt(i)<=57){
                    res+=str.charAt(i);
                }
            }
        }
        return res;
    }
    // 执行算法，得到结果
    public void CheckScatteredFunctionality() {
        // 初始化变量
        SFsmells.clear();
        HashMap<String, ArrayList<String>> componentConcerns = new HashMap<>();
        for(String t_str:input_T)
            componentConcerns.put(t_str,new ArrayList<>());//对每个topic而言，有哪些component是和它相关的
        // 遍历input_C
        for (String c : input_C) {
            ArrayList<String> Tc = getComponentConcerns(c);//先求每个componnet用四分位法求出的和它相关的topic集合
            for (String t : Tc)
                componentConcerns.get(t).add(c);//再反过来确定哪些topic的相关componnet集合里可以把这个componnet加到集合里去
        }
        // 统计每一个component的主要concern
        component_mainconcern = new HashMap<>();
        for (String c_str : input_C) {
            ArrayList<String> mainconcern = new ArrayList<>();
            ArrayList<Double> component_pvalue = new ArrayList<>(input_P.get(c_str).values());
            double th_mainconcern = new HighQuartile(component_pvalue).getResult();
            for (String t_str : input_T)
                if (input_P.get(c_str).get(t_str) > th_mainconcern)
                    mainconcern.add(t_str);
            component_mainconcern.put(c_str, mainconcern);
        }
        // 计算th_qua
        calculate_th_qua();

        // 遍历input_T
        for (String t : input_T) {
            ArrayList<String> relatedComponent = new ArrayList<>();
            // 获取Mt
            ArrayList<String> Mt = componentConcerns.get(t);
            if (Mt.size() > th_num)
                for (String mt : Mt)
                    relatedComponent.add(mt);
            // 初始化
            SFsmells.put(t, new ArrayList<>());
            // 检查是否存在关注点正交
            for (String c : relatedComponent) {//对于每个关注点而言，只要实现这个关注点的微服务中有一个微服务下的关注点与该关注点正交就认为符合
                    if (ifComponentHasQuadatureConcerns(c, t))
                        SFsmells.get(t).add(c);


            }
        }
        // 统计每一个component的SF数量
        for(String c_str:input_C)
            component_concerncount.put(c_str,0);
        for(String t_str:input_T)
            for(String c_str:SFsmells.get(t_str)){
                int result_value=component_concerncount.get(c_str)+1;
                component_concerncount.put(c_str,result_value);
            }
    }

    // 获取与某一个component相关的所有concerns
    private ArrayList<String> getComponentConcerns(String component) {
        // 构造componentConcernCounts
        ArrayList<String> result = new ArrayList<>();
        // 构建Tc列表
        ArrayList<String> Tc = new ArrayList<>(input_T);
        // 构建P(Tc)列表
        ArrayList<Double> P_Tc = new ArrayList<>();
        for (String t_str : Tc)
            P_Tc.add(input_P.get(component).get(t_str));
        // 获取th_zc
        double th_zc;
        if(ifmanu_thzc)
            th_zc=manu_thzc;
        else
            th_zc = new HighQuartile(P_Tc).getResult();
        // 遍历input_T
        for (String Tc_z : Tc) {
            if (input_P.get(component).get(Tc_z) > th_zc)
                result.add(Tc_z);
        }
        return result;
    }

    // 计算th_qua的值
    private void calculate_th_qua() {
        if (ifmanu_thqua == 3) {
            th_qua = manu_thqua;
            return;
        }
        ArrayList<Double> all_value = new ArrayList<>();
        // 穷举所有组合的可能性
        int size = concern_value.size();
        Set<String> all_key = concern_value.keySet();
        for (int i = 0; i < size - 1; i++)
            for (int j = i + 1; j < size; j++)
                all_value.add(calInnerProduct(concern_value.get(String.valueOf(i)), concern_value.get(String.valueOf(j))));
        if (ifmanu_thqua == 1)
            th_qua = new HighQuartile(all_value).getResult();
        else if (ifmanu_thqua == 2) {
            th_qua = Math.abs(new LowQuartile(all_value).getResult());
            //System.out.println(th_qua);
        }
    }

    // 判断是否含有正交关注
    private boolean ifComponentHasQuadatureConcerns(String component,String concern) {
        List<String> main_concern=component_mainconcern.get(component);
        for (int i = 0; i < main_concern.size(); i++)
            if(main_concern.get(i).equals(concern)){
                continue;
            }
            else if (calInnerProduct(concern_value.get(main_concern.get(i)), concern_value.get(concern)) < th_qua){
                    return true;
            }
                //zhengjiaomap.put(main_concern.get(i),concern);
        return false;
    }

    // 对两个矩阵做内积
    private double calInnerProduct(List<Double> list1, List<Double> list2){
        if(list1.size()!=list2.size())
            return Double.MIN_VALUE;
        double result=0d;
        for(int i=0;i<list1.size();i++)
            result+=list1.get(i)*list2.get(i);
        return result;
    }

    public LinkedHashMap<String, ArrayList<String>> getSFsmells() {
        return SFsmells;
    }

    public void setSFsmells(LinkedHashMap<String, ArrayList<String>> SFsmells) {
        this.SFsmells = SFsmells;
    }

    public LinkedHashMap<String, Integer> getComponent_concerncount() {
        return component_concerncount;
    }

    public void setComponent_concerncount(LinkedHashMap<String, Integer> component_concerncount) {
        this.component_concerncount = component_concerncount;
    }

}
