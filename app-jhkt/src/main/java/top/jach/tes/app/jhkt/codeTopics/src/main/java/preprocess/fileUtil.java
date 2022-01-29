package preprocess;

import java.io.*;

/*
* 专门用来把一个文件路径下的文件读取内容并存入一个字符串中的
* */
public class fileUtil {
    public static String readFile(String path) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            FileInputStream is = new FileInputStream(path);
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = "";
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder.toString().replaceAll("\\/\\*[\\w\\W]*?\\*\\/|\\/\\/.*","");
    }

}
