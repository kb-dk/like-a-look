<template>
  <div v-if="matchedFaces">
    <div
      v-for="(face, i) in matchedFaces.slice(0, 3)"
      :key="i"
      class="matchedFace"
    >
      <div class="matchedFaceHeadline">
        {{ face.similarPerson.firstName }} {{ face.similarPerson.lastName }}
      </div>
      <div class="matchedFaceImg">
        <img :src="face.similarImage.mediumURL" class="portrait" />
      </div>
      <div class="metaDataBoxBelowPortrait">
        <div>
          {{ face.similarPerson.occupation }}
        </div>
        <div>
          {{ face.similarPerson.birthday }} - {{ face.similarPerson.deathday }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "Comparison",
  props: {
    matchedFaces: { type: Array, default: () => [] }
  },

  methods: {}
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="scss">
.matchedFaceHeadline {
  height: 47px;
}

.matchedFace {
  float: left;
  margin: 0 20px 0 0;
  width: 256px;
}

.metaDataBoxBelowPortrait {
  margin: 10px 0 0 0;
}

img.portrait {
  display: block;
  max-width: 256px;
  max-height: 256px;
  margin: auto;
}
</style>
