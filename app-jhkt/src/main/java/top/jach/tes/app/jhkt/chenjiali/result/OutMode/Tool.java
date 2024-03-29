package top.jach.tes.app.jhkt.chenjiali.result.OutMode;

/**
 * @author:AdminChen
 * @date:2020/10/13
 * @description:
 */
public class Tool {
    private boolean isTab=true;
    public String stringToJSON(String strJson){
        int tabNum=0;
        StringBuffer jsonFormat=new StringBuffer();
        int length=strJson.length();
        for(int i=0;i<length;i++){
            char c=strJson.charAt(i);
            if(c=='{'){
                tabNum++;
                jsonFormat.append(c+"\n");
                jsonFormat.append(getSpaceOrTab(tabNum));
            }else if(c=='}'){
                tabNum--;
                jsonFormat.append("\n");
                jsonFormat.append(getSpaceOrTab(tabNum));
                jsonFormat.append(c);
            }else if(c==','){
                jsonFormat.append(c+"\n");
                jsonFormat.append(getSpaceOrTab(tabNum));
            }else{
                jsonFormat.append(c);
            }
        }
        return jsonFormat.toString();
    }


    public String getSpaceOrTab(int tabNum){
        StringBuffer sbTab=new StringBuffer();
        for(int i=0;i<tabNum;i++){
            if(isTab){
                sbTab.append('\t');
            }else{
                sbTab.append(" ");
            }
        }
        return sbTab.toString();
    }
}
