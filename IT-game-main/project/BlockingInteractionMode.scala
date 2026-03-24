import play.sbt.PlayInteractionMode

// Blocks indefinitely (no console required) — used for testing from non-interactive bash
object BlockingInteractionMode extends PlayInteractionMode {
  def waitForCancel(): Unit = Thread.sleep(Long.MaxValue)
  def doWithoutEcho(f: => Unit): Unit = f
}
