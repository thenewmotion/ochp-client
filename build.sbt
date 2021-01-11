import com.newmotion.sbt.plugins.SoapUIMockServicePlugin.soapui
import sbt._

// org.apache.cxf.xjcplugins:cxf-xjc-ts:3.3.7 hasn't been released https://repo1.maven.org/maven2/org/apache/cxf/xjcplugins/cxf-xjc-ts/
// and cannot be excluded from dependencies due to https://github.com/coursier/coursier/issues/853
val cxfVer = "3.3.1" // "3.3.7"

def cxfRt(lib: String) =
  "org.apache.cxf" % s"cxf-rt-$lib" % cxfVer

def specs(lib: String) =
  "org.specs2" %% s"specs2-$lib" % "3.8.9"

val ochp = (project in file("."))
  .enablePlugins(OssLibPlugin)
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    soapui.settings,

    organization := "com.newmotion",
    name := "ochp-client",
    moduleName := name.value,

    scalaVersion := tnm.ScalaVersion.prev,
    crossScalaVersions := Seq(tnm.ScalaVersion.prev, tnm.ScalaVersion.aged),

    libraryDependencies ++= Seq(
      cxfRt("frontend-jaxws"),
      cxfRt("transports-http"),
      cxfRt("ws-security"),
      "com.sun.xml.messaging.saaj" % "saaj-impl" % "1.3.28",
      "com.github.nscala-time" %% "nscala-time" % "2.16.0",
      "org.slf4j" % "slf4j-api" % "1.7.25",

      "com.typesafe" % "config" % "1.3.1" % "it,test",
      specs("junit") % "it,test",
      specs("mock") % "it,test"
    ),

    cxfVersion := cxfVer,
    cxfWsdls := Seq(
      CxfWsdl(
        (resourceDirectory in Compile).value / "wsdl" / "ochp-1.3.wsdl",
        Seq("-validate", "-xjc-verbose"), "ochp")
    ),
    fork in IntegrationTest := true,
    soapui.mockServices := Seq(
      soapui.MockService(
        (resourceDirectory in IntegrationTest).value / "soapui" / "OCHP-1-3-soapui-project.xml",
        "8088")
    ),
    mappings in (Compile, packageSrc) ++= Path.allSubpaths(target.value / "cxf" / "ochp").toSeq
)

val ochpCommandLine = (project in file("cmdline"))
  .enablePlugins(AppPlugin)
  .dependsOn(ochp)
  .settings(
    organization := "com.newmotion",
    name := "ochp-client-cmdline"
  )