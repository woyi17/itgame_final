// @GENERATOR:play-routes-compiler
// @SOURCE:D:/itgame/IT-game-main/IT-game-main/conf/routes
// @DATE:Mon Mar 23 05:34:01 GMT 2026


package router {
  object RoutesPrefix {
    private var _prefix: String = "/"
    def setPrefix(p: String): Unit = {
      _prefix = p
    }
    def prefix: String = _prefix
    val byNamePrefix: Function0[String] = { () => prefix }
  }
}
