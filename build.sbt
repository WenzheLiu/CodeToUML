
name := "CodeToUml"

version := "1.0"

scalaVersion := "2.12.2"

import com.github.retronym.SbtOneJar._

oneJarSettings

mainClass in oneJar := Some("wenzhe.umlgen.console.Main")
// Some("wenzhe.umlgen.ui.fx.MainFrameRun")

libraryDependencies ++= Seq(
 // "org.aspectj"             % "aspectjweaver"         % "1.8.10",
 // "org.aspectj"             % "aspectjrt"             % "1.8.10",

  "com.github.javaparser" % "javaparser-core" % "3.2.1",
  "net.sourceforge.plantuml" % "plantuml" % "8059",
  //"org.apache.xmlgraphics" % "batik-rasterizer" % "1.9",
  //"org.apache.xmlgraphics" % "fop" % "2.2",
  "org.scala-lang" % "scala-library" % "2.12.2",
  "org.scala-lang" % "scala-compiler" % "2.12.2",
  "org.scala-lang" % "scala-reflect" % "2.12.2",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
  //"io.reactivex" %% "rxscala" % "0.26.5",
  //"io.reactivex" % "rxjavafx" % "2.0.2",
  "org.scalafx" %% "scalafx" % "8.0.102-R11"
  // "com.jfoenix" % "jfoenix" % "1.9.1"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-Yoverride-vars"
)

javacOptions ++= Seq("-g")

//javaOptions in run += "-javaagent:" + System.getProperty("user.home") + "/.ivy2/cache/org.aspectj/aspectjweaver/jars/aspectjweaver-1.8.10.jar"

fork in run := true

connectInput in run := true

