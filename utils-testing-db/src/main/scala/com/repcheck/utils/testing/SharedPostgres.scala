package com.repcheck.utils.testing

import cats.effect.IO

/**
 * One container shared across a repo's whole test JVM: extend with that repo's config — `object MySharedDb extends
 * SharedPostgres(PostgresContainerConfig(initSchema = MigrationRunner.migrate))` — and every suite reads
 * `MySharedDb.info`. Torn down by a JVM shutdown hook.
 */
abstract class SharedPostgres(config: PostgresContainerConfig) {

  import cats.effect.unsafe.implicits.global

  private lazy val containerInfo: PostgresContainerInfo = {
    val (info, finalizer) = new DockerPostgres(config).resource.allocated.unsafeRunSync()
    val _                 = SharedPostgres.releaseOnJvmExit(finalizer)
    info
  }

  def info: PostgresContainerInfo = containerInfo
}

object SharedPostgres {

  private[testing] def releaseOnJvmExit(finalizer: IO[Unit]): Thread =
    sys.addShutdownHook(runFinalizer(finalizer))

  private[testing] def runFinalizer(finalizer: IO[Unit]): Unit = {
    import cats.effect.unsafe.implicits.global
    val _ = finalizer.attempt.unsafeRunSync()
    ()
  }

}
