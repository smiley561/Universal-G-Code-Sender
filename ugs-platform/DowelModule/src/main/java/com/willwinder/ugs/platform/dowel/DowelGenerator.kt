/*
    Copyprintln 2017 Will Winder

    This file is part of Universal Gcode Sender (UGS).

    UGS is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UGS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UGS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.willwinder.ugs.platform.dowel

import com.google.common.collect.Iterables
import com.willwinder.universalgcodesender.gcode.util.GcodeUtils
import com.willwinder.universalgcodesender.model.UnitUtils
import java.io.PrintWriter
import java.util.Date
import javax.vecmath.Point3d

/*

  @author wwinder
  Created on Oct 29, 2017
*/

public class DowelGenerator(var settings: DowelSettings) {

  public fun unitMultiplier() = UnitUtils.scaleUnits(settings.units, UnitUtils.Units.MM)

  public fun getLocations(target: UnitUtils.Units): List<Point3d> {
    val mult= UnitUtils.scaleUnits(settings.units, target)
    val offset = mult * (settings.dowelDiameter + settings.bitDiameter * 1.25)
    val corner = Point3d(
        mult * settings.dowelDiameter / 2.0,
        mult * settings.dowelDiameter / 2.0,
        0.0)

    val ret: MutableList<Point3d> = mutableListOf()
    for (x in 0 until settings.numDowelsX) {
      for (y in 0 until settings.numDowelsY) {
        ret.add(Point3d(corner.x + x * offset, corner.y + y * offset, 0.0))
      }
    }

    return ret
  }

  public fun generate(output: PrintWriter) {
    // Set units and absolute movement/IJK mode.
    output.println("(Generated by Universal Gcode Sender ${Date()})")
    output.println("${GcodeUtils.unitCommand(settings.units)} G90 G91.1")
    output.println("G17 F${settings.feed}")
    output.println("M3")

    for (point in getLocations(settings.units)) {
      generateOne(point, output)
    }

    output.println("\n(All done!)")
    output.println("M5")
    output.println("M30")
  }

  public fun generateOne(at: Point3d, output: PrintWriter) {
    output.println("\n(Dowel at x:${at.x} y:${at.y})")

    val radius = settings.bitDiameter / 2.0 + settings.dowelDiameter / 2.0
    val quarterDepth = settings.cutDepth / 4.0

    val arcSequence: Iterator<Point3d> = Iterables.cycle(listOf(
        Point3d(at.x - radius, at.y, 0.0),
        Point3d(at.x, at.y + radius, 0.0),
        Point3d(at.x + radius, at.y, 0.0),
        Point3d(at.x, at.y - radius, 0.0)
    )).iterator()

    var last = arcSequence.next();

    // Start
    output.println("G0 X${last.x} Y${last.y}")
    output.println("G1 Z0")
    output.println("G17 F${settings.feed}")

    // Create the helix
    var currentDepth = 0.0
    while (currentDepth > -settings.dowelLength) {
      val n = arcSequence.next()
      output.println("G02 X${n.x} Y${n.y} Z${currentDepth} I${at.x - last.x} J${at.y - last.y}")
      last = n
      currentDepth -= quarterDepth
    }

    // final loop at final depth
    for (i in 0..4) {
      val n = arcSequence.next()
      output.println("G02 X${n.x} Y${n.y} Z${-settings.dowelLength} I${at.x - last.x} J${at.y - last.y}")
      last = n
    }

    // Lift tool out of pocket.
    output.println("G0 Z${settings.safetyHeight}")
  }
}
