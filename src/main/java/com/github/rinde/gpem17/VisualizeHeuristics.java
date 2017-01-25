package com.github.rinde.gpem17;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.github.rinde.ecj.GPProgram;
import com.github.rinde.ecj.GPProgramParser;
import com.github.rinde.evo4mas.common.GpGlobal;
import com.github.rinde.gpem17.evo.FunctionSet;
import com.google.common.base.Charsets;

public class VisualizeHeuristics {

  public static void main(String[] args)
      throws IOException, InterruptedException {
    List<String> lines =
      Files.readAllLines(Paths.get("files/experiment-overview.csv"),
        Charsets.UTF_8);

    Files.createDirectories(Paths.get("files/heuristics"));

    boolean first = true;
    for (String line : lines) {
      if (first) {
        first = false;
        continue;
      }
      // header:
      // seed,scenario class,evo folder,realtime eval,rt folder,file
      // name,program
      String[] parts = line.split(",");

      String name = parts[1] + "-" + parts[0];
      String program = parts[parts.length - 1];
      final GPProgram<GpGlobal> progObj =
        GPProgramParser.parseProgramFunc(program, new FunctionSet().create());

      Files.write(Paths.get("files/heuristics/" + name + ".dot"),
        asList(GPProgramParser.toDot(progObj)), Charsets.UTF_8);
    }

  }

}
