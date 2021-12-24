package top.jach.tes.app.jhkt.chenjiali;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import top.jach.tes.app.dev.DevApp;
import top.jach.tes.app.jhkt.chenjiali.result.OutMode.OutToExcel;
import top.jach.tes.app.jhkt.chenjiali.result.OutMode.Tool;
import top.jach.tes.app.jhkt.chenjiali.result.OutputDataStruct;
import top.jach.tes.app.jhkt.chenjiali.result.SFDetailResult;
import top.jach.tes.app.mock.Environment;
import top.jach.tes.app.mock.InfoTool;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.plugin.jhkt.DataAction;
import top.jach.tes.plugin.jhkt.InfoNameConstant;
import top.jach.tes.plugin.jhkt.arcsmell.scatteredFunctionality.SFAction;
import top.jach.tes.plugin.tes.code.git.version.Version;
import top.jach.tes.plugin.tes.code.git.version.VersionsInfo;
import top.jach.tes.plugin.tes.code.repo.ReposInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.*;

/**
 * @author:AdminChen
 * @date:2020/10/12
 * @description:用来执行SFAcion的函数
 */
public class TestForSF extends DevApp {

    public static void main(String[] args) throws IOException{
        Context context = Environment.contextFactory.createContext(Environment.defaultProject);

        ReposInfo reposInfo = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.TargetSystem, ReposInfo.class);

        VersionsInfo versionsInfoForRelease = DataAction.queryLastInfo(context, InfoNameConstant.VersionsForRelease, VersionsInfo.class);

        ArrayList<OutputDataStruct> sf_result_list=new ArrayList<>();

        for (int i = 0; i < versionsInfoForRelease.getVersions().size(); i++) {
            String versionName = versionsInfoForRelease.getVersions().get(i).getVersionName();//脱敏后的版本名
            //System.out.println(versionName);
            Gson gson=new Gson();
            String mtstr= FileUtils.readFileToString(new File("D:\\data\\concernRelatedResult\\dataIn\\"+versionName+"\\msconcerns.json"),"utf8");
            JsonObjectForMT jmt=gson.fromJson(mtstr,JsonObjectForMT.class);
            String twstr=FileUtils.readFileToString(new File("D:\\data\\concernRelatedResult\\dataIn\\"+versionName+"\\topicwords.json"),"utf8");
            JsonObjectForTW jtw=gson.fromJson(twstr,JsonObjectForTW.class);
            List<String> input_C=jmt.getServiceNames();
            List<String> input_T=new ArrayList<>();
            for(int j=0;j<jmt.getConcerns();j++){
                input_T.add(String.valueOf(j));
            }
            Map<String, Map<String, Double>> input_P=new HashMap<>();
            for(Map.Entry<String,List<Double>> enn:jmt.getServiceConcernProbabilities().entrySet()){
                Map<String,Double> ts=new HashMap<>();
                for(int k=0;k<enn.getValue().size();k++){
                    ts.put(String.valueOf(k),enn.getValue().get(k));
                }
                input_P.put(enn.getKey(),ts);
            }
            Map<String,List<Double>> concern_value=new HashMap<>();
            Map<String,Map<String,Double>> topicWords=jtw.getTopic_words();
            List<String> list_word=new ArrayList<>();//由于topic_words是map无序的，故要统一word权重列表的顺序
            for(String key:topicWords.keySet()){
                for(String topi:topicWords.get(key).keySet()){
                    list_word.add(topi);
                }
                break;
            }

            for(String topicName:topicWords.keySet()){
                List<Double> value_list=new ArrayList<>();
                for(String wr:list_word){
                    value_list.add(topicWords.get(topicName).get(wr));
                }
                concern_value.put(getDigit(topicName),value_list);
            }
            SFAction sfa=new SFAction(input_C,input_T,input_P,concern_value,
                    true,0.003889,//这个阈值已经不起作用了
                    true,1,
                    3,0.0001);
            sfa.CheckScatteredFunctionality("D:\\data\\concernRelatedResult\\dataIn\\"+versionName+"\\microTopics.csv");
            OutputDataStruct ods=new OutputDataStruct();
            ods.setVersion(versionName);
            ods.setComponent_smellcount(sfa.getComponent_concerncount());
            ods.setDetail_info(sfa.getSFsmells());

            //Workbook wb=new HSSFWorkbook();
            Map<String,Integer> mtCount=new LinkedHashMap<>();
            for(Map.Entry<String,List<String>> enn:sfa.getComponent_mainconcern().entrySet()){
                mtCount.put(enn.getKey(),enn.getValue().size());
            }
            File ff=new File("D:\\data\\versions5\\co5\\arcade_co\\"+versionName+".csv");
            StringBuilder sb=new StringBuilder();
            for(String mi:mtCount.keySet()){
                sb.append(mi);
                sb.append(',');
                sb.append(mtCount.get(mi));
                sb.append('\n');
            }
            FileUtils.write(ff, sb.toString(), "utf8");
            //Gson gson1=new Gson();
            //写入json文件

            OutputStreamWriter osw=new OutputStreamWriter(new FileOutputStream("D:\\SFResult\\thqua0.0001\\"+"version"+i+".json"),"UTF-8");
            Tool tool=new Tool();
            Gson gson1=new Gson();
            String jso=gson1.toJson(ods);
            String fomatJson=tool.stringToJSON(jso);
            osw.write(fomatJson);
            osw.flush();
            osw.close();

            sf_result_list.add(ods);
        }
       /* // 将结果写入文件
        OutToExcel ote2=new OutToExcel();
        //ote2.setOutput_path(ifds.getOutput_sf_excel());
        ote2.setOutput_path("D:\\data\\concernRelatedResult\\sfResult.xls");
        ote2.setAlldata(sf_result_list);
        ote2.WriteToExcel();*/

