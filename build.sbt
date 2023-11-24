ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.5"

ThisBuild / evictionErrorLevel := Level.Warn

lazy val root = (project in file(".")).settings(
  name := "cats-effect-3-quick-start",
  libraryDependencies ++= Seq(
    // "core" module - IO, IOApp, schedulers
    // This pulls in the kernel and std modules automatically.
    "org.typelevel" %% "cats-effect" % "3.3.12",
    // concurrency abstractions and primitives (Concurrent, Sync, Async etc.)
    "org.typelevel" %% "cats-effect-kernel" % "3.3.12",
    // standard "effect" library (Queues, Console, Random etc.)
    "org.typelevel" %% "cats-effect-std" % "3.3.12",
    // better monadic for compiler plugin as suggested by documentation
    compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    // Test libs
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test,
    "org.scalameta" %% "munit-scalacheck" % "0.7.29" % Test,
    "com.github.chocpanda" %% "scalacheck-magnolia" % "0.5.1" % Test,
    "org.typelevel" %% "scalacheck-effect" % "1.0.4" % Test,
    "org.typelevel" %% "scalacheck-effect-munit" % "1.0.4" % Test,

    // Bring in Money type
    "org.typelevel" %% "squants" % "1.8.3",
    // typeclass derivation
    "tf.tofu" %% "derevo-cats" % "0.13.0",
    "tf.tofu" %% "derevo-circe-magnolia" % "0.13.0",
    // better AnyVal with no runtime overhead
    "io.estatico" %% "newtype" % "0.4.4",
    //refined
    "eu.timepit" %% "refined" % "0.11.0",
    "eu.timepit" %% "refined-cats" % "0.11.0",
    "eu.timepit" %% "refined-scalacheck" % "0.11.0",
    // htt4ps
    "org.http4s" %% "http4s-dsl" % "0.23.24",
    "org.http4s" %% "http4s-ember-client" % "0.23.24",
    "org.http4s" %% "http4s-circe" % "0.23.24",
    // circe
    "io.circe" %% "circe-core" % "0.14.6",
    "io.circe" %% "circe-generic" % "0.14.6",
    "io.circe" %% "circe-parser" % "0.14.6",
    "io.circe" %% "circe-refined" % "0.14.6"
  )
)

scalacOptions ++= Seq("-Ymacro-annotations")

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
