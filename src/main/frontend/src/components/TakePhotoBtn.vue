<template>
  <div class="btn-group dropright mr-2" >
  <button type="button" class="btn btn-success" @click.prevent="takeSnapshot">Take photo</button>
  <button type="button" class="btn btn-success dropdown-toggle dropdown-toggle-split" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" @click.prevent="toggleCollectionDD">
    <span class="sr-only">Choose collection</span>
  </button>
  <div class="dropdown-menu d-block" v-if="collectionDropdownVisible">
    <div class="ml-2 customTextColor">Choose Collection</div>
    <div class="dropdown-divider"></div>
    <span v-for="(collection, i) in availableCollections" :key="i">
    <a class="dropdown-item" :title="collection.description" href="#" @click.prevent="setCollection(collection.id)">{{collection.id}}</a>
    </span>
  </div>
</div>
</template>

<script>
export default {
  name: "TakePhotoBtn",
  props: {
    availableCollections: { type: Array, default: () => [] }
  },

 data() {
    return {
      collectionDropdownVisible: false,
      coellections:[]
    }
 },
  methods: {
    takeSnapshot() {
      this.$emit("take-snapshot");
    },

    toggleCollectionDD() {
      this.collectionDropdownVisible= !this.collectionDropdownVisible;
    },

    setCollection(type) {
      this.$emit("set-collection", type)
      this.toggleCollectionDD()
    }
     
  }
};
</script>

<style scoped lang="scss">
.customTextColor {
  color: #42b983;
  font-weight:bold;
}

</style>