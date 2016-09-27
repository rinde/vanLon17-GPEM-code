/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.gpem17;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.github.rinde.gpem17.evo.AvoidExitUtil;
import com.github.rinde.gpem17.evo.AvoidExitUtil.ExitTrappedException;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import ec.Evolve;

/**
 * 
 * @author Rinde van Lon
 */
public class Train {

  public static void main(String[] args) {
    if (args.length == 0) {
      run("files/config/gpem17.params");
    } else {
      for (String file : args) {
        File f = new File(file);
        if (f.isDirectory()) {
          File[] paramFiles = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
              return name.endsWith(".params");
            }
          });
          for (File paramFile : paramFiles) {
            run(paramFile.getPath());
          }
        } else {
          run(file);
        }
      }
    }
  }

  static void run(String configFile) {
    AvoidExitUtil.forbidSystemExitCall();
    try {
      Evolve.main(new String[] {"-file", configFile});
    } catch (ExitTrappedException e) {} finally {
      AvoidExitUtil.enableSystemExitCall();
    }
    File f = new File("nohup.out");
    if (f.exists()) {
      try {
        FileUtils.copyFileToDirectory(f, new File("files/results/evo/latest"),
          false);

        Files.write("", f, Charsets.UTF_8);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

}
