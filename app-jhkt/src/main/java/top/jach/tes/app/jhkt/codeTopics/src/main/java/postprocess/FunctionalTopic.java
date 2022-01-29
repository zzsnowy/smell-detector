package postprocess;

import java.util.ArrayList;

public class FunctionalTopic implements Comparable<FunctionalTopic> {
    public int topicID;
    public double tc;
    public ArrayList<String> files;
    public ArrayList<String> services;

    public int compareTo(FunctionalTopic o) {
        return (this.tc - o.tc) < 0 ? 1 : -1;
    }
}
