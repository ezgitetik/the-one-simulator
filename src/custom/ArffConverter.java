package custom;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArffConverter {

    public static void convert() throws IOException {
        String rootFolder = ArffReader.class.getClassLoader().getResource("custom/taxidata/bursa-0101_old").getPath();
        List<String> files = Stream.of(new File(rootFolder).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toList());

        files.stream().forEach(file -> {
            InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream("custom/taxidata/bursa-0101_old/" + file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            try {
                line = reader.readLine();
                String lineStringLine = "";
                while (line != null) {
                    lineStringLine = line;
                    LineStringReader lineStringReader = new LineStringReader(lineStringLine);
                    lineStringReader.parse();
                    lineStringReader.getLandmarks().forEach(landmark -> {
                        System.out.println(landmark.getX() + ", " + landmark.getY());
                    });
                    line = reader.readLine();
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    public static void main(String[] args) throws IOException {
        ArffConverter.convert();
    }
}
