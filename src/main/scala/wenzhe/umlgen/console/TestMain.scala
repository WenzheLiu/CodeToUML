package wenzhe.umlgen.console

import scala.util.Properties


object TestMain extends App {
  Properties.setProp("uml.propFile", """C:\Users\weliu\code\sbt\project3\src\main\resources\library.properties""")
  Properties.setProp("classdiagram.showPackage", "true")
  Properties.setProp("app.printToConsole", "false")
  wenzhe.umlgen.console.Main.main(Array(
      """C:\Users\weliu\code\pwo\pwo\gui\common\com.asml.jex\src\com\asml\jex\ddd""",
      """C:\Users\weliu\code\pwo\pwo\gui\common\com.asml.jex\src\com\asml\jex\rx\event"""
      ))
}
