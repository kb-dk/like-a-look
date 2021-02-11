<template>
  <div>
    <p>
      <center><canvas width="640" height="480"></canvas></center>
    </p>
    <button class="" @click.prevent="takeSnapshot">
      Smile!
    </button>
  </div>
</template>

<script>
import { camvas } from "../pico-assets/camvas";
import { pico } from "../pico-assets/pico";
import { getArcBounds } from "../utils/arcBoundsCalc";
import { getPicoImg } from "../utils/imgHelper";
import picoParams from "../pico-assets/picoParams";
import axios from "axios";
export default {
  name: "DanerFaceCam",
  data() {
    return {
      canvasElement: null,
      faceBoundingBox: {},
      cascadeUrl:
        "https://raw.githubusercontent.com/nenadmarkus/pico/c2e81f9d23cc11d1a612fd21e4f9de0921a5d0d9/rnt/cascades/facefinder"
    };
  },
  mounted() {
    //get the drawing context on the canvas
    this.canvasElement = document
      .getElementsByTagName("canvas")[0]
      .getContext("2d");
    this.initilizeFaceDetection();
  },
  methods: {
    initilizeFaceDetection() {
      fetch(this.cascadeUrl).then(response => {
        let facefinder_classify_region = () => {
          return -1.0;
        };
        response.arrayBuffer().then(buffer => {
          var bytes = new Int8Array(buffer);
          facefinder_classify_region = pico.unpack_cascade(bytes);
          console.log("* facefinder loaded");
          this.instantiateCameraHandling(facefinder_classify_region);
        });
      });

      //All is well
      //initialized = true;
    },

    instantiateCameraHandling(facefinder_classify_region) {
      //Camera handling (see https://github.com/cbrandolino/camvas)
      let canvasInstance = new camvas(
        this.canvasElement,
        this.getProcessFunction(facefinder_classify_region)
      );
    },

    getProcessFunction(facefinder_classify_region) {
      let update_memory = pico.instantiate_detection_memory(5); // we will use the detecions of the last 5 frames
      //this function is called each time a video frame becomes available
      return (video, dt) => {
        // render the video frame to the canvas element and extract RGBA pixel data
        this.canvasElement.drawImage(video, 0, 0);
        let rgba = this.canvasElement.getImageData(0, 0, 640, 480).data;
        // run the cascade over the frame and cluster the obtained detections
        // dets is an array that contains (r, c, s, q) quadruplets
        // (representing row, column, scale and detection score)
        let dets = pico.run_cascade(
          getPicoImg(rgba),
          facefinder_classify_region,
          picoParams
        );
        dets = update_memory(dets);
        dets = pico.cluster_detections(dets, 0.2); // set IoU threshold to 0.2
        this.drawDetections(dets);
      };
    },

    drawDetections(dets) {
      for (let i = 0; i < dets.length; ++i)
        // check the detection score
        // if it's above the threshold, draw it
        // (the constant 50.0 is empirical: other cascades might require a different one)
        if (dets[i][3] > 50.0) {
          //
          this.canvasElement.beginPath();
          let bb = getArcBounds(
            dets[i][1],
            dets[i][0],
            dets[i][2] / 1.5,
            0,
            2 * Math.PI
          );

          this.faceBoundingBox = bb;
          this.canvasElement.rect(bb.x, bb.y, bb.width, bb.height);
          this.canvasElement.lineWidth = 3;
          this.canvasElement.strokeStyle = "red";
          this.canvasElement.stroke();
        }
    },

    takeSnapshot() {
      console.log("snap!");
      console.log(this.faceBoundingBox);

      // get image data
      let ImageData = this.canvasElement.getImageData(
        this.faceBoundingBox.x + 5,
        this.faceBoundingBox.y + 5,
        this.faceBoundingBox.width - 5,
        this.faceBoundingBox.height - 7
      );
      console.log(ImageData);

      // create image element
      let faceCutOut = new Image();
      faceCutOut.src = this.getImageURL(
        ImageData,
        this.faceBoundingBox.width,
        this.faceBoundingBox.height
      );

      // append image element to body
      document.body.appendChild(faceCutOut);
    },

    getImageURL(imgData, width, height) {
      var canvas = document.createElement("canvas");
      var ctx = canvas.getContext("2d");
      canvas.width = width;
      canvas.height = height;
      ctx.putImageData(imgData, 0, 0);
      this.postImg(imgData, width, height);
      return canvas.toDataURL(); //image URL
    },

    postImg(imgData, width, height) {
      var canvas = document.createElement("canvas");
      var ctx = canvas.getContext("2d");
      canvas.width = width;
      canvas.height = height;
      ctx.putImageData(imgData, 0, 0);
      canvas.toBlob(function(blob) {
        const formData = new FormData();
        formData.append("image", blob, "face_" + new Date().getTime());
        console.log(formData);
        axios.post("/api/upload/", formData);
      });
    }

    /*  canvas.toBlob(function(blob) {
  const formData = new FormData();
  formData.append('my-file', blob, 'filename.png');

  // Post via axios or other transport method
  axios.post('/api/upload', formData);
});*/
  }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="scss">
h3 {
  margin: 40px 0 0;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: inline-block;
  margin: 0 10px;
}
a {
  color: #42b983;
}
</style>
