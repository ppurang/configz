scalacOptions += "-deprecation"

resolvers += Resolver.url(
    "sbt-plugin-releases",
    new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/")
  )(Resolver.ivyStylePatterns)

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.4")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.5")