       /* OutToJson otj2=new OutToJson();
        SFDetailResult sfdr=new SFDetailResult();
        otj2.ConvertOutputDataStructToSFDetailResult(sf_result_list,sfdr);
        otj2.setMode("SF");
        otj2.setOutput_path(ifds.getOutput_sf_json());
        otj2.setResult_sf(sfdr);
        otj2.WriteToJson();*/

    }
    //获取一个字符串中数字部分
    public static String getDigit(String str){
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
    //检测某一个版本的SF
    public void SFcheck(boolean ifmanu_thzc,double manu_thzc,
                         boolean ifmanu_thnum,int manu_thnum,
                         int ifmanu_thqua,double manu_thqua,String versionName){




    }

    // SF的自动检测
/*    public void SFCheck(boolean ifmanu_thzc,double manu_thzc,
                        boolean ifmanu_thnum,int manu_thnum,
                        int ifmanu_thqua,double manu_thqua){
        try {
            ArrayList<OutputDataStruct> sf_result_list=new ArrayList<>();
            for(int i=0;i<ifds.getVersion().size();i++){
                // 获取文件数据
                ArrayList<String> input_C = gdfj[i].getInput_C();
                ArrayList<String> input_T = gdfj[i].getInput_T();
                HashMap<String, HashMap<String, Double>> input_P = gdfj[i].getInput_P();
                HashMap<String,ArrayList<Double>> concern_value=gdfj[i].getConcern_value();
                // 调用方法进行检测
                SFCheckOut_oldmethod2 sfco=new SFCheckOut_oldmethod2(input_C,input_T,input_P,concern_value,ifmanu_thzc,manu_thzc,
                        ifmanu_thnum,manu_thnum,ifmanu_thqua,manu_thqua);
                sfco.CheckScatteredFunctionality();
                OutputDataStruct ods=new OutputDataStruct();
                ods.setVersion(ifds.getVersion().get(i));
                ods.setComponent_smellcount(sfco.getComponent_concerncount());
                ods.setDetail_info(sfco.getSFsmells());
                sf_result_list.add(ods);
            }
            // 将结果写入文件
            OutToExcel ote2=new OutToExcel();
            ote2.setOutput_path(ifds.getOutput_sf_excel());
            ote2.setAlldata(sf_result_list);
            ote2.WriteToExcel();

            OutToJson otj2=new OutToJson();
            SFDetailResult sfdr=new SFDetailResult();
            otj2.ConvertOutputDataStructToSFDetailResult(sf_result_list,sfdr);
            otj2.setMode("SF");
            otj2.setOutput_path(ifds.getOutput_sf_json());
            otj2.setResult_sf(sfdr);
            otj2.WriteToJson();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
}
