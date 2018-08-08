package extensions.mycommands

import org.nlogo.agent.{Agent => AAgent}
import org.nlogo.api._
import org.nlogo.core.prim._const
import org.nlogo.core.{CompilerException, Syntax, Token, TokenType}
import org.nlogo.nvm
import org.nlogo.nvm.{Activation, ExtensionContext}
import org.nlogo.parse.FrontEnd

import scala.collection.JavaConverters._

object CustomTypes {

    type NLogoTokensJ = java.util.List[Token]
}

object TokenValues {

    val unknownIdentifier = "_unknownidentifier"
}

class SimpleScalaExtension extends DefaultClassManager {

    def load(manager: PrimitiveManager): Unit = {

        manager.addPrimitive("ask", new Ask)
    }
}

object ContextManager {

    import CustomTypes.NLogoTokensJ
    import TokenValues.unknownIdentifier

    def getTokenSimpleName(token: Token): String = token.value.getClass.getSimpleName

    def getContextForApiAgent(context: nvm.Context, agent: Agent, ws: nvm.Workspace): nvm.Context =
        getContextForAgent(context, agent.asInstanceOf[org.nlogo.agent.Agent], ws)

    def getContextForAgent(context: nvm.Context, agent: AAgent, ws: nvm.Workspace): nvm.Context =
        new nvm.Context(context.job, agent, context.ip, context.activation, ws)

    def extractLets(activation: nvm.Activation, nLogoTokensJ: NLogoTokensJ): Map[String, AnyRef] = {

        val unindentifiedTokens = nLogoTokensJ.asScala.toList.filter(token => getTokenSimpleName(token) == unknownIdentifier)
        println(unindentifiedTokens.map(x => x.text))
        unindentifiedTokens.foldLeft(Map[String, AnyRef]())((map, token) => {
            val value = activation.procedure.args.indexOf(token.text.toUpperCase) match {

                case -1 => ??? // let logo compiler handle variable not defined
                case index => activation.args(index)
            }
            println(value)
            map + (token.text -> value)
        })
    }
}

object CodeManager {

    type NLogoTokens = List[Token]
    type NLogoTokensJ = java.util.List[Token]

    object RunAndRunResultValue {

        val run = "_run()"
        val runresult = "_runresult()"
    }

    private def eval(ws: nvm.Workspace, context: nvm.Context, tokens: NLogoTokens): AnyRef = {

        val code = buildCode(tokens)
        val proc = ws.compileForRun(code, context, reporter = true)
        proc.init(ws)

        val newActivation = Activation.forRunOrRunresult(proc, context.activation, context.ip)
        val result = context.callReporterProcedure(newActivation)

        result

//            proc.procedureDeclaration.tokens.toList
    }

    def evalReporters(ws: nvm.Workspace, context: nvm.Context, tokens: NLogoTokensJ): NLogoTokens = {

        val reporterArgs = List(TokenType.Reporter, TokenType.Ident, TokenType.Literal)
        val stopTokens = List(TokenType.Reporter, TokenType.Ident, TokenType.Literal, TokenType.OpenParen, TokenType.CloseParen)
        val ts = tokens.asScala.toList

        def _inner(tokens: NLogoTokens, acc: NLogoTokens): NLogoTokens = {

            tokens match {

                case keep :: xs =>
                    val resultingTokens = tokens.takeWhile(token => reporterArgs.contains(token.tpe))
//                    val nextDrop = resultingTokens.length
                    val result = eval(ws, context, resultingTokens)
                    val refinedKeep = keep.refine(_const(result), result.toString, TokenType.Literal)

                    val rest = ts.drop(acc.length).dropWhile(token => resultingTokens.contains(token))
//                    val tokensKept = (acc :+ refinedKeep) ::: rest

                    val additionToAcc = rest.takeWhile(token => token.tpe != TokenType.Reporter)
                    val newAcc = (acc :+ refinedKeep) ::: additionToAcc

                    _inner(tokens.drop(additionToAcc.length + 1), newAcc)
//                    _inner(tokensKept.dropWhile(token => token.tpe != TokenType.Reporter)), (acc :+ refinedKeep)
//                    tokensKept
                case Nil => acc
            }

//            val tokensKept = ts.filter(token => !resultingTokens.tail.contains(token))
        }

        val n = ts.takeWhile(token => token.tpe != TokenType.Reporter).length
        _inner(ts.drop(n), ts.take(n))
    }

    def format(tokens: NLogoTokensJ) = {

        val ts = tokens.asScala.toList

        ts.foldLeft(Map[Int, NLogoTokens]())((map, token) =>
          if(token.tpe == TokenType.Command)
              map + (map.size -> List(token))
          else {
              val key = map.size - 1
              map + (key -> (map(key) :+ token))
          }
        )
    }

    def extractCommands(tokens: NLogoTokensJ): NLogoTokens =
        tokens.asScala.toList.filter(token => token.tpe == TokenType.Command)

    def wrapInRun(code: String) = s"run [ $code ]"
    def wrapInRunResult(code: String) = s"runresult [ $code ]"

    def buildCodeWith(wrapper: String => String)(tokens: NLogoTokensJ): String = wrapper(buildCode(tokens))

    // BORROWED FROM LEVELSPACE
    def buildCode(tokens: CodeManager.NLogoTokens): String = {

        val sb = new StringBuilder()
        sb.clear()
        tokens.foreach(t =>
            sb.append(t.text).append(' ')
        )
        sb.toString
    }

    def buildCode(tokens: NLogoTokensJ): String = buildCode(tokens.asScala.toList)
}

class Ask extends Command {

    override def perform(args: Array[Argument], context: Context): Unit = {

        val agentset = args(0).getAgentSet
        val codeTokens = args(1).getCode
//        val codeTokens = new java.util.ArrayList[Token]()

        val defaultWrapper = CodeManager.wrapInRun _
        val code = CodeManager.buildCodeWith(defaultWrapper)(codeTokens)
        val bareCode = CodeManager.buildCode(codeTokens)
        val ws = context.workspace.asInstanceOf[nvm.Workspace]
        val extCtx = context.asInstanceOf[ExtensionContext]
        val nvmContext = extCtx.nvmContext

//        val command = args(1).getCommand
//        val cache = ContextManager.extractLets(context.activation.asInstanceOf[nvm.Activation], codeTokens)
//        val nvmCommand = command.asInstanceOf[nvm.Command]

        agentset.agents.forEach(agent => {

            val ctx = ContextManager.getContextForApiAgent(nvmContext, agent, ws)
            val compiledForRun = ws.compileForRun(bareCode, ctx, reporter = false)
            val evaluatedTokens = CodeManager.evalReporters(ws, ctx, codeTokens)

            val code = CodeManager.buildCode(evaluatedTokens)
            val cmpForRun = ws.compileForRun(code, ctx, reporter = false)

            cmpForRun.init(ws)
            compiledForRun.init(ws)
//            val proc = ctx.workspace.compileCommands(code, agent.kind)

//            proc.init(ws)

            cmpForRun.code.foreach(c => c.perform(ctx))
//            compiledForRun.code.foreach(c => c.perform(ctx))
//            proc.code.foreach(c => c.perform(ctx))
//            command.perform(ctx, Array[AnyRef]())
        })
    }

    override def getSyntax: Syntax = Syntax.commandSyntax(
        right = List(Syntax.AgentsetType | Syntax.AgentType, Syntax.CodeBlockType),
        agentClassString = "OTPL",
        blockAgentClassString = Option("?")
    )
}
