package com.google.testrunner;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Test runner for iOS designed to be used by bazel */
public class IosTestRunner {

  private static final String FLAG_DEBUG_KEEP_TEST_DIR = "keep_test_dir";
  private static final String FLAG_TEST_BUNDLE_PATH = "test_bundle_path";
  private static final String FLAG_DEVICE_TYPE = "device_type";
  private static final String FLAG_TEST_HOST_PATH = "test_host_path";
  private static final String FLAG_TEST_TYPE = "test_type";
  private static final String FLAG_XCODE_PATH = "xcode_path";

  private static final int TIME_TO_WAIT_FOR_SIMULATOR_MS = 2000;

  private static Map<String, String> argMap;

  public IosTestRunner() {}

  public static void main(String[] args) throws IOException, InterruptedException {

    argMap = parseArgs(args);

    File testDirectory =
        createDirectory(Optional.empty(), System.getenv("TMPDIR") + "/test." + UUID.randomUUID());

    File testBundlePath = new File(argMap.get(FLAG_TEST_BUNDLE_PATH));

    Result unzipResult =
        runAndGetResult(
            "unzip", "-qq", testBundlePath.getPath(), "Payload/*", "-d", testDirectory.toString());
    assert (unzipResult.returnValue == 0);

    String xctestName =
        testBundlePath.getName().substring(0, testBundlePath.getName().lastIndexOf('.'));
    File testBundle = new File(testDirectory.getPath() + "/Payload/" + xctestName + ".xctest");

    Result sdkResult = runAndGetResult("xcrun", "--sdk", "iphonesimulator", "--show-sdk-version");
    assert (sdkResult.returnValue == 0);
    String sdkVersion = sdkResult.output;

    String xctestPath;
    if (argMap.containsKey(FLAG_XCODE_PATH)) {
      xctestPath = argMap.get(FLAG_XCODE_PATH);
    } else {
    Result xctestPathResult = runAndGetResult("xcrun", "xcode-select", "-p");
    assert (xctestPathResult.returnValue == 0);
      xctestPath = xctestPathResult.output;
    }
    xctestPath += "/Platforms/iPhoneSimulator.platform/Developer/Library/Xcode/Agents/xctest";

    IosDeviceModel iosDeviceModel =
        IosDeviceModel.valueOf(argMap.getOrDefault(FLAG_DEVICE_TYPE, "IPHONE_6"));

    Result simulationIdResult =
        runAndGetResult(
            "xcrun",
            "simctl",
            "create",
            UUID.randomUUID().toString(),
            iosDeviceModel.getSimulatorDeviceTypeId(),
            sdkVersion);
    assert (simulationIdResult.returnValue == 0);
    String simulationId = simulationIdResult.output;

    int result = -1;
    try {
      Thread.sleep(TIME_TO_WAIT_FOR_SIMULATOR_MS);
      File hostBundle = new File(argMap.get(FLAG_TEST_HOST_PATH));
      if (argMap.containsKey(FLAG_TEST_HOST_PATH)) {
        unzipResult =
            runAndGetResult(
                "unzip", "-qq", hostBundle.getPath(), "Payload/*", "-d", testDirectory.toString());
        assert (unzipResult.returnValue == 0);

        String appName = hostBundle.getName().substring(0, hostBundle.getName().lastIndexOf('.'));
        hostBundle = new File(testDirectory.getPath() + "/Payload/" + appName + ".app");
        File pluginDirectory = new File(hostBundle, "PlugIns");
        Files.copy(testBundle.toPath(), pluginDirectory.toPath());

        File testProjectDirectory =
            createDirectory(
                Optional.empty(), System.getenv("TMPDIR") + "/test_project." + UUID.randomUUID());

        unzipAndReplace(
            "TestProject.zip",
            testProjectDirectory,
            new Replacement("%TestProject%", appName),
            new Replacement("%TestProjectXctest%", xctestName));

        Process process =
            new ProcessBuilder(
                    "xcodebuild",
                    "-verbose",
                    "test",
                    "BUILT_PRODUCTS_DIR="
                        + testDirectory.toString()
                        + "/Payload",
                    "-project",
                    testProjectDirectory.toString() + "/TestProject/TestProject.xcodeproj",
                    "-scheme",
                    "TestProject"
                        + argMap.get(FLAG_TEST_TYPE).toUpperCase().substring(0, 1)
                        + argMap.get(FLAG_TEST_TYPE).toLowerCase().substring(1),
                    "-destination",
                    "id=" + simulationId,
                    "-configuration",
                    "Debug")
                .directory(testProjectDirectory)
                .redirectOutput(Redirect.INHERIT)
                .redirectError(Redirect.INHERIT)
                .start();
        result = process.waitFor();
      } else {
        result =
            runAndGetResult(
                    "xcrun", "simctl", "spawn", simulationId, xctestPath, testBundle.toString())
                .returnValue;
      }
    } finally {
      runAndGetResult("xcrun", "simctl", "shutdown", simulationId);
      runAndGetResult("xcrun", "simctl", "delete", simulationId);
    }

    System.exit(result);
  }

