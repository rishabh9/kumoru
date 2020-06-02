package com.github.rishabh9.kumoru.snapshots;

import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Log4j2
class FileComparator {

  /**
   * Compares content of two files.
   *
   * @param file1 The first file
   * @param file2 Path to the second file
   * @return TRUE if their contents are same, else FALSE.
   */
  public static boolean compare(final byte[] file1, final String file2) {
    try (InputStreamReader stream1 = new InputStreamReader(new ByteArrayInputStream(file1));
        InputStreamReader stream2 = new FileReader(file2)) {
      int c = -1;
      int d = -1;
      while (true) {
        c = stream1.read();
        d = stream2.read();
        if (c == -1 || d == -1) {
          break;
        }
        if (c != d) {
          return false;
        }
      }
      if (c != d) {
        return false;
      }
    } catch (IOException e) {
      log.error("Error comparing {} & {}", file1, file2, e);
    }
    return true;
  }
}
