package main

import java.io.File

import org.nlogo.api._
import org.nlogo.core.Program
import org.nlogo.headless.HeadlessWorkspace
import org.nlogo.lite.InterfaceComponent
import org.nlogo.nvm.CompilerFlags
import org.nlogo.parse._
import org.nlogo.prim.etc._show
import org.nlogo.workspace.{Evaluator, OpenModelFromURI}

object Main {

    def main(args: Array[String]): Unit = {

        val filepath = getClass.getClassLoader.getResource("test.nlogo").getPath
        val f = new File(filepath)

        val ws = HeadlessWorkspace.newInstance
        val compiler = new org.nlogo.compile.Compiler(NetLogoLegacyDialect)

        val loadedModel = OpenModelFromURI.readModel(org.nlogo.fileformat.standardLoader(compiler.utilities), f.toURI).get
        val program = Program.fromDialect(NetLogoLegacyDialect).copy(interfaceGlobals = loadedModel.interfaceGlobals)

        val environment = ws.getCompilationEnvironment
        val flags = CompilerFlags()
        val compiled = compiler.compileProgram(loadedModel.code, program, ws.getExtensionManager, environment, flags)

        //        println(s"code: ${loadedModel.code}")
        //        println(s"program ${compiled.program}")
        //        println(compiled.procedures.headOption)
        //
        //        println("Interface globals:")
        //        loadedModel.widgets.foreach(println)
        //        loadedModel.interfaceGlobalCommands.foreach(println)
        //        loadedModel.interfaceGlobals.foreach(println)
        //
        //        println("procedures:")
        compiled.copy(procedures = compiled.procedures.map(x => {
            x.code = new _show() +: x.code
            x
        }))

        ws.dispose

        val frame = new javax.swing.JFrame
        val comp = new InterfaceComponent(frame)

        val tokens = FrontEnd.frontEnd(loadedModel.code)
        val evaluator = new Evaluator(ws)

        wait {
            frame.setSize(1000, 700)
            frame.add(comp)
            frame.setVisible(true)

            comp.open(filepath)
        }

        val workspace = comp.workspace

        println("NetLogo code modification:\n")

//        workspace.procedures = workspace.procedures.map(x => {
//
//            val currentProcedure = x._2.procedureDeclaration.name.name
//            println(currentProcedure)
//            val compiled = workspace.compileCommands("show 10")
//            val owner = new SimpleJobOwner("test", new MersenneTwisterFast)
//
//
//            val moreCode = workspace.compiler.compileMoreCode(s"to $currentProcedure\n set color red \nend", None,
//                workspace.world.program,
//                new ProceduresMap,
//                workspace.getExtensionManager,
//                workspace.getCompilationEnvironment)
//
//            //                val commandsCompiled = workspace.compileCommands("set color red", AgentKind.Turtle)
//            val procedure = x._2
//
//            val newProc = moreCode.procedures.head
//
//            procedure.addChild(newProc)
//            moreCode.procedures.foreach(x => x.init(workspace))
//            procedure.code = procedure.code.head +: (moreCode.head.code ++ procedure.code.tail)
//
//
////            workspace.runCompiledCommands(owner, compiled)
//
//            x
//        })
        comp.command("crt 100")
        comp.command("repeat 50 [ generate-data ]")

        comp.command("show 100")

        println(comp.report("count turtles"))

        println("ending")
    }

    def wait(block: => Unit) {
        java.awt.EventQueue.invokeAndWait(
            () => {
                block
            })
    }
}
