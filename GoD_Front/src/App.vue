<template>
  <div class="viewport">
    <router-view class="mobile-box" />
  </div>
</template>

<style scoped>
body {
  margin: 0px;
}

.viewport {
  margin: 0px;
  padding: 0px;
  width: 100vw;
  height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  justify-content: center;
  align-items: center;
}

.mobile-box {
  width: 420px;
  height: 740px;
  background-color: white;
  border-radius: 24px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  margin: 0px;
  padding: 0px;
}
</style>
