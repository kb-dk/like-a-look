/**
 * Find the bounding box of your arc and then calculate
 * the width and height from the bounding box.
 *
 * @param {Number} cx //arc x
 * @param {Number} cy //arc y
 * @param {Number} radious
 * @param {Number} startAngle
 * @param {Number} endAngle
 */
export function getArcBounds(cx, cy, radius, startAngle, endAngle) {
  let minX = 1000000;
  let minY = 1000000;
  let maxX = -1000000;
  let maxY = -1000000;

  let possibleBoundingPoints = [];
  // centerpoint
  possibleBoundingPoints.push({ x: cx, y: cy });
  // starting angle
  possibleBoundingPoints.push(arcpoint(cx, cy, radius, startAngle));
  // ending angle
  possibleBoundingPoints.push(arcpoint(cx, cy, radius, endAngle));
  // 0 radians
  if (0 >= startAngle && 0 <= endAngle) {
    possibleBoundingPoints.push(arcpoint(cx, cy, radius, 0));
  }
  // PI/2 radians
  let angle = Math.PI / 2;
  if (angle >= startAngle && angle <= endAngle) {
    possibleBoundingPoints.push(arcpoint(cx, cy, radius, angle));
  }
  // PI radians
  angle = Math.PI;
  if (angle >= startAngle && angle <= endAngle) {
    possibleBoundingPoints.push(arcpoint(cx, cy, radius, angle));
  }
  // PI*3/2 radians
  angle = (Math.PI * 3) / 2;
  if (angle >= startAngle && angle <= endAngle) {
    possibleBoundingPoints.push(arcpoint(cx, cy, radius, angle));
  }

  for (let i = 0; i < possibleBoundingPoints.length; i++) {
    let pt = possibleBoundingPoints[i];
    if (pt.x < minX) {
      minX = pt.x;
    }
    if (pt.y < minY) {
      minY = pt.y;
    }
    if (pt.x > maxX) {
      maxX = pt.x;
    }
    if (pt.y > maxY) {
      maxY = pt.y;
    }
  }

  return { x: minX, y: minY, width: maxX - minX, height: maxY - minY };
}

function arcpoint(cx, cy, radius, angle) {
  let x = cx + radius * Math.cos(angle);
  let y = cy + radius * Math.sin(angle);
  return { x: x, y: y };
}
