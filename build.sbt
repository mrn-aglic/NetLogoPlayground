name := "NetlogoPlayground"

version := "0.1"

scalaVersion := "2.12.6"

resolvers += sbt.Resolver.bintrayRepo("netlogo", "NetLogo-JVM")
resolvers += sbt.Resolver.bintrayRepo("netlogo", "TortoiseAux")

val nlDependencyVersion = "6.0.4-d9443d2"
val nl = "6.0.4"
val compilerJVMDependencyVersion = "1.0-0abb163"

libraryDependencies ++= Seq(
    "org.nlogo" % "netlogo" % nl,
//    "org.nlogo" % "netlogowebjs" % "1.0-0abb163"
//    "org.nlogo" % "compilerjvm" % compilerJVMDependencyVersion
)