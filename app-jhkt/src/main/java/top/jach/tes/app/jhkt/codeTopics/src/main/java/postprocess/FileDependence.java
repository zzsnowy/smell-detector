package postprocess;

import java.util.List;

public class FileDependence {
    public List<String> fileSequence;
    public int[][] dependGraph;

    public FileDependence(List<String> fileSequence, int[][] dependGraph) {
        this.fileSequence = fileSequence;
        this.dependGraph = dependGraph;
    }
}


