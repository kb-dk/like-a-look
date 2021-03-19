<template>
  <div>
    <error-msg v-if="error" />
    <comparison v-show="showConfirmation" @close="closeConfirmation">
      <h4 slot="header" class="modal-title w-100">Look alike?</h4>
      <div slot="body">
        <div class="selfieSnap">
          <div class="selfieSnapHeadline">Your pretty face</div>
          <div class="selfieSnapImgBox">
            <img :src="selfieImgSrc" class="selfieSnapImg" />
          </div>
        </div>
        <div class="lookALikeSnap">
          <div class="lookAlikesHeadline">
            Your look alikes from our collection
          </div>
          <matched-faces :matchedFaces="lookLikeData" />
        </div>
      </div>
    </comparison>
    <p>
      <center><canvas width="640" height="480"></canvas></center>
    </p>



    <take-photo-btn @take-snapshot="takeSnapshot" :availableCollections="availableCollections" @set-collection="setCollection" v-if="camInitialized"/>
    <span>Samling: {{currentCollection}}</span>
  </div>
</template>

<script>
import { camvas } from "../pico-assets/camvas";
import { pico } from "../pico-assets/pico";
import { getArcBounds } from "../utils/arcBoundsCalc";
import { getPicoImg } from "../utils/imgHelper";
import picoParams from "../pico-assets/picoParams";
import { lookLikeService } from "../api/looklike-service";
import Comparison from "./Comparison.vue";
import MatchedFaces from "./MatchedFaced.vue";
import ErrorMsg from "./ErrorMsg.vue";
import TakePhotoBtn from './TakePhotoBtn.vue';

export default {
  name: "FaceCam",
  components: {
    Comparison,
    MatchedFaces,
    ErrorMsg,
    TakePhotoBtn 
  },
  data() {
    return {
      canvasElement: null,
      error: false,
      faceBoundingBox: {},
      showConfirmation: false,
      selfieImgSrc: "",
      lookLikeImgSrc: "",
      lookLikeData: null,
      camInitialized: false,
      availableCollections:[],
      currentCollection: '',
      borderCompensate: 7, //compensate for red border
      cascadeUrl:
        "https://raw.githubusercontent.com/nenadmarkus/pico/c2e81f9d23cc11d1a612fd21e4f9de0921a5d0d9/rnt/cascades/facefinder"
    };
  },

  mounted() {
    //Get the available collections to search in
    this.getCollections();

    //get the drawing context on the canvas
    this.canvasElement = document
      .getElementsByTagName("canvas")[0]
      .getContext("2d");
    this.initilizeFaceDetection();
    
  },
  methods: {
    closeConfirmation() {
      this.showConfirmation = false;
    },

    initilizeFaceDetection() {
      fetch(this.cascadeUrl).then(response => {
        let facefinder_classify_region = () => {
          return -1.0;
        };
        response.arrayBuffer().then(buffer => {
          var bytes = new Int8Array(buffer);
          facefinder_classify_region = pico.unpack_cascade(bytes);
          this.instantiateCameraHandling(facefinder_classify_region);
          this.camInitialized = true;
        });
      });
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
          let bb = getArcBounds(
            dets[i][1],
            dets[i][0],
            dets[i][2] / 1.5,
            0,
            2 * Math.PI
          );
          this.drawRect(bb);
        }
    },

    drawRect(bb) {
      this.canvasElement.beginPath();
      this.faceBoundingBox = bb;
      this.canvasElement.rect(bb.x, bb.y, bb.width, bb.height);
      this.canvasElement.lineWidth = 3;
      this.canvasElement.strokeStyle = "red";
      this.canvasElement.stroke();
    },

    takeSnapshot() {
      this.selfieImgSrc = "";
      // get image data
      let imgData = this.getImageFromCanvas();
      let snapshotOnCanvas = this.getSnapshotOnCanvas(imgData);
      this.selfieImgSrc = this.getImageURL(snapshotOnCanvas);
      this.getSimilarFaces(snapshotOnCanvas);
    },

    getSnapshotOnCanvas(imgData) {
      var canvas = document.createElement("canvas");
      var ctx = canvas.getContext("2d");
      canvas.width = this.faceBoundingBox.width - this.borderCompensate;
      canvas.height = this.faceBoundingBox.width - this.borderCompensate;
      ctx.putImageData(imgData, 0, 0);
      return canvas;
    },

    getImageFromCanvas() {
      return this.canvasElement.getImageData(
        this.faceBoundingBox.x + 4, //compensate for red rect
        this.faceBoundingBox.y + 4, //compensate for red rect
        this.faceBoundingBox.width,
        this.faceBoundingBox.height
      );
    },

    getImageURL(canvas) {
      return canvas.toDataURL(); //image URL
    },

    getSimilarFaces(canvas) {
      this.error = false;
      this.lookLikeData = [];
      canvas.toBlob(blob => {
        const faceData = new FormData();
        faceData.append("image", blob, "face_" + new Date().getTime());
        lookLikeService
          .getLookALike(faceData, this.collection)
          .then(faces => {
            this.showConfirmation = true;
            this.lookLikeData = faces;
          })
          .catch(error => {
            this.error = true;
            return Promise.reject(error);
          });
      });
    },

    setCollection(type) {
      this.currentCollection = type
    },

    getCollections(){
      lookLikeService
          .getCollections()
          .then(collections => {
           this.availableCollections = collections;
           this.currentCollection = collections[0].id;
          })
          .catch(error => {
            this.error = true;
            return Promise.reject(error);
          });
    }
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

.lookAlikesHeadline {
  margin: 0 0 10px 0;
  font-weight: bold;
}

.selfieSnapHeadline {
  margin: 0 0 34px 0;
  font-weight: bold;
}

.selfieSnapImgBox {
  margin-top: 54px;
  width: 256px;
  height: 256px;
  margin-right: 30px;
}
.selfieSnapImg {
  --x: 50%;
  --y: 50%;
  width: 256px;
  height: 256px;
  border: 4px solid transparent;
  background: linear-gradient(#000, #000) padding-box,
    radial-gradient(farthest-corner at var(--x) var(--y), #5b00c9, #5ec298)
      border-box;
}
</style>
