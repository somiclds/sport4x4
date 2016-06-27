name := "sport4x4"

version := "1.0"

lazy val `sport4x4` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc , ws , specs2 % Test, cache ,
  "com.typesafe" % "config" % "1.2.1",
  "jp.t2v" %% "play2-auth"        % "0.14.2",
  "jp.t2v" %% "play2-auth-social" % "0.14.2",
  "jp.t2v" %% "play2-auth-test"   % "0.14.2" % "test",
  play.sbt.Play.autoImport.cache,
  "org.mindrot"           % "jbcrypt"                           % "0.3m",
  "org.scalikejdbc"      %% "scalikejdbc"                       % "2.2.7",
  "org.scalikejdbc"      %% "scalikejdbc-config"                % "2.2.7",
  "org.scalikejdbc"      %% "scalikejdbc-syntax-support-macro"  % "2.2.7",
  "org.scalikejdbc"      %% "scalikejdbc-test"                  % "2.2.7"   % "test",
  "org.scalikejdbc"      %% "scalikejdbc-play-initializer"      % "2.4.0",
  "org.scalikejdbc"      %% "scalikejdbc-play-dbapi-adapter"    % "2.4.0",
  "org.scalikejdbc"      %% "scalikejdbc-play-fixture"          % "2.4.0",
  "org.flywaydb"         %% "flyway-play"                       % "2.0.1",
  evolutions
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

// do not include API documentation
sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false