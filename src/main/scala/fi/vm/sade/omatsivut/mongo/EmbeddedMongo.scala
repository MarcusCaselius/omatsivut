package fi.vm.sade.omatsivut.mongo

import de.flapdoodle.embed.mongo.{Command, MongodProcess, MongodExecutable, MongodStarter}
import de.flapdoodle.embed.mongo.config.{RuntimeConfigBuilder, Net, MongodConfigBuilder, IMongodConfig}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.config.io.ProcessOutput
import de.flapdoodle.embed.process.runtime.Network
import fi.vm.sade.omatsivut.{PortChecker, AppConfig}
import fi.vm.sade.omatsivut.AppConfig.AppConfig

object EmbeddedMongo {
  val port = 28018

  def start = {
    if (PortChecker.isFreeLocalPort(port)) {
      Some(new MongoServer(port))
    } else {
      None
    }
  }

  def withEmbeddedMongo(config: AppConfig)(block: => Unit) {
    config match {
      case AppConfig.IT =>
        val server = start
        block
        server.foreach(_.stop)
      case _ => block
    }
  }
}

class MongoServer(val port: Int) {
  private val mongodConfig: IMongodConfig = new MongodConfigBuilder()
    .version(Version.Main.PRODUCTION)
    .net(new Net(port, Network.localhostIsIPv6))
    .build
  private val runtimeConfig = new RuntimeConfigBuilder()
    .defaults(Command.MongoD)
    .processOutput(ProcessOutput.getDefaultInstanceSilent())
    .build();
  private val runtime: MongodStarter = MongodStarter.getInstance(runtimeConfig)
  private val mongodExecutable = runtime.prepare(mongodConfig)
  private val mongod = mongodExecutable.start

  def stop {
    mongod.stop
    mongodExecutable.stop
  }
}