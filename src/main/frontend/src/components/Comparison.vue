<template>
  <transition name="slide">
    <div class="modal-back" role="dialog">
      <div class="modal-content" ref="modal">
        <header class="modal-header text-center" id="modalTitle">
          <slot name="header">This is the default title</slot>
          <button type="button" class="close" @click="close">×</button>
        </header>
        <section class="modal-body" id="modalDescription">
          <slot name="body">I'm the default body!</slot>
        </section>
        <footer class="modal-footer">
          <button type="button" class="btn btn-success" @click="close">
            <i class="fas fa-times"></i> Try again
          </button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script>
export default {
  name: "Comparison",
  methods: {
    close() {
      this.$emit("close");
    }
  }
};
</script>

<style lang="scss" scoped>
.modal-back {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 99999;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-content {
  width: 70%;
  box-shadow: 2px 2px 20px 1px rgba(56, 56, 56, 1);
  border-radius: 3px;
  overflow: hidden;
  background-color: white;

  display: flex;
  flex-direction: column;
}

.modal-header,
.modal-footer {
  padding: 15px;
  display: flex;
}

.modal-header {
  border-bottom: 1px solid #eeeeee;
  justify-content: space-between;
  background-color: white;

  display: flex;
}

.modal-footer {
  border-top: 1px solid #eeeeee;
  justify-content: flex-end;

  display: flex;
}

.modal-body {
  position: relative;
  padding: 20px 10px;
}

//animations-transition
.slide-enter {
  opacity: 0;
}

.slide-enter-active {
  opacity: 1;
  animation: slide-in 1s ease-out forwards;
  transition: opacity 0.5s;
}

.slide-leave-to {
  opacity: 1;
}

.slide-leave-active {
  animation: slide-out 1s ease-out forwards;
  opacity: 0;
  transition: opacity 1s;
}

@keyframes slide-in {
  0% {
    transform: translateY(-15px);
  }
  30% {
    transform: translateY(15px);
  }
  100% {
    transform: translateY(0);
  }
}

@keyframes slide-out {
  from {
    transform: translateY(0);
  }
  to {
    transform: translateY(-30px);
  }
}
</style>
