/**
 * Pico image
 *
 * @param {Object} rgba
 *
 */
export function getPicoImg(rgba) {
  return {
    pixels: rgba_to_grayscale(rgba, 480, 640),
    nrows: 480,
    ncols: 640,
    ldim: 640
  };
}

/**
 * function to transform an RGBA image to grayscale
 *
 * @param {Object} rgba
 * @param {Number} nrows
 * @param {Number} ncols
 */
function rgba_to_grayscale(rgba, nrows, ncols) {
  var gray = new Uint8Array(nrows * ncols);
  for (var r = 0; r < nrows; ++r)
    for (var c = 0; c < ncols; ++c)
      gray[r * ncols + c] =
        (2 * rgba[r * 4 * ncols + 4 * c + 0] +
          7 * rgba[r * 4 * ncols + 4 * c + 1] +
          1 * rgba[r * 4 * ncols + 4 * c + 2]) /
        10;
  return gray;
}
