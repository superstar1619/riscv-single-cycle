name := "riscv-single-cycle"
version := "0.1.0-SNAPSHOT"
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

scalaVersion := "2.13.14"
val chiselVersion = "3.6.1"
addCompilerPlugin(
  "edu.berkeley.cs" %% "chisel3-plugin" % chiselVersion cross CrossVersion.full
)
libraryDependencies += "edu.berkeley.cs" %% "chisel3" % chiselVersion
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.6.2" % Test

Compile / run / mainClass := Some("riscvsingle.GenerateExtend")