  // This implements functionality which is implmented elsewhere, at present we belive that the
  // cost in .jar size exceeds the benifits of code reuse. TODO(gpounder): Review this in June 2017.
  private static Map<String, String> parseArgs(String[] args) {
    Map<String, String> argMap = new HashMap<>();
    int argIndex = 0;
    while (argIndex < args.length - 1) {
      if (args[argIndex + 1].startsWith("-")) {
        argIndex++;
      } else if (args[argIndex + 1].startsWith("%")) {
        argIndex += 2;
      } else if (args[argIndex].startsWith("--")) {
        argMap.put(args[argIndex].substring(2), args[argIndex + 1]);
        argIndex += 2;
      } else if (args[argIndex].startsWith("-")) {
        argMap.put(args[argIndex].substring(1), args[argIndex + 1]);
        argIndex += 2;
      }
    }
    return argMap;
  }

  private static Result runAndGetResult(String... command)
      throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(command);
    Process process = pb.start();
    int result = process.waitFor();
    String output =
        new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
    String error =
        new BufferedReader(new InputStreamReader(process.getErrorStream(), UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
    if (error != null && !error.isEmpty()) {
      System.err.print("command: ");
      for (String s : command) {
        System.err.print(s);
        System.err.print(" ");
      }
      System.err.println();
      System.err.println("error: " + error);
    }
    return new Result(result, output);
  }

  private static void unzipAndReplace(
      String zipFile, File parentDirectory, Replacement... replacements) throws IOException {
    ZipInputStream testProjectStream =
        new ZipInputStream(IosTestRunner.class.getResourceAsStream(zipFile));
    BufferedReader testProjectReader =
        new BufferedReader(new InputStreamReader(testProjectStream, UTF_8));
    ZipEntry zipEntry = testProjectStream.getNextEntry();
    while (zipEntry != null) {
      String fileName = zipEntry.getName();
      if (zipEntry.isDirectory()) {
        createDirectory(Optional.of(parentDirectory), fileName);
        zipEntry = testProjectStream.getNextEntry();
        continue;
      }
      File outputFile = new File(parentDirectory, fileName);
      PrintStream fileWriter = new PrintStream(new FileOutputStream(outputFile));
      String line = testProjectReader.readLine();
      while (line != null) {
        for (Replacement replacement : replacements) {
          line = line.replaceAll(replacement.matcher, replacement.replacement);
        }
        fileWriter.println(line);
        line = testProjectReader.readLine();
      }
      zipEntry = testProjectStream.getNextEntry();
    }
  }

  private static File createDirectory(Optional<File> parent, String name) throws IOException {
    File directory = parent.isPresent() ? new File(parent.get(), name) : new File(name);
    if (!directory.mkdir()) {
      throw new IOException("Failed to create directory: " + name);
    }
    if (!"true".equals(argMap.get(FLAG_DEBUG_KEEP_TEST_DIR))) {
      directory.deleteOnExit();
    }
    directory.setWritable(true);
    return directory;
  }

  private static class Replacement {
    public String matcher;
    public String replacement;

    Replacement(String matcher, String replacement) {
      this.matcher = matcher;
      this.replacement = replacement;
    }
  }

  private static class Result {
    public final int returnValue;
    public final String output;

    Result(int returnValue, String output) {
      this.returnValue = returnValue;
      this.output = output;
    }
  }
}
