<template>
  <div class="home">
    <h1>Daner Face Detection Test</h1>
    <DanerFaceCam />
  </div>
</template>

<script>
// @ is an alias to /src
import DanerFaceCam from "@/components/DanerFaceCam.vue";

export default {
  name: "Home",
  components: {
    DanerFaceCam
  }
};
</script>
