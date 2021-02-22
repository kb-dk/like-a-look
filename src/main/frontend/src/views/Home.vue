<template>
  <div class="home">
    <h1>Photo Booth Face Detection Test</h1>
    <FaceCam />
  </div>
</template>

<script>
// @ is an alias to /src
import FaceCam from "@/components/FaceCam.vue";

export default {
  name: "Home",
  components: {
    FaceCam
  }
};
</script>
