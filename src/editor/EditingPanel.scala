package editor

import java.awt._
import javax.swing.JPanel

import main.CurveDrawer
import utilities.{CubicCurve, MyMath, Vec2}

/**
  * Created by weijiayi on 2/29/16.
  */
class EditingPanel(editor: Editor, var pixelPerUnit: Int = 40, var displayPixelScale: Int = 4)
  extends JPanel with EditorListener {

  var imageOffset = Vec2.zero

  val baselineColor = Color.blue.darker()
  val gridColor = Color.green
  val backgroundColor = Color.white
  val mainStrokeColor = Color.black
  val endpointColor = CurveDrawer.colorWithAlpha(Color.red, 0.75)
  val controlPointColor = CurveDrawer.colorWithAlpha(Color.orange, 0.75)
  val curveHighlightColor = Color.cyan.darker()
  val editThicknessColor = Color.red

  setPreferredSize(new Dimension(windowWidthFromBoard, windowHeightFromBoard))
  setMinimumSize(new Dimension(windowWidthFromBoard, windowHeightFromBoard))
  setBackground(backgroundColor)

  def pointTrans(p: Vec2): Vec2 = {
    val s = pixelPerUnit*displayPixelScale
    Vec2(p.x*s, (p.y + editor.currentEditing().letter.tall)*s) + imageOffset
  }

  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)

    val g2d = g.asInstanceOf[Graphics2D]

    val drawer = new CurveDrawer(g2d, pointTrans, pixelPerUnit)

    drawBoardLines(drawer,2,2)

    editor.currentEditing() match {
      case Editing(letter, selects) =>
        val selectedCurves = selects.map(letter.segs)
        selectedCurves.foreach(c => drawer.drawCurveControlPoints(c, endpointColor, controlPointColor, 0.03))

        editor.mode match{
          case EditControlPoint(pid) =>
            drawEditingLines(drawer)(selectedCurves.map(_.curve), pid, controlPointColor)
          case edt@EditThickness(_) =>
            drawEditingLines(drawer)(selectedCurves.map(_.curve), edt.pid, editThicknessColor)
          case _ => ()
        }

        drawer.drawLetter(letter, mainStrokeColor, curveHighlightColor, selects)
    }

  }

  def boardHeight = MyMath.ceil(editor.currentEditing().letter.height * pixelPerUnit)

  def boardWidth = MyMath.ceil(editor.currentEditing().letter.width * pixelPerUnit)

  def boardBaseLine = MyMath.ceil(editor.currentEditing().letter.tall * pixelPerUnit)

  def windowWidthFromBoard = boardWidth * displayPixelScale

  def windowHeightFromBoard = boardHeight * displayPixelScale

  def displayWidth = boardWidth * displayPixelScale

  def displayHeight = boardHeight * displayPixelScale

  def drawBoardLines(drawer: CurveDrawer, width: Double, height: Double): Unit ={

    drawer.setColor(baselineColor)
    drawer.drawLine(Vec2.zero, Vec2(width,0), 0.05)
    drawer.drawLine(Vec2(0,-width), Vec2(0,width), 0.05)

    drawer.setColor(gridColor)
    import collection.immutable.List
    List(-2,-1,1,2).foreach(i =>{
      drawer.drawLine(Vec2(0, i), Vec2(width,i), 0.03)
    })

    drawer.drawLine(Vec2(1,-height), Vec2(1,height), 0.03)
  }

  val moveSpeed = 1.0
  def dragAction(drag: Vec2, init: Vec2, current: Vec2) = {
    editor.mode match {
      case MoveCamera =>
        dragImage(drag)
      case EditControlPoint(id) =>
        editor.dragControlPoint(id, drag/(pixelPerUnit*displayPixelScale))
      case edt@ EditThickness(isHead) =>
        editor.currentEditing().selectedInkCurves.headOption.foreach{ ink =>
          val targetPoint = pointTrans(ink.curve.getPoint(edt.pid))
          val relative = init - targetPoint
          val dis = math.max(relative.length,0.1)
          val ratio = (drag dot relative.normalized)/dis + 1
          editor.scaleThickness(isHead, ratio)
        }
    }
  }

  def dragFinishAction(): Unit = {
    editor.mode match{
      case MoveCamera => ()
      case _ => editor.recordNow()
    }
  }

  new MouseManager(this, dragAction, dragFinishAction)

  def dragImage(drag: Vec2): Unit ={
    imageOffset += drag
    repaint()
  }


  def drawEditingLines(drawer: CurveDrawer)(curves: Seq[CubicCurve], pid: Int, color: Color): Unit ={
    val width = 0.05
    drawer.setColor(color)
    curves.foreach{ c=>
      val Vec2(x,y) = c.getPoint(pid)
      drawer.drawLine(Vec2(-1,y), Vec2(3,y), width)
      drawer.drawLine(Vec2(x, -3), Vec2(x, 3), width)
    }
  }


  override def editingUpdated() = {
    repaint()
  }

}