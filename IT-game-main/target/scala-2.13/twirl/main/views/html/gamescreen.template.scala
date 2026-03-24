
package views.html

import _root_.play.twirl.api.TwirlFeatureImports._
import _root_.play.twirl.api.TwirlHelperImports._
import _root_.play.twirl.api.Html
import _root_.play.twirl.api.JavaScript
import _root_.play.twirl.api.Txt
import _root_.play.twirl.api.Xml
import models._
import controllers._
import play.api.i18n._
import views.html._
import play.api.templates.PlayMagic._
import java.lang._
import java.util._
import scala.collection.JavaConverters._
import play.core.j.PlayMagicForJava._
import play.mvc._
import play.api.data.Field
import play.data._
import play.core.j.PlayFormsMagicForJava._

object gamescreen extends _root_.play.twirl.api.BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,_root_.play.twirl.api.Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with _root_.play.twirl.api.Template2[play.mvc.Http.Request,String,play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*1.2*/(request: play.mvc.Http.Request, user: String):play.twirl.api.HtmlFormat.Appendable = {
    _display_ {
      {


Seq[Any](format.raw/*2.1*/("""<!DOCTYPE html>
<html>
    <head>
        <title>ITSD Card Game Main Screen</title>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            #game-over-overlay """),format.raw/*9.32*/("""{"""),format.raw/*9.33*/("""
                """),format.raw/*10.17*/("""display: none;
                position: fixed;
                top: 0; left: 0;
                width: 100%; height: 100%;
                background: rgba(0, 0, 0, 0.75);
                z-index: 9999;
                justify-content: center;
                align-items: center;
                flex-direction: column;
            """),format.raw/*19.13*/("""}"""),format.raw/*19.14*/("""
            """),format.raw/*20.13*/("""#game-over-overlay.show """),format.raw/*20.37*/("""{"""),format.raw/*20.38*/("""
                """),format.raw/*21.17*/("""display: flex;
            """),format.raw/*22.13*/("""}"""),format.raw/*22.14*/("""
            """),format.raw/*23.13*/("""#game-over-box """),format.raw/*23.28*/("""{"""),format.raw/*23.29*/("""
                """),format.raw/*24.17*/("""background: #1a1a2e;
                border: 3px solid #e94560;
                border-radius: 16px;
                padding: 60px 80px;
                text-align: center;
                box-shadow: 0 0 40px rgba(233,69,96,0.6);
            """),format.raw/*30.13*/("""}"""),format.raw/*30.14*/("""
            """),format.raw/*31.13*/("""#game-over-title """),format.raw/*31.30*/("""{"""),format.raw/*31.31*/("""
                """),format.raw/*32.17*/("""font-size: 64px;
                font-weight: bold;
                letter-spacing: 4px;
                margin-bottom: 16px;
            """),format.raw/*36.13*/("""}"""),format.raw/*36.14*/("""
            """),format.raw/*37.13*/("""#game-over-title.win  """),format.raw/*37.35*/("""{"""),format.raw/*37.36*/(""" """),format.raw/*37.37*/("""color: #ffd700; text-shadow: 0 0 20px #ffd700; """),format.raw/*37.84*/("""}"""),format.raw/*37.85*/("""
            """),format.raw/*38.13*/("""#game-over-title.lose """),format.raw/*38.35*/("""{"""),format.raw/*38.36*/(""" """),format.raw/*38.37*/("""color: #e94560; text-shadow: 0 0 20px #e94560; """),format.raw/*38.84*/("""}"""),format.raw/*38.85*/("""
            """),format.raw/*39.13*/("""#game-over-sub """),format.raw/*39.28*/("""{"""),format.raw/*39.29*/("""
                """),format.raw/*40.17*/("""font-size: 24px;
                color: #ccc;
                margin-bottom: 40px;
                font-family: Monaco, monospace;
            """),format.raw/*44.13*/("""}"""),format.raw/*44.14*/("""
            """),format.raw/*45.13*/("""#game-over-btn """),format.raw/*45.28*/("""{"""),format.raw/*45.29*/("""
                """),format.raw/*46.17*/("""font-size: 18px;
                padding: 14px 40px;
                border-radius: 8px;
                border: none;
                cursor: pointer;
                background: #e94560;
                color: #fff;
                font-family: Monaco, monospace;
                letter-spacing: 2px;
                transition: background 0.2s;
            """),format.raw/*56.13*/("""}"""),format.raw/*56.14*/("""
            """),format.raw/*57.13*/("""#game-over-btn:hover """),format.raw/*57.34*/("""{"""),format.raw/*57.35*/(""" """),format.raw/*57.36*/("""background: #c73652; """),format.raw/*57.57*/("""}"""),format.raw/*57.58*/("""
        """),format.raw/*58.9*/("""</style>
        <link rel="stylesheet" href=""""),_display_(/*59.39*/routes/*59.45*/.Assets.at("css/uikit.css")),format.raw/*59.72*/("""" />
        
        <script src=""""),_display_(/*61.23*/routes/*61.29*/.Assets.at("js/jquery.3.4.1.js")),format.raw/*61.61*/(""""></script> 
        <script src=""""),_display_(/*62.23*/routes/*62.29*/.Assets.at("js/uikit.js")),format.raw/*62.54*/(""""></script>
        <script src=""""),_display_(/*63.23*/routes/*63.29*/.Assets.at("js/uikit-icons.js")),format.raw/*63.60*/(""""></script>
		<script src=""""),_display_(/*64.17*/routes/*64.23*/.Assets.at("js/hexi.min.js")),format.raw/*64.51*/(""""></script>

    </head>
    <body id="mainBody" wsdata=""""),_display_(/*67.34*/routes/*67.40*/.GameScreenController.socket.webSocketURL(request)),format.raw/*67.90*/("""" onload="init()">

    <div id="game-over-overlay">
        <div id="game-over-box">
            <div id="game-over-title"></div>
            <div id="game-over-sub"></div>
            <button id="game-over-btn" onclick="location.reload()">Play Again</button>
        </div>
    </div>


	

	<script src=""""),_display_(/*80.16*/routes/*80.22*/.Assets.at("js/cardgame.js")),format.raw/*80.50*/(""""></script>
	<script type="text/javascript">
	
	// // Load them google fonts before starting...!
	window.WebFontConfig = """),format.raw/*84.25*/("""{"""),format.raw/*84.26*/("""
    	"""),format.raw/*85.6*/("""google: """),format.raw/*85.14*/("""{"""),format.raw/*85.15*/("""
        	"""),format.raw/*86.10*/("""families: ['Roboto']
    	"""),format.raw/*87.6*/("""}"""),format.raw/*87.7*/(""",

   		active: function() """),format.raw/*89.25*/("""{"""),format.raw/*89.26*/("""
        	"""),format.raw/*90.10*/("""// do something
        	init();
   		"""),format.raw/*92.6*/("""}"""),format.raw/*92.7*/("""
	"""),format.raw/*93.2*/("""}"""),format.raw/*93.3*/(""";
	
	let stageWidth = 1920;
	let stageHeight = 1080;
	let moveVelocity = 2;
	
	var ws;
	var userDataSession;
	var g;
	var gameActorInitalized = false;
	var gameStart = false;
	var sinceLastHeartbeat = 0;
	
	// game objects
	let boardTiles = new Map()
	let spriteContainers = new Map()
	let sprites = new Map()
	let attackLabels = new Map()
	let healthLabels = new Map()
	let handContainers = [null,null,null,null,null,null]
	let handSprites = [null,null,null,null,null,null];
	let cardJSON = [null,null,null,null,null,null];
	let cardPreview = null;
	let prevewCountdown = 0;
	
	let activeMoves = new Map()
	let activeProjectiles = [];
	let drawUnitQueue = [];
	let drawTileQueue = [];
	
	let player1ManaIcons = new Map()
	let player2ManaIcons = new Map()
	
	let player1Health = null;
	let player2Health = null;
	
	let player1Notification = null;
	let player2Notification = null;
	let player1NotificationText = null;
	let player2NotificationText = null;
	
	let playingEffects = [];
	
	function init() """),format.raw/*136.18*/("""{"""),format.raw/*136.19*/("""
		"""),format.raw/*137.3*/("""openWebSocketConnection();
	"""),format.raw/*138.2*/("""}"""),format.raw/*138.3*/("""

	"""),format.raw/*140.2*/("""function showGameOverScreen(winner) """),format.raw/*140.38*/("""{"""),format.raw/*140.39*/("""
		"""),format.raw/*141.3*/("""var overlay = document.getElementById('game-over-overlay');
		var title   = document.getElementById('game-over-title');
		var sub     = document.getElementById('game-over-sub');
		if (winner === 'player') """),format.raw/*144.28*/("""{"""),format.raw/*144.29*/("""
			"""),format.raw/*145.4*/("""title.textContent = 'YOU WIN!';
			title.className   = 'win';
			sub.textContent   = 'Congratulations! You defeated the AI.';
		"""),format.raw/*148.3*/("""}"""),format.raw/*148.4*/(""" """),format.raw/*148.5*/("""else """),format.raw/*148.10*/("""{"""),format.raw/*148.11*/("""
			"""),format.raw/*149.4*/("""title.textContent = 'YOU LOSE';
			title.className   = 'lose';
			sub.textContent   = 'The AI wins this time. Better luck next game!';
		"""),format.raw/*152.3*/("""}"""),format.raw/*152.4*/("""
		"""),format.raw/*153.3*/("""overlay.classList.add('show');
	"""),format.raw/*154.2*/("""}"""),format.raw/*154.3*/("""
	
	
	"""),format.raw/*157.2*/("""function openWebSocketConnection() """),format.raw/*157.37*/("""{"""),format.raw/*157.38*/("""
        """),format.raw/*158.9*/("""var wsURL = document.getElementById("mainBody").getAttribute("wsdata");

        //alert(wsURL);
        ws = new WebSocket(wsURL);
        ws.onmessage = function (event) """),format.raw/*162.41*/("""{"""),format.raw/*162.42*/("""
            """),format.raw/*163.13*/("""var message;
            message = JSON.parse(event.data);
			console.log(message);
            switch (message.messagetype) """),format.raw/*166.42*/("""{"""),format.raw/*166.43*/("""
                case "actorReady":
					initHexi(message.preloadImages);

					gameActorInitalized = true;
					break;
				case "drawTile":
					//console.log(message);
					drawTileQueue.push(message);
					break;
				case "drawUnit":
					drawUnitQueue.push(message);
					break;
				case "moveUnit":
				    moveUnit(message.unitID,message.tilex,message.tiley);
                    break;
				case "moveUnitToTile":
					moveUnitToTile(message);
					break;
				case "setUnitHealth":
					setUnitHealth(message);
					break;
				case "setUnitAttack":
					setUnitAttack(message);
					break;
				case "setPlayer1Health":
					setPlayer1Health(message);
					break;
				case "setPlayer2Health":
					setPlayer2Health(message);
					break;
				case "setPlayer1Mana":
					setPlayer1Mana(message);
					break;
				case "setPlayer2Mana":
					setPlayer2Mana(message);
					break;
				case "addPlayer1Notification":
					addPlayer1Notification(message);
					break;
				case "addPlayer2Notification":
					addPlayer2Notification(message);
					break;
				case "playUnitAnimation":
					playUnitAnimation(message);
					break;
				case "drawCard":
					drawCard(message);
					break;
				case "deleteCard":
					deleteCard(message);
					break;
				case "playEffectAnimation":
					playEffectAnimation(message);
					break;
				case "deleteUnit":
					deleteUnit(message);
					break;
				case "drawProjectile":
					drawProjectile(message);
					break;
				case "gameOver":
					showGameOverScreen(message.winner);
					break;
                default:
                    return console.log(message);
            """),format.raw/*232.13*/("""}"""),format.raw/*232.14*/("""
        """),format.raw/*233.9*/("""}"""),format.raw/*233.10*/(""";
	"""),format.raw/*234.2*/("""}"""),format.raw/*234.3*/("""
	
	"""),format.raw/*236.2*/("""</script>
     
    </body>
</html>
"""))
      }
    }
  }

  def render(request:play.mvc.Http.Request,user:String): play.twirl.api.HtmlFormat.Appendable = apply(request,user)

  def f:((play.mvc.Http.Request,String) => play.twirl.api.HtmlFormat.Appendable) = (request,user) => apply(request,user)

  def ref: this.type = this

}


              /*
                  -- GENERATED --
                  DATE: 2026-03-23T05:34:02.512377400
                  SOURCE: D:/itgame/IT-game-main/IT-game-main/app/views/gamescreen.scala.html
                  HASH: ea3e01c338f8b275bfe09d1003f6d7a9224b8ea0
                  MATRIX: 935->1|1075->48|1341->287|1369->288|1414->305|1776->639|1805->640|1846->653|1898->677|1927->678|1972->695|2027->722|2056->723|2097->736|2140->751|2169->752|2214->769|2485->1012|2514->1013|2555->1026|2600->1043|2629->1044|2674->1061|2840->1199|2869->1200|2910->1213|2960->1235|2989->1236|3018->1237|3093->1284|3122->1285|3163->1298|3213->1320|3242->1321|3271->1322|3346->1369|3375->1370|3416->1383|3459->1398|3488->1399|3533->1416|3704->1559|3733->1560|3774->1573|3817->1588|3846->1589|3891->1606|4279->1966|4308->1967|4349->1980|4398->2001|4427->2002|4456->2003|4505->2024|4534->2025|4570->2034|4644->2081|4659->2087|4707->2114|4770->2150|4785->2156|4838->2188|4900->2223|4915->2229|4961->2254|5022->2288|5037->2294|5089->2325|5144->2353|5159->2359|5208->2387|5293->2445|5308->2451|5379->2501|5713->2808|5728->2814|5777->2842|5926->2963|5955->2964|5988->2970|6024->2978|6053->2979|6091->2989|6144->3015|6172->3016|6227->3043|6256->3044|6294->3054|6359->3092|6387->3093|6416->3095|6444->3096|7474->4097|7504->4098|7535->4101|7591->4129|7620->4130|7651->4133|7716->4169|7746->4170|7777->4173|8011->4378|8041->4379|8073->4383|8229->4511|8258->4512|8287->4513|8321->4518|8351->4519|8383->4523|8548->4660|8577->4661|8608->4664|8668->4696|8697->4697|8731->4703|8795->4738|8825->4739|8862->4748|9063->4920|9093->4921|9135->4934|9289->5059|9319->5060|10960->6672|10990->6673|11027->6682|11057->6683|11088->6686|11117->6687|11149->6691
                  LINES: 27->1|32->2|39->9|39->9|40->10|49->19|49->19|50->20|50->20|50->20|51->21|52->22|52->22|53->23|53->23|53->23|54->24|60->30|60->30|61->31|61->31|61->31|62->32|66->36|66->36|67->37|67->37|67->37|67->37|67->37|67->37|68->38|68->38|68->38|68->38|68->38|68->38|69->39|69->39|69->39|70->40|74->44|74->44|75->45|75->45|75->45|76->46|86->56|86->56|87->57|87->57|87->57|87->57|87->57|87->57|88->58|89->59|89->59|89->59|91->61|91->61|91->61|92->62|92->62|92->62|93->63|93->63|93->63|94->64|94->64|94->64|97->67|97->67|97->67|110->80|110->80|110->80|114->84|114->84|115->85|115->85|115->85|116->86|117->87|117->87|119->89|119->89|120->90|122->92|122->92|123->93|123->93|166->136|166->136|167->137|168->138|168->138|170->140|170->140|170->140|171->141|174->144|174->144|175->145|178->148|178->148|178->148|178->148|178->148|179->149|182->152|182->152|183->153|184->154|184->154|187->157|187->157|187->157|188->158|192->162|192->162|193->163|196->166|196->166|262->232|262->232|263->233|263->233|264->234|264->234|266->236
                  -- GENERATED --
              */
          