package calypsox.tk.engine.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtil {



    public void dropfile(String line, String filename) throws IOException {
        String csvFilePath = "C://TTCSInt//"+filename+"export.csv";
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(csvFilePath));

        // write header line containing column names

        fileWriter.write(line);
        fileWriter.close();


    }

}
