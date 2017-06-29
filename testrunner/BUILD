# Description:
#   A test runner to be shared externally with Bazel.

package(default_visibility = ["//third_party/java/testrunner:__pkg__"])

licenses(["notice"])

exports_files(["LICENSE"])

java_library(
    name = "testrunner",
    srcs = glob(["java/**/*.java"]),
    resources = ["java/com/google/testrunner/TestProject.zip"],
)
