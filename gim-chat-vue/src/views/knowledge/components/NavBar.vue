<template>
  <div class="navbar">
    <!-- 左侧 Logo -->
    <div class="logo">
      <i class="el-icon-notebook-2 logo-icon"></i>
      <span class="logo-text">AI知识库</span>
    </div>

    <!-- 右侧用户信息 -->
    <div class="user">
      <el-badge is-dot class="notify">
        <i class="el-icon-bell"></i>
      </el-badge>
      <el-avatar
        size="small"
        style="margin-left: 16px; background-color: #409EFF"
      >
        张
      </el-avatar>
      <span class="username">张小明</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NavBar',
  data() {
    return {
      activeMenu: 'knowledge'
    }
  }
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px;
  background-color: #fff;
  border-bottom: 1px solid #eee;
}
.logo {
  display: flex;
  align-items: center;
}
.logo-icon {
  font-size: 20px;
  color: #409EFF;
}
.logo-text {
  font-size: 18px;
  font-weight: bold;
  margin-left: 8px;
}
.menu {
  flex: 1;
  margin-left: 40px;
}
.user {
  display: flex;
  align-items: center;
}
.notify {
  cursor: pointer;
  font-size: 18px;
  color: #666;
}
.username {
  margin-left: 8px;
  font-size: 14px;
  color: #333;
}
</style>
